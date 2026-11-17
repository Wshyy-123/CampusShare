package org.campus.campussharebackend.service;

import org.campus.campussharebackend.common.BusinessException;
import org.campus.campussharebackend.common.RequestIdContext;
import org.campus.campussharebackend.config.SportsProperties;
import org.campus.campussharebackend.entity.Reservation;
import org.campus.campussharebackend.mapper.ReservationMapper;
import org.campus.campussharebackend.reservation.mapper.ReservationRepository;
import org.campus.campussharebackend.resource.entity.ResourceRecord;
import org.campus.campussharebackend.resource.mapper.ResourceMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

import static org.campus.campussharebackend.service.SportsBookingRules.REASON_USER_CANCEL;
import static org.campus.campussharebackend.service.SportsBookingRules.STATUS_CANCELLED;
import static org.campus.campussharebackend.service.SportsBookingRules.STATUS_CHECKED_IN;
import static org.campus.campussharebackend.service.SportsBookingRules.STATUS_COMPLETED;
import static org.campus.campussharebackend.service.SportsBookingRules.STATUS_RESERVED;
import static org.campus.campussharebackend.service.SportsBookingRules.canCancel;
import static org.campus.campussharebackend.service.SportsBookingRules.inCheckInWindow;
import static org.campus.campussharebackend.service.SportsBookingRules.isSportsResource;

/**
 * 体育预约取消/签到/签退及教室取消/签到（XIA-7 / XIA-8 / XIA-13），运行在主工程 reservation 业务链路内。
 *
 * <p>写操作遵循共同锁顺序：先锁 resource 行，再锁 reservation 行；锁内重读状态与时间后，
 * 用带状态条件的 UPDATE 落库，影响行数不为 1 即视为并发冲突。身份来自服务端会话。</p>
 */
@Service
public class SportsReservationService {

    private final ReservationMapper reservationMapper;
    private final ResourceMapper resourceMapper;
    private final ReservationRepository reservationRepository;
    private final Clock clock;
    private final SportsProperties sportsProperties;
    private final org.campus.campussharebackend.lifecycle.LifecycleMapper lifecycle;

    public SportsReservationService(ReservationMapper reservationMapper,
                                    ResourceMapper resourceMapper,
                                    ReservationRepository reservationRepository,
                                    @Qualifier("lifecycleClock") Clock clock,
                                    SportsProperties sportsProperties, org.campus.campussharebackend.lifecycle.LifecycleMapper lifecycle) {
        this.reservationMapper = reservationMapper;
        this.resourceMapper = resourceMapper;
        this.reservationRepository = reservationRepository;
        this.clock = clock;
        this.sportsProperties = sportsProperties;
        this.lifecycle = lifecycle;
    }

    @Transactional
    public void cancel(long userId, long reservationId) {
        LockedReservation locked = lock(userId, reservationId);
        if (!isSportsResource(locked.resource().getResourceType())
                && !"CLASSROOM".equals(locked.resource().getResourceType())
                && !"DISCUSSION_ROOM".equals(locked.resource().getResourceType())) {
            throw new BusinessException(409, "INVALID_STATE", "仅讨论室、教室和体育预约支持使用前主动取消，座位不可主动取消");
        }
        Reservation reservation = locked.reservation();

        if (STATUS_CANCELLED.equals(reservation.getStatus())) {
            if (REASON_USER_CANCEL.equals(reservation.getCancelReason())) {
                // AC3：同一取消重复提交，不重复释放、不重复写事件
                return;
            }
            throw new BusinessException(409, "INVALID_STATE", "预约已因未到或停用等原因取消，不能改为本人取消");
        }
        if (!STATUS_RESERVED.equals(reservation.getStatus())) {
            throw new BusinessException(409, "INVALID_STATE", cancelBlockedMessage(reservation.getStatus()));
        }

        LocalDateTime now = now();
        if (!canCancel(reservation, now)) {
            throw new BusinessException(409, "INVALID_STATE", "预约已到开始时间，不能按使用前规则取消");
        }
        if (reservationMapper.cancelIfReserved(reservationId, REASON_USER_CANCEL, now) != 1) {
            throw new BusinessException(409, "INVALID_STATE", "预约状态已变化，请刷新后重试");
        }
        recordEvent(reservationId, "CANCELLED", userId, now, REASON_USER_CANCEL);
    }

