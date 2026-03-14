package edu.upb.tmservice.dao;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class TicketHistoryEntity {
    private final long ticketId;
    private final long eventId;
    private final String eventName;
    private final Timestamp eventDate;
    private final String seatNumber;
    private final String seatType;
    private final BigDecimal price;

    public TicketHistoryEntity(long ticketId, long eventId, String eventName, Timestamp eventDate,
            String seatNumber, String seatType, BigDecimal price) {
        this.ticketId = ticketId;
        this.eventId = eventId;
        this.eventName = eventName;
        this.eventDate = eventDate;
        this.seatNumber = seatNumber;
        this.seatType = seatType;
        this.price = price;
    }

    public long getTicketId() {
        return ticketId;
    }

    public long getEventId() {
        return eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public Timestamp getEventDate() {
        return eventDate;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public String getSeatType() {
        return seatType;
    }

    public BigDecimal getPrice() {
        return price;
    }
}
