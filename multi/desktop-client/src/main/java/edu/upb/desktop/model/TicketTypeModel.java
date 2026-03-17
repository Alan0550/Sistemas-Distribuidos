package edu.upb.desktop.model;

import java.math.BigDecimal;

public class TicketTypeModel {
    private final long id;
    private final String tipoAsiento;
    private final int cantidad;
    private final BigDecimal precio;
    private final BigDecimal precioBase;
    private final BigDecimal descuentoPorcentaje;

    public TicketTypeModel(long id, String tipoAsiento, int cantidad, BigDecimal precio, BigDecimal precioBase,
            BigDecimal descuentoPorcentaje) {
        this.id = id;
        this.tipoAsiento = tipoAsiento;
        this.cantidad = cantidad;
        this.precio = precio;
        this.precioBase = precioBase;
        this.descuentoPorcentaje = descuentoPorcentaje;
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

    public BigDecimal getPrecioBase() {
        return precioBase;
    }

    public BigDecimal getDescuentoPorcentaje() {
        return descuentoPorcentaje;
    }

    @Override
    public String toString() {
        if (descuentoPorcentaje != null && descuentoPorcentaje.compareTo(BigDecimal.ZERO) > 0
                && precioBase != null && precioBase.compareTo(precio) != 0) {
            return tipoAsiento + " | disponibles: " + cantidad + " | Bs " + precio + " (" + descuentoPorcentaje
                    + "% desc., base Bs " + precioBase + ")";
        }
        return tipoAsiento + " | disponibles: " + cantidad + " | Bs " + precio;
    }
}
