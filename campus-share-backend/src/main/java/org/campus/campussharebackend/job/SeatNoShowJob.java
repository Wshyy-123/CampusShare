package org.campus.campussharebackend.job;

import java.time.Clock;
import org.campus.campussharebackend.common.TimeFormats;
import org.campus.campussharebackend.lifecycle.LifecycleMapper;
import org.campus.campussharebackend.reservation.service.SeatNoShowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "campusshare.seat-no-show.enabled", havingValue = "true", matchIfMissing = true)
public class SeatNoShowJob {
    private static final Logger LOG = LoggerFactory.getLogger(SeatNoShowJob.class);
    private final LifecycleMapper mapper;
    private final SeatNoShowService service;
    private final Clock clock;

    public SeatNoShowJob(LifecycleMapper mapper, SeatNoShowService service,
                         @Qualifier("lifecycleClock") Clock clock) {
        this.mapper = mapper;
        this.service = service;
        this.clock = clock;
    }

    // 启动立即补查停机积压；单条事务失败后继续，下一轮重试。
    @Scheduled(fixedDelayString = "${campusshare.seat-no-show.scan-delay-ms:30000}", initialDelay = 0)
    public void scan() {
        var cutoff = TimeFormats.now(clock).minusMinutes(30);
        long afterId = 0;
        while (true) {
            var batch = mapper.findExpiredSeats(cutoff, afterId, 100);
            if (batch.isEmpty()) {
                return;
            }
            for (var candidate : batch) {
                try {
                    service.expire(candidate);
                } catch (RuntimeException failure) {
                    LOG.error("Seat expiry failed: reservationId={}, resourceId={}; retry next scan",
                            candidate.id(), candidate.resourceId(), failure);
                }
                afterId = candidate.id();
            }
        }
    }
}
