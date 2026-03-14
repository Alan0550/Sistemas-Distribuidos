package edu.upb.tmservice;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import edu.upb.tmservice.dao.TicketDao;
import edu.upb.tmservice.dao.TicketHistoryEntity;
import edu.upb.tmservice.dao.UsuarioDao;
import edu.upb.tmservice.dao.UsuarioEntity;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLException;
import java.util.List;

public class UsuariosHandler implements HttpHandler {
    private static final Logger log = LoggerFactory.getLogger(UsuariosHandler.class);
    private final UsuarioDao usuarioDao = new UsuarioDao();
    private final TicketDao ticketDao = new TicketDao();

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
                boolean includeHistory = "true".equalsIgnoreCase(getQueryParam(he, "include_history"));
                if (includeHistory) {
                    AuthSessionStore.SessionData session = requireSession(he);
                    if (session == null) {
                        return;
                    }
                    if (!"ADMIN".equalsIgnoreCase(session.getRol())) {
                        sendSimpleJson(he, 403, "{\"status\":\"NOK\",\"message\":\"Acceso denegado para este rol\"}");
                        return;
                    }
                    JsonObject response = new JsonObject();
                    JsonArray usersArr = new JsonArray();
                    List<UsuarioEntity> users = usuarioDao.listPublicUsers();
                    for (UsuarioEntity entity : users) {
                        JsonObject user = toJsonUser(entity);
                        user.add("historial", toJsonHistory(ticketDao.listHistoryByUserId(entity.getId())));
                        usersArr.add(user);
                    }
                    response.add("users", usersArr);
                    byte[] out = response.toString().getBytes(StandardCharsets.UTF_8);
                    he.sendResponseHeaders(200, out.length);
                    try (OutputStream os = he.getResponseBody()) {
                        os.write(out);
                    }
                    return;
                }

                String usernameFilter = getQueryParam(he, "username");
                if (usernameFilter != null && !usernameFilter.isEmpty()) {
                    UsuarioEntity entity = usuarioDao.findPublicByUsername(usernameFilter);
                    JsonObject user = toJsonUser(entity);

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
                List<UsuarioEntity> users = usuarioDao.listPublicUsers();
                for (UsuarioEntity entity : users) {
                    arr.add(toJsonUser(entity));
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
                String rol = "CLIENTE";

                if (username.isEmpty() || nombre.isEmpty() || password.isEmpty()) {
                    error = true;
                    byte[] out = "{\"status\":\"NOK\",\"message\":\"Faltan datos\"}".getBytes(StandardCharsets.UTF_8);
                    he.sendResponseHeaders(400, out.length);
                    try (OutputStream os = he.getResponseBody()) {
                        os.write(out);
                    }
                    return;
                }

                try {
                    String passwordHash = BCrypt.hashpw(password, BCrypt.gensalt(12));
                    long id = usuarioDao.createUser(username, nombre, passwordHash, rol);
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

    private JsonObject toJsonUser(UsuarioEntity entity) {
        if (entity == null) {
            return null;
        }
        JsonObject user = new JsonObject();
        user.addProperty("id", entity.getId());
        user.addProperty("username", entity.getUsername());
        user.addProperty("nombre", entity.getNombre());
        user.addProperty("rol", entity.getRol());
        return user;
    }

    private JsonArray toJsonHistory(List<TicketHistoryEntity> history) {
        JsonArray arr = new JsonArray();
        for (TicketHistoryEntity item : history) {
            JsonObject row = new JsonObject();
            row.addProperty("ticket_id", item.getTicketId());
            row.addProperty("event_id", item.getEventId());
            row.addProperty("event_name", item.getEventName());
            row.addProperty("event_date", item.getEventDate() != null ? item.getEventDate().toString() : "");
            row.addProperty("seat_number", item.getSeatNumber());
            row.addProperty("seat_type", item.getSeatType());
            row.addProperty("price", item.getPrice());
            arr.add(row);
        }
        return arr;
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

    private AuthSessionStore.SessionData requireSession(HttpExchange he) throws IOException {
        String authHeader = he.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendSimpleJson(he, 401, "{\"status\":\"NOK\",\"message\":\"No autorizado\"}");
            return null;
        }

        String token = authHeader.substring("Bearer ".length()).trim();
        AuthSessionStore.SessionData session = AuthSessionStore.getInstance().getSession(token);
        if (session == null) {
            sendSimpleJson(he, 401, "{\"status\":\"NOK\",\"message\":\"Sesion invalida o expirada\"}");
            return null;
        }
        return session;
    }

    private void sendSimpleJson(HttpExchange he, int statusCode, String bodyText) throws IOException {
        byte[] out = bodyText.getBytes(StandardCharsets.UTF_8);
        he.sendResponseHeaders(statusCode, out.length);
        try (OutputStream os = he.getResponseBody()) {
            os.write(out);
        }
    }
}
