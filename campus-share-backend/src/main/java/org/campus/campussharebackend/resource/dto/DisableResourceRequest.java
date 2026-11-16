package org.campus.campussharebackend.resource.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record DisableResourceRequest(
        @NotNull OffsetDateTime startTime,
        @NotNull OffsetDateTime endTime,
        @NotBlank @Size(max = 500) String reason) {
}
