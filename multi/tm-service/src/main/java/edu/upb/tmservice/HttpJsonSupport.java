package edu.upb.tmservice;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

final class HttpJsonSupport {
    private HttpJsonSupport() {
    }

    static void addJsonHeaders(Headers headers, String methods) {
        headers.add("Access-Control-Allow-Origin", "*");
        headers.add("Content-Type", "application/json");
        headers.add("Access-Control-Allow-Methods", methods);
        headers.add("Access-Control-Allow-Headers", "Content-Type, Authorization");
    }

    static JsonObject readJsonBody(HttpExchange he) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(he.getRequestBody(), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    static void sendJson(HttpExchange he, int statusCode, JsonObject body) throws IOException {
        byte[] out = body.toString().getBytes(StandardCharsets.UTF_8);
        he.sendResponseHeaders(statusCode, out.length);
        try (OutputStream os = he.getResponseBody()) {
            os.write(out);
        }
    }

    static void sendJsonText(HttpExchange he, int statusCode, String bodyText) throws IOException {
        byte[] out = bodyText.getBytes(StandardCharsets.UTF_8);
        he.sendResponseHeaders(statusCode, out.length);
        try (OutputStream os = he.getResponseBody()) {
            os.write(out);
        }
    }

    static String getQueryParam(HttpExchange he, String key) {
        String raw = he.getRequestURI().getRawQuery();
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        String[] parts = raw.split("&");
        for (String part : parts) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && key.equals(kv[0])) {
                return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    static JsonObject jsonStatus(String status, String message) {
        JsonObject resp = new JsonObject();
        resp.addProperty("status", status);
        resp.addProperty("message", message);
        return resp;
    }
}
