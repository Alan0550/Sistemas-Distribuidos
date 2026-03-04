package edu.upb.lb;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class RegisterHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange he) throws IOException {
        Headers h = he.getResponseHeaders();
        h.add("Access-Control-Allow-Origin", "*");
        h.add("Content-Type", "application/json");
        h.add("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        h.add("Access-Control-Allow-Headers", "Content-Type");

        String method = he.getRequestMethod();
        if ("OPTIONS".equals(method)) {
            send(he, 200, "{}");
            return;
        }

        if ("GET".equals(method)) {
            List<String> list = BackendRegistry.getInstance().list();
            JsonArray arr = new JsonArray();
            for (String b : list) {
                arr.add(b);
            }
            JsonObject out = new JsonObject();
            out.add("backends", arr);
            out.addProperty("count", list.size());
            send(he, 200, out.toString());
            return;
        }

        if (!"POST".equals(method)) {
            send(he, 405, "{\"status\":\"NOK\",\"message\":\"Metodo no soportado\"}");
            return;
        }

        try {
            JsonObject body = JsonParser.parseReader(
                    new InputStreamReader(he.getRequestBody(), StandardCharsets.UTF_8)).getAsJsonObject();
            String ip = body.has("ip") ? body.get("ip").getAsString() : "localhost";
            int port = body.has("port") ? body.get("port").getAsInt() : -1;

            if (port < 1 || port > 65535) {
                send(he, 400, "{\"status\":\"NOK\",\"message\":\"Puerto invalido\"}");
                return;
            }
            //
            String backend = BackendRegistry.getInstance().register(ip, port);
            JsonObject out = new JsonObject();
            out.addProperty("status", "OK");
            out.addProperty("backend", backend);
            out.addProperty("count", BackendRegistry.getInstance().list().size());
            send(he, 200, out.toString());
        } catch (Exception e) {
            send(he, 400, "{\"status\":\"NOK\",\"message\":\"Body invalido\"}");
        }
    }

    private void send(HttpExchange he, int statusCode, String bodyText) throws IOException {
        byte[] body = bodyText.getBytes(StandardCharsets.UTF_8);
        he.sendResponseHeaders(statusCode, body.length);
        try (OutputStream os = he.getResponseBody()) {
            os.write(body);
        }
    }
}
