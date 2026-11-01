package com.campusshare.lifecycle;

import java.time.LocalDateTime;

/** Shared with the attendance owner. Exactly at the deadline, arrival is too late. */
public final class SeatArrivalDeadline {
    private SeatArrivalDeadline() {}

    public static LocalDateTime deadline(LocalDateTime acceptedAt) { return acceptedAt.plusMinutes(30); }

    public static boolean expired(LocalDateTime acceptedAt, LocalDateTime now) {
        return !now.isBefore(deadline(acceptedAt));
    }
}
