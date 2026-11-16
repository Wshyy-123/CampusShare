package com.campusshare.availability.service;

import java.time.LocalDateTime;

public record TimeInterval(LocalDateTime startAt, LocalDateTime endAt) {

    public TimeInterval {
        if (startAt == null || endAt == null || !startAt.isBefore(endAt)) {
            throw new IllegalArgumentException("时间区间必须满足 startAt < endAt");
        }
    }

    public TimeInterval clip(LocalDateTime lower, LocalDateTime upper) {
        LocalDateTime start = startAt.isBefore(lower) ? lower : startAt;
        LocalDateTime end = endAt.isAfter(upper) ? upper : endAt;
        return start.isBefore(end) ? new TimeInterval(start, end) : null;
    }
}
