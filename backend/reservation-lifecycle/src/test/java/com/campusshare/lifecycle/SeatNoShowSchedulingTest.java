package com.campusshare.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

class SeatNoShowSchedulingTest {
    @Configuration
    @EnableScheduling
    static class Scheduling {}

    @Test
    void scheduledScannerRunsAtStartupWithoutAnyHttpRequest() throws Exception {
        var scanned = new CountDownLatch(1);
        var mapper = mock(LifecycleMapper.class);
        when(mapper.findExpiredSeats(any(), anyLong(), anyInt())).thenAnswer(invocation -> {
            scanned.countDown();
            return List.of();
        });
        var service = mock(SeatNoShowService.class);
        var clock = Clock.fixed(Instant.parse("2026-11-01T02:30:00Z"), ZoneId.of("Asia/Shanghai"));
        try (var context = new AnnotationConfigApplicationContext()) {
            context.register(Scheduling.class);
            context.registerBean(SeatNoShowJob.class, () -> new SeatNoShowJob(mapper, service, clock));
            context.refresh();
            assertThat(scanned.await(5, TimeUnit.SECONDS)).isTrue();
        }
    }
}
