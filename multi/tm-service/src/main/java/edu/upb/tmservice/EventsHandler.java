package edu.upb.tmservice;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

public class EventsHandler implements HttpHandler {
    private static final Logger log = LoggerFactory.getLogger(EventsHandler.class);

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

        if ("OPTIONS".equals(method)) {
            byte[] out = "{}".getBytes(StandardCharsets.UTF_8);
            he.sendResponseHeaders(200, out.length);
            try (OutputStream os = he.getResponseBody()) {
                os.write(out);
            } finally {
                MonitorStore.getInstance().record("/events", start, false);
            }
            return;
        }

        try {
            String requestPath = he.getRequestURI().getPath();
            if ("GET".equals(method)) {
                handleGet(he);
                return;
            }
            if ("POST".equals(method)) {
                if ("/events".equals(requestPath)) {
                    handleCreateEvent(he);
                    return;
                }
                if (requestPath.matches("^/events/\\d+/ticket-types$")) {
                    handleCreateTicketType(he, requestPath);
                    return;
                }
            }

            JsonObject resp = new JsonObject();
            resp.addProperty("status", "NOK");
            resp.addProperty("message", "Metodo o ruta no soportada");
            byte[] out = resp.toString().getBytes(StandardCharsets.UTF_8);
            he.sendResponseHeaders(405, out.length);
            try (OutputStream os = he.getResponseBody()) {
                os.write(out);
            }
        } catch (SQLException e) {
            error = true;
            log.error("Database error in /events", e);
            JsonObject resp = new JsonObject();
            resp.addProperty("status", "NOK");
            resp.addProperty("message", "Error de base de datos en /events");
            byte[] out = resp.toString().getBytes(StandardCharsets.UTF_8);
            he.sendResponseHeaders(500, out.length);
            try (OutputStream os = he.getResponseBody()) {
                os.write(out);
            }
        } catch (Exception e) {
            error = true;
            log.error("Error in /events", e);
            JsonObject resp = new JsonObject();
            resp.addProperty("status", "NOK");
            resp.addProperty("message", "Error en /events");
            byte[] out = resp.toString().getBytes(StandardCharsets.UTF_8);
            he.sendResponseHeaders(500, out.length);
            try (OutputStream os = he.getResponseBody()) {
                os.write(out);
            }
        } finally {
            MonitorStore.getInstance().record("/events", start, error);
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            log.info("Finished {} {} in {} ms (error={})", method, path, elapsedMs, error);
        }
    }

    private void handleGet(HttpExchange he) throws Exception {
        String keyword = "";
        String query = he.getRequestURI().getQuery();
        if (query != null && query.contains("keyword=")) {
            keyword = query.split("keyword=")[1];
        }
        JsonArray arr = new JsonArray();

        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            String sql = "SELECT id, nombre, fecha, capacidad FROM eventos";
            boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
            if (hasKeyword) {
                sql += " WHERE LOWER(nombre) LIKE ?";
            }
            sql += " ORDER BY fecha";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                if (hasKeyword) {
                    String like = "%" + keyword.toLowerCase() + "%";
                    ps.setString(1, like);
                }

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        long eventId = rs.getLong("id");
                        JsonObject ev = new JsonObject();
                        ev.addProperty("id", eventId);
                        ev.addProperty("nombre", rs.getString("nombre"));
                        Timestamp fecha = rs.getTimestamp("fecha");
                        ev.addProperty("fecha", fecha != null ? fecha.toString() : "");
                        ev.addProperty("capacidad", rs.getInt("capacidad"));
                        ev.add("tipos_ticket", loadTiposTicket(conn, eventId));
                        arr.add(ev);
                    }
                }
            }
        }

        JsonObject resp = new JsonObject();
        resp.addProperty("backend", System.getenv().getOrDefault("TM_SERVICE_PORT", "9101"));
        resp.add("events", arr);
        sendJson(he, 200, resp);
    }

    private void handleCreateEvent(HttpExchange he) throws Exception {
        JsonObject body = JsonParser.parseReader(
                new InputStreamReader(he.getRequestBody(), StandardCharsets.UTF_8)).getAsJsonObject();

        String nombre = body.has("nombre") ? body.get("nombre").getAsString().trim() : "";
        String fecha = body.has("fecha") ? body.get("fecha").getAsString().trim() : "";
        int capacidad = body.has("capacidad") ? body.get("capacidad").getAsInt() : 0;

        if (nombre.isEmpty() || fecha.isEmpty() || capacidad <= 0) {
            JsonObject resp = new JsonObject();
            resp.addProperty("status", "NOK");
            resp.addProperty("message", "Faltan datos del evento");
            sendJson(he, 400, resp);
            return;
        }

        Timestamp fechaTs;
        try {
            fechaTs = Timestamp.valueOf(fecha);
        } catch (IllegalArgumentException e) {
            JsonObject resp = new JsonObject();
            resp.addProperty("status", "NOK");
            resp.addProperty("message", "La fecha debe tener formato yyyy-MM-dd HH:mm:ss");
            sendJson(he, 400, resp);
            return;
        }

        long eventId = 0;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO eventos (nombre, fecha, capacidad) VALUES (?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre);
            ps.setTimestamp(2, fechaTs);
            ps.setInt(3, capacidad);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    eventId = keys.getLong(1);
                }
            }
        }

        JsonObject resp = new JsonObject();
        resp.addProperty("status", "OK");
        resp.addProperty("id", eventId);
        resp.addProperty("nombre", nombre);
        resp.addProperty("fecha", fechaTs.toString());
        resp.addProperty("capacidad", capacidad);
        sendJson(he, 201, resp);
    }

    private void handleCreateTicketType(HttpExchange he, String requestPath) throws Exception {
        long eventId = extractEventId(requestPath);
        if (eventId <= 0) {
            JsonObject resp = new JsonObject();
            resp.addProperty("status", "NOK");
            resp.addProperty("message", "Evento invalido");
            sendJson(he, 400, resp);
            return;
        }

        JsonObject body = JsonParser.parseReader(
                new InputStreamReader(he.getRequestBody(), StandardCharsets.UTF_8)).getAsJsonObject();

        String tipoAsiento = body.has("tipo_asiento") ? body.get("tipo_asiento").getAsString().trim() : "";
        int cantidad = body.has("cantidad") ? body.get("cantidad").getAsInt() : 0;
        java.math.BigDecimal precio = body.has("precio") ? body.get("precio").getAsBigDecimal() : java.math.BigDecimal.ZERO;

        if (tipoAsiento.isEmpty() || cantidad <= 0 || precio.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            JsonObject resp = new JsonObject();
            resp.addProperty("status", "NOK");
            resp.addProperty("message", "Datos invalidos para el tipo de ticket");
            sendJson(he, 400, resp);
            return;
        }

        long ticketTypeId = 0;
        int remainingCapacity;
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                int eventCapacity = loadEventCapacity(conn, eventId);
                if (eventCapacity <= 0) {
                    throw new IllegalArgumentException("El evento no existe");
                }

                int usedCapacity = loadAssignedCapacity(conn, eventId);
                remainingCapacity = eventCapacity - usedCapacity;
                if (cantidad > remainingCapacity) {
                    throw new IllegalArgumentException("Solo quedan " + Math.max(remainingCapacity, 0) + " espacios disponibles");
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO tipo_ticket (id_evento, tipo_asiento, cantidad, precio) VALUES (?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setLong(1, eventId);
                    ps.setString(2, tipoAsiento);
                    ps.setInt(3, cantidad);
                    ps.setBigDecimal(4, precio);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            ticketTypeId = keys.getLong(1);
                        }
                    }
                }
                conn.commit();
                remainingCapacity -= cantidad;
            } catch (IllegalArgumentException e) {
                conn.rollback();
                JsonObject resp = new JsonObject();
                resp.addProperty("status", "NOK");
                resp.addProperty("message", e.getMessage());
                sendJson(he, 409, resp);
                return;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }

        JsonObject resp = new JsonObject();
        resp.addProperty("status", "OK");
        resp.addProperty("id", ticketTypeId);
        resp.addProperty("id_evento", eventId);
        resp.addProperty("tipo_asiento", tipoAsiento);
        resp.addProperty("cantidad", cantidad);
        resp.addProperty("precio", precio);
        resp.addProperty("capacidad_restante", remainingCapacity);
        sendJson(he, 201, resp);
    }

    private JsonArray loadTiposTicket(Connection conn, long eventId) throws SQLException {
        JsonArray arr = new JsonArray();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, tipo_asiento, cantidad, precio FROM tipo_ticket WHERE id_evento = ? ORDER BY id")) {
            ps.setLong(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JsonObject tipo = new JsonObject();
                    tipo.addProperty("id", rs.getLong("id"));
                    tipo.addProperty("tipo_asiento", rs.getString("tipo_asiento"));
                    tipo.addProperty("cantidad", rs.getInt("cantidad"));
                    tipo.addProperty("precio", rs.getBigDecimal("precio"));
                    arr.add(tipo);
                }
            }
        }
        return arr;
    }

    private int loadEventCapacity(Connection conn, long eventId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT capacidad FROM eventos WHERE id = ? LIMIT 1")) {
            ps.setLong(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("capacidad");
                }
            }
        }
        return 0;
    }

    private int loadAssignedCapacity(Connection conn, long eventId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COALESCE(SUM(cantidad), 0) AS total FROM tipo_ticket WHERE id_evento = ?")) {
            ps.setLong(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        }
        return 0;
    }

    private long extractEventId(String requestPath) {
        String[] parts = requestPath.split("/");
        if (parts.length < 3) {
            return -1;
        }
        try {
            return Long.parseLong(parts[2]);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void sendJson(HttpExchange he, int statusCode, JsonObject body) throws IOException {
        byte[] out = body.toString().getBytes(StandardCharsets.UTF_8);
        he.sendResponseHeaders(statusCode, out.length);
        try (OutputStream os = he.getResponseBody()) {
            os.write(out);
        }
    }
}
