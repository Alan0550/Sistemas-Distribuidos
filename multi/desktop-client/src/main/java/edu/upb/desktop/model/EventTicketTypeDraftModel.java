package edu.upb.desktop.model;

import java.math.BigDecimal;

public class EventTicketTypeDraftModel {
    private final String seatType;
    private final int quantity;
    private final BigDecimal price;

    public EventTicketTypeDraftModel(String seatType, int quantity, BigDecimal price) {
        this.seatType = seatType;
        this.quantity = quantity;
        this.price = price;
    }

    public String getSeatType() {
        return seatType;
    }

    public int getQuantity() {
        return quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }
}
