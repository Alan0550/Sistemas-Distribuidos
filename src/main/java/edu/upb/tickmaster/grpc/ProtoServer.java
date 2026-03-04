package edu.upb.tickmaster.grpc;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.protobuf.services.ProtoReflectionService;

import java.io.IOException;

public class ProtoServer {

    public ProtoServer()  {
    }

    public void start() throws IOException, InterruptedException {
        // Registramos la lógica
        Server server = ServerBuilder.forPort(8081)
                .addService((BindableService) new ProductoServiceImpl()) // Registramos la lógica
                .addService(ProtoReflectionService.newInstance())
                .build();
        // 2. Iniciar
        System.out.println("Iniciando servidor gRPC en el puerto 8081...");
        server.start();

        // 3. Mantener vivo (bloquear el main para que no se cierre)
        System.out.println("Servidor escuchando...");
        server.awaitTermination();
    }
}