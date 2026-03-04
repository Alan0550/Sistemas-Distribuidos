package edu.upb.desktop.model;

import java.math.BigDecimal;

public class TicketTypeModel {
    private final long id;
    private final String tipoAsiento;
    private final int cantidad;
    private final BigDecimal precio;

    public TicketTypeModel(long id, String tipoAsiento, int cantidad, BigDecimal precio) {
        this.id = id;
        this.tipoAsiento = tipoAsiento;
        this.cantidad = cantidad;
        this.precio = precio;
    }

    public long getId() {
        return id;
    }

    public String getTipoAsiento() {
        return tipoAsiento;
    }

    public int getCantidad() {
        return cantidad;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    @Override
    public String toString() {
        return tipoAsiento + " | disponibles: " + cantidad + " | Bs " + precio;
    }
}
