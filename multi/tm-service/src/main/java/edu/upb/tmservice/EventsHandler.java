package edu.upb.tmservice;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;

public class EventsHandler implements HttpHandler {
    private static final Logger log = LoggerFactory.getLogger(EventsHandler.class);
    private final EventManagementService eventService = new EventManagementService();

    @Override
    public void handle(HttpExchange he) throws IOException {
        long start = System.nanoTime();
        boolean error = false;
        String method = he.getRequestMethod();
        String path = he.getRequestURI().toString();
        Headers h = he.getResponseHeaders();
        HttpJsonSupport.addJsonHeaders(h, "GET, POST, OPTIONS");
        log.info("Request {} {}", method, path);

        if ("OPTIONS".equals(method)) {
            try {
                HttpJsonSupport.sendJsonText(he, 200, "{}");
            } finally {
                MonitorStore.getInstance().record("/events", start, false);
            }
            return;
        }

        try {
            String requestPath = he.getRequestURI().getPath();
            AuthSessionStore.SessionData session = AuthSupport.requireSession(he);
            if (session == null) {
                return;
            }

            if ("GET".equals(method)) {
                handleGet(he, session);
                return;
            }
            if ("POST".equals(method)) {
                if ("/events".equals(requestPath)) {
                    if (!AuthSupport.requireRole(he, session, "ADMIN")) {
                        return;
                    }
                    handleCreateEvent(he);
                    return;
                }
                if ("/events/full".equals(requestPath)) {
                    if (!AuthSupport.requireRole(he, session, "ADMIN")) {
                        return;
                    }
                    handleCreateFullEvent(he);
                    return;
                }
                if (requestPath.matches("^/events/\\d+/ticket-types$")) {
                    if (!AuthSupport.requireRole(he, session, "ADMIN")) {
                        return;
                    }
                    handleCreateTicketType(he, requestPath);
                    return;
                }
            }

            JsonObject resp = new JsonObject();
            resp.addProperty("status", "NOK");
            resp.addProperty("message", "Metodo o ruta no soportada");
            HttpJsonSupport.sendJson(he, 405, resp);
        } catch (IllegalArgumentException e) {
            error = true;
            int statusCode = isConflictMessage(e.getMessage()) ? 409 : 400;
            HttpJsonSupport.sendJson(he, statusCode, HttpJsonSupport.jsonStatus("NOK", e.getMessage()));
        } catch (SQLException e) {
            error = true;
            log.error("Database error in /events", e);
            JsonObject resp = new JsonObject();
            resp.addProperty("status", "NOK");
            resp.addProperty("message", "Error de base de datos en /events");
            HttpJsonSupport.sendJson(he, 500, resp);
        } catch (Exception e) {
            error = true;
            log.error("Error in /events", e);
            JsonObject resp = new JsonObject();
            resp.addProperty("status", "NOK");
            resp.addProperty("message", "Error en /events");
            HttpJsonSupport.sendJson(he, 500, resp);
        } finally {
            MonitorStore.getInstance().record("/events", start, error);
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            log.info("Finished {} {} in {} ms (error={})", method, path, elapsedMs, error);
        }
    }

    private void handleGet(HttpExchange he, AuthSessionStore.SessionData session) throws Exception {
        String keyword = HttpJsonSupport.getQueryParam(he, "keyword");
        int backendPort = Integer.parseInt(System.getenv().getOrDefault("TM_SERVICE_PORT", "9101"));
        JsonObject resp = eventService.listEvents(keyword, backendPort, session.getUserId(), session.getRol());
        sendJson(he, 200, resp);
    }

    private void handleCreateEvent(HttpExchange he) throws Exception {
        JsonObject body = HttpJsonSupport.readJsonBody(he);
        String nombre = body.has("nombre") ? body.get("nombre").getAsString() : "";
        String fecha = body.has("fecha") ? body.get("fecha").getAsString() : "";
        int capacidad = body.has("capacidad") ? body.get("capacidad").getAsInt() : 0;
        boolean descuentoFrecuente = body.has("descuento_frecuente") && body.get("descuento_frecuente").getAsBoolean();
        JsonObject resp = eventService.createEvent(nombre, fecha, capacidad, descuentoFrecuente);
        sendJson(he, 201, resp);
    }

    private void handleCreateTicketType(HttpExchange he, String requestPath) throws Exception {
        long eventId = extractEventId(requestPath);
        JsonObject body = HttpJsonSupport.readJsonBody(he);
        String tipoAsiento = body.has("tipo_asiento") ? body.get("tipo_asiento").getAsString().trim() : "";
        int cantidad = body.has("cantidad") ? body.get("cantidad").getAsInt() : 0;
        java.math.BigDecimal precio = body.has("precio") ? body.get("precio").getAsBigDecimal() : java.math.BigDecimal.ZERO;
        JsonObject resp = eventService.createTicketType(eventId, tipoAsiento, cantidad, precio);
        sendJson(he, 201, resp);
    }

    private void handleCreateFullEvent(HttpExchange he) throws Exception {
        JsonObject body = HttpJsonSupport.readJsonBody(he);

        String nombre = body.has("nombre") ? body.get("nombre").getAsString().trim() : "";
        String fecha = body.has("fecha") ? body.get("fecha").getAsString().trim() : "";
        int capacidad = body.has("capacidad") ? body.get("capacidad").getAsInt() : 0;
        boolean descuentoFrecuente = body.has("descuento_frecuente") && body.get("descuento_frecuente").getAsBoolean();
        JsonArray tipos = body.has("tipos_ticket") ? body.getAsJsonArray("tipos_ticket") : new JsonArray();
        JsonObject resp = eventService.createFullEvent(nombre, fecha, capacidad, descuentoFrecuente, tipos);
        sendJson(he, 201, resp);
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
        HttpJsonSupport.sendJson(he, statusCode, body);
    }

    private boolean isConflictMessage(String message) {
        if (message == null) {
            return false;
        }
        return message.contains("supera la capacidad")
                || message.contains("Debes asignar exactamente")
                || message.contains("espacios disponibles")
                || message.contains("El evento no existe");
    }
}
