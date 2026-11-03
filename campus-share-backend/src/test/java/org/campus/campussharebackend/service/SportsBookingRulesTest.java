package org.campus.campussharebackend.service;

import org.campus.campussharebackend.entity.Reservation;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 体育预约的边界口径测试：只覆盖纯规则，不依赖数据库。
 */
class SportsBookingRulesTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 4, 10, 0);

    @Test
    void cancelAllowedOnlyBeforeStartTime() {
        Reservation reservation = reservedAt(START);

        assertThat(SportsBookingRules.canCancel(reservation, START.minusNanos(1))).isTrue();
        assertThat(SportsBookingRules.canCancel(reservation, START)).isFalse();
        assertThat(SportsBookingRules.canCancel(reservation, START.plusMinutes(1))).isFalse();
    }

    @Test
    void cancelRejectedWhenAlreadyCheckedIn() {
        Reservation reservation = reservedAt(START);
        reservation.setStatus(SportsBookingRules.STATUS_CHECKED_IN);

        assertThat(SportsBookingRules.canCancel(reservation, START.minusHours(1))).isFalse();
    }

    @Test
    void checkInWindowStartsAtStartTimeByDefault() {
        Reservation reservation = reservedAt(START);

        assertThat(SportsBookingRules.inCheckInWindow(reservation, START.minusSeconds(1), 0, 15)).isFalse();
        assertThat(SportsBookingRules.inCheckInWindow(reservation, START, 0, 15)).isTrue();
        assertThat(SportsBookingRules.inCheckInWindow(reservation, START.plusMinutes(15).minusNanos(1), 0, 15)).isTrue();
        assertThat(SportsBookingRules.inCheckInWindow(reservation, START.plusMinutes(15), 0, 15)).isFalse();
    }

    @Test
    void earlyCheckInWindowIsConfigurable() {
        Reservation reservation = reservedAt(START);

        assertThat(SportsBookingRules.inCheckInWindow(reservation, START.minusMinutes(10), 10, 15)).isTrue();
        assertThat(SportsBookingRules.inCheckInWindow(
                reservation, START.minusMinutes(10).minusNanos(1), 10, 15)).isFalse();
    }

    @Test
    void missingStartTimeIsNeverEligible() {
        Reservation reservation = reservedAt(START);
        reservation.setStartAt(null);

        assertThat(SportsBookingRules.canCancel(reservation, START)).isFalse();
        assertThat(SportsBookingRules.inCheckInWindow(reservation, START, 0, 15)).isFalse();
        assertThat(SportsBookingRules.isNoShow(reservation, START, 15)).isFalse();
    }

    @Test
    void noShowBoundaryIsStartPlusGrace() {
        Reservation reservation = reservedAt(START);

        assertThat(SportsBookingRules.isNoShow(reservation, START.plusMinutes(15).minusNanos(1), 15)).isFalse();
        assertThat(SportsBookingRules.isNoShow(reservation, START.plusMinutes(15), 15)).isTrue();
    }

    @Test
    void onlySportsResourceTypesAreSupported() {
        assertThat(SportsBookingRules.isSportsResource("BADMINTON")).isTrue();
        assertThat(SportsBookingRules.isSportsResource("TABLE_TENNIS")).isTrue();
        assertThat(SportsBookingRules.isSportsResource("TENNIS")).isTrue();
        assertThat(SportsBookingRules.isSportsResource("LIBRARY_SEAT")).isFalse();
        assertThat(SportsBookingRules.isSportsResource("CLASSROOM")).isFalse();
        assertThat(SportsBookingRules.isSportsResource(null)).isFalse();
    }

    private static Reservation reservedAt(LocalDateTime start) {
        Reservation reservation = new Reservation();
        reservation.setStatus(SportsBookingRules.STATUS_RESERVED);
        reservation.setStartAt(start);
        return reservation;
    }
}
