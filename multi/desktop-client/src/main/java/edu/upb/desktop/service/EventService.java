package edu.upb.desktop.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.upb.desktop.model.EventModel;
import edu.upb.desktop.model.EventTicketTypeDraftModel;
import edu.upb.desktop.model.TicketTypeModel;
import edu.upb.desktop.util.SessionManager;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class EventService {
    private final String baseUrl = System.getenv().getOrDefault("LB_UI_BASE_URL", "http://localhost:1915/tm");

    public List<EventModel> fetchEvents() throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(baseUrl + "/events").openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(4000);
        conn.setReadTimeout(4000);
        applyAuthHeader(conn);

        if (conn.getResponseCode() != 200) {
            throw new IllegalStateException("No se pudo cargar eventos");
        }

        try (Reader reader = new java.io.InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
            JsonObject resp = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray arr = resp.getAsJsonArray("events");
            List<EventModel> events = new ArrayList<>();
            for (JsonElement el : arr) {
                JsonObject item = el.getAsJsonObject();
                List<TicketTypeModel> ticketTypes = new ArrayList<>();
                JsonArray types = item.getAsJsonArray("tipos_ticket");
                if (types != null) {
                    for (JsonElement typeEl : types) {
                        JsonObject type = typeEl.getAsJsonObject();
                        ticketTypes.add(new TicketTypeModel(
                                type.get("id").getAsLong(),
                                type.get("tipo_asiento").getAsString(),
                                type.get("cantidad").getAsInt(),
                                type.get("precio").getAsBigDecimal(),
                                readBigDecimal(type, "precio_base", type.get("precio").getAsBigDecimal()),
                                readBigDecimal(type, "descuento_porcentaje", BigDecimal.ZERO)));
                    }
                }
                events.add(new EventModel(
                        item.get("id").getAsLong(),
                        item.get("nombre").getAsString(),
                        item.get("fecha").getAsString(),
                        item.get("capacidad").getAsInt(),
                        item.has("descuento_frecuente") && item.get("descuento_frecuente").getAsBoolean(),
                        ticketTypes));
            }
            return events;
        }
    }

    public EventModel createEvent(String nombre, String fecha, int capacidad, boolean descuentoFrecuente) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("nombre", nombre);
        body.addProperty("fecha", fecha);
        body.addProperty("capacidad", capacidad);
        body.addProperty("descuento_frecuente", descuentoFrecuente);

        HttpURLConnection conn = openConnection(baseUrl + "/events", "POST");
        writeBody(conn, body.toString());

        if (conn.getResponseCode() != 201) {
            throw new IllegalArgumentException(readErrorMessage(conn, "No se pudo crear el evento"));
        }

        try (Reader reader = new java.io.InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
            JsonObject item = JsonParser.parseReader(reader).getAsJsonObject();
            return new EventModel(
                    item.get("id").getAsLong(),
                    item.get("nombre").getAsString(),
                    item.get("fecha").getAsString(),
                    item.get("capacidad").getAsInt(),
                    item.has("descuento_frecuente") && item.get("descuento_frecuente").getAsBoolean(),
                    new ArrayList<>());
        }
    }

    public TicketTypeModel addTicketType(long eventId, String tipoAsiento, int cantidad, BigDecimal precio) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("tipo_asiento", tipoAsiento);
        body.addProperty("cantidad", cantidad);
        body.addProperty("precio", precio);

        HttpURLConnection conn = openConnection(baseUrl + "/events/" + eventId + "/ticket-types", "POST");
        writeBody(conn, body.toString());

        if (conn.getResponseCode() != 201) {
            throw new IllegalArgumentException(readErrorMessage(conn, "No se pudo agregar el tipo de ticket"));
        }

        try (Reader reader = new java.io.InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
            JsonObject item = JsonParser.parseReader(reader).getAsJsonObject();
            return new TicketTypeModel(
                    item.get("id").getAsLong(),
                    item.get("tipo_asiento").getAsString(),
                    item.get("cantidad").getAsInt(),
                    item.get("precio").getAsBigDecimal(),
                    item.get("precio").getAsBigDecimal(),
                    BigDecimal.ZERO);
        }
    }

    public EventModel createFullEvent(String nombre, String fecha, int capacidad, boolean descuentoFrecuente,
            List<EventTicketTypeDraftModel> ticketTypes)
            throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("nombre", nombre);
        body.addProperty("fecha", fecha);
        body.addProperty("capacidad", capacidad);
        body.addProperty("descuento_frecuente", descuentoFrecuente);

        JsonArray types = new JsonArray();
        for (EventTicketTypeDraftModel draft : ticketTypes) {
            JsonObject type = new JsonObject();
            type.addProperty("tipo_asiento", draft.getSeatType());
            type.addProperty("cantidad", draft.getQuantity());
            type.addProperty("precio", draft.getPrice());
            types.add(type);
        }
        body.add("tipos_ticket", types);

        HttpURLConnection conn = openConnection(baseUrl + "/events/full", "POST");
        writeBody(conn, body.toString());

        if (conn.getResponseCode() != 201) {
            throw new IllegalArgumentException(readErrorMessage(conn, "No se pudo crear el evento completo"));
        }

        try (Reader reader = new java.io.InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8)) {
            JsonObject item = JsonParser.parseReader(reader).getAsJsonObject();
            List<TicketTypeModel> createdTypes = new ArrayList<TicketTypeModel>();
            JsonArray createdArray = item.getAsJsonArray("tipos_ticket");
            if (createdArray != null) {
                for (JsonElement el : createdArray) {
                    JsonObject type = el.getAsJsonObject();
                    createdTypes.add(new TicketTypeModel(
                            type.get("id").getAsLong(),
                            type.get("tipo_asiento").getAsString(),
                            type.get("cantidad").getAsInt(),
                            type.get("precio").getAsBigDecimal(),
                            type.get("precio").getAsBigDecimal(),
                            BigDecimal.ZERO));
                }
            }
            return new EventModel(
                    item.get("id").getAsLong(),
                    item.get("nombre").getAsString(),
                    item.get("fecha").getAsString(),
                    item.get("capacidad").getAsInt(),
                    item.has("descuento_frecuente") && item.get("descuento_frecuente").getAsBoolean(),
                    createdTypes);
        }
    }

    private BigDecimal readBigDecimal(JsonObject object, String key, BigDecimal fallback) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsBigDecimal() : fallback;
    }

    private HttpURLConnection openConnection(String target, String method) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(target).openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(4000);
        conn.setReadTimeout(4000);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        applyAuthHeader(conn);
        return conn;
    }

    private void applyAuthHeader(HttpURLConnection conn) {
        String token = SessionManager.getAuthToken();
        if (token != null && !token.trim().isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + token.trim());
        }
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
