package edu.upb.lb;

import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class LoadBalancerApp {
    private static final Logger log = LoggerFactory.getLogger(LoadBalancerApp.class);

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(9000), 0);
        server.createContext("/", new ProxyHandler());
        server.setExecutor(Executors.newFixedThreadPool(4));
        log.info("Load balancer on http://localhost:9000");
        server.start();
    }
}
