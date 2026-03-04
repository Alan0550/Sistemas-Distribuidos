package edu.upb.tmservice;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLException;
import java.sql.Statement;

public class UsuariosHandler implements HttpHandler {
    private static final Logger log = LoggerFactory.getLogger(UsuariosHandler.class);

    @Override
    public void handle(HttpExchange he) throws IOException {
        long start = System.nanoTime();
        boolean error = false;
        String method = he.getRequestMethod();
        String path = he.getRequestURI().toString();
        Headers h = he.getResponseHeaders();
        h.add("Access-Control-Allow-Origin", "*");
        h.add("Content-Type", "application/json");
        log.info("Request {} {}", method, path);

        try {
            if ("OPTIONS".equals(method)) {
                byte[] out = "{}".getBytes(StandardCharsets.UTF_8);
                he.sendResponseHeaders(200, out.length);
                try (OutputStream os = he.getResponseBody()) {
                    os.write(out);
                }
                return;
            }

            if ("GET".equals(method)) {
                String usernameFilter = getQueryParam(he, "username");
                if (usernameFilter != null && !usernameFilter.isEmpty()) {
                    JsonObject user = null;
                    try (Connection conn = DatabaseConnection.getInstance().getConnection();
                            PreparedStatement ps = conn.prepareStatement(
                                    "SELECT id, username, nombre, rol FROM usuarios WHERE username = ? LIMIT 1")) {
                        ps.setString(1, usernameFilter);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                user = new JsonObject();
                                user.addProperty("id", rs.getLong("id"));
                                user.addProperty("username", rs.getString("username"));
                                user.addProperty("nombre", rs.getString("nombre"));
                                user.addProperty("rol", rs.getString("rol"));
                            }
                        }
                    }

                    if (user == null) {
                        error = true;
                        byte[] out = "{\"status\":\"NOK\",\"message\":\"Usuario no encontrado\"}"
                                .getBytes(StandardCharsets.UTF_8);
                        he.sendResponseHeaders(404, out.length);
                        try (OutputStream os = he.getResponseBody()) {
                            os.write(out);
                        }
                        return;
                    }

                    byte[] out = user.toString().getBytes(StandardCharsets.UTF_8);
                    he.sendResponseHeaders(200, out.length);
                    try (OutputStream os = he.getResponseBody()) {
                        os.write(out);
                    }
                    return;
                }

                JsonArray arr = new JsonArray();
                try (Connection conn = DatabaseConnection.getInstance().getConnection();
                        PreparedStatement ps = conn
                                .prepareStatement("SELECT id, username, nombre, rol FROM usuarios ORDER BY id");
                        ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        JsonObject user = new JsonObject();
                        user.addProperty("id", rs.getLong("id"));
                        user.addProperty("username", rs.getString("username"));
                        user.addProperty("nombre", rs.getString("nombre"));
                        user.addProperty("rol", rs.getString("rol"));
                        arr.add(user);
                    }
                }
                byte[] out = arr.toString().getBytes(StandardCharsets.UTF_8);
                he.sendResponseHeaders(200, out.length);
                try (OutputStream os = he.getResponseBody()) {
                    os.write(out);
                }
                return;
            }

            if ("POST".equals(method)) {
                JsonObject body = JsonParser.parseReader(
                        new InputStreamReader(he.getRequestBody(), StandardCharsets.UTF_8)).getAsJsonObject();

                String username = body.has("username") ? body.get("username").getAsString().trim() : "";
                String nombre = body.has("nombre") ? body.get("nombre").getAsString() : "";
                String password = body.has("password") ? body.get("password").getAsString() : "";
                String rol = body.has("rol") ? body.get("rol").getAsString() : "";

                if (username.isEmpty() || nombre.isEmpty() || password.isEmpty() || rol.isEmpty()) {
                    error = true;
                    byte[] out = "{\"status\":\"NOK\",\"message\":\"Faltan datos\"}".getBytes(StandardCharsets.UTF_8);
                    he.sendResponseHeaders(400, out.length);
                    try (OutputStream os = he.getResponseBody()) {
                        os.write(out);
                    }
                    return;
                }

                try {
                    long id = 0;
                    String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt(12));
                    try (Connection conn = DatabaseConnection.getInstance().getConnection();
                            PreparedStatement ps = conn.prepareStatement(
                                    "INSERT INTO usuarios (username, nombre, password, rol) VALUES (?,?,?,?)",
                                    Statement.RETURN_GENERATED_KEYS)) {
                        ps.setString(1, username);
                        ps.setString(2, nombre);
                        ps.setString(3, passwordHash);
                        ps.setString(4, rol);
                        ps.executeUpdate();
                        try (ResultSet keys = ps.getGeneratedKeys()) {
                            if (keys.next()) {
                                id = keys.getLong(1);
                            }
                        }
                    }
                    JsonObject resp = new JsonObject();
                    resp.addProperty("id", id);
                    resp.addProperty("username", username);
                    resp.addProperty("nombre", nombre);
                    resp.addProperty("rol", rol);
                    byte[] out = resp.toString().getBytes(StandardCharsets.UTF_8);
                    he.sendResponseHeaders(201, out.length);
                    try (OutputStream os = he.getResponseBody()) {
                        os.write(out);
                    }
                    return;
                } catch (SQLIntegrityConstraintViolationException e) {
                    error = true;
                    byte[] out = "{\"status\":\"NOK\",\"message\":\"Username ya existe\"}"
                            .getBytes(StandardCharsets.UTF_8);
                    he.sendResponseHeaders(409, out.length);
                    try (OutputStream os = he.getResponseBody()) {
                        os.write(out);
                    }
                    return;
                }
            }

            error = true;
            byte[] out = "{\"status\":\"NOK\",\"message\":\"Metodo no soportado\"}".getBytes(StandardCharsets.UTF_8);
            he.sendResponseHeaders(405, out.length);
            try (OutputStream os = he.getResponseBody()) {
                os.write(out);
            }
        } catch (SQLException e) {
            error = true;
            log.error("Database error in /usuarios", e);
            byte[] out = "{\"status\":\"NOK\",\"message\":\"Error de base de datos\"}".getBytes(StandardCharsets.UTF_8);
            he.sendResponseHeaders(500, out.length);
            try (OutputStream os = he.getResponseBody()) {
                os.write(out);
            }
        } catch (Exception e) {
            error = true;
            log.error("Error in /usuarios", e);
            byte[] out = "{\"status\":\"NOK\",\"message\":\"Error en /usuarios\"}".getBytes(StandardCharsets.UTF_8);
            he.sendResponseHeaders(500, out.length);
            try (OutputStream os = he.getResponseBody()) {
                os.write(out);
            }
        } finally {
            MonitorStore.getInstance().record("/usuarios", start, error);
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            log.info("Finished {} {} in {} ms (error={})", method, path, elapsedMs, error);
        }
    }

    private String getQueryParam(HttpExchange he, String key) {
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
}
