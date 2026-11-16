package org.campus.campussharebackend.resource.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * XIA-2 资源管理员新增/修改资源的请求体。字段与 resource 表及
 * docs/api/admin-resources.yaml 一致；开放安排与占用维护仍由 XIA-3 负责。
 */
public record ResourceSaveRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Size(max = 100) String location,
        @NotBlank @Pattern(
                regexp = "LIBRARY_SEAT|CLASSROOM|BADMINTON|TABLE_TENNIS|TENNIS|DISCUSSION_ROOM",
                message = "不支持该资源类型") String resourceType,
        @NotBlank @Size(max = 50) String unitLabel,
        @NotNull Boolean enabled) {
}
