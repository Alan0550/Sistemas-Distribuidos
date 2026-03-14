package edu.upb.tmservice.dao;

import edu.upb.tmservice.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class EventoDao {
    public List<EventoEntity> listEvents(String keyword) throws SQLException {
        List<EventoEntity> events = new ArrayList<EventoEntity>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            String sql = "SELECT id, nombre, fecha, capacidad FROM eventos";
            boolean hasKeyword = keyword != null && !keyword.trim().isEmpty();
            if (hasKeyword) {
                sql += " WHERE LOWER(nombre) LIKE ?";
            }
            sql += " ORDER BY fecha";

            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                if (hasKeyword) {
                    ps.setString(1, "%" + keyword.toLowerCase() + "%");
                }
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        events.add(new EventoEntity(
                                rs.getLong("id"),
                                rs.getString("nombre"),
                                rs.getTimestamp("fecha"),
                                rs.getInt("capacidad")));
                    }
                }
            }
        }
        return events;
    }

    public long createEvent(String nombre, Timestamp fecha, int capacidad) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection()) {
            return createEvent(conn, nombre, fecha, capacidad);
        }
    }

    public long createEvent(Connection conn, String nombre, Timestamp fecha, int capacidad) throws SQLException {
        long eventId = 0;
        try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO eventos (nombre, fecha, capacidad) VALUES (?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre);
            ps.setTimestamp(2, fecha);
            ps.setInt(3, capacidad);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    eventId = keys.getLong(1);
                }
            }
        }
        return eventId;
    }

    public int loadEventCapacity(Connection conn, long eventId) throws SQLException {
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

    public boolean existsById(Connection conn, long eventId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM eventos WHERE id = ? LIMIT 1")) {
            ps.setLong(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
