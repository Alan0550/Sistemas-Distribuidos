package edu.upb.desktop.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MonitorService {
    private final String baseUrl = System.getenv().getOrDefault("LB_MONITOR_BASE_URL", "http://localhost:1915");

    public JsonObject fetchHealth() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl + "/monitor/health").openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(3000);

        if (conn.getResponseCode() != 200) {
            throw new IllegalStateException("No se pudo obtener estado de servicios");
        }

        try (Reader reader = new java.io.InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    public JsonObject fetchMetrics() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl + "/monitor/metrics").openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(3000);
        conn.setReadTimeout(3000);

        if (conn.getResponseCode() != 200) {
            throw new IllegalStateException("No se pudo obtener metricas");
        }

        try (Reader reader = new java.io.InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
