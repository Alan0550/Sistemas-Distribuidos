package edu.upb.tmservice;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLException;
import java.util.List;

public class UsuariosHandler implements HttpHandler {
    private static final Logger log = LoggerFactory.getLogger(UsuariosHandler.class);
    private final UsuarioDao usuarioDao = new UsuarioDao();
    private final TicketDao ticketDao = new TicketDao();
    private final UserManagementService userManagementService = new UserManagementService();

    @Override
    public void handle(HttpExchange he) throws IOException {
        long start = System.nanoTime();
        boolean error = false;
        String method = he.getRequestMethod();
        String path = he.getRequestURI().toString();
        Headers h = he.getResponseHeaders();
        HttpJsonSupport.addJsonHeaders(h, "GET, POST, PATCH, OPTIONS");
        log.info("Request {} {}", method, path);

        try {
            if ("OPTIONS".equals(method)) {
                HttpJsonSupport.sendJsonText(he, 200, "{}");
                return;
            }

            String requestPath = he.getRequestURI().getPath();
            if ("GET".equals(method)) {
                boolean includeHistory = "true".equalsIgnoreCase(HttpJsonSupport.getQueryParam(he, "include_history"));
                if (includeHistory) {
                    AuthSessionStore.SessionData session = AuthSupport.requireSession(he);
                    if (session == null) {
                        return;
                    }
                    if (!AuthSupport.requireRole(he, session, "ADMIN")) {
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
                    HttpJsonSupport.sendJson(he, 200, response);
                    return;
                }

                String usernameFilter = HttpJsonSupport.getQueryParam(he, "username");
                if (usernameFilter != null && !usernameFilter.isEmpty()) {
                    UsuarioEntity entity = usuarioDao.findPublicByUsername(usernameFilter);
                    JsonObject user = toJsonUser(entity);

                    if (user == null) {
                        error = true;
                        HttpJsonSupport.sendJson(he, 404, HttpJsonSupport.jsonStatus("NOK", "Usuario no encontrado"));
                        return;
                    }

                    HttpJsonSupport.sendJson(he, 200, user);
                    return;
                }

                AuthSessionStore.SessionData session = AuthSupport.requireSession(he);
                if (session == null) {
                    return;
                }
                if (!AuthSupport.requireRole(he, session, "ADMIN")) {
                    return;
                }

                JsonArray arr = new JsonArray();
                List<UsuarioEntity> users = usuarioDao.listPublicUsers();
                for (UsuarioEntity entity : users) {
                    arr.add(toJsonUser(entity));
                }
                HttpJsonSupport.sendJsonText(he, 200, arr.toString());
                return;
            }

            if ("POST".equals(method)) {
                JsonObject body = HttpJsonSupport.readJsonBody(he);

                String username = body.has("username") ? body.get("username").getAsString().trim() : "";
                String nombre = body.has("nombre") ? body.get("nombre").getAsString() : "";
                String password = body.has("password") ? body.get("password").getAsString() : "";
                String rol = "CLIENTE";

                if (username.isEmpty() || nombre.isEmpty() || password.isEmpty()) {
                    error = true;
                    HttpJsonSupport.sendJson(he, 400, HttpJsonSupport.jsonStatus("NOK", "Faltan datos"));
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
                    HttpJsonSupport.sendJson(he, 201, resp);
                    return;
                } catch (SQLIntegrityConstraintViolationException e) {
                    error = true;
                    HttpJsonSupport.sendJson(he, 409, HttpJsonSupport.jsonStatus("NOK", "Username ya existe"));
                    return;
                }
            }

            if ("PATCH".equals(method) && requestPath.matches("^/usuarios/\\d+/(ban|unban)$")) {
                AuthSessionStore.SessionData session = AuthSupport.requireSession(he);
                if (session == null) {
                    return;
                }
                if (!AuthSupport.requireRole(he, session, "ADMIN")) {
                    return;
                }
                handleBanUpdate(he, requestPath);
                return;
            }

            error = true;
            HttpJsonSupport.sendJson(he, 405, HttpJsonSupport.jsonStatus("NOK", "Metodo no soportado"));
        } catch (SQLException e) {
            error = true;
            log.error("Database error in /usuarios", e);
            HttpJsonSupport.sendJson(he, 500, HttpJsonSupport.jsonStatus("NOK", "Error de base de datos"));
        } catch (Exception e) {
            error = true;
            log.error("Error in /usuarios", e);
            HttpJsonSupport.sendJson(he, 500, HttpJsonSupport.jsonStatus("NOK", "Error en /usuarios"));
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
        user.addProperty("baneado", entity.isBaneado());
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

    private void handleBanUpdate(HttpExchange he, String requestPath) throws Exception {
        String[] parts = requestPath.split("/");
        long userId = Long.parseLong(parts[2]);
        boolean ban = "ban".equalsIgnoreCase(parts[3]);

        UserManagementService.BanResult result = ban
                ? userManagementService.banUser(userId)
                : userManagementService.unbanUser(userId);

        JsonObject resp = new JsonObject();
        resp.addProperty("status", "OK");
        resp.addProperty("username", result.getUsername());
        resp.addProperty("baneado", result.isBanned());
        resp.addProperty("tickets_liberados", result.getReleasedTickets());
        resp.addProperty("message", ban
                ? "Usuario baneado correctamente"
                : "Ban retirado correctamente");
        HttpJsonSupport.sendJson(he, 200, resp);
    }
}
