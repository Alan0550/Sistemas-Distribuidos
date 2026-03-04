package edu.upb.desktop.model;

public class PurchaseResultModel {
    private final long primerTicketId;
    private final int ticketsCreados;
    private final String status;
    private final String mensaje;

    public PurchaseResultModel(long primerTicketId, int ticketsCreados, String status, String mensaje) {
        this.primerTicketId = primerTicketId;
        this.ticketsCreados = ticketsCreados;
        this.status = status;
        this.mensaje = mensaje;
    }

    public long getPrimerTicketId() {
        return primerTicketId;
    }

    public int getTicketsCreados() {
        return ticketsCreados;
    }

    public String getStatus() {
        return status;
    }

    public String getMensaje() {
        return mensaje;
    }
}
