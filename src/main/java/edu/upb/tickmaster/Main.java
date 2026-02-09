package edu.upb.tickmaster;

import edu.upb.tickmaster.httpserver.ApacheServer;

public class Main {
    public static void main(String[] args) {
        ApacheServer apacheServer = new ApacheServer();
        apacheServer.start();
    }
}
