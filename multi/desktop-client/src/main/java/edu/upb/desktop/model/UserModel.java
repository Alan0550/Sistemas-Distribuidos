package edu.upb.desktop.model;

public class UserModel {
    private final long id;
    private final String username;
    private final String nombre;
    private final String rol;

    public UserModel(long id, String username, String nombre, String rol) {
        this.id = id;
        this.username = username;
        this.nombre = nombre;
        this.rol = rol;
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
}
