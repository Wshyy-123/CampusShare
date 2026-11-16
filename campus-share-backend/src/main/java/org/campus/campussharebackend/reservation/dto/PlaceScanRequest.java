package org.campus.campussharebackend.reservation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotNull;
public record PlaceScanRequest(@NotBlank @Pattern(regexp = "^[1-9][0-9]*$") String resourceId,
                               @NotNull Source source) {
    public enum Source { QR }
}
