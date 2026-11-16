package com.campusshare.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

@EnabledIfEnvironmentVariable(named = "LIFECYCLE_MYSQL_TEST", matches = "true")
@SpringBootTest(classes = LifecycleMysqlTest.App.class, properties = {
        "spring.datasource.url=${LIFECYCLE_TEST_DB_URL}",
        "spring.datasource.username=${LIFECYCLE_TEST_DB_USER:root}",
        "spring.datasource.password=${LIFECYCLE_TEST_DB_PASSWORD}",
        "campusshare.seat-no-show.enabled=false"})
@AutoConfigureMockMvc
class LifecycleMysqlTest {
    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import({LifecycleConfiguration.class, TimeConfig.class})
    static class App {}

    @TestConfiguration
    static class TimeConfig {
        @Bean
        @Primary
        @Qualifier("lifecycleClock")
        MutableClock testClock() { return new MutableClock(); }
    }

    static class MutableClock extends Clock {
        private final AtomicReference<Instant> value = new AtomicReference<>();
        void set(String time) { value.set(OffsetDateTime.parse(time).toInstant()); }
        public ZoneId getZone() { return ZoneId.of("Asia/Shanghai"); }
        public Clock withZone(ZoneId zone) { return Clock.fixed(instant(), zone); }
        public Instant instant() { return value.get(); }
    }

    @Autowired DataSource dataSource;
    @Autowired JdbcTemplate jdbc;
    @Autowired MutableClock clock;
    @Autowired ResourceDisableService disable;
    @Autowired SeatNoShowService expiry;
    @Autowired ResourceAvailabilityGuard guard;
    @Autowired LifecycleMapper mapper;
    @Autowired TransactionTemplate tx;
    @Autowired MockMvc mvc;

