package edu.upb.tmservice;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class TicketGrpcServer {
    private static final Logger log = LoggerFactory.getLogger(TicketGrpcServer.class);

    private final int port;
    private Server server;

    public TicketGrpcServer(int port) {
        this.port = port;
    }

    public void start() throws IOException {
        server = ServerBuilder.forPort(port)
                .addService(new TicketPurchaseServiceImpl())
                .addService(ProtoReflectionService.newInstance())
                .build()
                .start();
        log.info("gRPC ticket service on port {}", port);
    }

    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }
}
