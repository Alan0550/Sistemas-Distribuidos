package edu.upb.tmservice;

import com.sun.net.httpserver.HttpServer;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public class TmServiceApp {
    private static final Logger log = LoggerFactory.getLogger(TmServiceApp.class);

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("TM_SERVICE_PORT", "9101"));
        int grpcPort = Integer.parseInt(System.getenv().getOrDefault("TM_GRPC_PORT", String.valueOf(port + 100)));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/auth/login", new LoginHandler());
        server.createContext("/events", new EventsHandler());
        server.createContext("/usuarios", new UsuariosHandler());
        server.createContext("/monitor/health", new MonitorHandler(false, port));
        server.createContext("/monitor/metrics", new MonitorHandler(true, port));
        server.setExecutor(Executors.newFixedThreadPool(4));
        log.info("tm-service on http://localhost:{}", port);
        server.start();
        TicketGrpcServer grpcServer = new TicketGrpcServer(grpcPort);
        grpcServer.start();
        Runtime.getRuntime().addShutdownHook(new Thread(grpcServer::stop));
        registerInLoadBalancer(port);
    }//

    private static void registerInLoadBalancer(int port) {
        String registerUrl = System.getenv().getOrDefault("LB_REGISTER_URL", "http://localhost:9000/registrar");
        String host = System.getenv().getOrDefault("TM_SERVICE_HOST", "localhost");

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(registerUrl).openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type", "application/json");

            JsonObject body = new JsonObject();
            body.addProperty("ip", host);
            body.addProperty("port", port);
            byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload);
            }

            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                log.info("Backend registered in load-balancer: {}:{}", host, port);
            } else {
                log.warn("Could not register backend (status={}): {}:{}", code, host, port);
            }
        } catch (Exception e) {
            log.warn("Could not register backend in load-balancer: {}:{}", host, port);
        }
    }
}
