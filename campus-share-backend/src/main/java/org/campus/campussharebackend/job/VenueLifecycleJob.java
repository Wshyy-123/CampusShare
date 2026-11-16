package org.campus.campussharebackend.job;

import java.time.Clock;
import org.campus.campussharebackend.common.TimeFormats;
import org.campus.campussharebackend.mapper.ReservationMapper;
import org.campus.campussharebackend.service.SportsNoShowService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "campusshare.venue-lifecycle.enabled", havingValue = "true", matchIfMissing = true)
public class VenueLifecycleJob {
    private static final Logger LOG = LoggerFactory.getLogger(VenueLifecycleJob.class);
    private final ReservationMapper mapper;
    private final SportsNoShowService service;
    private final Clock clock;

    public VenueLifecycleJob(ReservationMapper mapper, SportsNoShowService service,
                             @Qualifier("lifecycleClock") Clock clock) {
        this.mapper = mapper;
        this.service = service;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${campusshare.venue-lifecycle.scan-delay-ms:10000}", initialDelay = 0)
    public void scan() {
        scanKind(true);
        scanKind(false);
    }

    private void scanKind(boolean end) {
        long after = 0;
        var now = TimeFormats.now(clock);
        while (true) {
            var rows = end ? mapper.findVenueEnds(now, after, 100)
                    : mapper.findVenueNoShows(now.minusMinutes(15), after, 100);
            if (rows.isEmpty()) return;
            for (var row : rows) {
                try {
                    if (end) service.complete(row.getId());
                    else service.handle(row.getId());
                } catch (RuntimeException ex) {
                    LOG.error("Venue lifecycle failed: reservation={}, end={}; retry next scan", row.getId(), end, ex);
                }
                after = row.getId();
            }
        }
    }
}
