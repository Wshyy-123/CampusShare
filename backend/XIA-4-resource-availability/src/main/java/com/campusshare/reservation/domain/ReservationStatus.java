package com.campusshare.reservation.domain;

public enum ReservationStatus {
    RESERVED,
    CHECKED_IN,
    COMPLETED,
    CANCELLED,
    EXPIRED;

    public boolean blocksAvailability() {
        return this == RESERVED || this == CHECKED_IN;
    }
}
