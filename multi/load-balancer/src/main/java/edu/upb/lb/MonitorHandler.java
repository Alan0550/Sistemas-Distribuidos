package edu.upb.lb;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MonitorHandler implements HttpHandler {

    private final boolean metricsMode;
    private final int port;

    public MonitorHandler(boolean metricsMode, int port) {
        this.metricsMode = metricsMode;
        this.port = port;
    }

    @Override
    public void handle(HttpExchange he) throws IOException {
        Headers h = he.getResponseHeaders();
        h.add("Access-Control-Allow-Origin", "*");
        h.add("Content-Type", "application/json");

        JsonObject out;
        if (metricsMode) {
            out = MonitorStore.getInstance().metrics("load-balancer", port);
        } else {
            out = MonitorStore.getInstance().health("load-balancer", port);
        }

        byte[] body = out.toString().getBytes(StandardCharsets.UTF_8);
        he.sendResponseHeaders(200, body.length);
        try (OutputStream os = he.getResponseBody()) {
            os.write(body);
        }
    }
}