    @Transactional
    public void checkIn(long userId, long reservationId, long scannedResourceId) {
        LockedReservation locked = lock(userId, reservationId);
        if (!isSportsResource(locked.resource().getResourceType()) && !"CLASSROOM".equals(locked.resource().getResourceType()))
            throw new BusinessException(409, "INVALID_STATE", "该资源不支持地点码签到");
        requirePlace(locked, scannedResourceId);
        Reservation reservation = locked.reservation();

        LocalDateTime now = now();
        if (reservation.getEndAt() == null || !now.isBefore(reservation.getEndAt()))
            throw new BusinessException(409, "INVALID_STATE", "预约已经结束，不能签到");
        if (!locked.resource().isEnabled() || lifecycle.lockOverlappingBlock(locked.resource().getId(), reservation.getStartAt(), reservation.getEndAt()) != null)
            throw new BusinessException(409, "RESOURCE_UNAVAILABLE", "该地点在预约范围内不可使用");
        if (STATUS_CHECKED_IN.equals(reservation.getStatus())) {
            return;
        }
        if (!STATUS_RESERVED.equals(reservation.getStatus())) {
            throw new BusinessException(409, "INVALID_STATE", checkInBlockedMessage(reservation.getStatus()));
        }

        int earlyMinutes = sportsProperties.getCheckInEarlyMinutes();
        int graceMinutes = sportsProperties.getCheckInGraceMinutes();
        if (!inCheckInWindow(reservation, now, earlyMinutes, graceMinutes)) {
            throw new BusinessException(409, "INVALID_STATE", checkInWindowMessage(reservation, now, earlyMinutes));
        }
        if (reservationMapper.checkInIfReserved(reservationId, now) != 1) {
            throw new BusinessException(409, "INVALID_STATE", "预约状态已变化，请刷新后重试");
        }
        recordEvent(reservationId, "CHECKED_IN", userId, now, "QR_CHECK_IN:" + scannedResourceId);
    }

    @Transactional
    public void checkOut(long userId, long reservationId, long scannedResourceId) {
        LockedReservation locked = lock(userId, reservationId);
        requireSports(locked.resource(), "签退");
        requirePlace(locked, scannedResourceId);
        Reservation reservation = locked.reservation();
        LocalDateTime now = now();
        if (reservation.getEndAt() == null || now.isBefore(reservation.getEndAt()))
            throw new BusinessException(409, "INVALID_STATE", "预约尚未结束，不能提前签退释放");
        if (STATUS_COMPLETED.equals(reservation.getStatus())) {
            if (reservation.getCheckOutAt() != null) return;
            if (reservation.getCheckInAt() == null) throw new BusinessException(409, "INVALID_STATE", "没有可签退的到场记录");
            if (reservationMapper.recordCheckOutIfCompleted(reservationId, now) == 1)
                recordEvent(reservationId, "CHECKED_OUT", userId, now, "QR_CHECK_OUT:" + scannedResourceId);
            return;
        }
        if (!STATUS_CHECKED_IN.equals(reservation.getStatus()))
            throw new BusinessException(409, "INVALID_STATE", checkOutBlockedMessage(reservation.getStatus()));
        if (reservationMapper.checkOutIfCheckedIn(reservationId, now) != 1)
            throw new BusinessException(409, "INVALID_STATE", "预约状态已变化，请刷新后重试");
        recordEvent(reservationId, "COMPLETED", userId, now, "QR_CHECK_OUT:" + scannedResourceId);
    }

    private void requirePlace(LockedReservation locked, long scannedResourceId) {
        if (scannedResourceId != locked.resource().getId())
            throw new BusinessException(409, "WRONG_PLACE", "扫描地点与预约地点不一致");
    }

    private LockedReservation lock(long userId, long reservationId) {
        Reservation snapshot = reservationMapper.findById(reservationId);
        if (snapshot == null || !Objects.equals(snapshot.getUserId(), userId)) {
            // 他人预约与不存在的预约返回同一结果，避免泄露记录归属
            throw new BusinessException(404, "RESERVATION_NOT_FOUND", "预约不存在或无权操作");
        }
        ResourceRecord resource = resourceMapper.lockById(snapshot.getResourceId());
        if (resource == null) {
            throw new BusinessException(404, "RESOURCE_NOT_FOUND", "资源不存在");
        }
        Reservation reservation = reservationMapper.findByIdForUpdate(reservationId);
        if (reservation == null || !Objects.equals(reservation.getUserId(), userId) || !Objects.equals(reservation.getResourceId(), resource.getId())) {
            throw new BusinessException(404, "RESERVATION_NOT_FOUND", "预约不存在或无权操作");
        }
        return new LockedReservation(reservation, resource);
    }

    private void requireSports(ResourceRecord resource, String action) {
        if (!isSportsResource(resource.getResourceType())) {
            throw new BusinessException(409, "INVALID_STATE", "该预约不是体育场地预约，不支持" + action);
        }
    }

    private void recordEvent(long reservationId, String eventType, Long operatorId,
                             LocalDateTime occurredAt, String reason) {
        reservationRepository.insertEvent(reservationId, eventType, operatorId, occurredAt,
                RequestIdContext.get(), reason);
    }

    private LocalDateTime now() {
        return org.campus.campussharebackend.common.TimeFormats.now(clock);
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

    private record LockedReservation(Reservation reservation, ResourceRecord resource) {
    }
}
