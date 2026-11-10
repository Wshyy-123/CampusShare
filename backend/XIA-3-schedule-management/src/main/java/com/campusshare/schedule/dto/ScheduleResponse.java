package com.campusshare.schedule.dto;

import com.campusshare.schedule.domain.SchedulePeriodType;
import java.time.OffsetDateTime;

public record ScheduleResponse(
        Long id,
        Long unitId,
        SchedulePeriodType type,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String note) {
}
