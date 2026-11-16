package org.campus.campussharebackend.service;

import org.campus.campussharebackend.config.SportsProperties;
import org.campus.campussharebackend.entity.Reservation;
import org.campus.campussharebackend.mapper.ReservationMapper;
import org.campus.campussharebackend.mapper.ViolationRecordMapper;
import org.campus.campussharebackend.reservation.mapper.ReservationRepository;
import org.campus.campussharebackend.resource.entity.ResourceRecord;
import org.campus.campussharebackend.resource.mapper.ResourceMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

import static org.campus.campussharebackend.service.SportsBookingRules.REASON_NO_SHOW;
import static org.campus.campussharebackend.service.SportsBookingRules.STATUS_RESERVED;
import static org.campus.campussharebackend.service.SportsBookingRules.isNoShow;
import static org.campus.campussharebackend.service.SportsBookingRules.isSportsResource;

/**
 * 教室/体育未到与到期处理；保留同学的服务类名，座位/讨论室不套用本服务。
 *
 * <p>每条候选单独一个事务：先锁 resource、再锁 reservation，锁内重读状态与期限；
 * 取消、写事件、记违规同事务提交，任一步失败整条回滚，下一轮可重试。</p>
 */
@Service
public class SportsNoShowService {

    private static final Logger LOG = LoggerFactory.getLogger(SportsNoShowService.class);
    private static final String VIOLATION_SPORTS_NO_SHOW = "SPORTS_NO_SHOW";

    private final ReservationMapper reservationMapper;
    private final ResourceMapper resourceMapper;
    private final ReservationRepository reservationRepository;
    private final ViolationRecordMapper violationRecordMapper;
    private final Clock clock;
    private final SportsProperties sportsProperties;

    public SportsNoShowService(ReservationMapper reservationMapper,
                               ResourceMapper resourceMapper,
                               ReservationRepository reservationRepository,
                               ViolationRecordMapper violationRecordMapper,
                               @Qualifier("lifecycleClock") Clock clock,
                               SportsProperties sportsProperties) {
        this.reservationMapper = reservationMapper;
        this.resourceMapper = resourceMapper;
        this.reservationRepository = reservationRepository;
        this.violationRecordMapper = violationRecordMapper;
        this.clock = clock;
        this.sportsProperties = sportsProperties;
    }

    @Transactional
    public boolean complete(long reservationId) {
        var hint = reservationMapper.findById(reservationId);
        if (hint == null) return false;
        var resource = resourceMapper.lockById(hint.getResourceId());
        if (resource == null || (!isSportsResource(resource.getResourceType()) && !"CLASSROOM".equals(resource.getResourceType()))) return false;
        var reservation = reservationMapper.findByIdForUpdate(reservationId);
        var now = org.campus.campussharebackend.common.TimeFormats.now(clock);
        if (reservation == null || !"CHECKED_IN".equals(reservation.getStatus()) || now.isBefore(reservation.getEndAt())) return false;
        if (reservationMapper.completeAtEnd(reservationId, now) != 1) return false;
        reservationRepository.insertEvent(reservationId, "COMPLETED", null, now, java.util.UUID.randomUUID().toString(), "AUTO_END");
        return true;
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
        ResourceRecord resource = resourceMapper.lockById(snapshot.getResourceId());
        if (resource == null) {
            return false;
        }
        Reservation reservation = reservationMapper.findByIdForUpdate(reservationId);
        if (reservation == null || reservation.getCheckInAt() != null || !STATUS_RESERVED.equals(reservation.getStatus())) {
            return false;
        }
        if (!isSportsResource(resource.getResourceType()) && !"CLASSROOM".equals(resource.getResourceType())) {
            return false;
        }

        LocalDateTime now = org.campus.campussharebackend.common.TimeFormats.now(clock);
        if (!isNoShow(reservation, now, sportsProperties.getCheckInGraceMinutes())) {
            return false;
        }
        // 带状态条件的更新：与签到并发时只有一方成功
        if (reservationMapper.cancelIfReserved(reservationId, REASON_NO_SHOW, now) != 1) {
            return false;
        }

        reservationRepository.insertEvent(reservationId, "NO_SHOW", null, now,
                "system-sports-no-show", "NO_SHOW");
        if (isSportsResource(resource.getResourceType())) violationRecordMapper.insertViolationIfAbsent(
                reservation.getUserId(), reservationId, VIOLATION_SPORTS_NO_SHOW, now);
        LOG.info("地点预约 {} 超时未签到，已按资源规则取消", reservationId);
        return true;
    }
}
