package edu.upb.desktop.model;

import java.math.BigDecimal;

public class AdminTicketSaleModel {
    private final long ticketId;
    private final long eventId;
    private final String username;
    private final String fullName;
    private final String eventName;
    private final String eventDate;
    private final String seatType;
    private final String seatNumber;
    private final BigDecimal price;

    public AdminTicketSaleModel(long ticketId, long eventId, String username, String fullName, String eventName,
            String eventDate, String seatType, String seatNumber, BigDecimal price) {
        this.ticketId = ticketId;
        this.eventId = eventId;
        this.username = username;
        this.fullName = fullName;
        this.eventName = eventName;
        this.eventDate = eventDate;
        this.seatType = seatType;
        this.seatNumber = seatNumber;
        this.price = price;
    }

    public long getTicketId() {
        return ticketId;
    }

    public long getEventId() {
        return eventId;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEventName() {
        return eventName;
    }

    public String getEventDate() {
        return eventDate;
    }

    public String getSeatType() {
        return seatType;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public BigDecimal getPrice() {
        return price;
    }
}
