package org.campus.campussharebackend.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

/**
 * 修改时段安排请求。type 必须与记录现有类型一致，不支持把开放时段改为教学占用或反之。
 */
public record ScheduleUpdateRequest(
        @NotBlank String type,
        @NotNull OffsetDateTime startTime,
        @NotNull OffsetDateTime endTime,
        @Size(max = 200) String note) {
}
