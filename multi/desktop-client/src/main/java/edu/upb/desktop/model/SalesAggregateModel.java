package edu.upb.desktop.model;

import java.math.BigDecimal;

public class SalesAggregateModel {
    private final long id;
    private final String label;
    private final int ticketsSold;
    private final BigDecimal revenue;

    public SalesAggregateModel(long id, String label, int ticketsSold, BigDecimal revenue) {
        this.id = id;
        this.label = label;
        this.ticketsSold = ticketsSold;
        this.revenue = revenue;
    }

    public long getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public int getTicketsSold() {
        return ticketsSold;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }
}
