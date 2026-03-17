package edu.upb.desktop.model;

import java.util.List;

public class EventModel {
    private final long id;
    private final String nombre;
    private final String fecha;
    private final int capacidad;
    private final boolean descuentoFrecuente;
    private final List<TicketTypeModel> ticketTypes;

    public EventModel(long id, String nombre, String fecha, int capacidad, boolean descuentoFrecuente,
            List<TicketTypeModel> ticketTypes) {
        this.id = id;
        this.nombre = nombre;
        this.fecha = fecha;
        this.capacidad = capacidad;
        this.descuentoFrecuente = descuentoFrecuente;
        this.ticketTypes = ticketTypes;
    }

    public long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getFecha() {
        return fecha;
    }

    public int getCapacidad() {
        return capacidad;
    }

    public boolean isDescuentoFrecuente() {
        return descuentoFrecuente;
    }

    public List<TicketTypeModel> getTicketTypes() {
        return ticketTypes;
    }

    public int getUsedCapacity() {
        int total = 0;
        for (TicketTypeModel type : ticketTypes) {
            total += type.getCantidad();
        }
        return total;
    }

    public int getRemainingCapacity() {
        return Math.max(0, capacidad - getUsedCapacity());
    }
}
