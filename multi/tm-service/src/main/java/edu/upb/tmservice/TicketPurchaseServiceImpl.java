package edu.upb.tmservice;

import edu.upb.tmservice.grpc.CompraTicketRequest;
import edu.upb.tmservice.grpc.CompraTicketResponse;
import edu.upb.tmservice.grpc.TicketPurchaseServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class TicketPurchaseServiceImpl extends TicketPurchaseServiceGrpc.TicketPurchaseServiceImplBase {
    private static final Logger log = LoggerFactory.getLogger(TicketPurchaseServiceImpl.class);

    @Override
    public void comprarTicket(CompraTicketRequest request, StreamObserver<CompraTicketResponse> responseObserver) {
        long userId = request.getIdUsuario();
        long eventId = request.getIdEvento();
        long tipoTicketId = request.getIdTipoTicket();
        int cantidad = request.getCantidad();
        String idempotencyKey = request.getIdempotencyKey().trim();

        if (userId <= 0 || eventId <= 0 || tipoTicketId <= 0 || cantidad <= 0 || idempotencyKey.isEmpty()) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("id_usuario, id_evento, id_tipo_ticket, cantidad e idempotency_key deben ser validos")
                    .asRuntimeException());
            return;
        }

        try {
            PurchaseResult result = savePurchase(userId, eventId, tipoTicketId, cantidad, idempotencyKey);
            CompraTicketResponse response = CompraTicketResponse.newBuilder()
                    .setPrimerTicketId(result.firstTicketId)
                    .setTicketsCreados(result.ticketsCreated)
                    .setStatus("OK")
                    .setMensaje(result.message)
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
            log.info("Compra gRPC procesada (ticketInicial={}, usuario={}, evento={}, tipo={}, cantidad={})",
                    result.firstTicketId, userId, eventId, tipoTicketId, result.ticketsCreated);
        } catch (IllegalArgumentException e) {
            log.warn("Compra rechazada: {}", e.getMessage());
            responseObserver.onError(Status.FAILED_PRECONDITION
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        } catch (Exception e) {
            log.error("Error registrando compra gRPC", e);
            responseObserver.onError(Status.INTERNAL
                    .withDescription("No se pudo registrar la compra")
                    .asRuntimeException());
        }
    }

    private PurchaseResult savePurchase(long userId, long eventId, long tipoTicketId, int cantidad, String baseKey)
            throws Exception {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                PurchaseResult existing = findExistingPurchase(conn, baseKey);
                if (existing != null) {
                    conn.commit();
                    return existing;
                }

                if (!existsById(conn, "usuarios", userId) || !existsById(conn, "eventos", eventId)) {
                    throw new IllegalArgumentException("Usuario o evento no existe");
                }

                TipoTicketData tipoTicket = lockTipoTicket(conn, tipoTicketId, eventId);
                if (tipoTicket == null) {
                    throw new IllegalArgumentException("Tipo de ticket no valido para el evento");
                }
                if (tipoTicket.available < cantidad) {
                    throw new IllegalArgumentException("No hay suficiente disponibilidad");
                }

                int soldCount = countSoldTickets(conn, tipoTicketId);
                long firstTicketId = 0;

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO tickets (id_evento, id_usuario, nro_asiento, precio, idempotency_key, id_tipo_ticket) " +
                                "VALUES (?,?,?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    for (int i = 1; i <= cantidad; i++) {
                        String seat = buildSeatNumber(tipoTicket.seatType, soldCount + i);
                        String requestKey = baseKey + "#" + i;
                        ps.setLong(1, eventId);
                        ps.setLong(2, userId);
                        ps.setString(3, seat);
                        ps.setBigDecimal(4, tipoTicket.price);
                        ps.setString(5, requestKey);
                        ps.setLong(6, tipoTicketId);
                        ps.executeUpdate();

                        try (ResultSet keys = ps.getGeneratedKeys()) {
                            if (keys.next() && firstTicketId == 0) {
                                firstTicketId = keys.getLong(1);
                            }
                        }
                    }
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE tipo_ticket SET cantidad = cantidad - ? WHERE id = ?")) {
                    ps.setInt(1, cantidad);
                    ps.setLong(2, tipoTicketId);
                    ps.executeUpdate();
                }

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

    private PurchaseResult findExistingPurchase(Connection conn, String baseKey) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT MIN(id) AS first_ticket_id, COUNT(*) AS total " +
                        "FROM tickets WHERE idempotency_key LIKE ?")) {
            ps.setString(1, baseKey + "#%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt("total") > 0) {
                    return new PurchaseResult(
                            rs.getLong("first_ticket_id"),
                            rs.getInt("total"),
                            "Compra ya procesada previamente");
                }
            }
        }
        return null;
    }

    private boolean existsById(Connection conn, String table, long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM " + table + " WHERE id = ? LIMIT 1")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private TipoTicketData lockTipoTicket(Connection conn, long tipoTicketId, long eventId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT tipo_asiento, cantidad, precio FROM tipo_ticket WHERE id = ? AND id_evento = ? FOR UPDATE")) {
            ps.setLong(1, tipoTicketId);
            ps.setLong(2, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new TipoTicketData(
                            rs.getString("tipo_asiento"),
                            rs.getInt("cantidad"),
                            rs.getBigDecimal("precio"));
                }
            }
        }
        return null;
    }

    private int countSoldTickets(Connection conn, long tipoTicketId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM tickets WHERE id_tipo_ticket = ?")) {
            ps.setLong(1, tipoTicketId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    private String buildSeatNumber(String seatType, int sequence) {
        String prefix = seatType == null || seatType.trim().isEmpty() ? "ASIENTO" : seatType.trim().toUpperCase();
        return prefix + "-" + sequence;
    }

    private static class TipoTicketData {
        private final String seatType;
        private final int available;
        private final java.math.BigDecimal price;

        private TipoTicketData(String seatType, int available, java.math.BigDecimal price) {
            this.seatType = seatType;
            this.available = available;
            this.price = price;
        }
    }

    private static class PurchaseResult {
        private final long firstTicketId;
        private final int ticketsCreated;
        private final String message;

        private PurchaseResult(long firstTicketId, int ticketsCreated, String message) {
            this.firstTicketId = firstTicketId;
            this.ticketsCreated = ticketsCreated;
            this.message = message;
        }
    }
}
