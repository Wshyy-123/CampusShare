package org.campus.campussharebackend.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.LocalDateTime;
import org.campus.campussharebackend.CampusShareBackendApplication;
import org.campus.campussharebackend.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

@SpringBootTest(classes = CampusShareBackendApplication.class, properties = {
        "campusshare.seat-no-show.enabled=true", "campusshare.seat-no-show.scan-delay-ms=100"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SeatNoShowSchedulingIntegrationTest extends IntegrationTestBase {
    @Test
    void applicationSchedulerReleasesWithoutHttpOrManualInvocation() {
        LocalDateTime start = tomorrowAt(14, 0);
        long id = insertReservation(1, 101, "scheduled-release", start, start.plusHours(1), "RESERVED", null, null);
        LocalDateTime accepted = LocalDateTime.now(ZONE).minusMinutes(31);
        jdbcTemplate.update("UPDATE reservation SET accepted_at = ?, arrival_deadline = ? WHERE id = ?", accepted, accepted.plusMinutes(30), id);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(jdbcTemplate.queryForObject("SELECT status FROM reservation WHERE id = ?", String.class, id)).isEqualTo("CANCELLED");
            assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM violation_record WHERE reservation_id = ?", Integer.class, id)).isEqualTo(1);
            assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reservation_event WHERE reservation_id = ? AND reason = 'NO_SHOW'", Integer.class, id)).isEqualTo(1);
        });
    }
}
