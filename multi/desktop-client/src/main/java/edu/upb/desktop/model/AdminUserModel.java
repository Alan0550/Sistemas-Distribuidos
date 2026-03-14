package edu.upb.desktop.model;

import java.util.List;

public class AdminUserModel {
    private final long id;
    private final String username;
    private final String nombre;
    private final String rol;
    private final List<UserTicketHistoryModel> historial;

    public AdminUserModel(long id, String username, String nombre, String rol, List<UserTicketHistoryModel> historial) {
        this.id = id;
        this.username = username;
        this.nombre = nombre;
        this.rol = rol;
        this.historial = historial;
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getNombre() {
        return nombre;
    }

    public String getRol() {
        return rol;
    }

    public List<UserTicketHistoryModel> getHistorial() {
        return historial;
    }
}
