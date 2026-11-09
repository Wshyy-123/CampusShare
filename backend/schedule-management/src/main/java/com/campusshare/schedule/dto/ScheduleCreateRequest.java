package com.campusshare.schedule.dto;

import com.campusshare.schedule.domain.SchedulePeriodType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;

public record ScheduleCreateRequest(
        @NotEmpty(message = "至少选择一个预约单位")
        List<Long> unitIds,

        @NotNull(message = "type 不能为空")
        SchedulePeriodType type,

        @NotNull(message = "startAt 不能为空")
        OffsetDateTime startAt,

        @NotNull(message = "endAt 不能为空")
        OffsetDateTime endAt,

        @Size(max = 200, message = "note 最多 200 个字符")
        String note) {
}
