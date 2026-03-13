package edu.upb.desktop.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.upb.desktop.model.LoginResultModel;
import edu.upb.desktop.model.UserModel;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class AuthService {
    private final String baseUrl = System.getenv().getOrDefault("LB_UI_BASE_URL", "http://localhost:1915/tm");

    public LoginResultModel login(String username, String password) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("username", username);
        body.addProperty("password", password);

        HttpURLConnection conn = openConnection(baseUrl + "/auth/login", "POST");
        writeBody(conn, body.toString());

        int code = conn.getResponseCode();
        if (code != 200) {
            throw new IllegalArgumentException(readErrorMessage(conn, "Credenciales invalidas"));
        }

        try (Reader reader = new java.io.InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
            JsonObject resp = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject user = resp.getAsJsonObject("user");
            UserModel userModel = new UserModel(
                    user.get("id").getAsLong(),
                    user.get("username").getAsString(),
                    user.get("nombre").getAsString(),
                    user.get("rol").getAsString());
            String token = user.has("token") ? user.get("token").getAsString() : "";
            return new LoginResultModel(userModel, token);
        }
    }

    public UserModel register(String username, String nombre, String password) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("username", username);
        body.addProperty("nombre", nombre);
        body.addProperty("password", password);
        body.addProperty("rol", "CLIENTE");

        HttpURLConnection conn = openConnection(baseUrl + "/usuarios", "POST");
        writeBody(conn, body.toString());

        int code = conn.getResponseCode();
        if (code != 201) {
            throw new IllegalArgumentException(readErrorMessage(conn, "No se pudo registrar"));
        }

        try (Reader reader = new java.io.InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
            JsonObject user = JsonParser.parseReader(reader).getAsJsonObject();
            return new UserModel(
                    user.get("id").getAsLong(),
                    user.get("username").getAsString(),
                    user.get("nombre").getAsString(),
                    user.get("rol").getAsString());
        }
    }

    private HttpURLConnection openConnection(String target, String method) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(target).openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(4000);
        conn.setReadTimeout(4000);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        return conn;
    }

    private void writeBody(HttpURLConnection conn, String body) throws Exception {
        try (OutputStream os = conn.getOutputStream();
                OutputStreamWriter writer = new OutputStreamWriter(os, StandardCharsets.UTF_8)) {
            writer.write(body);
        }
    }

    private String readErrorMessage(HttpURLConnection conn, String fallback) {
        try (Reader reader = new java.io.InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8)) {
            JsonObject resp = JsonParser.parseReader(reader).getAsJsonObject();
            if (resp.has("message")) {
                return resp.get("message").getAsString();
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }
}
