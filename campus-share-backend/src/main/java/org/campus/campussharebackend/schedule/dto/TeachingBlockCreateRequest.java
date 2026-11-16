package org.campus.campussharebackend.schedule.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

/**
 * 新增教学占用请求。resourceId 由路径 /admin/resources/{resourceId}/teaching-blocks 提供。
 */
public record TeachingBlockCreateRequest(
        @NotNull OffsetDateTime startTime,
        @NotNull OffsetDateTime endTime,
        @Size(max = 200) String reason) {
}
