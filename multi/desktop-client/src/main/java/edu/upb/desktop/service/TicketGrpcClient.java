package edu.upb.desktop.service;

import edu.upb.desktop.model.PurchaseResultModel;
import edu.upb.tmservice.grpc.CompraTicketRequest;
import edu.upb.tmservice.grpc.CompraTicketResponse;
import edu.upb.tmservice.grpc.TicketPurchaseServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class TicketGrpcClient {

    public PurchaseResultModel buyTickets(long userId, long eventId, long ticketTypeId, int cantidad) {
        String host = System.getenv().getOrDefault("TM_GRPC_HOST", "localhost");
        int port = Integer.parseInt(System.getenv().getOrDefault("TM_GRPC_TARGET_PORT", "9201"));
        String idempotencyKey = "UI-" + userId + "-" + eventId + "-" + ticketTypeId + "-" + System.currentTimeMillis();

        ManagedChannel channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();

        try {
            TicketPurchaseServiceGrpc.TicketPurchaseServiceBlockingStub stub =
                    TicketPurchaseServiceGrpc.newBlockingStub(channel);

            CompraTicketRequest request = CompraTicketRequest.newBuilder()
                    .setIdUsuario(userId)
                    .setIdEvento(eventId)
                    .setIdTipoTicket(ticketTypeId)
                    .setCantidad(cantidad)
                    .setIdempotencyKey(idempotencyKey)
                    .build();

            CompraTicketResponse response = stub.comprarTicket(request);
            return new PurchaseResultModel(
                    response.getPrimerTicketId(),
                    response.getTicketsCreados(),
                    response.getStatus(),
                    response.getMensaje());
        } finally {
            channel.shutdownNow();
        }
    }
}
