package edu.upb.tmservice;

import edu.upb.tmservice.dao.EventoDao;
import edu.upb.tmservice.dao.PurchaseLookup;
import edu.upb.tmservice.dao.TicketDao;
import edu.upb.tmservice.dao.TipoTicketDao;
import edu.upb.tmservice.dao.TipoTicketEntity;
import edu.upb.tmservice.dao.UsuarioDao;

import java.sql.Connection;
import java.sql.SQLException;

public class TicketPurchaseService {
    private final UsuarioDao usuarioDao = new UsuarioDao();
    private final EventoDao eventoDao = new EventoDao();
    private final TipoTicketDao tipoTicketDao = new TipoTicketDao();
    private final TicketDao ticketDao = new TicketDao();

    public PurchaseResult processPurchase(long userId, long eventId, long tipoTicketId, int cantidad, String baseKey)
            throws Exception {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                PurchaseLookup existing = ticketDao.findExistingPurchase(conn, baseKey);
                if (existing != null) {
                    conn.commit();
                    return new PurchaseResult(
                            existing.getFirstTicketId(),
                            existing.getTotal(),
                            "Compra ya procesada previamente");
                }

                if (!usuarioDao.existsById(conn, userId) || !eventoDao.existsById(conn, eventId)) {
                    throw new IllegalArgumentException("Usuario o evento no existe");
                }
                String rol = usuarioDao.findRoleById(conn, userId);
                if (!"CLIENTE".equalsIgnoreCase(rol)) {
                    throw new IllegalArgumentException("Solo los usuarios CLIENTE pueden comprar tickets");
                }

                TipoTicketEntity tipoTicket = tipoTicketDao.lockByIdAndEvent(conn, tipoTicketId, eventId);
                if (tipoTicket == null) {
                    throw new IllegalArgumentException("Tipo de ticket no valido para el evento");
                }
                if (tipoTicket.getCantidad() < cantidad) {
                    throw new IllegalArgumentException("No hay suficiente disponibilidad");
                }

                int soldCount = tipoTicketDao.countSoldTickets(conn, tipoTicketId);
                long firstTicketId = 0;

                for (int i = 1; i <= cantidad; i++) {
                    String seat = buildSeatNumber(tipoTicket.getTipoAsiento(), soldCount + i);
                    String requestKey = baseKey + "#" + i;
                    long currentTicketId = ticketDao.insertTicket(
                            conn,
                            eventId,
                            userId,
                            seat,
                            tipoTicket.getPrecio(),
                            requestKey,
                            tipoTicketId);
                    if (firstTicketId == 0) {
                        firstTicketId = currentTicketId;
                    }
                }

                tipoTicketDao.decreaseAvailable(conn, tipoTicketId, cantidad);

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
