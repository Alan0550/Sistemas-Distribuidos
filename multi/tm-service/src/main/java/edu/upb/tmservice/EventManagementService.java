package edu.upb.tmservice;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import edu.upb.tmservice.dao.EventoDao;
import edu.upb.tmservice.dao.EventoEntity;
import edu.upb.tmservice.dao.TipoTicketDao;
import edu.upb.tmservice.dao.TipoTicketEntity;
import edu.upb.tmservice.dao.UsuarioDao;
import edu.upb.tmservice.dao.UsuarioEntity;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class EventManagementService {
    private static final BigDecimal TEN_PERCENT = new BigDecimal("10.00");
    private final EventoDao eventoDao = new EventoDao();
    private final TipoTicketDao tipoTicketDao = new TipoTicketDao();
    private final UsuarioDao usuarioDao = new UsuarioDao();

    public JsonObject listEvents(String keyword, int backendPort, long userId, String sessionRole) throws Exception {
        String search = keyword == null ? "" : keyword.trim();
        JsonArray arr = new JsonArray();

        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            String effectiveRole = sessionRole;
            UsuarioEntity currentUser = usuarioDao.findById(conn, userId);
            if (currentUser != null) {
                effectiveRole = currentUser.getRol();
            }
            boolean includePast = "ADMIN".equalsIgnoreCase(effectiveRole);
            List<EventoEntity> events = eventoDao.listEvents(search, includePast);
            for (EventoEntity event : events) {
                JsonObject ev = new JsonObject();
                ev.addProperty("id", event.getId());
                ev.addProperty("nombre", event.getNombre());
                Timestamp fecha = event.getFecha();
                ev.addProperty("fecha", fecha != null ? fecha.toString() : "");
                ev.addProperty("capacidad", event.getCapacidad());
                ev.addProperty("descuento_frecuente", event.isDescuentoFrecuente());
                ev.add("tipos_ticket", loadTiposTicket(conn, event, effectiveRole));
                arr.add(ev);
            }
        }

        JsonObject resp = new JsonObject();
        resp.addProperty("backend", String.valueOf(backendPort));
        resp.add("events", arr);
        return resp;
    }

    public JsonObject createEvent(String nombre, String fecha, int capacidad, boolean descuentoFrecuente) throws Exception {
        String normalizedName = requireText(nombre, "Faltan datos del evento");
        Timestamp fechaTs = parseFecha(fecha);
        if (capacidad <= 0) {
            throw new IllegalArgumentException("Faltan datos del evento");
        }
        validateFutureFecha(fechaTs);

        long eventId = eventoDao.createEvent(normalizedName, fechaTs, capacidad, descuentoFrecuente);
        JsonObject resp = new JsonObject();
        resp.addProperty("status", "OK");
        resp.addProperty("id", eventId);
        resp.addProperty("nombre", normalizedName);
        resp.addProperty("fecha", fechaTs.toString());
        resp.addProperty("capacidad", capacidad);
        resp.addProperty("descuento_frecuente", descuentoFrecuente);
        return resp;
    }

    public JsonObject createTicketType(long eventId, String tipoAsiento, int cantidad, BigDecimal precio) throws Exception {
        if (eventId <= 0) {
            throw new IllegalArgumentException("Evento invalido");
        }

        String seatType = normalizeSeatType(tipoAsiento, "Datos invalidos para el tipo de ticket");
        if (cantidad <= 0 || precio == null || precio.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Datos invalidos para el tipo de ticket");
        }

        long ticketTypeId;
        int remainingCapacity;
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                int eventCapacity = eventoDao.loadEventCapacity(conn, eventId);
                if (eventCapacity <= 0) {
                    throw new IllegalArgumentException("El evento no existe");
                }
                if (tipoTicketDao.existsByEventAndSeatType(conn, eventId, seatType)) {
                    throw new IllegalArgumentException("Ya existe un tipo de ticket con ese nombre para el evento");
                }

                int usedCapacity = tipoTicketDao.loadAssignedCapacity(conn, eventId);
                remainingCapacity = eventCapacity - usedCapacity;
                if (cantidad > remainingCapacity) {
                    throw new IllegalArgumentException(
                            "Solo quedan " + Math.max(remainingCapacity, 0) + " espacios disponibles");
                }

                ticketTypeId = tipoTicketDao.create(conn, eventId, seatType, cantidad, precio);
                conn.commit();
                remainingCapacity -= cantidad;
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
        resp.addProperty("tipo_asiento", seatType);
        resp.addProperty("cantidad", cantidad);
        resp.addProperty("precio", precio);
        resp.addProperty("capacidad_restante", remainingCapacity);
        return resp;
    }

    public JsonObject createFullEvent(String nombre, String fecha, int capacidad, boolean descuentoFrecuente, JsonArray tipos)
            throws Exception {
        String normalizedName = requireText(nombre, "Debes completar evento y tipos de ticket");
        Timestamp fechaTs = parseFecha(fecha);
        if (capacidad <= 0 || tipos == null || tipos.size() == 0) {
            throw new IllegalArgumentException("Debes completar evento y tipos de ticket");
        }
        validateFutureFecha(fechaTs);

        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            conn.setAutoCommit(false);
            try {
                int assignedCapacity = 0;
                Set<String> uniqueSeatTypes = new HashSet<String>();
                for (int i = 0; i < tipos.size(); i++) {
                    JsonObject tipo = tipos.get(i).getAsJsonObject();
                    String seatType = normalizeSeatType(readString(tipo, "tipo_asiento"),
                            "Todos los tipos de ticket deben ser validos");
                    int cantidad = readInt(tipo, "cantidad");
                    BigDecimal precio = readDecimal(tipo, "precio");

                    if (!uniqueSeatTypes.add(seatType)) {
                        throw new IllegalArgumentException("No puede haber tipos de ticket repetidos en el evento");
                    }
                    if (cantidad <= 0 || precio.compareTo(BigDecimal.ZERO) <= 0) {
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

                long eventId = eventoDao.createEvent(conn, normalizedName, fechaTs, capacidad, descuentoFrecuente);
                JsonArray tiposResp = new JsonArray();
                for (int i = 0; i < tipos.size(); i++) {
                    JsonObject tipo = tipos.get(i).getAsJsonObject();
                    String seatType = normalizeSeatType(tipo.get("tipo_asiento").getAsString(),
                            "Todos los tipos de ticket deben ser validos");
                    int cantidad = tipo.get("cantidad").getAsInt();
                    BigDecimal precio = tipo.get("precio").getAsBigDecimal();

                    long ticketTypeId = tipoTicketDao.create(conn, eventId, seatType, cantidad, precio);
                    JsonObject tipoResp = new JsonObject();
                    tipoResp.addProperty("id", ticketTypeId);
                    tipoResp.addProperty("tipo_asiento", seatType);
                    tipoResp.addProperty("cantidad", cantidad);
                    tipoResp.addProperty("precio", precio);
                    tiposResp.add(tipoResp);
                }
                conn.commit();

                JsonObject resp = new JsonObject();
                resp.addProperty("status", "OK");
                resp.addProperty("id", eventId);
                resp.addProperty("nombre", normalizedName);
                resp.addProperty("fecha", fechaTs.toString());
                resp.addProperty("capacidad", capacidad);
                resp.addProperty("descuento_frecuente", descuentoFrecuente);
                resp.add("tipos_ticket", tiposResp);
                return resp;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private JsonArray loadTiposTicket(Connection conn, EventoEntity event, String userRole) throws Exception {
        JsonArray arr = new JsonArray();
        List<TipoTicketEntity> types = tipoTicketDao.listByEventId(conn, event.getId());
        BigDecimal discountPercent = calculateDiscountPercent(userRole, event.isDescuentoFrecuente());
        for (TipoTicketEntity type : types) {
            BigDecimal finalPrice = applyDiscount(type.getPrecio(), discountPercent);
            JsonObject tipo = new JsonObject();
            tipo.addProperty("id", type.getId());
            tipo.addProperty("tipo_asiento", type.getTipoAsiento());
            tipo.addProperty("cantidad", type.getCantidad());
            tipo.addProperty("precio", finalPrice);
            tipo.addProperty("precio_base", type.getPrecio());
            tipo.addProperty("descuento_porcentaje", discountPercent);
            arr.add(tipo);
        }
        return arr;
    }

    private String requireText(String value, String message) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private String normalizeSeatType(String value, String message) {
        return requireText(value, message).toUpperCase(Locale.ROOT);
    }

    private Timestamp parseFecha(String fecha) {
        try {
            return Timestamp.valueOf(requireText(fecha, "La fecha debe tener formato yyyy-MM-dd HH:mm:ss"));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("La fecha debe tener formato yyyy-MM-dd HH:mm:ss");
        }
    }

    private void validateFutureFecha(Timestamp fecha) {
        if (fecha == null || !fecha.toInstant().isAfter(Instant.now())) {
            throw new IllegalArgumentException("La fecha del evento debe ser posterior al momento actual");
        }
    }

    private BigDecimal calculateDiscountPercent(String userRole, boolean descuentoFrecuente) {
        if ("VIP".equalsIgnoreCase(userRole)) {
            return TEN_PERCENT;
        }
        if ("FRECUENTE".equalsIgnoreCase(userRole) && descuentoFrecuente) {
            return TEN_PERCENT;
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal applyDiscount(BigDecimal basePrice, BigDecimal discountPercent) {
        if (basePrice == null || discountPercent.compareTo(BigDecimal.ZERO) <= 0) {
            return basePrice == null ? BigDecimal.ZERO : basePrice;
        }
        return basePrice.multiply(BigDecimal.valueOf(100).subtract(discountPercent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private String readString(JsonObject body, String key) {
        return body.has(key) && !body.get(key).isJsonNull() ? body.get(key).getAsString().trim() : "";
    }

    private int readInt(JsonObject body, String key) {
        return body.has(key) && !body.get(key).isJsonNull() ? body.get(key).getAsInt() : 0;
    }

    private BigDecimal readDecimal(JsonObject body, String key) {
        return body.has(key) && !body.get(key).isJsonNull() ? body.get(key).getAsBigDecimal() : BigDecimal.ZERO;
    }
}
