package edu.upb.tmservice;

import edu.upb.tmservice.dao.EventoDao;
import edu.upb.tmservice.dao.PurchaseLookup;
import edu.upb.tmservice.dao.TicketDao;
import edu.upb.tmservice.dao.TipoTicketDao;
import edu.upb.tmservice.dao.TipoTicketEntity;
import edu.upb.tmservice.dao.UsuarioDao;
import edu.upb.tmservice.grpc.CompraTicketRequest;
import edu.upb.tmservice.grpc.CompraTicketResponse;
import edu.upb.tmservice.grpc.TicketPurchaseServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;

public class TicketPurchaseServiceImpl extends TicketPurchaseServiceGrpc.TicketPurchaseServiceImplBase {
    private static final Logger log = LoggerFactory.getLogger(TicketPurchaseServiceImpl.class);
    private final UsuarioDao usuarioDao = new UsuarioDao();
    private final EventoDao eventoDao = new EventoDao();
    private final TipoTicketDao tipoTicketDao = new TipoTicketDao();
    private final TicketDao ticketDao = new TicketDao();

    @Override
    public void comprarTicket(CompraTicketRequest request, StreamObserver<CompraTicketResponse> responseObserver) {
        long userId = request.getIdUsuario();
        long eventId = request.getIdEvento();
        long tipoTicketId = request.getIdTipoTicket();
        int cantidad = request.getCantidad();
        String idempotencyKey = request.getIdempotencyKey().trim();

        if (userId <= 0 || eventId <= 0 || tipoTicketId <= 0 || cantidad <= 0 || idempotencyKey.isEmpty()) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription(
                            "id_usuario, id_evento, id_tipo_ticket, cantidad e idempotency_key deben ser validos")
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
