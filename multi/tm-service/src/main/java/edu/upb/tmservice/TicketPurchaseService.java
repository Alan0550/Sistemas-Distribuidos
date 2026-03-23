package edu.upb.tmservice;

import edu.upb.tmservice.dao.EventoDao;
import edu.upb.tmservice.dao.EventoEntity;
import edu.upb.tmservice.dao.PurchaseLookup;
import edu.upb.tmservice.dao.TicketDao;
import edu.upb.tmservice.dao.TipoTicketDao;
import edu.upb.tmservice.dao.TipoTicketEntity;
import edu.upb.tmservice.dao.UsuarioDao;
import edu.upb.tmservice.dao.UsuarioEntity;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;

public class TicketPurchaseService {
    private static final BigDecimal TEN_PERCENT = new BigDecimal("10.00");
    private final UsuarioDao usuarioDao = new UsuarioDao();
    private final EventoDao eventoDao = new EventoDao();
    private final TipoTicketDao tipoTicketDao = new TipoTicketDao();
    private final TicketDao ticketDao = new TicketDao();
    private final UserManagementService userManagementService = new UserManagementService();

    public PurchaseResult processPurchase(long userId, long eventId, long tipoTicketId, int cantidad, String baseKey)
            throws Exception {
        if (userId <= 0 || eventId <= 0 || tipoTicketId <= 0) {
            throw new IllegalArgumentException("Usuario, evento o tipo de ticket invalido");
        }
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
        }

        String normalizedKey = normalizeBaseKey(baseKey);
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                PurchaseLookup existing = ticketDao.findExistingPurchase(conn, normalizedKey);
                if (existing != null) {
                    conn.commit();
                    return new PurchaseResult(
                            existing.getFirstTicketId(),
                            existing.getTotal(),
                            "Compra ya procesada previamente");
                }

                UsuarioEntity usuario = usuarioDao.findById(conn, userId);
                EventoEntity evento = eventoDao.findById(conn, eventId);
                if (usuario == null || evento == null) {
                    throw new IllegalArgumentException("Usuario o evento no existe");
                }
                if (usuario.isBaneado()) {
                    throw new IllegalArgumentException("Tu cuenta fue baneada");
                }
                if (evento.getFecha() == null || !evento.getFecha().toInstant().isAfter(Instant.now())) {
                    throw new IllegalArgumentException("El evento ya no se encuentra vigente");
                }

                String rol = usuario.getRol();
                if (!isBuyerRole(rol)) {
                    throw new IllegalArgumentException("Solo CLIENTE, FRECUENTE y VIP pueden comprar tickets");
                }

                TipoTicketEntity tipoTicket = tipoTicketDao.lockByIdAndEvent(conn, tipoTicketId, eventId);
                if (tipoTicket == null) {
                    throw new IllegalArgumentException("Tipo de ticket no valido para el evento");
                }
                if (tipoTicket.getCantidad() < cantidad) {
                    throw new IllegalArgumentException("No hay suficiente disponibilidad");
                }

                int firstSequence = tipoTicket.getProximoAsiento();
                tipoTicketDao.reserveInventoryAndAdvanceSequence(conn, tipoTicketId, cantidad);

                long firstTicketId = 0;
                BigDecimal discountPercent = calculateDiscountPercent(rol, evento.isDescuentoFrecuente());
                BigDecimal finalPrice = applyDiscount(tipoTicket.getPrecio(), discountPercent);

                for (int i = 0; i < cantidad; i++) {
                    String seat = buildSeatNumber(tipoTicket.getTipoAsiento(), firstSequence + i);
                    String requestKey = normalizedKey + "#" + (i + 1);
                    long currentTicketId = ticketDao.insertTicket(
                            conn,
                            eventId,
                            userId,
                            seat,
                            finalPrice,
                            tipoTicket.getPrecio(),
                            discountPercent,
                            requestKey,
                            tipoTicketId);
                    if (firstTicketId == 0) {
                        firstTicketId = currentTicketId;
                    }
                }

                userManagementService.refreshAutomaticRole(conn, userId, rol);

                conn.commit();
                return new PurchaseResult(firstTicketId, cantidad, "Compra registrada correctamente");
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof IllegalArgumentException) {
                    throw e;
                }
                throw new SQLException("No se pudo guardar la compra", e);
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private String buildSeatNumber(String seatType, int sequence) {
        String prefix = seatType == null || seatType.trim().isEmpty() ? "ASIENTO" : seatType.trim().toUpperCase();
        return prefix + "-" + sequence;
    }

    private String normalizeBaseKey(String baseKey) {
        String sanitized = baseKey == null ? "" : baseKey.trim();
        if (sanitized.isEmpty()) {
            throw new IllegalArgumentException("idempotency_key es obligatorio");
        }
        if (sanitized.contains("#")) {
            throw new IllegalArgumentException("idempotency_key no puede contener '#'");
        }
        if (sanitized.length() > 100) {
            throw new IllegalArgumentException("idempotency_key es demasiado largo");
        }
        return sanitized;
    }

    private boolean isBuyerRole(String role) {
        return "CLIENTE".equalsIgnoreCase(role)
                || "FRECUENTE".equalsIgnoreCase(role)
                || "VIP".equalsIgnoreCase(role);
    }

    private BigDecimal calculateDiscountPercent(String role, boolean descuentoFrecuente) {
        if ("VIP".equalsIgnoreCase(role)) {
            return TEN_PERCENT;
        }
        if ("FRECUENTE".equalsIgnoreCase(role) && descuentoFrecuente) {
            return TEN_PERCENT;
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal applyDiscount(BigDecimal basePrice, BigDecimal discountPercent) {
        if (discountPercent.compareTo(BigDecimal.ZERO) <= 0) {
            return basePrice;
        }
        return basePrice.multiply(BigDecimal.valueOf(100).subtract(discountPercent))
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
    }

    public static class PurchaseResult {
        private final long firstTicketId;
        private final int ticketsCreated;
        private final String message;

        public PurchaseResult(long firstTicketId, int ticketsCreated, String message) {
            this.firstTicketId = firstTicketId;
            this.ticketsCreated = ticketsCreated;
            this.message = message;
        }

        public long getFirstTicketId() {
            return firstTicketId;
        }

        public int getTicketsCreated() {
            return ticketsCreated;
        }

        public String getMessage() {
            return message;
        }
    }
}
