package com.campusshare.lifecycle;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class SeatNoShowJobTest {
    @Test
    void failedCandidateDoesNotStarveLaterCandidatesOrNextScan() {
        var mapper = mock(LifecycleMapper.class);
        var service = mock(SeatNoShowService.class);
        var first = new LifecycleMapper.Candidate(1, 101);
        var second = new LifecycleMapper.Candidate(2, 102);
        when(mapper.findExpiredSeats(any(), eq(0L), eq(100))).thenReturn(List.of(first, second));
        when(mapper.findExpiredSeats(any(), eq(2L), eq(100))).thenReturn(List.of());
        when(service.expire(first)).thenThrow(new IllegalStateException("simulated transaction failure"));
        var clock = Clock.fixed(Instant.parse("2026-11-01T02:30:00Z"), ZoneId.of("Asia/Shanghai"));
        new SeatNoShowJob(mapper, service, clock).scan();
        verify(service).expire(first);
        verify(service).expire(second);
        verify(mapper).findExpiredSeats(java.time.LocalDateTime.parse("2026-11-01T10:00:00"), 0, 100);
        verify(mapper).findExpiredSeats(java.time.LocalDateTime.parse("2026-11-01T10:00:00"), 2, 100);
    }
}
