package edu.upb.tickmaster;

import edu.upb.tickmaster.grpc.ProtoServer;
import edu.upb.tickmaster.httpserver.ApacheServer;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        boolean httpEnabled = Boolean.parseBoolean(System.getenv().getOrDefault("LEGACY_HTTP_ENABLED", "true"));
        boolean grpcEnabled = Boolean.parseBoolean(System.getenv().getOrDefault("LEGACY_PROTO_ENABLED", "true"));

        System.out.println("[legacy] Root Tickmaster stack started in compatibility mode.");
        System.out.println("[legacy] The active distributed architecture lives under multi/.");

        if (httpEnabled) {
            ApacheServer apacheServer = new ApacheServer();
            apacheServer.start();
        } else {
            System.out.println("[legacy] HTTP compatibility gateway disabled by LEGACY_HTTP_ENABLED=false");
        }

        if (grpcEnabled) {
            ProtoServer protoServer = new ProtoServer();
            protoServer.start();
        } else {
            System.out.println("[legacy] Legacy gRPC server disabled by LEGACY_PROTO_ENABLED=false");
        }
    }
}
