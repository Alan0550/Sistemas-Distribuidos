package edu.upb.tickmaster.httpserver;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;

public class EstadoHandler implements HttpHandler {

    private static final String LB_BASE_URL = System.getenv().getOrDefault("LB_BASE_URL", "http://localhost:9000");

    @Override
    public void handle(HttpExchange he) throws IOException {
        Headers responseHeaders = he.getResponseHeaders();
        responseHeaders.add("Access-Control-Allow-Origin", "*");
        responseHeaders.add("Content-Type", "application/json");

        JsonObject response = new JsonObject();
        response.addProperty("service", "tickmaster-root");

        boolean dbUp = false;
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            dbUp = conn != null && conn.isValid(2);
        } catch (Exception ignored) {
        }

        boolean lbUp = false;
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(LB_BASE_URL + "/monitor/health").openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(1500);
            conn.setReadTimeout(2000);
            lbUp = conn.getResponseCode() == 200;
        } catch (Exception ignored) {
        }

        response.addProperty("db", dbUp ? "UP" : "DOWN");
        response.addProperty("load_balancer", lbUp ? "UP" : "DOWN");
        response.addProperty("status", (dbUp && lbUp) ? "UP" : "PARTIAL");

        byte[] body = response.toString().getBytes(StandardCharsets.UTF_8);
        he.sendResponseHeaders(200, body.length);
        try (OutputStream os = he.getResponseBody()) {
            os.write(body);
        }
    }
}
