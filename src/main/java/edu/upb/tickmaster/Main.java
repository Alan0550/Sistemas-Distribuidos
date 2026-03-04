package edu.upb.tickmaster;

import edu.upb.tickmaster.grpc.ProtoServer;
import edu.upb.tickmaster.httpserver.ApacheServer;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        ApacheServer apacheServer = new ApacheServer();
        apacheServer.start();

        ProtoServer protoServer = new ProtoServer();
        protoServer.start();
    }
}
