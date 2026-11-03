package org.campus.campussharebackend.service;

import org.campus.campussharebackend.config.SportsProperties;
import org.campus.campussharebackend.entity.Reservation;
import org.campus.campussharebackend.entity.Resource;
import org.campus.campussharebackend.mapper.ReservationEventMapper;
import org.campus.campussharebackend.mapper.ReservationMapper;
import org.campus.campussharebackend.mapper.ResourceMapper;
import org.campus.campussharebackend.mapper.ViolationRecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

import static org.campus.campussharebackend.service.SportsBookingRules.REASON_NO_SHOW;
import static org.campus.campussharebackend.service.SportsBookingRules.STATUS_RESERVED;
import static org.campus.campussharebackend.service.SportsBookingRules.isNoShow;
import static org.campus.campussharebackend.service.SportsBookingRules.isSportsResource;

/**
 * 体育预约超时未到处理（XIA-9 的后台逻辑，本次为配合 XIA-8 做一致性修正）。
 *
 * <p>每一条候选单独一个事务：先锁 resource 行、再锁 reservation 行，锁内重读状态与期限。
 * 取消、写事件、记违规在同一事务内完成；任一步失败整条回滚，下一轮可重试。</p>
 */
@Service
public class SportsNoShowService {

    private static final Logger log = LoggerFactory.getLogger(SportsNoShowService.class);
    private static final String VIOLATION_SPORTS_NO_SHOW = "SPORTS_NO_SHOW";

    private final ReservationMapper reservationMapper;
    private final ResourceMapper resourceMapper;
    private final ReservationEventMapper reservationEventMapper;
    private final ViolationRecordMapper violationRecordMapper;
    private final Clock clock;
    private final SportsProperties sportsProperties;

    public SportsNoShowService(ReservationMapper reservationMapper,
                               ResourceMapper resourceMapper,
                               ReservationEventMapper reservationEventMapper,
                               ViolationRecordMapper violationRecordMapper,
                               Clock clock,
                               SportsProperties sportsProperties) {
        this.reservationMapper = reservationMapper;
        this.resourceMapper = resourceMapper;
        this.reservationEventMapper = reservationEventMapper;
        this.violationRecordMapper = violationRecordMapper;
        this.clock = clock;
        this.sportsProperties = sportsProperties;
    }

    /**
     * @return 本次是否真的处理了该预约（用于调度日志）
     */
    @Transactional
    public boolean handle(long reservationId) {
        Reservation snapshot = reservationMapper.findById(reservationId);
        if (snapshot == null) {
            return false;
        }
        Resource resource = resourceMapper.findByIdForUpdate(snapshot.getResourceId());
        if (resource == null) {
            return false;
        }
        Reservation reservation = reservationMapper.findByIdForUpdate(reservationId);
        if (reservation == null || !STATUS_RESERVED.equals(reservation.getStatus())) {
            return false;
        }
        if (!isSportsResource(resource.getResourceType())) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        if (!isNoShow(reservation, now, sportsProperties.getCheckInGraceMinutes())) {
            return false;
        }
        // 带状态条件的更新：与签到并发时只有一方成功
        if (reservationMapper.cancelIfReserved(reservationId, REASON_NO_SHOW, now) != 1) {
            return false;
        }

        reservationEventMapper.insert(reservationId, "NO_SHOW", null, now, null, REASON_NO_SHOW);
        violationRecordMapper.insertViolationIfAbsent(
                reservation.getUserId(), reservationId, VIOLATION_SPORTS_NO_SHOW, now);
        log.info("体育预约 {} 超时未签到，已自动取消并记录违规", reservationId);
        return true;
    }
}
