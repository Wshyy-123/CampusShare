package com.campusshare.availability.dto;

import java.time.OffsetDateTime;

public record TimeRangeResponse(
        OffsetDateTime startAt,
        OffsetDateTime endAt) {
}
