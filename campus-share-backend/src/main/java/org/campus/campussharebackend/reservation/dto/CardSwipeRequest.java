package org.campus.campussharebackend.reservation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** The Session identifies the account; resourceId identifies the simulated seat terminal. */
public record CardSwipeRequest(@NotBlank @Pattern(regexp = "^[1-9][0-9]*$") String resourceId,
                               @NotNull Source source) {
    public enum Source { CARD_SIMULATION }
}
