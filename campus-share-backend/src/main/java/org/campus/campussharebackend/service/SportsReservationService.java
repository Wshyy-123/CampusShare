package org.campus.campussharebackend.service;

import org.campus.campussharebackend.common.ApiException;
import org.campus.campussharebackend.common.ErrorCode;
import org.campus.campussharebackend.common.RequestIdFilter;
import org.campus.campussharebackend.config.SportsProperties;
import org.campus.campussharebackend.dto.ReservationDetailResponse;
import org.campus.campussharebackend.entity.Reservation;
import org.campus.campussharebackend.entity.Resource;
import org.campus.campussharebackend.mapper.ReservationEventMapper;
import org.campus.campussharebackend.mapper.ReservationMapper;
import org.campus.campussharebackend.mapper.ResourceMapper;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

import static org.campus.campussharebackend.service.SportsBookingRules.STATUS_CANCELLED;
import static org.campus.campussharebackend.service.SportsBookingRules.STATUS_CHECKED_IN;
import static org.campus.campussharebackend.service.SportsBookingRules.STATUS_COMPLETED;
import static org.campus.campussharebackend.service.SportsBookingRules.STATUS_RESERVED;
import static org.campus.campussharebackend.service.SportsBookingRules.REASON_USER_CANCEL;
import static org.campus.campussharebackend.service.SportsBookingRules.canCancel;
import static org.campus.campussharebackend.service.SportsBookingRules.inCheckInWindow;
import static org.campus.campussharebackend.service.SportsBookingRules.isSportsResource;

/**
 * 体育预约的取消、签到、签退与详情查询（XIA-7 / XIA-8）。
 *
 * <p>所有写操作遵循共同锁顺序：先锁 resource 行，再锁 reservation 行；锁内重读状态与时间后，
 * 用带状态条件的 UPDATE 落库，影响行数不为 1 即视为并发冲突。</p>
 */
@Service
public class SportsReservationService {

    private final ReservationMapper reservationMapper;
    private final ResourceMapper resourceMapper;
    private final ReservationEventMapper reservationEventMapper;
    private final Clock clock;
    private final SportsProperties sportsProperties;

    public SportsReservationService(ReservationMapper reservationMapper,
                                    ResourceMapper resourceMapper,
                                    ReservationEventMapper reservationEventMapper,
                                    Clock clock,
                                    SportsProperties sportsProperties) {
        this.reservationMapper = reservationMapper;
        this.resourceMapper = resourceMapper;
        this.reservationEventMapper = reservationEventMapper;
        this.clock = clock;
        this.sportsProperties = sportsProperties;
    }

    @Transactional
    public ReservationDetailResponse cancel(long userId, long reservationId) {
        LockedReservation locked = lock(userId, reservationId);
        requireSports(locked.resource(), "取消");
        Reservation reservation = locked.reservation();

        if (STATUS_CANCELLED.equals(reservation.getStatus())) {
            if (REASON_USER_CANCEL.equals(reservation.getCancelReason())) {
                // AC3：同一取消重复提交，不重复释放、不重复写事件，直接返回当前结果
                return detail(reservationId, userId);
            }
            throw new ApiException(ErrorCode.INVALID_STATE, "预约已因超时未到被取消，不能按用户取消处理");
        }
        if (!STATUS_RESERVED.equals(reservation.getStatus())) {
            throw new ApiException(ErrorCode.INVALID_STATE, cancelBlockedMessage(reservation.getStatus()));
        }

        LocalDateTime now = now();
        if (!canCancel(reservation, now)) {
            throw new ApiException(ErrorCode.INVALID_STATE, "预约已到开始时间，不能按使用前规则取消");
        }
        if (reservationMapper.cancelIfReserved(reservationId, REASON_USER_CANCEL, now) != 1) {
            throw new ApiException(ErrorCode.INVALID_STATE, "预约状态已变化，请刷新后重试");
        }
        recordEvent(reservationId, "CANCEL", userId, now, REASON_USER_CANCEL);
        return detail(reservationId, userId);
    }

    @Transactional
    public ReservationDetailResponse checkIn(long userId, long reservationId) {
        LockedReservation locked = lock(userId, reservationId);
        requireSports(locked.resource(), "签到");
        Reservation reservation = locked.reservation();

        if (STATUS_CHECKED_IN.equals(reservation.getStatus())) {
            return detail(reservationId, userId);
        }
        if (!STATUS_RESERVED.equals(reservation.getStatus())) {
            throw new ApiException(ErrorCode.INVALID_STATE, checkInBlockedMessage(reservation.getStatus()));
        }

        LocalDateTime now = now();
        int earlyMinutes = sportsProperties.getCheckInEarlyMinutes();
        int graceMinutes = sportsProperties.getCheckInGraceMinutes();
        if (!inCheckInWindow(reservation, now, earlyMinutes, graceMinutes)) {
            throw new ApiException(ErrorCode.INVALID_STATE, checkInWindowMessage(reservation, now, earlyMinutes));
        }
        if (reservationMapper.checkInIfReserved(reservationId, now) != 1) {
            throw new ApiException(ErrorCode.INVALID_STATE, "预约状态已变化，请刷新后重试");
        }
        recordEvent(reservationId, "CHECK_IN", userId, now, null);
        return detail(reservationId, userId);
    }

