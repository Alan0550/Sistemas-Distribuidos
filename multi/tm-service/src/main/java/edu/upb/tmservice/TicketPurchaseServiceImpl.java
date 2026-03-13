package edu.upb.tmservice;

import edu.upb.tmservice.grpc.CompraTicketRequest;
import edu.upb.tmservice.grpc.CompraTicketResponse;
import edu.upb.tmservice.grpc.TicketPurchaseServiceGrpc;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TicketPurchaseServiceImpl extends TicketPurchaseServiceGrpc.TicketPurchaseServiceImplBase {
    private static final Logger log = LoggerFactory.getLogger(TicketPurchaseServiceImpl.class);
    private final TicketPurchaseService purchaseService = new TicketPurchaseService();

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
            TicketPurchaseService.PurchaseResult result = purchaseService.processPurchase(
                    userId, eventId, tipoTicketId, cantidad, idempotencyKey);
            CompraTicketResponse response = CompraTicketResponse.newBuilder()
                    .setPrimerTicketId(result.getFirstTicketId())
                    .setTicketsCreados(result.getTicketsCreated())
                    .setStatus("OK")
                    .setMensaje(result.getMessage())
                    .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();
            log.info("Compra gRPC procesada (ticketInicial={}, usuario={}, evento={}, tipo={}, cantidad={})",
                    result.getFirstTicketId(), userId, eventId, tipoTicketId, result.getTicketsCreated());
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
}
