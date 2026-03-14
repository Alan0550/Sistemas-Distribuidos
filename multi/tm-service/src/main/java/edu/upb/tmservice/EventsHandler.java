package edu.upb.tmservice;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import edu.upb.tmservice.dao.EventoDao;
import edu.upb.tmservice.dao.EventoEntity;
import edu.upb.tmservice.dao.TipoTicketDao;
import edu.upb.tmservice.dao.TipoTicketEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

public class EventsHandler implements HttpHandler {
    private static final Logger log = LoggerFactory.getLogger(EventsHandler.class);
    private final EventoDao eventoDao = new EventoDao();
    private final TipoTicketDao tipoTicketDao = new TipoTicketDao();

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
            AuthSessionStore.SessionData session = requireSession(he);
            if (session == null) {
                return;
            }

            if ("GET".equals(method)) {
                handleGet(he);
                return;
            }
            if ("POST".equals(method)) {
                if ("/events".equals(requestPath)) {
                    if (!isAdmin(session)) {
                        sendForbidden(he);
                        return;
                    }
                    handleCreateEvent(he);
                    return;
                }
                if ("/events/full".equals(requestPath)) {
                    if (!isAdmin(session)) {
                        sendForbidden(he);
                        return;
                    }
                    handleCreateFullEvent(he);
                    return;
                }
                if (requestPath.matches("^/events/\\d+/ticket-types$")) {
                    if (!isAdmin(session)) {
                        sendForbidden(he);
                        return;
                    }
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
            List<EventoEntity> events = eventoDao.listEvents(keyword);
            for (EventoEntity event : events) {
                long eventId = event.getId();
                JsonObject ev = new JsonObject();
                ev.addProperty("id", eventId);
                ev.addProperty("nombre", event.getNombre());
                Timestamp fecha = event.getFecha();
                ev.addProperty("fecha", fecha != null ? fecha.toString() : "");
                ev.addProperty("capacidad", event.getCapacidad());
                ev.add("tipos_ticket", loadTiposTicket(conn, eventId));
                arr.add(ev);
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
        eventId = eventoDao.createEvent(nombre, fechaTs, capacidad);

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
                    int eventCapacity = eventoDao.loadEventCapacity(conn, eventId);
                    if (eventCapacity <= 0) {
                        throw new IllegalArgumentException("El evento no existe");
                    }

                    int usedCapacity = tipoTicketDao.loadAssignedCapacity(conn, eventId);
                    remainingCapacity = eventCapacity - usedCapacity;
                    if (cantidad > remainingCapacity) {
                        throw new IllegalArgumentException("Solo quedan " + Math.max(remainingCapacity, 0) + " espacios disponibles");
                    }

                    ticketTypeId = tipoTicketDao.create(conn, eventId, tipoAsiento, cantidad, precio);
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

    private void handleCreateFullEvent(HttpExchange he) throws Exception {
        JsonObject body = JsonParser.parseReader(
                new InputStreamReader(he.getRequestBody(), StandardCharsets.UTF_8)).getAsJsonObject();

        String nombre = body.has("nombre") ? body.get("nombre").getAsString().trim() : "";
        String fecha = body.has("fecha") ? body.get("fecha").getAsString().trim() : "";
        int capacidad = body.has("capacidad") ? body.get("capacidad").getAsInt() : 0;
        JsonArray tipos = body.has("tipos_ticket") ? body.getAsJsonArray("tipos_ticket") : new JsonArray();

        if (nombre.isEmpty() || fecha.isEmpty() || capacidad <= 0 || tipos.isEmpty()) {
            JsonObject resp = new JsonObject();
            resp.addProperty("status", "NOK");
            resp.addProperty("message", "Debes completar evento y tipos de ticket");
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

        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                int assignedCapacity = 0;
                for (int i = 0; i < tipos.size(); i++) {
                    JsonObject tipo = tipos.get(i).getAsJsonObject();
                    String tipoAsiento = tipo.has("tipo_asiento") ? tipo.get("tipo_asiento").getAsString().trim() : "";
                    int cantidad = tipo.has("cantidad") ? tipo.get("cantidad").getAsInt() : 0;
                    java.math.BigDecimal precio = tipo.has("precio")
                            ? tipo.get("precio").getAsBigDecimal()
                            : java.math.BigDecimal.ZERO;

                    if (tipoAsiento.isEmpty() || cantidad <= 0 || precio.compareTo(java.math.BigDecimal.ZERO) <= 0) {
                        throw new IllegalArgumentException("Todos los tipos de ticket deben ser validos");
                    }
                    assignedCapacity += cantidad;
                    if (assignedCapacity > capacidad) {
                        throw new IllegalArgumentException("La suma de asientos supera la capacidad del evento");
                    }
                }

                if (assignedCapacity != capacidad) {
                    throw new IllegalArgumentException("Debes asignar exactamente toda la capacidad del evento");
                }

                long eventId = eventoDao.createEvent(conn, nombre, fechaTs, capacidad);
                JsonArray tiposResp = new JsonArray();
                for (int i = 0; i < tipos.size(); i++) {
                    JsonObject tipo = tipos.get(i).getAsJsonObject();
                    String tipoAsiento = tipo.get("tipo_asiento").getAsString().trim();
                    int cantidad = tipo.get("cantidad").getAsInt();
                    java.math.BigDecimal precio = tipo.get("precio").getAsBigDecimal();

                    long ticketTypeId = tipoTicketDao.create(conn, eventId, tipoAsiento, cantidad, precio);
                    JsonObject tipoResp = new JsonObject();
                    tipoResp.addProperty("id", ticketTypeId);
                    tipoResp.addProperty("tipo_asiento", tipoAsiento);
                    tipoResp.addProperty("cantidad", cantidad);
                    tipoResp.addProperty("precio", precio);
                    tiposResp.add(tipoResp);
                }
                conn.commit();

                JsonObject resp = new JsonObject();
                resp.addProperty("status", "OK");
                resp.addProperty("id", eventId);
                resp.addProperty("nombre", nombre);
                resp.addProperty("fecha", fechaTs.toString());
                resp.addProperty("capacidad", capacidad);
                resp.add("tipos_ticket", tiposResp);
                sendJson(he, 201, resp);
            } catch (IllegalArgumentException e) {
                conn.rollback();
                JsonObject resp = new JsonObject();
                resp.addProperty("status", "NOK");
                resp.addProperty("message", e.getMessage());
                sendJson(he, 409, resp);
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private JsonArray loadTiposTicket(Connection conn, long eventId) throws SQLException {
        JsonArray arr = new JsonArray();
        List<TipoTicketEntity> types = tipoTicketDao.listByEventId(conn, eventId);
        for (TipoTicketEntity type : types) {
            JsonObject tipo = new JsonObject();
            tipo.addProperty("id", type.getId());
            tipo.addProperty("tipo_asiento", type.getTipoAsiento());
            tipo.addProperty("cantidad", type.getCantidad());
            tipo.addProperty("precio", type.getPrecio());
            arr.add(tipo);
        }
        return arr;
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

    private AuthSessionStore.SessionData requireSession(HttpExchange he) throws IOException {
        String authHeader = he.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            JsonObject resp = new JsonObject();
            resp.addProperty("status", "NOK");
            resp.addProperty("message", "No autorizado");
            sendJson(he, 401, resp);
            return null;
        }

        String token = authHeader.substring("Bearer ".length()).trim();
        AuthSessionStore.SessionData session = AuthSessionStore.getInstance().getSession(token);
        if (session == null) {
            JsonObject resp = new JsonObject();
            resp.addProperty("status", "NOK");
            resp.addProperty("message", "Sesion invalida o expirada");
            sendJson(he, 401, resp);
            return null;
        }
        return session;
    }

    private boolean isAdmin(AuthSessionStore.SessionData session) {
        return session != null && "ADMIN".equalsIgnoreCase(session.getRol());
    }

    private void sendForbidden(HttpExchange he) throws IOException {
        JsonObject resp = new JsonObject();
        resp.addProperty("status", "NOK");
        resp.addProperty("message", "Acceso denegado para este rol");
        sendJson(he, 403, resp);
    }
}
