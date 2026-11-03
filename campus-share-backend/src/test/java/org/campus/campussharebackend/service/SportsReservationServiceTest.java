package org.campus.campussharebackend.service;

import org.campus.campussharebackend.common.ApiException;
import org.campus.campussharebackend.common.ErrorCode;
import org.campus.campussharebackend.config.SportsProperties;
import org.campus.campussharebackend.dto.ReservationDetailResponse;
import org.campus.campussharebackend.entity.Reservation;
import org.campus.campussharebackend.entity.Resource;
import org.campus.campussharebackend.mapper.ReservationEventMapper;
import org.campus.campussharebackend.mapper.ReservationMapper;
import org.campus.campussharebackend.mapper.ResourceMapper;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * XIA-7 / XIA-8 的服务层行为测试：权限、状态冲突、时间窗口、幂等与事件记录。
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
    private ReservationEventMapper reservationEventMapper;

    private SportsReservationService service;

    @BeforeEach
    void setUp() {
        SportsProperties properties = new SportsProperties();
        Clock clock = Clock.fixed(NOW.atZone(ZoneId.of("Asia/Shanghai")).toInstant(), ZoneId.of("Asia/Shanghai"));
        service = new SportsReservationService(
                reservationMapper, resourceMapper, reservationEventMapper, clock, properties);
    }

    // ===================== XIA-7 取消 =====================

    @Test
    void cancelsReservationBeforeStartAndRecordsEvent() {
        Reservation reservation = reservation(USER_ID, SportsBookingRules.STATUS_RESERVED, NOW.plusHours(2));
        stubLock(reservation, "BADMINTON");
        when(resourceMapper.findById(RESOURCE_ID)).thenReturn(resource("BADMINTON"));
        when(reservationMapper.cancelIfReserved(RESERVATION_ID, SportsBookingRules.REASON_USER_CANCEL, NOW))
                .thenAnswer(invocation -> {
                    reservation.setStatus(SportsBookingRules.STATUS_CANCELLED);
                    reservation.setCancelReason(SportsBookingRules.REASON_USER_CANCEL);
                    return 1;
                });

        ReservationDetailResponse response = service.cancel(USER_ID, RESERVATION_ID);

        assertThat(response.status()).isEqualTo(SportsBookingRules.STATUS_CANCELLED);
        assertThat(response.cancelReason()).isEqualTo(SportsBookingRules.REASON_USER_CANCEL);
        verify(reservationEventMapper).insert(
                eq(RESERVATION_ID), eq("CANCEL"), eq(USER_ID), eq(NOW), isNull(),
                eq(SportsBookingRules.REASON_USER_CANCEL));
    }

    @Test
    void rejectsCancelAtOrAfterStartTime() {
        Reservation reservation = reservation(USER_ID, SportsBookingRules.STATUS_RESERVED, NOW);
        stubLock(reservation, "BADMINTON");

        assertThatThrownBy(() -> service.cancel(USER_ID, RESERVATION_ID))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATE));
        verify(reservationMapper, never()).cancelIfReserved(any(), any(), any());
        verifyNoInteractions(reservationEventMapper);
    }

    @Test
    void repeatedCancelIsIdempotentAndDoesNotWriteAgain() {
        Reservation reservation = reservation(USER_ID, SportsBookingRules.STATUS_CANCELLED, NOW.plusHours(2));
        reservation.setCancelReason(SportsBookingRules.REASON_USER_CANCEL);
        stubLock(reservation, "BADMINTON");
        when(resourceMapper.findById(RESOURCE_ID)).thenReturn(resource("BADMINTON"));

        ReservationDetailResponse response = service.cancel(USER_ID, RESERVATION_ID);

        assertThat(response.status()).isEqualTo(SportsBookingRules.STATUS_CANCELLED);
        verify(reservationMapper, never()).cancelIfReserved(any(), any(), any());
        verifyNoInteractions(reservationEventMapper);
    }

    @Test
    void cancelAfterNoShowCancellationIsRejected() {
        Reservation reservation = reservation(USER_ID, SportsBookingRules.STATUS_CANCELLED, NOW.plusHours(2));
        reservation.setCancelReason(SportsBookingRules.REASON_NO_SHOW);
        stubLock(reservation, "BADMINTON");

        assertThatThrownBy(() -> service.cancel(USER_ID, RESERVATION_ID))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATE));
        verifyNoInteractions(reservationEventMapper);
    }

    @Test
    void cancelRejectedForCheckedInReservation() {
        Reservation reservation = reservation(USER_ID, SportsBookingRules.STATUS_CHECKED_IN, NOW.plusHours(2));
        stubLock(reservation, "BADMINTON");

        assertThatThrownBy(() -> service.cancel(USER_ID, RESERVATION_ID))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATE));
    }

    @Test
    void otherUsersReservationIsReportedAsNotFound() {
        when(reservationMapper.findById(RESERVATION_ID))
                .thenReturn(reservation(OTHER_USER_ID, SportsBookingRules.STATUS_RESERVED, NOW.plusHours(2)));

        assertThatThrownBy(() -> service.cancel(USER_ID, RESERVATION_ID))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESERVATION_NOT_FOUND));
        verifyNoInteractions(resourceMapper, reservationEventMapper);
    }

    @Test
    void nonSportsReservationCannotBeCancelled() {
        Reservation reservation = reservation(USER_ID, SportsBookingRules.STATUS_RESERVED, NOW.plusHours(2));
        stubLock(reservation, "LIBRARY_SEAT");

        assertThatThrownBy(() -> service.cancel(USER_ID, RESERVATION_ID))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATE));
        verify(reservationMapper, never()).cancelIfReserved(any(), any(), any());
    }

    // ===================== XIA-8 签到 =====================

    @Test
    void checksInInsideWindowAndRecordsEvent() {
        Reservation reservation = reservation(USER_ID, SportsBookingRules.STATUS_RESERVED, NOW.minusMinutes(5));
        stubLock(reservation, "BADMINTON");
        when(resourceMapper.findById(RESOURCE_ID)).thenReturn(resource("BADMINTON"));
        when(reservationMapper.checkInIfReserved(RESERVATION_ID, NOW)).thenAnswer(invocation -> {
            reservation.setStatus(SportsBookingRules.STATUS_CHECKED_IN);
            reservation.setCheckInAt(NOW);
            return 1;
        });

        ReservationDetailResponse response = service.checkIn(USER_ID, RESERVATION_ID);

        assertThat(response.status()).isEqualTo(SportsBookingRules.STATUS_CHECKED_IN);
        assertThat(response.checkInAt()).isEqualTo("2026-11-04T09:30:00+08:00");
        verify(reservationEventMapper).insert(
                eq(RESERVATION_ID), eq("CHECK_IN"), eq(USER_ID), eq(NOW), isNull(), isNull());
    }

    @Test
    void rejectsCheckInBeforeWindow() {
        Reservation reservation = reservation(USER_ID, SportsBookingRules.STATUS_RESERVED, NOW.plusMinutes(10));
        stubLock(reservation, "BADMINTON");

        assertThatThrownBy(() -> service.checkIn(USER_ID, RESERVATION_ID))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATE));
        verify(reservationMapper, never()).checkInIfReserved(any(), any());
    }

    @Test
    void rejectsCheckInAfterGracePeriod() {
        Reservation reservation = reservation(USER_ID, SportsBookingRules.STATUS_RESERVED, NOW.minusMinutes(15));
        stubLock(reservation, "BADMINTON");

        assertThatThrownBy(() -> service.checkIn(USER_ID, RESERVATION_ID))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATE));
        verify(reservationMapper, never()).checkInIfReserved(any(), any());
    }

    @Test
    void cancelledReservationCannotBeRestoredByCheckIn() {
        Reservation reservation = reservation(USER_ID, SportsBookingRules.STATUS_CANCELLED, NOW.minusMinutes(5));
        reservation.setCancelReason(SportsBookingRules.REASON_NO_SHOW);
        stubLock(reservation, "BADMINTON");

        assertThatThrownBy(() -> service.checkIn(USER_ID, RESERVATION_ID))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATE));
        verify(reservationMapper, never()).checkInIfReserved(any(), any());
        verifyNoInteractions(reservationEventMapper);
    }

    @Test
    void repeatedCheckInReturnsCurrentState() {
        Reservation reservation = reservation(USER_ID, SportsBookingRules.STATUS_CHECKED_IN, NOW.minusMinutes(5));
        reservation.setCheckInAt(NOW.minusMinutes(3));
        stubLock(reservation, "BADMINTON");
        when(resourceMapper.findById(RESOURCE_ID)).thenReturn(resource("BADMINTON"));

        ReservationDetailResponse response = service.checkIn(USER_ID, RESERVATION_ID);

        assertThat(response.status()).isEqualTo(SportsBookingRules.STATUS_CHECKED_IN);
        verify(reservationMapper, never()).checkInIfReserved(any(), any());
        verifyNoInteractions(reservationEventMapper);
    }

    // ===================== XIA-8 签退 =====================

    @Test
    void checkOutCompletesReservation() {
        Reservation reservation = reservation(USER_ID, SportsBookingRules.STATUS_CHECKED_IN, NOW.minusMinutes(30));
        stubLock(reservation, "BADMINTON");
        when(resourceMapper.findById(RESOURCE_ID)).thenReturn(resource("BADMINTON"));
        when(reservationMapper.checkOutIfCheckedIn(RESERVATION_ID, NOW)).thenAnswer(invocation -> {
            reservation.setStatus(SportsBookingRules.STATUS_COMPLETED);
            reservation.setCheckOutAt(NOW);
            return 1;
        });

        ReservationDetailResponse response = service.checkOut(USER_ID, RESERVATION_ID);

        assertThat(response.status()).isEqualTo(SportsBookingRules.STATUS_COMPLETED);
        assertThat(response.checkOutAt()).isEqualTo("2026-11-04T09:30:00+08:00");
        verify(reservationEventMapper).insert(
                eq(RESERVATION_ID), eq("CHECK_OUT"), eq(USER_ID), eq(NOW), isNull(), isNull());
    }

    @Test
    void checkOutRejectedBeforeCheckIn() {
        Reservation reservation = reservation(USER_ID, SportsBookingRules.STATUS_RESERVED, NOW.minusMinutes(30));
        stubLock(reservation, "BADMINTON");

        assertThatThrownBy(() -> service.checkOut(USER_ID, RESERVATION_ID))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATE));
        verify(reservationMapper, never()).checkOutIfCheckedIn(any(), any());
    }

    @Test
    void repeatedCheckOutReturnsCurrentState() {
        Reservation reservation = reservation(USER_ID, SportsBookingRules.STATUS_COMPLETED, NOW.minusMinutes(30));
        reservation.setCheckOutAt(NOW.minusMinutes(1));
        stubLock(reservation, "BADMINTON");
        when(resourceMapper.findById(RESOURCE_ID)).thenReturn(resource("BADMINTON"));

        ReservationDetailResponse response = service.checkOut(USER_ID, RESERVATION_ID);

        assertThat(response.status()).isEqualTo(SportsBookingRules.STATUS_COMPLETED);
        verify(reservationMapper, never()).checkOutIfCheckedIn(any(), any());
    }

    // ===================== 查询 =====================

    @Test
    void detailFormatsTimesWithShanghaiOffset() {
        Reservation reservation = reservation(USER_ID, SportsBookingRules.STATUS_CHECKED_IN, NOW.plusHours(1));
        reservation.setCheckInAt(NOW);
        when(reservationMapper.findById(RESERVATION_ID)).thenReturn(reservation);
        when(resourceMapper.findById(RESOURCE_ID)).thenReturn(resource("BADMINTON"));

        ReservationDetailResponse response = service.get(USER_ID, RESERVATION_ID);

        assertThat(response.reservationId()).isEqualTo("100");
        assertThat(response.resourceType()).isEqualTo("BADMINTON");
        assertThat(response.checkInAt()).isEqualTo("2026-11-04T09:30:00+08:00");
        assertThat(response.checkOutAt()).isNull();
    }

    @Test
    void detailHidesOtherUsersReservation() {
        when(reservationMapper.findById(RESERVATION_ID))
                .thenReturn(reservation(OTHER_USER_ID, SportsBookingRules.STATUS_RESERVED, NOW.plusHours(1)));

        assertThatThrownBy(() -> service.get(USER_ID, RESERVATION_ID))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESERVATION_NOT_FOUND));
    }

    // ===================== 工具方法 =====================

    private void stubLock(Reservation reservation, String resourceType) {
        when(reservationMapper.findById(RESERVATION_ID)).thenReturn(reservation);
        when(resourceMapper.findByIdForUpdate(RESOURCE_ID)).thenReturn(resource(resourceType));
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

    private static Resource resource(String resourceType) {
        Resource resource = new Resource();
        resource.setId(RESOURCE_ID);
        resource.setResourceType(resourceType);
        resource.setEnabled(true);
        return resource;
    }
}
