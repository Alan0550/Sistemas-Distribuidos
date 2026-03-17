package edu.upb.tmservice.dao;

public class UsuarioEntity {
    private final long id;
    private final String username;
    private final String nombre;
    private final String rol;
    private final String password;
    private final boolean baneado;

    public UsuarioEntity(long id, String username, String nombre, String rol, String password, boolean baneado) {
        this.id = id;
        this.username = username;
        this.nombre = nombre;
        this.rol = rol;
        this.password = password;
        this.baneado = baneado;
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

    public String getPassword() {
        return password;
    }

    public boolean isBaneado() {
        return baneado;
    }
}
