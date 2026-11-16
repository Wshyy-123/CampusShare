package org.campus.campussharebackend.service;

import org.campus.campussharebackend.common.BusinessException;
import org.campus.campussharebackend.config.SportsProperties;
import org.campus.campussharebackend.entity.Reservation;
import org.campus.campussharebackend.mapper.ReservationMapper;
import org.campus.campussharebackend.reservation.mapper.ReservationRepository;
import org.campus.campussharebackend.resource.entity.ResourceRecord;
import org.campus.campussharebackend.resource.mapper.ResourceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * XIA-7 / XIA-8 服务层行为测试（主工程会话身份 + BusinessException 口径）。
 */
@ExtendWith(MockitoExtension.class)
class SportsReservationServiceTest {

    private static final long USER_ID = 1L;
    private static final long OTHER_USER_ID = 2L;
    private static final long RESERVATION_ID = 100L;
    private static final long RESOURCE_ID = 2L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 4, 9, 30);

    @Mock
    private ReservationMapper reservationMapper;
    @Mock
    private ResourceMapper resourceMapper;
    @Mock
    private ReservationRepository reservationRepository;

    private SportsReservationService service;

    @BeforeEach
    void setUp() {
        SportsProperties properties = new SportsProperties();
        Clock clock = Clock.fixed(NOW.atZone(ZoneId.of("Asia/Shanghai")).toInstant(), ZoneId.of("Asia/Shanghai"));
        var lifecycle = org.mockito.Mockito.mock(org.campus.campussharebackend.lifecycle.LifecycleMapper.class);
        org.mockito.Mockito.lenient().when(lifecycle.lockOverlappingBlock(org.mockito.ArgumentMatchers.anyLong(), any(), any())).thenReturn(null);
        service = new SportsReservationService(
                reservationMapper, resourceMapper, reservationRepository, clock, properties, lifecycle);
    }

    // ===================== XIA-7 取消 =====================

    @Test
    void cancelsReservationBeforeStartAndRecordsEvent() {
        Reservation reservation = reservation(USER_ID, SportsBookingRules.STATUS_RESERVED, NOW.plusHours(2));
        stubLock(reservation, "BADMINTON");
        when(reservationMapper.cancelIfReserved(RESERVATION_ID, SportsBookingRules.REASON_USER_CANCEL, NOW))
                .thenReturn(1);

        service.cancel(USER_ID, RESERVATION_ID);

        verify(reservationMapper).cancelIfReserved(RESERVATION_ID, SportsBookingRules.REASON_USER_CANCEL, NOW);
        verify(reservationRepository).insertEvent(
                eq(RESERVATION_ID), eq("CANCELLED"), eq(USER_ID), eq(NOW), any(), eq("USER_CANCEL"));
    }

    @Test
    void rejectsCancelAtOrAfterStartTime() {
        Reservation reservation = reservation(USER_ID, SportsBookingRules.STATUS_RESERVED, NOW);
        stubLock(reservation, "BADMINTON");

        assertBusinessError(409, "INVALID_STATE", () -> service.cancel(USER_ID, RESERVATION_ID));
        verify(reservationMapper, never()).cancelIfReserved(any(), any(), any());
        verifyNoInteractions(reservationRepository);
    }

    @Test
    void repeatedCancelIsIdempotentAndDoesNotWriteAgain() {
        Reservation reservation = reservation(USER_ID, SportsBookingRules.STATUS_CANCELLED, NOW.plusHours(2));
        reservation.setCancelReason(SportsBookingRules.REASON_USER_CANCEL);
        stubLock(reservation, "BADMINTON");

        service.cancel(USER_ID, RESERVATION_ID);

        verify(reservationMapper, never()).cancelIfReserved(any(), any(), any());
        verifyNoInteractions(reservationRepository);
    }

    @Test
    void cancelAfterNoShowCancellationIsRejected() {
        Reservation reservation = reservation(USER_ID, SportsBookingRules.STATUS_CANCELLED, NOW.plusHours(2));
        reservation.setCancelReason(SportsBookingRules.REASON_NO_SHOW);
        stubLock(reservation, "BADMINTON");

        assertBusinessError(409, "INVALID_STATE", () -> service.cancel(USER_ID, RESERVATION_ID));
        verifyNoInteractions(reservationRepository);
    }

    @Test
    void cancelRejectedForCheckedInReservation() {
        Reservation reservation = reservation(USER_ID, SportsBookingRules.STATUS_CHECKED_IN, NOW.plusHours(2));
        stubLock(reservation, "BADMINTON");

        assertBusinessError(409, "INVALID_STATE", () -> service.cancel(USER_ID, RESERVATION_ID));
    }

    @Test
    void otherUsersReservationIsReportedAsNotFound() {
        when(reservationMapper.findById(RESERVATION_ID))
                .thenReturn(reservation(OTHER_USER_ID, SportsBookingRules.STATUS_RESERVED, NOW.plusHours(2)));

        assertBusinessError(404, "RESERVATION_NOT_FOUND", () -> service.cancel(USER_ID, RESERVATION_ID));
        verifyNoInteractions(resourceMapper, reservationRepository);
    }

    @Test
    void nonSportsReservationCannotBeCancelled() {
        Reservation reservation = reservation(USER_ID, SportsBookingRules.STATUS_RESERVED, NOW.plusHours(2));
        stubLock(reservation, "LIBRARY_SEAT");

        assertBusinessError(409, "INVALID_STATE", () -> service.cancel(USER_ID, RESERVATION_ID));
        verify(reservationMapper, never()).cancelIfReserved(any(), any(), any());
    }

    // ===================== XIA-8 签到 =====================

    @Test
    void checksInInsideWindowAndRecordsEvent() {
        Reservation reservation = reservation(USER_ID, SportsBookingRules.STATUS_RESERVED, NOW.minusMinutes(5));
        stubLock(reservation, "BADMINTON");
        when(reservationMapper.checkInIfReserved(RESERVATION_ID, NOW)).thenReturn(1);

        service.checkIn(USER_ID, RESERVATION_ID, RESOURCE_ID);

        verify(reservationMapper).checkInIfReserved(RESERVATION_ID, NOW);
        verify(reservationRepository).insertEvent(
                eq(RESERVATION_ID), eq("CHECKED_IN"), eq(USER_ID), eq(NOW), any(), eq("QR_CHECK_IN:" + RESOURCE_ID));
    }

    @Test
    void rejectsCheckInBeforeWindow() {
        Reservation reservation = reservation(USER_ID, SportsBookingRules.STATUS_RESERVED, NOW.plusMinutes(10));
        stubLock(reservation, "BADMINTON");

        assertBusinessError(409, "INVALID_STATE", () -> service.checkIn(USER_ID, RESERVATION_ID, RESOURCE_ID));
        verify(reservationMapper, never()).checkInIfReserved(any(), any());
    }

    @Test
    void rejectsCheckInAfterGracePeriod() {
        Reservation reservation = reservation(USER_ID, SportsBookingRules.STATUS_RESERVED, NOW.minusMinutes(15));
        stubLock(reservation, "BADMINTON");

        assertBusinessError(409, "INVALID_STATE", () -> service.checkIn(USER_ID, RESERVATION_ID, RESOURCE_ID));
        verify(reservationMapper, never()).checkInIfReserved(any(), any());
    }

    @Test
    void cancelledReservationCannotBeRestoredByCheckIn() {
        Reservation reservation = reservation(USER_ID, SportsBookingRules.STATUS_CANCELLED, NOW.minusMinutes(5));
        reservation.setCancelReason(SportsBookingRules.REASON_NO_SHOW);
        stubLock(reservation, "BADMINTON");

        assertBusinessError(409, "INVALID_STATE", () -> service.checkIn(USER_ID, RESERVATION_ID, RESOURCE_ID));
        verify(reservationMapper, never()).checkInIfReserved(any(), any());
        verifyNoInteractions(reservationRepository);
    }

    @Test
    void repeatedCheckInIsNoOp() {
        Reservation reservation = reservation(USER_ID, SportsBookingRules.STATUS_CHECKED_IN, NOW.minusMinutes(5));
        stubLock(reservation, "BADMINTON");

        service.checkIn(USER_ID, RESERVATION_ID, RESOURCE_ID);

        verify(reservationMapper, never()).checkInIfReserved(any(), any());
        verifyNoInteractions(reservationRepository);
    }

    // ===================== XIA-8 签退 =====================

    @Test
    void checkOutCompletesReservation() {
        Reservation reservation = reservation(USER_ID, SportsBookingRules.STATUS_CHECKED_IN, NOW.minusHours(1));
        stubLock(reservation, "BADMINTON");
        when(reservationMapper.checkOutIfCheckedIn(RESERVATION_ID, NOW)).thenReturn(1);

        service.checkOut(USER_ID, RESERVATION_ID, RESOURCE_ID);

        verify(reservationMapper).checkOutIfCheckedIn(RESERVATION_ID, NOW);
        verify(reservationRepository).insertEvent(
                eq(RESERVATION_ID), eq("COMPLETED"), eq(USER_ID), eq(NOW), any(), eq("QR_CHECK_OUT:" + RESOURCE_ID));
    }

    @Test
    void checkOutRejectedBeforeCheckIn() {
        Reservation reservation = reservation(USER_ID, SportsBookingRules.STATUS_RESERVED, NOW.minusMinutes(30));
        stubLock(reservation, "BADMINTON");

        assertBusinessError(409, "INVALID_STATE", () -> service.checkOut(USER_ID, RESERVATION_ID, RESOURCE_ID));
        verify(reservationMapper, never()).checkOutIfCheckedIn(any(), any());
    }

    @Test
    void repeatedCheckOutIsNoOp() {
        Reservation reservation = reservation(USER_ID, SportsBookingRules.STATUS_COMPLETED, NOW.minusHours(1));
        reservation.setCheckOutAt(NOW);
        stubLock(reservation, "BADMINTON");

        service.checkOut(USER_ID, RESERVATION_ID, RESOURCE_ID);

        verify(reservationMapper, never()).checkOutIfCheckedIn(any(), any());
    }

    // ===================== 工具方法 =====================

    private void assertBusinessError(int status, String code, Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.status()).isEqualTo(status);
            assertThat(exception.code()).isEqualTo(code);
        });
    }

    private void stubLock(Reservation reservation, String resourceType) {
        when(reservationMapper.findById(RESERVATION_ID)).thenReturn(reservation);
        when(resourceMapper.lockById(RESOURCE_ID)).thenReturn(resource(resourceType));
        when(reservationMapper.findByIdForUpdate(RESERVATION_ID)).thenReturn(reservation);
    }

    private static Reservation reservation(long userId, String status, LocalDateTime startAt) {
        Reservation reservation = new Reservation();
        reservation.setId(RESERVATION_ID);
        reservation.setUserId(userId);
        reservation.setResourceId(RESOURCE_ID);
        reservation.setStatus(status);
        reservation.setStartAt(startAt);
        reservation.setEndAt(startAt.plusHours(1));
        return reservation;
    }

    private static ResourceRecord resource(String resourceType) {
        ResourceRecord resource = new ResourceRecord();
        resource.setId(RESOURCE_ID);
        resource.setResourceType(resourceType);
        resource.setEnabled(true);
        return resource;
    }
}
