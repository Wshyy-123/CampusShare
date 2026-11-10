package com.campusshare.lifecycle;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
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
        if (resource == null || !"LIBRARY_SEAT".equals(resource.resourceType())) { return false; }
        var reservation = mapper.lockReservation(candidate.id());
        var now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
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
