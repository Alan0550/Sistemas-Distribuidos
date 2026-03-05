package edu.upb.tmservice.dao;

import java.sql.Timestamp;

public class EventoEntity {
    private final long id;
    private final String nombre;
    private final Timestamp fecha;
    private final int capacidad;

    public EventoEntity(long id, String nombre, Timestamp fecha, int capacidad) {
        this.id = id;
        this.nombre = nombre;
        this.fecha = fecha;
        this.capacidad = capacidad;
    }

    public long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public Timestamp getFecha() {
        return fecha;
    }

    public int getCapacidad() {
        return capacidad;
    }
}
