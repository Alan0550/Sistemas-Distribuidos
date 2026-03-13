package edu.upb.tickmaster.httpserver;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class UsuariosProxyHandler implements HttpHandler {

    private static final String LB_BASE_URL = System.getenv().getOrDefault("LB_BASE_URL", "http://localhost:1915");

    @Override
    public void handle(HttpExchange he) throws IOException {
        Headers responseHeaders = he.getResponseHeaders();
        responseHeaders.add("Access-Control-Allow-Origin", "*");
        responseHeaders.add("Content-Type", "application/json");

        String method = he.getRequestMethod();
        if ("OPTIONS".equals(method)) {
            byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
            he.sendResponseHeaders(200, body.length);
            try (OutputStream os = he.getResponseBody()) {
                os.write(body);
            }
            return;
        }

        if (!"GET".equals(method) && !"POST".equals(method)) {
            byte[] body = "{\"status\":\"NOK\",\"message\":\"Metodo no soportado\"}".getBytes(StandardCharsets.UTF_8);
            he.sendResponseHeaders(405, body.length);
            try (OutputStream os = he.getResponseBody()) {
                os.write(body);
            }
            return;
        }

        String query = he.getRequestURI().getRawQuery();
        String target = LB_BASE_URL + "/tm/usuarios" + (query != null ? "?" + query : "");

        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(target).openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(1500);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("Content-Type", "application/json");

            if ("POST".equals(method)) {
                conn.setDoOutput(true);
                byte[] requestBody = readBytes(he.getRequestBody());
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(requestBody);
                }
            }

            int statusCode = conn.getResponseCode();
            InputStream stream = statusCode >= 400 ? conn.getErrorStream() : conn.getInputStream();
            byte[] body = stream != null ? readBytes(stream) : new byte[0];

            he.sendResponseHeaders(statusCode, body.length);
            try (OutputStream os = he.getResponseBody()) {
                os.write(body);
            }
        } catch (Exception e) {
            byte[] body = ("{\"status\":\"NOK\",\"message\":\"No se pudo conectar al balanceador\",\"detail\":\""
                    + e.getClass().getSimpleName() + "\"}").getBytes(StandardCharsets.UTF_8);
            he.sendResponseHeaders(502, body.length);
            try (OutputStream os = he.getResponseBody()) {
                os.write(body);
            }
        }
    }

    private byte[] readBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, read);
        }
        return baos.toByteArray();
    }
}
