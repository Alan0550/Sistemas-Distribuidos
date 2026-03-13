package edu.upb.lb;

import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class LoadBalancerApp {
    private static final Logger log = LoggerFactory.getLogger(LoadBalancerApp.class);
    private static final int LB_PORT = Integer.parseInt(System.getenv().getOrDefault("LB_PORT", "1915"));

    public static void main(String[] args) throws IOException {
        Esclavo worker = new Esclavo(BackendRegistry.getInstance());
        HttpServer server = HttpServer.create(new InetSocketAddress(LB_PORT), 0);
        server.createContext("/", new ProxyHandler());
        server.setExecutor(Executors.newFixedThreadPool(4));
        log.info("Load balancer on http://localhost:{}", LB_PORT);
        server.start();
        worker.start();
        Runtime.getRuntime().addShutdownHook(new Thread(worker::shutdown));
    }
}
