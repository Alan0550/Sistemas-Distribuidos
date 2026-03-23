package edu.upb.tmservice.dao;

import edu.upb.tmservice.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class TipoTicketDao {
    public List<TipoTicketEntity> listByEventId(long eventId) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            return listByEventId(conn, eventId);
        }
    }

    public List<TipoTicketEntity> listByEventId(Connection conn, long eventId) throws SQLException {
        List<TipoTicketEntity> types = new ArrayList<TipoTicketEntity>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, id_evento, tipo_asiento, cantidad, precio, COALESCE(proximo_asiento, 1) AS proximo_asiento FROM tipo_ticket WHERE id_evento = ? ORDER BY id")) {
            ps.setLong(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    types.add(new TipoTicketEntity(
                            rs.getLong("id"),
                            rs.getLong("id_evento"),
                            rs.getString("tipo_asiento"),
                            rs.getInt("cantidad"),
                            rs.getBigDecimal("precio"),
                            rs.getInt("proximo_asiento")));
                }
            }
        }
        return types;
    }

    public long create(Connection conn, long eventId, String tipoAsiento, int cantidad, BigDecimal precio) throws SQLException {
        long typeId = 0;
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO tipo_ticket (id_evento, tipo_asiento, cantidad, precio, proximo_asiento) VALUES (?,?,?,?,1)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, eventId);
            ps.setString(2, tipoAsiento);
            ps.setInt(3, cantidad);
            ps.setBigDecimal(4, precio);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    typeId = keys.getLong(1);
                }
            }
        }
        return typeId;
    }

    public int loadAssignedCapacity(Connection conn, long eventId) throws SQLException {
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

    public TipoTicketEntity lockByIdAndEvent(Connection conn, long tipoTicketId, long eventId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, id_evento, tipo_asiento, cantidad, precio, COALESCE(proximo_asiento, 1) AS proximo_asiento FROM tipo_ticket WHERE id = ? AND id_evento = ? FOR UPDATE")) {
            ps.setLong(1, tipoTicketId);
            ps.setLong(2, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new TipoTicketEntity(
                            rs.getLong("id"),
                            rs.getLong("id_evento"),
                            rs.getString("tipo_asiento"),
                            rs.getInt("cantidad"),
                            rs.getBigDecimal("precio"),
                            rs.getInt("proximo_asiento"));
                }
            }
        }
        return null;
    }

    public boolean existsByEventAndSeatType(Connection conn, long eventId, String tipoAsiento) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id FROM tipo_ticket WHERE id_evento = ? AND UPPER(TRIM(tipo_asiento)) = ? LIMIT 1")) {
            ps.setLong(1, eventId);
            ps.setString(2, tipoAsiento == null ? "" : tipoAsiento.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void reserveInventoryAndAdvanceSequence(Connection conn, long tipoTicketId, int cantidad) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE tipo_ticket SET cantidad = cantidad - ?, proximo_asiento = proximo_asiento + ? WHERE id = ?")) {
            ps.setInt(1, cantidad);
            ps.setInt(2, cantidad);
            ps.setLong(3, tipoTicketId);
            ps.executeUpdate();
        }
    }

    public void increaseAvailable(Connection conn, long tipoTicketId, int cantidad) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE tipo_ticket SET cantidad = cantidad + ? WHERE id = ?")) {
            ps.setInt(1, cantidad);
            ps.setLong(2, tipoTicketId);
            ps.executeUpdate();
        }
    }
}
