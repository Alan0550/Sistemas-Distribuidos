package edu.upb.tickmaster.grpc;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;

import java.io.IOException;

@Deprecated
public class ProtoServer {
    private final int port;

    public ProtoServer() {
        this.port = Integer.parseInt(System.getenv().getOrDefault("LEGACY_PROTO_PORT", "8081"));
    }

    public void start() throws IOException, InterruptedException {
        Server server = ServerBuilder.forPort(port)
                .addService((BindableService) new ProductoServiceImpl())
                .addService(ProtoReflectionService.newInstance())
                .build();

        System.out.println("[legacy] Iniciando servidor gRPC en el puerto " + port + "...");
        server.start();
        System.out.println("[legacy] Servidor gRPC legacy escuchando...");
        server.awaitTermination();
    }
}
