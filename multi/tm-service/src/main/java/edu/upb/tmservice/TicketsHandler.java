package edu.upb.tmservice;

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

public class TicketsHandler implements HttpHandler {
    private static final Logger log = LoggerFactory.getLogger(TicketsHandler.class);
    private final TicketPurchaseService purchaseService = new TicketPurchaseService();

    @Override
    public void handle(HttpExchange he) throws IOException {
        long start = System.nanoTime();
        boolean error = false;
        String method = he.getRequestMethod();
        String path = he.getRequestURI().toString();
        Headers h = he.getResponseHeaders();
        h.add("Access-Control-Allow-Origin", "*");
        h.add("Content-Type", "application/json");
        h.add("Access-Control-Allow-Methods", "POST, OPTIONS");
        h.add("Access-Control-Allow-Headers", "Content-Type, Authorization");
        log.info("Request {} {}", method, path);

        try {
            if ("OPTIONS".equals(method)) {
                sendJson(he, 200, new JsonObject());
                return;
            }
            if (!"POST".equals(method)) {
                JsonObject resp = new JsonObject();
                resp.addProperty("status", "NOK");
                resp.addProperty("message", "Metodo no soportado");
                sendJson(he, 405, resp);
                error = true;
                return;
            }

            JsonObject body = JsonParser.parseReader(
                    new InputStreamReader(he.getRequestBody(), StandardCharsets.UTF_8)).getAsJsonObject();

            long userId = resolveUserId(he, body);
            long eventId = readLong(body, "id_evento", "idEvento");
            long tipoTicketId = readLong(body, "id_tipo_ticket", "idTipoTicket");
            int cantidad = readInt(body, "cantidad");

            String idempotencyKey = readString(body, "idempotency_key", "idempotencyKey");
            if (idempotencyKey.isEmpty()) {
                idempotencyKey = "HTTP-" + userId + "-" + eventId + "-" + tipoTicketId + "-" + System.currentTimeMillis();
            }

            if (userId <= 0 || eventId <= 0 || tipoTicketId <= 0 || cantidad <= 0) {
                JsonObject resp = new JsonObject();
                resp.addProperty("status", "NOK");
                resp.addProperty("message", "id_usuario, id_evento, id_tipo_ticket y cantidad son obligatorios");
                sendJson(he, 400, resp);
                error = true;
                return;
            }

            TicketPurchaseService.PurchaseResult result = purchaseService.processPurchase(
                    userId, eventId, tipoTicketId, cantidad, idempotencyKey);

            JsonObject resp = new JsonObject();
            resp.addProperty("status", "OK");
            resp.addProperty("primer_ticket_id", result.getFirstTicketId());
            resp.addProperty("tickets_creados", result.getTicketsCreated());
            resp.addProperty("mensaje", result.getMessage());
            sendJson(he, 201, resp);
        } catch (IllegalArgumentException e) {
            error = true;
            JsonObject resp = new JsonObject();
            resp.addProperty("status", "NOK");
            resp.addProperty("message", e.getMessage());
            sendJson(he, 409, resp);
        } catch (Exception e) {
            error = true;
            log.error("Error en /tickets", e);
            JsonObject resp = new JsonObject();
            resp.addProperty("status", "NOK");
            resp.addProperty("message", "Error en /tickets");
            sendJson(he, 500, resp);
        } finally {
            MonitorStore.getInstance().record("/tickets", start, error);
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            log.info("Finished {} {} in {} ms (error={})", method, path, elapsedMs, error);
        }
    }

    private long resolveUserId(HttpExchange he, JsonObject body) {
        String authHeader = he.getRequestHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring("Bearer ".length()).trim();
            AuthSessionStore.SessionData session = AuthSessionStore.getInstance().getSession(token);
            if (session != null) {
                return session.getUserId();
            }
        }
        return readLong(body, "id_usuario", "idUsuario");
    }

    private String readString(JsonObject body, String... keys) {
        for (String key : keys) {
            if (body.has(key) && !body.get(key).isJsonNull()) {
                return body.get(key).getAsString().trim();
            }
        }
        return "";
    }

    private long readLong(JsonObject body, String... keys) {
        for (String key : keys) {
            if (body.has(key) && !body.get(key).isJsonNull()) {
                return body.get(key).getAsLong();
            }
        }
        return 0L;
    }

    private int readInt(JsonObject body, String... keys) {
        for (String key : keys) {
            if (body.has(key) && !body.get(key).isJsonNull()) {
                return body.get(key).getAsInt();
            }
        }
        return 0;
    }

    private void sendJson(HttpExchange he, int statusCode, JsonObject body) throws IOException {
        byte[] out = body.toString().getBytes(StandardCharsets.UTF_8);
        he.sendResponseHeaders(statusCode, out.length);
        try (OutputStream os = he.getResponseBody()) {
            os.write(out);
        }
    }
}
