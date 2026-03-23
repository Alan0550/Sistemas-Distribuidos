package edu.upb.tmservice.dao;

import java.math.BigDecimal;

public class TipoTicketEntity {
    private final long id;
    private final long idEvento;
    private final String tipoAsiento;
    private final int cantidad;
    private final BigDecimal precio;
    private final int proximoAsiento;

    public TipoTicketEntity(long id, long idEvento, String tipoAsiento, int cantidad, BigDecimal precio,
            int proximoAsiento) {
        this.id = id;
        this.idEvento = idEvento;
        this.tipoAsiento = tipoAsiento;
        this.cantidad = cantidad;
        this.precio = precio;
        this.proximoAsiento = proximoAsiento;
    }

    public long getId() {
        return id;
    }

    public long getIdEvento() {
        return idEvento;
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

    public int getProximoAsiento() {
        return proximoAsiento;
    }
}
