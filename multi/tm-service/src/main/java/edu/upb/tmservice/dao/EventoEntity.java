package edu.upb.tmservice.dao;

import java.sql.Timestamp;

public class EventoEntity {
    private final long id;
    private final String nombre;
    private final Timestamp fecha;
    private final int capacidad;
    private final boolean descuentoFrecuente;

    public EventoEntity(long id, String nombre, Timestamp fecha, int capacidad, boolean descuentoFrecuente) {
        this.id = id;
        this.nombre = nombre;
        this.fecha = fecha;
        this.capacidad = capacidad;
        this.descuentoFrecuente = descuentoFrecuente;
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

    public boolean isDescuentoFrecuente() {
        return descuentoFrecuente;
    }
}
