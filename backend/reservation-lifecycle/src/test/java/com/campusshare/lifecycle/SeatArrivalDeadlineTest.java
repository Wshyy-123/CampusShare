package com.campusshare.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class SeatArrivalDeadlineTest {
    @Test
    void expiresAtThirtyMinutesFromAcceptance() {
        var accepted = LocalDateTime.parse("2026-11-01T10:00:00");
        assertThat(SeatArrivalDeadline.expired(accepted, accepted.plusMinutes(30).minusNanos(1))).isFalse();
        assertThat(SeatArrivalDeadline.expired(accepted, accepted.plusMinutes(30))).isTrue();
        assertThat(SeatArrivalDeadline.expired(accepted, accepted.plusDays(1))).isTrue();
    }
}
