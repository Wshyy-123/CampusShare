package org.campus.campussharebackend.reservation.service;

import java.time.Clock;
import java.util.UUID;
import org.campus.campussharebackend.common.TimeFormats;
import org.campus.campussharebackend.lifecycle.LifecycleMapper;
import org.campus.campussharebackend.lifecycle.SeatArrivalDeadline;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SeatNoShowService {
    private final LifecycleMapper mapper;
    private final Clock clock;

    public SeatNoShowService(LifecycleMapper mapper, @Qualifier("lifecycleClock") Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional
    public boolean expire(LifecycleMapper.Candidate candidate) {
        var resource = mapper.lockResource(candidate.resourceId());
        if (resource == null || !"LIBRARY_SEAT".equals(resource.resourceType())) {
            return false;
        }
        var reservation = mapper.lockReservation(candidate.id());
        // 等待锁后重新取时间和状态，不能拿扫描时的快照裁定。
        var now = TimeFormats.now(clock);
        if (reservation == null || reservation.resourceId() != candidate.resourceId()
                || !"RESERVED".equals(reservation.status()) || reservation.checkInAt() != null
                || !SeatArrivalDeadline.expired(reservation.acceptedAt(), now)) {
            return false;
        }
        if (mapper.cancel(reservation.id(), "NO_SHOW", now) != 1) {
            throw new IllegalStateException("Locked reservation could not be cancelled");
        }
        mapper.insertViolation(reservation.id(), reservation.userId(), now);
        mapper.insertEvent(reservation.id(), null, now, UUID.randomUUID().toString(), "NO_SHOW");
        return true;
    }
}
