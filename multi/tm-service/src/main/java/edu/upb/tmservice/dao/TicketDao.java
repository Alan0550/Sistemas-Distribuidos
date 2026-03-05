package edu.upb.tmservice.dao;

import java.math.BigDecimal;
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

    public long insertTicket(Connection conn, long eventId, long userId, String seat, BigDecimal price,
            String requestKey, long tipoTicketId) throws SQLException {
        long ticketId = 0;
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO tickets (id_evento, id_usuario, nro_asiento, precio, idempotency_key, id_tipo_ticket) VALUES (?,?,?,?,?,?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, eventId);
            ps.setLong(2, userId);
            ps.setString(3, seat);
            ps.setBigDecimal(4, price);
            ps.setString(5, requestKey);
            ps.setLong(6, tipoTicketId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    ticketId = keys.getLong(1);
                }
            }
        }
        return ticketId;
    }
}
