package edu.upb.tmservice.dao;

import edu.upb.tmservice.DatabaseConnection;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class TicketDao {
    public PurchaseLookup findExistingPurchase(Connection conn, String baseKey) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT MIN(id) AS first_ticket_id, COUNT(*) AS total FROM tickets WHERE idempotency_key LIKE ?")) {
            ps.setString(1, baseKey + "#%");
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next() && rs.getInt("total") > 0) {
                    return new PurchaseLookup(
                            rs.getLong("first_ticket_id"),
                            rs.getInt("total"));
                }
            }
        }
        return null;
    }

    public long insertTicket(Connection conn, long eventId, long userId, String seat, BigDecimal finalPrice,
            BigDecimal basePrice, BigDecimal descuentoPorcentaje, String requestKey, long tipoTicketId)
            throws SQLException {
        long ticketId = 0;
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO tickets (id_evento, id_usuario, nro_asiento, precio, precio_base, descuento_porcentaje, estado, idempotency_key, id_tipo_ticket) VALUES (?,?,?,?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, eventId);
            ps.setLong(2, userId);
            ps.setString(3, seat);
            ps.setBigDecimal(4, finalPrice);
            ps.setBigDecimal(5, basePrice);
            ps.setBigDecimal(6, descuentoPorcentaje);
            ps.setString(7, "ACTIVO");
            ps.setString(8, requestKey);
            ps.setLong(9, tipoTicketId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    ticketId = keys.getLong(1);
                }
            }
        }
        return ticketId;
    }

    public List<TicketHistoryEntity> listHistoryByUserId(long userId) throws SQLException {
        List<TicketHistoryEntity> history = new ArrayList<TicketHistoryEntity>();
        String sql = "SELECT t.id, t.id_evento, e.nombre AS evento_nombre, e.fecha AS evento_fecha, " +
                "t.nro_asiento, tt.tipo_asiento, t.precio " +
                "FROM tickets t " +
                "JOIN eventos e ON e.id = t.id_evento " +
                "JOIN tipo_ticket tt ON tt.id = t.id_tipo_ticket " +
                "WHERE t.id_usuario = ? AND t.estado = 'ACTIVO' " +
                "ORDER BY e.fecha DESC, t.id DESC";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    history.add(new TicketHistoryEntity(
                            rs.getLong("id"),
                            rs.getLong("id_evento"),
                            rs.getString("evento_nombre"),
                            rs.getTimestamp("evento_fecha"),
                            rs.getString("nro_asiento"),
                            rs.getString("tipo_asiento"),
                            rs.getBigDecimal("precio")));
                }
            }
        }
        return history;
    }

    public int countPurchasedTicketsByUser(Connection conn, long userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM tickets WHERE id_usuario = ?")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public List<TicketReleaseItem> loadFutureActiveTicketReleaseByUser(Connection conn, long userId) throws SQLException {
        List<TicketReleaseItem> items = new ArrayList<TicketReleaseItem>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT t.id_tipo_ticket, COUNT(*) AS total " +
                        "FROM tickets t " +
                        "JOIN eventos e ON e.id = t.id_evento " +
                        "WHERE t.id_usuario = ? AND t.estado = 'ACTIVO' AND e.fecha > CURRENT_TIMESTAMP " +
                        "GROUP BY t.id_tipo_ticket")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(new TicketReleaseItem(
                            rs.getLong("id_tipo_ticket"),
                            rs.getInt("total")));
                }
            }
        }
        return items;
    }

    public int cancelFutureActiveTicketsByUser(Connection conn, long userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE tickets t " +
                        "JOIN eventos e ON e.id = t.id_evento " +
                        "SET t.estado = 'CANCELADO' " +
                        "WHERE t.id_usuario = ? AND t.estado = 'ACTIVO' AND e.fecha > CURRENT_TIMESTAMP")) {
            ps.setLong(1, userId);
            return ps.executeUpdate();
        }
    }
}