    @BeforeEach
    void setup() throws Exception {
        // Never initialize or clear a shared/development database.
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.getCatalog()).isEqualTo("campusshare_lifecycle_test");
        }
        new ResourceDatabasePopulator(new ClassPathResource("lifecycle-schema.sql")).execute(dataSource);
        for (var table : List.of("violation_record", "reservation_event", "reservation", "resource_block", "resource", "app_user")) {
            jdbc.update("DELETE FROM " + table);
        }
        jdbc.update("INSERT INTO app_user VALUES (1,'admin','RESOURCE_ADMIN',true),(2,'student','STUDENT',true)");
        jdbc.update("INSERT INTO resource VALUES (101,'LIBRARY_SEAT',true),(102,'BADMINTON',true)");
        clock.set("2026-11-01T10:30:00+08:00");
        admin();
    }

    private void admin() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "admin", "unused", List.of(new SimpleGrantedAuthority("ROLE_RESOURCE_ADMIN"))));
    }

    @AfterEach
    void clearIdentity() { SecurityContextHolder.clearContext(); }

    private void reservation(long id, long resource, String start, String end, String accepted, String state) {
        var acceptedAt = LocalDateTime.parse(accepted);
        jdbc.update("""
                INSERT INTO reservation(id,user_id,resource_id,client_request_id,start_at,end_at,
                  accepted_at,arrival_deadline,status,check_in_at,created_at,updated_at)
                VALUES(?,2,?,?,?, ?,?,?,?, ?,?,?)
                """, id, resource, "test-" + id, LocalDateTime.parse(start), LocalDateTime.parse(end),
                acceptedAt, acceptedAt.plusMinutes(30), state,
                "CHECKED_IN".equals(state) ? acceptedAt.plusMinutes(1) : null, acceptedAt, acceptedAt);
    }

    private void seat(long id, String state) {
        reservation(id, 101, "2026-11-01T10:00:00", "2026-11-01T12:00:00", "2026-11-01T10:00:00", state);
    }

    private ResourceDisableService.Result disable(String start, String end) {
        return disable.disable(101, OffsetDateTime.parse(start), OffsetDateTime.parse(end), "维修", "test-disable");
    }

    private long count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class); }
    private String state(long id) { return jdbc.queryForObject("SELECT status FROM reservation WHERE id=?", String.class, id); }

    @Test
    void disableCancelsOnlyOverlappingActiveReservationsAndKeepsHistory() {
        seat(10, "RESERVED");
        reservation(11, 101, "2026-11-01T12:00:00", "2026-11-01T13:00:00", "2026-11-01T10:00:00", "RESERVED");
        reservation(12, 101, "2026-10-31T10:00:00", "2026-10-31T12:00:00", "2026-10-31T10:00:00", "COMPLETED");
        seat(13, "CANCELLED");
        reservation(14, 102, "2026-11-01T10:00:00", "2026-11-01T12:00:00", "2026-11-01T10:00:00", "RESERVED");
        var result = disable("2026-11-01T10:30:00+08:00", "2026-11-01T12:00:00+08:00");
        assertThat(result.cancelledReservationIds()).containsExactly("10");
        assertThat(state(10)).isEqualTo("CANCELLED");
        assertThat(state(11)).isEqualTo("RESERVED");
        assertThat(state(12)).isEqualTo("COMPLETED");
        assertThat(state(14)).isEqualTo("RESERVED");
        assertThat(count("reservation")).isEqualTo(5);
        assertThat(count("reservation_event")).isEqualTo(1);
        assertThat(count("violation_record")).isZero();
        assertThat(jdbc.queryForObject("SELECT cancel_reason FROM reservation WHERE id=10", String.class)).isEqualTo("RESOURCE_DISABLED");
        assertThat(jdbc.queryForObject("SELECT operator_id FROM reservation_event", Long.class)).isEqualTo(1);
        assertThatThrownBy(() -> tx.executeWithoutResult(s -> guard.assertNotBlocked(101,
                LocalDateTime.parse("2026-11-01T11:00:00"), LocalDateTime.parse("2026-11-01T11:30:00"))))
                .isInstanceOf(LifecycleException.class).hasMessageContaining("停用");
        tx.executeWithoutResult(s -> guard.assertNotBlocked(101,
                LocalDateTime.parse("2026-11-01T12:00:00"), LocalDateTime.parse("2026-11-01T13:00:00")));
    }

    @Test
    void inUseReservationRejectsWholeDisable() {
        seat(10, "RESERVED"); seat(11, "CHECKED_IN");
        assertThatThrownBy(() -> disable("2026-11-01T10:00:00+08:00", "2026-11-01T12:00:00+08:00"))
                .isInstanceOf(LifecycleException.class).hasMessageContaining("已开始使用");
        assertThat(state(10)).isEqualTo("RESERVED");
        assertThat(state(11)).isEqualTo("CHECKED_IN");
        assertThat(count("resource_block")).isZero();
        assertThat(count("reservation_event")).isZero();
    }

    @Test
    void earlyCheckInCanBeCancelledBeforeUseStartsAndOffsetsAreNormalized() {
        reservation(10, 101, "2026-11-01T11:00:00", "2026-11-01T12:00:00", "2026-11-01T10:00:00", "CHECKED_IN");
        disable("2026-11-01T03:00:00Z", "2026-11-01T04:00:00Z");
        assertThat(state(10)).isEqualTo("CANCELLED");
        assertThat(jdbc.queryForObject("SELECT start_at FROM resource_block", LocalDateTime.class))
                .isEqualTo(LocalDateTime.parse("2026-11-01T11:00:00"));
    }

    @Test
    void disableRollsBackBlockAndAllCancellationsIfHistoryCannotBeWritten() {
        seat(10, "RESERVED");
        assertThatThrownBy(() -> disable.disable(101, OffsetDateTime.parse("2026-11-01T10:00:00+08:00"),
                OffsetDateTime.parse("2026-11-01T12:00:00+08:00"), "维修", null)).isInstanceOf(RuntimeException.class);
        assertThat(state(10)).isEqualTo("RESERVED");
        assertThat(count("resource_block")).isZero();
        assertThat(count("reservation_event")).isZero();
    }

    @Test
    void unauthenticatedAndNonAdminCannotDisable() {
        SecurityContextHolder.clearContext();
        assertThatThrownBy(() -> disable("2026-11-01T10:00:00+08:00", "2026-11-01T12:00:00+08:00"))
                .isInstanceOfSatisfying(LifecycleException.class, e -> assertThat(e.status()).isEqualTo(401));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "student", "unused", List.of(new SimpleGrantedAuthority("ROLE_RESOURCE_ADMIN"))));
        assertThatThrownBy(() -> disable("2026-11-01T10:00:00+08:00", "2026-11-01T12:00:00+08:00"))
                .isInstanceOfSatisfying(LifecycleException.class, e -> assertThat(e.status()).isEqualTo(403));
        assertThat(count("resource_block")).isZero();
    }

    @Test
    void expiresAtDeadlineOnlyOnceWithoutBrowserAndPreservesNewReservation() {
        seat(10, "RESERVED");
        var candidate = new LifecycleMapper.Candidate(10, 101);
        clock.set("2026-11-01T10:29:59.999+08:00");
        assertThat(expiry.expire(candidate)).isFalse();
        clock.set("2026-11-01T10:30:00+08:00");
        new SeatNoShowJob(mapper, expiry, clock).scan();
        assertThat(state(10)).isEqualTo("CANCELLED");
        assertThat(count("violation_record")).isEqualTo(1);
        assertThat(count("reservation_event")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT cancel_reason FROM reservation WHERE id=10", String.class)).isEqualTo("NO_SHOW");
        reservation(11, 101, "2026-11-01T10:30:00", "2026-11-01T12:00:00", "2026-11-01T10:30:00", "RESERVED");
        assertThat(expiry.expire(candidate)).isFalse();
        new SeatNoShowJob(mapper, expiry, clock).scan();
        assertThat(state(11)).isEqualTo("RESERVED");
        assertThat(count("violation_record")).isEqualTo(1);
        tx.executeWithoutResult(s -> guard.assertNotBlocked(101,
                LocalDateTime.parse("2026-11-01T10:30:00"), LocalDateTime.parse("2026-11-01T12:00:00")));
    }

    @Test
    void timelyArrivalAndOtherResourceTypesAreNotExpired() {
        seat(10, "CHECKED_IN");
        reservation(11, 102, "2026-11-01T10:00:00", "2026-11-01T12:00:00", "2026-11-01T10:00:00", "RESERVED");
        clock.set("2026-11-01T11:00:00+08:00");
        new SeatNoShowJob(mapper, expiry, clock).scan();
        assertThat(expiry.expire(new LifecycleMapper.Candidate(11, 102))).isFalse();
        assertThat(state(10)).isEqualTo("CHECKED_IN");
        assertThat(state(11)).isEqualTo("RESERVED");
        assertThat(count("violation_record")).isZero();
    }

    @Test
    void cancelledReservationIsNotPenalizedAgainAndDowntimeBacklogIsProcessed() {
        seat(10, "CANCELLED");
        seat(11, "RESERVED");
        clock.set("2026-11-02T10:30:00+08:00");
        new SeatNoShowJob(mapper, expiry, clock).scan();
        assertThat(state(11)).isEqualTo("CANCELLED");
        assertThat(count("violation_record")).isEqualTo(1);
    }

    @Test
    void expiryRollsBackCancellationWhenViolationInsertFails() {
        seat(10, "RESERVED");
        jdbc.update("INSERT INTO violation_record(reservation_id,user_id,violation_type,occurred_at) VALUES(10,2,'NO_SHOW',NOW(3))");
        assertThatThrownBy(() -> expiry.expire(new LifecycleMapper.Candidate(10, 101))).isInstanceOf(RuntimeException.class);
        assertThat(state(10)).isEqualTo("RESERVED");
        assertThat(count("reservation_event")).isZero();
    }

    @Test
    void concurrentWorkersCancelAndPenalizeOnce() throws Exception {
        seat(10, "RESERVED");
        try (var pool = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1);
            var first = pool.submit(() -> { start.await(); return expiry.expire(new LifecycleMapper.Candidate(10, 101)); });
            var second = pool.submit(() -> { start.await(); return expiry.expire(new LifecycleMapper.Candidate(10, 101)); });
            start.countDown();
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        }
        assertThat(count("violation_record")).isEqualTo(1);
        assertThat(count("reservation_event")).isEqualTo(1);
    }

    @Test
    void arrivalUnderResourceLockWinsAgainstStaleExpiryCandidate() throws Exception {
        seat(10, "RESERVED");
        clock.set("2026-11-01T10:29:59+08:00");
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var arrival = pool.submit(() -> tx.executeWithoutResult(s -> {
                mapper.lockResource(101);
                mapper.lockReservation(10);
                assertThat(SeatArrivalDeadline.expired(LocalDateTime.parse("2026-11-01T10:00:00"), LocalDateTime.now(clock))).isFalse();
                jdbc.update("UPDATE reservation SET status='CHECKED_IN',check_in_at=? WHERE id=10", LocalDateTime.now(clock));
                locked.countDown();
                try { assertThat(release.await(5, TimeUnit.SECONDS)).isTrue(); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new RuntimeException(e); }
            }));
            assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();
            clock.set("2026-11-01T10:30:00+08:00");
            var expired = pool.submit(() -> expiry.expire(new LifecycleMapper.Candidate(10, 101)));
            release.countDown();
            arrival.get(10, TimeUnit.SECONDS);
            assertThat(expired.get(10, TimeUnit.SECONDS)).isFalse();
        }
        assertThat(state(10)).isEqualTo("CHECKED_IN");
        assertThat(count("violation_record")).isZero();
    }

    @Test
    void disableAndExpiryRaceProducesOneCancellationWithoutDuplicateEvents() throws Exception {
        seat(10, "RESERVED");
        try (var pool = Executors.newFixedThreadPool(2)) {
            var start = new CountDownLatch(1);
            var disabled = pool.submit(() -> {
                start.await();
                admin();
                try { return disable("2026-11-01T10:00:00+08:00", "2026-11-01T12:00:00+08:00"); }
                finally { SecurityContextHolder.clearContext(); }
            });
            var expired = pool.submit(() -> { start.await(); return expiry.expire(new LifecycleMapper.Candidate(10, 101)); });
            start.countDown();
            disabled.get(10, TimeUnit.SECONDS);
            var noShowWon = expired.get(10, TimeUnit.SECONDS);
            assertThat(count("violation_record")).isEqualTo(noShowWon ? 1 : 0);
        }
        assertThat(state(10)).isEqualTo("CANCELLED");
        assertThat(count("resource_block")).isEqualTo(1);
        assertThat(count("reservation_event")).isEqualTo(1);
    }

    @Test
    void bookingGuardSeesCommittedDisableEvenAfterOldRepeatableReadSnapshot() throws Exception {
        try (var pool = Executors.newSingleThreadExecutor()) {
            tx.executeWithoutResult(s -> {
                assertThat(count("resource_block")).isZero(); // establishes an old RR snapshot
                var write = pool.submit(() -> {
                    admin();
                    try { return disable("2026-11-01T10:00:00+08:00", "2026-11-01T12:00:00+08:00"); }
                    finally { SecurityContextHolder.clearContext(); }
                });
                try { write.get(10, TimeUnit.SECONDS); }
                catch (Exception e) { throw new RuntimeException(e); }
                assertThatThrownBy(() -> guard.assertNotBlocked(101,
                        LocalDateTime.parse("2026-11-01T10:30:00"), LocalDateTime.parse("2026-11-01T11:00:00")))
                        .isInstanceOfSatisfying(LifecycleException.class,
                                e -> assertThat(e.code()).isEqualTo("RESOURCE_UNAVAILABLE"));
                s.setRollbackOnly();
            });
        }
    }

    @Test
    void releasedSeatCanBeReusedWithinOpeningAndWithoutReservationConflict() {
        seat(10, "RESERVED");
        expiry.expire(new LifecycleMapper.Candidate(10, 101));
        tx.executeWithoutResult(s -> {
            var start = LocalDateTime.parse("2026-11-01T10:30:00");
            var end = LocalDateTime.parse("2026-11-01T12:00:00");
            guard.assertNotBlocked(101, start, end);
            assertThat(mapper.lockAffected(101, start, end)).isEmpty();
            // Simulates the other story's INSERT after availability/opening validation.
            reservation(11, 101, start.toString(), end.toString(), "2026-11-01T10:30:00", "RESERVED");
        });
        assertThat(state(10)).isEqualTo("CANCELLED");
        assertThat(state(11)).isEqualTo("RESERVED");
        assertThat(count("reservation")).isEqualTo(2);
    }

    @Test
    void endpointValidatesInputAndReturnsStringIds() throws Exception {
        seat(10, "RESERVED");
        mvc.perform(post("/api/v1/admin/resources/101/disable-periods").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"startTime":"2026-11-01T10:30:00+08:00","endTime":"2026-11-01T12:00:00+08:00","reason":"维修"}
                        """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.resourceId").value("101"))
                .andExpect(jsonPath("$.data.cancelledReservationIds[0]").value("10"))
                .andExpect(jsonPath("$.requestId").isString());
        mvc.perform(post("/api/v1/admin/resources/101/disable-periods").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
        mvc.perform(post("/api/v1/admin/resources/101/disable-periods").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"startTime":"2026-11-01T12:00:00+08:00","endTime":"2026-11-01T11:00:00+08:00","reason":"维修"}
                        """))
                .andExpect(status().isBadRequest());
    }
}
