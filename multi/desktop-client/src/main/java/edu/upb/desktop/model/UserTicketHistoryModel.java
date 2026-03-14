package edu.upb.desktop.model;

public class UserTicketHistoryModel {
    private final long ticketId;
    private final String eventName;
    private final String eventDate;
    private final String seatType;
    private final String seatNumber;
    private final String price;

    public UserTicketHistoryModel(long ticketId, String eventName, String eventDate, String seatType,
            String seatNumber, String price) {
        this.ticketId = ticketId;
        this.eventName = eventName;
        this.eventDate = eventDate;
        this.seatType = seatType;
        this.seatNumber = seatNumber;
        this.price = price;
    }

    public long getTicketId() {
        return ticketId;
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

    public String getPrice() {
        return price;
    }
}
