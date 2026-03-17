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
    private static final int REGISTER_ATTEMPTS = Integer
            .parseInt(System.getenv().getOrDefault("LB_REGISTER_ATTEMPTS", "5"));
    private static final long REGISTER_RETRY_DELAY_MS = Long
            .parseLong(System.getenv().getOrDefault("LB_REGISTER_RETRY_DELAY_MS", "2000"));

    public static void main(String[] args) throws IOException {
        int port = Integer.parseInt(System.getenv().getOrDefault("TM_SERVICE_PORT", "9101"));
        int grpcPort = Integer.parseInt(System.getenv().getOrDefault("TM_GRPC_PORT", String.valueOf(port + 100)));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/auth/login", new LoginHandler());
        server.createContext("/events", new EventsHandler());
        server.createContext("/usuarios", new UsuariosHandler());
        server.createContext("/tickets", new TicketsHandler());
        server.createContext("/health", new MonitorHandler(false, port));
        server.createContext("/metrics", new MonitorHandler(true, port));
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
        String registerUrl = System.getenv().getOrDefault("LB_REGISTER_URL", "http://localhost:1915/register");
        String scheme = System.getenv().getOrDefault("TM_SERVICE_SCHEME", "http");
        String host = System.getenv().getOrDefault("TM_SERVICE_HOST", "localhost");

        JsonObject body = new JsonObject();
        body.addProperty("url", scheme + "://" + host + ":" + port + "/");
        body.addProperty("ip", host);
        body.addProperty("port", port);
        byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);

        for (int attempt = 1; attempt <= REGISTER_ATTEMPTS; attempt++) {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(registerUrl).openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(2000);
                conn.setReadTimeout(2000);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json");

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload);
                }

                int code = conn.getResponseCode();
                if (code >= 200 && code < 300) {
                    log.info("Backend registered in load-balancer: {}:{} (attempt={})", host, port, attempt);
                    return;
                }
                log.warn("Could not register backend (status={}, attempt={}): {}:{}", code, attempt, host, port);
            } catch (Exception e) {
                log.warn("Could not register backend in load-balancer (attempt={}): {}:{} ({})",
                        attempt, host, port, e.getClass().getSimpleName());
            }

            try {
                Thread.sleep(REGISTER_RETRY_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        log.warn("Backend was not registered after {} attempts: {}:{}", REGISTER_ATTEMPTS, host, port);
    }
}
