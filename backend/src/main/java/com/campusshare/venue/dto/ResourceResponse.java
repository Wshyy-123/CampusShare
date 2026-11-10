package com.campusshare.venue.dto;

import com.campusshare.venue.BookingUnit;
import com.campusshare.venue.ResourceType;

import java.time.LocalDateTime;

public record ResourceResponse(
        Long id,
        String name,
        String location,
        ResourceType type,
        String typeLabel,
        BookingUnit bookingUnit,
        String bookingUnitLabel,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
