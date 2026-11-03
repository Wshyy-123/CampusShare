package org.campus.campussharebackend.service;

import org.campus.campussharebackend.config.SportsProperties;
import org.campus.campussharebackend.entity.Reservation;
import org.campus.campussharebackend.mapper.ReservationMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 定时扫描体育超时未签到的预约。
 *
 * <p>只负责找候选，真正的处理在 {@link SportsNoShowService} 的事务里重新校验，
 * 因此重复扫描、与签到并发都不会重复取消或重复记违规。</p>
 */
@Component
public class SportsTimeoutTask {

    private static final Logger log = LoggerFactory.getLogger(SportsTimeoutTask.class);

    private final ReservationMapper reservationMapper;
    private final SportsNoShowService sportsNoShowService;
    private final Clock clock;
    private final SportsProperties sportsProperties;

    public SportsTimeoutTask(ReservationMapper reservationMapper,
                             SportsNoShowService sportsNoShowService,
                             Clock clock,
                             SportsProperties sportsProperties) {
        this.reservationMapper = reservationMapper;
        this.sportsNoShowService = sportsNoShowService;
        this.clock = clock;
        this.sportsProperties = sportsProperties;
    }

    @Scheduled(cron = "${app.sports.no-show-cron:0/10 * * * * ?}")
    public void checkTimeoutReservations() {
        LocalDateTime deadline = LocalDateTime.now(clock).minusMinutes(sportsProperties.getCheckInGraceMinutes());
        List<Reservation> candidates = reservationMapper.findSportsNoShowCandidates(deadline);
        int handled = 0;
        for (Reservation candidate : candidates) {
            try {
                if (sportsNoShowService.handle(candidate.getId())) {
                    handled++;
                }
            } catch (RuntimeException exception) {
                log.warn("处理超时预约 {} 失败，将在下一轮重试", candidate.getId(), exception);
            }
        }
        if (handled > 0) {
            log.info("本轮自动取消 {} 条超时未签到的体育预约", handled);
        }
    }
}
