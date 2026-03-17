package edu.upb.tmservice.dao;

public class TicketReleaseItem {
    private final long tipoTicketId;
    private final int quantity;

    public TicketReleaseItem(long tipoTicketId, int quantity) {
        this.tipoTicketId = tipoTicketId;
        this.quantity = quantity;
    }

    public long getTipoTicketId() {
        return tipoTicketId;
    }

    public int getQuantity() {
        return quantity;
    }
}
