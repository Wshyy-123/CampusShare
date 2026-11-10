package com.campusshare.venue;

public enum BookingUnit {

    TABLE("桌"),
    COURT("场"),
    SEAT("座位"),
    ROOM("整间房");

    private final String label;

    BookingUnit(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
