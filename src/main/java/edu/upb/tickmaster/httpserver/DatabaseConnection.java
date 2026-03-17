package edu.upb.tickmaster.httpserver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Deprecated
public class DatabaseConnection {
    private static DatabaseConnection instance;
    private final String url;
    private final String user;
    private final String password;

    private DatabaseConnection() {
        this.url = System.getenv().getOrDefault("TM_DB_URL", "jdbc:mysql://localhost:3306/sis_distribuidos");
        this.user = System.getenv().getOrDefault("TM_DB_USER", "root");
        this.password = System.getenv().getOrDefault("TM_DB_PASS", "12345689");
        loadJdbcDriver();
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    private void loadJdbcDriver() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("MySQL JDBC driver not found", e);
        }
    }
}
