package org.campus.campussharebackend.service;

import java.time.Clock;
import java.util.Objects;
import org.campus.campussharebackend.common.BusinessException;
import org.campus.campussharebackend.common.RequestIdContext;
import org.campus.campussharebackend.common.TimeFormats;
import org.campus.campussharebackend.entity.Reservation;
import org.campus.campussharebackend.lifecycle.LifecycleMapper;
import org.campus.campussharebackend.lifecycle.SeatArrivalDeadline;
import org.campus.campussharebackend.mapper.ReservationMapper;
import org.campus.campussharebackend.reservation.mapper.ReservationRepository;
import org.campus.campussharebackend.resource.entity.ResourceRecord;
import org.campus.campussharebackend.resource.mapper.ResourceMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** XIA-10 simulated library card events, using the shared account, database and lock order. */
@Service
public class CheckInService {
    private final ReservationMapper reservations;
    private final ResourceMapper resources;
    private final ReservationRepository history;
    private final LifecycleMapper lifecycle;
    private final Clock clock;

    public CheckInService(ReservationMapper reservations, ResourceMapper resources,
                          ReservationRepository history, LifecycleMapper lifecycle,
                          @Qualifier("lifecycleClock") Clock clock) {
        this.reservations = reservations;
        this.resources = resources;
        this.history = history;
        this.lifecycle = lifecycle;
        this.clock = clock;
    }

    @Transactional
    public void enter(long userId, long reservationId, long seatId) {
        var locked = lock(userId, reservationId, seatId);
        var reservation = locked.reservation();
        // A repeated entry confirms the same event, including after its original arrival deadline.
        if ("CHECKED_IN".equals(reservation.getStatus()) && reservation.getCheckInAt() != null) return;
        if (!"RESERVED".equals(reservation.getStatus())) {
            throw conflict("当前预约不能刷卡入场，已失效预约不会恢复");
        }
        var now = TimeFormats.now(clock);
        if (reservation.getAcceptedAt() == null || now.isBefore(reservation.getAcceptedAt())
                || SeatArrivalDeadline.expired(reservation.getAcceptedAt(), now)
                || reservation.getEndAt() == null || !now.isBefore(reservation.getEndAt())) {
            throw conflict("已超过到馆期限或预约已经结束，不能刷卡入场");
        }
        if (!locked.resource().isEnabled()
                || lifecycle.lockOverlappingBlock(seatId, reservation.getStartAt(), reservation.getEndAt()) != null) {
            throw new BusinessException(409, "RESOURCE_UNAVAILABLE", "座位已停用或预约时段不可使用");
        }
        if (reservations.checkInIfReserved(reservationId, now) != 1) {
            throw conflict("预约状态已变化，请刷新后重试");
        }
        history.insertEvent(reservationId, "CHECKED_IN", userId, now,
                RequestIdContext.get(), "CARD_SIMULATION_ENTRY:" + seatId);
    }

    @Transactional
    public void leave(long userId, long reservationId, long seatId) {
        var reservation = lock(userId, reservationId, seatId).reservation();
        if ("COMPLETED".equals(reservation.getStatus()) && reservation.getCheckInAt() != null
                && reservation.getCheckOutAt() != null) return;
        if (!"CHECKED_IN".equals(reservation.getStatus()) || reservation.getCheckInAt() == null) {
            throw conflict("尚未刷卡入场或预约已失效，不能刷卡离场");
        }
        var now = TimeFormats.now(clock);
        if (now.isBefore(reservation.getCheckInAt())) throw conflict("离场时间不能早于入场时间");
        // Library departure ends this booking immediately, including before its reserved end.
        if (reservations.checkOutIfCheckedIn(reservationId, now) != 1) {
            throw conflict("预约状态已变化，请刷新后重试");
        }
        history.insertEvent(reservationId, "COMPLETED", userId, now,
                RequestIdContext.get(), "CARD_SIMULATION_EXIT:" + seatId);
    }

    private LockedSeat lock(long userId, long reservationId, long seatId) {
        var snapshot = reservations.findById(reservationId);
        if (snapshot == null || !Objects.equals(snapshot.getUserId(), userId)) throw notFound();
        var resource = resources.lockById(snapshot.getResourceId());
        var reservation = reservations.findByIdForUpdate(reservationId);
        if (resource == null || reservation == null || !Objects.equals(reservation.getUserId(), userId)
                || !Objects.equals(reservation.getResourceId(), resource.getId())) throw notFound();
        if (!"LIBRARY_SEAT".equals(resource.getResourceType())) throw conflict("刷卡仅适用于图书馆座位预约");
        if (resource.getId() != seatId) {
            throw new BusinessException(409, "WRONG_PLACE", "刷卡座位与预约座位不一致");
        }
        return new LockedSeat(reservation, resource);
    }

    private static BusinessException conflict(String message) {
        return new BusinessException(409, "INVALID_STATE", message);
    }

    private static BusinessException notFound() {
        return new BusinessException(404, "RESERVATION_NOT_FOUND", "预约不存在或无权操作");
    }

    private record LockedSeat(Reservation reservation, ResourceRecord resource) {}
}
