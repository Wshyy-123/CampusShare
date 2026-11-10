package com.campusshare.availability.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class AvailabilityCalculatorTest {

    private final AvailabilityCalculator calculator = new AvailabilityCalculator();

    @Test
    void shouldSubtractTeachingReservationAndBlockFromOpenPeriod() {
        LocalDateTime d = LocalDateTime.of(2026, 11, 3, 0, 0);

        List<TimeInterval> open = List.of(
                new TimeInterval(d.withHour(8), d.withHour(22)));

        List<TimeInterval> busy = List.of(
                new TimeInterval(d.withHour(10), d.withHour(12)),
                new TimeInterval(d.withHour(14), d.withHour(15)),
                new TimeInterval(d.withHour(18), d.withHour(19)));

        assertThat(calculator.subtract(open, busy))
                .containsExactly(
                        new TimeInterval(d.withHour(8), d.withHour(10)),
                        new TimeInterval(d.withHour(12), d.withHour(14)),
                        new TimeInterval(d.withHour(15), d.withHour(18)),
                        new TimeInterval(d.withHour(19), d.withHour(22)));
    }

    @Test
    void shouldMergeOverlappingAndAdjacentPeriods() {
        LocalDateTime d = LocalDateTime.of(2026, 11, 3, 0, 0);

        List<TimeInterval> input = List.of(
                new TimeInterval(d.withHour(8), d.withHour(10)),
                new TimeInterval(d.withHour(9), d.withHour(11)),
                new TimeInterval(d.withHour(11), d.withHour(12)));

        assertThat(calculator.merge(input))
                .containsExactly(
                        new TimeInterval(d.withHour(8), d.withHour(12)));
    }

    @Test
    void shouldReturnNoAvailabilityWhenNoOpenPeriodExists() {
        LocalDateTime d = LocalDateTime.of(2026, 11, 3, 0, 0);

        assertThat(calculator.subtract(
                List.of(),
                List.of(new TimeInterval(d.withHour(10), d.withHour(12)))))
                .isEmpty();
    }

    @Test
    void busyPeriodsOutsideOpenRangeShouldNotAffectResult() {
        LocalDateTime d = LocalDateTime.of(2026, 11, 3, 0, 0);

        List<TimeInterval> open = List.of(
                new TimeInterval(d.withHour(8), d.withHour(10)));

        List<TimeInterval> busy = List.of(
                new TimeInterval(d.withHour(12), d.withHour(13)));

        assertThat(calculator.subtract(open, busy))
                .containsExactly(
                        new TimeInterval(d.withHour(8), d.withHour(10)));
    }
}
