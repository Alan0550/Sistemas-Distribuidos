package edu.upb.tickmaster.grpc;

import io.grpc.stub.StreamObserver;

import java.time.Instant;

public class ProductoServiceImpl extends edu.upb.tickmaster.grpc.ProductoServiceGrpc.ProductoServiceImplBase {
    @Override
    public void registrarProducto(edu.upb.tickmaster.grpc.ProductoRequest request, StreamObserver<edu.upb.tickmaster.grpc.ProductoResponse> responseObserver) {
        System.out.println(request.toString());

        edu.upb.tickmaster.grpc.ProductoResponse response = edu.upb.tickmaster.grpc.ProductoResponse.newBuilder()
                .setCodigoRespuesta("201_CREATED")
                .setMensaje("Producto " + request.getNombre() + " registrado con éxito.")
                .setTimestamp(Instant.now().toString())
                .build();

        // Enviar la respuesta al cliente
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}