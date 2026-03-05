package edu.upb.tmservice.dao;

import edu.upb.tmservice.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDao {
    public UsuarioEntity findPublicByUsername(String username) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT id, username, nombre, rol FROM usuarios WHERE username = ? LIMIT 1")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new UsuarioEntity(
                            rs.getLong("id"),
                            rs.getString("username"),
                            rs.getString("nombre"),
                            rs.getString("rol"),
                            null);
                }
            }
        }
        return null;
    }

    public UsuarioEntity findAuthByUsername(String username) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT id, username, nombre, rol, password FROM usuarios WHERE username = ? LIMIT 1")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new UsuarioEntity(
                            rs.getLong("id"),
                            rs.getString("username"),
                            rs.getString("nombre"),
                            rs.getString("rol"),
                            rs.getString("password"));
                }
            }
        }
        return null;
    }

    public List<UsuarioEntity> listPublicUsers() throws SQLException {
        List<UsuarioEntity> users = new ArrayList<UsuarioEntity>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "SELECT id, username, nombre, rol FROM usuarios ORDER BY id");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(new UsuarioEntity(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("nombre"),
                        rs.getString("rol"),
                        null));
            }
        }
        return users;
    }

    public long createUser(String username, String nombre, String passwordHash, String rol) throws SQLException {
        long id = 0;
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO usuarios (username, nombre, password, rol) VALUES (?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, nombre);
            ps.setString(3, passwordHash);
            ps.setString(4, rol);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    id = keys.getLong(1);
                }
            }
        }
        return id;
    }

    public void updatePasswordHash(long userId, String passwordHash) throws SQLException {
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
                PreparedStatement ps = conn.prepareStatement(
                        "UPDATE usuarios SET password = ? WHERE id = ?")) {
            ps.setString(1, passwordHash);
            ps.setLong(2, userId);
            ps.executeUpdate();
        }
    }

    public boolean existsById(Connection conn, long userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM usuarios WHERE id = ? LIMIT 1")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
