package org.campus.campussharebackend.schedule.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

/**
 * 新增开放安排请求。resourceId 由路径 /admin/resources/{resourceId}/openings 提供。
 */
public record OpeningCreateRequest(
        @NotNull OffsetDateTime startTime,
        @NotNull OffsetDateTime endTime,
        @Size(max = 200) String note) {
}
