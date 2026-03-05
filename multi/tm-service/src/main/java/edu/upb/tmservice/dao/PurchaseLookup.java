package edu.upb.tmservice.dao;

public class PurchaseLookup {
    private final long firstTicketId;
    private final int total;

    public PurchaseLookup(long firstTicketId, int total) {
        this.firstTicketId = firstTicketId;
        this.total = total;
    }

    public long getFirstTicketId() {
        return firstTicketId;
    }

    public int getTotal() {
        return total;
    }
}
