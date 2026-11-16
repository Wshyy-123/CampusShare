package com.campusshare.venue;

public enum ResourceType {

    PING_PONG_TABLE("乒乓球桌", BookingUnit.TABLE),
    BADMINTON_COURT("羽毛球场", BookingUnit.COURT),
    TENNIS_COURT("网球场", BookingUnit.COURT),
    LIBRARY_SEAT("图书馆座位", BookingUnit.SEAT),
    DISCUSSION_ROOM("讨论室", BookingUnit.ROOM),
    CLASSROOM("教室", BookingUnit.ROOM);

    private final String label;
    private final BookingUnit bookingUnit;

    ResourceType(String label, BookingUnit bookingUnit) {
        this.label = label;
        this.bookingUnit = bookingUnit;
    }

    public String getLabel() {
        return label;
    }

    public BookingUnit getBookingUnit() {
        return bookingUnit;
    }
}
