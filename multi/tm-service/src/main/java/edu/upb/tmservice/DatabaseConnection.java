package edu.upb.tmservice;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

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
        ensureSchema();
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

    private void ensureSchema() {
        String usuariosSql = "CREATE TABLE IF NOT EXISTS usuarios (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "username VARCHAR(80) NOT NULL UNIQUE," +
                "nombre VARCHAR(120) NOT NULL," +
                "password VARCHAR(255) NOT NULL," +
                "rol VARCHAR(50) NOT NULL" +
                ")";
        String eventosSql = "CREATE TABLE IF NOT EXISTS eventos (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "nombre VARCHAR(120) NOT NULL," +
                "fecha DATETIME NOT NULL," +
                "capacidad INT NOT NULL" +
                ")";
        String tiposSql = "CREATE TABLE IF NOT EXISTS tipo_ticket (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "id_evento INT NOT NULL," +
                "tipo_asiento VARCHAR(80) NOT NULL," +
                "cantidad INT NOT NULL," +
                "precio DECIMAL(10,2) NOT NULL," +
                "CONSTRAINT fk_tipo_ticket_evento FOREIGN KEY (id_evento) REFERENCES eventos(id) ON DELETE CASCADE" +
                ")";
        String ticketsSql = "CREATE TABLE IF NOT EXISTS tickets (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "id_evento INT NOT NULL," +
                "id_usuario INT NOT NULL," +
                "nro_asiento VARCHAR(30) NOT NULL," +
                "precio DECIMAL(10,2) NOT NULL," +
                "idempotency_key VARCHAR(120) NOT NULL UNIQUE," +
                "id_tipo_ticket INT NOT NULL," +
                "CONSTRAINT fk_ticket_evento FOREIGN KEY (id_evento) REFERENCES eventos(id)," +
                "CONSTRAINT fk_ticket_usuario FOREIGN KEY (id_usuario) REFERENCES usuarios(id)," +
                "CONSTRAINT fk_ticket_tipo FOREIGN KEY (id_tipo_ticket) REFERENCES tipo_ticket(id)" +
                ")";
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement st = conn.createStatement()) {
            st.execute(usuariosSql);
            st.execute(eventosSql);
            st.execute(tiposSql);
            st.execute(ticketsSql);
        } catch (SQLException ignored) {
        }
    }

    private void loadJdbcDriver() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ignored) {
        }
    }
}
