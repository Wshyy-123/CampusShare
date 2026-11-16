package com.campusshare.schedule.dto;

import com.campusshare.schedule.domain.SchedulePeriodType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record ScheduleUpdateRequest(
        @NotNull(message = "type 不能为空")
        SchedulePeriodType type,

        @NotNull(message = "startAt 不能为空")
        OffsetDateTime startAt,

        @NotNull(message = "endAt 不能为空")
        OffsetDateTime endAt,

        @Size(max = 200, message = "note 最多 200 个字符")
        String note) {
}