    @Transactional
    public ReservationDetailResponse checkOut(long userId, long reservationId) {
        LockedReservation locked = lock(userId, reservationId);
        requireSports(locked.resource(), "签退");
        Reservation reservation = locked.reservation();

        if (STATUS_COMPLETED.equals(reservation.getStatus())) {
            return detail(reservationId, userId);
        }
        if (!STATUS_CHECKED_IN.equals(reservation.getStatus())) {
            throw new ApiException(ErrorCode.INVALID_STATE, checkOutBlockedMessage(reservation.getStatus()));
        }

        LocalDateTime now = now();
        if (reservationMapper.checkOutIfCheckedIn(reservationId, now) != 1) {
            throw new ApiException(ErrorCode.INVALID_STATE, "预约状态已变化，请刷新后重试");
        }
        recordEvent(reservationId, "CHECK_OUT", userId, now, null);
        return detail(reservationId, userId);
    }

    @Transactional(readOnly = true)
    public ReservationDetailResponse get(long userId, long reservationId) {
        Reservation reservation = reservationMapper.findById(reservationId);
        if (reservation == null || !Objects.equals(reservation.getUserId(), userId)) {
            throw new ApiException(ErrorCode.RESERVATION_NOT_FOUND);
        }
        Resource resource = resourceMapper.findById(reservation.getResourceId());
        return ReservationDetailResponse.of(reservation, resource == null ? null : resource.getResourceType());
    }

    // ------------------------------------------------------------------

    private LockedReservation lock(long userId, long reservationId) {
        Reservation snapshot = reservationMapper.findById(reservationId);
        if (snapshot == null || !Objects.equals(snapshot.getUserId(), userId)) {
            // 他人预约与不存在的预约返回同一结果，避免泄露记录归属
            throw new ApiException(ErrorCode.RESERVATION_NOT_FOUND);
        }
        Resource resource = resourceMapper.findByIdForUpdate(snapshot.getResourceId());
        if (resource == null) {
            throw new ApiException(ErrorCode.RESOURCE_NOT_FOUND);
        }
        Reservation reservation = reservationMapper.findByIdForUpdate(reservationId);
        if (reservation == null) {
            throw new ApiException(ErrorCode.RESERVATION_NOT_FOUND);
        }
        return new LockedReservation(reservation, resource);
    }

    private void requireSports(Resource resource, String action) {
        if (!isSportsResource(resource.getResourceType())) {
            throw new ApiException(ErrorCode.INVALID_STATE, "该预约不是体育场地预约，不支持" + action);
        }
    }

    private ReservationDetailResponse detail(long reservationId, long userId) {
        Reservation current = reservationMapper.findById(reservationId);
        if (current == null || !Objects.equals(current.getUserId(), userId)) {
            throw new ApiException(ErrorCode.RESERVATION_NOT_FOUND);
        }
        Resource resource = resourceMapper.findById(current.getResourceId());
        return ReservationDetailResponse.of(current, resource == null ? null : resource.getResourceType());
    }

    private void recordEvent(long reservationId, String eventType, Long operatorId,
                             LocalDateTime occurredAt, String reason) {
        reservationEventMapper.insert(reservationId, eventType, operatorId, occurredAt,
                MDC.get(RequestIdFilter.MDC_KEY), reason);
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private String cancelBlockedMessage(String status) {
        return switch (status) {
            case STATUS_CHECKED_IN -> "预约已签到，不能取消";
            case STATUS_COMPLETED -> "预约已完成，不能取消";
            default -> "预约当前状态不允许取消";
        };
    }

    private String checkInBlockedMessage(String status) {
        return switch (status) {
            case STATUS_CANCELLED -> "预约已取消，不能签到";
            case STATUS_COMPLETED -> "预约已完成，不能签到";
            default -> "预约当前状态不允许签到";
        };
    }

    private String checkInWindowMessage(Reservation reservation, LocalDateTime now, int earlyMinutes) {
        if (reservation.getStartAt() != null && now.isBefore(reservation.getStartAt().minusMinutes(earlyMinutes))) {
            return "未到可签到时间";
        }
        return "已超过签到时间，预约已不可签到";
    }

    private String checkOutBlockedMessage(String status) {
        return switch (status) {
            case STATUS_RESERVED -> "预约尚未签到，不能签退";
            case STATUS_CANCELLED -> "预约已取消，不能签退";
            default -> "预约当前状态不允许签退";
        };
    }

    private record LockedReservation(Reservation reservation, Resource resource) {
    }
}
