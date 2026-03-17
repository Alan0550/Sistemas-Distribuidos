package edu.upb.tmservice;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

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
        HttpJsonSupport.addJsonHeaders(h, "POST, OPTIONS");
        log.info("Request {} {}", method, path);

        try {
            if ("OPTIONS".equals(method)) {
                HttpJsonSupport.sendJson(he, 200, new JsonObject());
                return;
            }
            if (!"POST".equals(method)) {
                HttpJsonSupport.sendJson(he, 405, HttpJsonSupport.jsonStatus("NOK", "Metodo no soportado"));
                error = true;
                return;
            }

            AuthSessionStore.SessionData session = AuthSupport.requireSession(he);
            if (session == null) {
                error = true;
                return;
            }

            if (!AuthSupport.hasAnyRole(session, "CLIENTE", "FRECUENTE", "VIP")) {
                HttpJsonSupport.sendJson(he, 403,
                        HttpJsonSupport.jsonStatus("NOK", "Solo CLIENTE, FRECUENTE y VIP pueden comprar tickets"));
                error = true;
                return;
            }

            JsonObject body = HttpJsonSupport.readJsonBody(he);

            long userId = session.getUserId();
            long eventId = readLong(body, "id_evento", "idEvento");
            long tipoTicketId = readLong(body, "id_tipo_ticket", "idTipoTicket");
            int cantidad = readInt(body, "cantidad");

            String idempotencyKey = readString(body, "idempotency_key", "idempotencyKey");
            if (idempotencyKey.isEmpty()) {
                idempotencyKey = "HTTP-" + userId + "-" + eventId + "-" + tipoTicketId + "-" + System.currentTimeMillis();
            }

            if (userId <= 0 || eventId <= 0 || tipoTicketId <= 0 || cantidad <= 0) {
                HttpJsonSupport.sendJson(he, 400,
                        HttpJsonSupport.jsonStatus("NOK", "id_evento, id_tipo_ticket y cantidad son obligatorios"));
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
            HttpJsonSupport.sendJson(he, 201, resp);
        } catch (IllegalArgumentException e) {
            error = true;
            HttpJsonSupport.sendJson(he, 409, HttpJsonSupport.jsonStatus("NOK", e.getMessage()));
        } catch (Exception e) {
            error = true;
            log.error("Error en /tickets", e);
            HttpJsonSupport.sendJson(he, 500, HttpJsonSupport.jsonStatus("NOK", "Error en /tickets"));
        } finally {
            MonitorStore.getInstance().record("/tickets", start, error);
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            log.info("Finished {} {} in {} ms (error={})", method, path, elapsedMs, error);
        }
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
}
