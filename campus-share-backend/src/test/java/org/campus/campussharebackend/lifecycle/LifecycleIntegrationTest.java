package org.campus.campussharebackend.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.campus.campussharebackend.job.SeatNoShowJob;
import org.campus.campussharebackend.reservation.service.SeatNoShowService;
import org.campus.campussharebackend.support.IntegrationTestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.ResultActions;

@WithMockUser(username = "admin01")
class LifecycleIntegrationTest extends IntegrationTestBase {
    @MockitoBean(name = "lifecycleClock") Clock clock;
    @MockitoSpyBean LifecycleMapper mapper;
    @Autowired SeatNoShowService seatNoShow;
    private LocalDateTime now;

    @BeforeEach
    void setClock() {
        now = tomorrowAt(10, 30);
        at(now);
    }

    private void at(LocalDateTime time) {
        when(clock.getZone()).thenReturn(ZONE);
        when(clock.instant()).thenReturn(time.atZone(ZONE).toInstant());
    }

    @Test
    void disableCancelsOnlyOverlappingActiveBookingsAndKeepsHistory() throws Exception {
        long affected = booking(101, 12, 14, "RESERVED", null);
        long before = booking(101, 10, 12, "RESERVED", null);
        long after = booking(101, 14, 15, "RESERVED", null);
        long completed = booking(101, 12, 13, "COMPLETED", now.minusHours(2));
        long cancelled = booking(101, 12, 13, "CANCELLED", null);
        long other = booking(102, 12, 14, "RESERVED", null);
        disable(101, tomorrowAt(12, 0), tomorrowAt(14, 0))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("disable-integration"))
                .andExpect(jsonPath("$.data.resourceId").value("101"))
                .andExpect(jsonPath("$.data.cancelledReservationIds[0]").value(Long.toString(affected)))
                .andExpect(jsonPath("$.data.cancelledReservationIds.length()").value(1));
        assertThat(state(affected)).isEqualTo("CANCELLED");
        for (long id : List.of(before, after, other)) assertThat(state(id)).isEqualTo("RESERVED");
        assertThat(state(completed)).isEqualTo("COMPLETED");
        assertThat(state(cancelled)).isEqualTo("CANCELLED");
        assertThat(count("reservation")).isEqualTo(6);
        assertThat(count("violation_record")).isZero();
        assertThat(jdbcTemplate.queryForMap("SELECT operator_id, request_id, reason FROM reservation_event WHERE reservation_id = ?", affected))
                .containsEntry("operator_id", 4L).containsEntry("request_id", "disable-integration")
                .containsEntry("reason", "RESOURCE_DISABLED");
        mockMvc.perform(get("/api/v1/reservations/" + affected).with(user("student01")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.cancelReason").value("RESOURCE_DISABLED"));
        create(101, 12, 13, "after-disable").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESOURCE_UNAVAILABLE"));
        // Touching the right endpoint is allowed; choose a free slot beyond the existing booking.
        create(101, 15, 16, "outside-disable").andExpect(status().isCreated());
    }

    @Test
    void inUseConflictRollsBackWholeDisable() throws Exception {
        long unused = booking(101, 10, 12, "RESERVED", null);
        long inUse = booking(101, 10, 11, "CHECKED_IN", now.minusMinutes(20));
        disable(101, tomorrowAt(10, 0), tomorrowAt(12, 0))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INVALID_STATE"));
        assertThat(state(unused)).isEqualTo("RESERVED");
        assertThat(state(inUse)).isEqualTo("CHECKED_IN");
        assertThat(count("resource_block")).isZero();
        assertThat(count("reservation_event")).isZero();
    }

    @Test
    void earlyArrivalCanBeCancelledButAtStartIsAlreadyInUse() throws Exception {
        long early = booking(101, 11, 12, "CHECKED_IN", now.minusMinutes(5));
        disable(101, tomorrowAt(11, 0), tomorrowAt(12, 0)).andExpect(status().isOk());
        assertThat(state(early)).isEqualTo("CANCELLED");
        booking(102, 11, 12, "CHECKED_IN", now);
        at(tomorrowAt(11, 0));
        disable(102, tomorrowAt(11, 0), tomorrowAt(12, 0)).andExpect(status().isConflict());
        assertThat(count("violation_record")).isZero();
    }

    @Test
    void emptyDisableStillBlocksAndHalfOpenEndpointsRemainBookable() throws Exception {
        disable(101, tomorrowAt(12, 0), tomorrowAt(13, 0)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cancelledReservationIds").isEmpty());
        create(101, 12, 13, "blocked").andExpect(status().isConflict());
        create(101, 11, 12, "left-boundary").andExpect(status().isCreated());
        create(101, 13, 14, "right-boundary").andExpect(status().isCreated());
    }

    @Test
    void adminRoleIsCheckedAgainstDatabaseAndSharedCsrfIsRequired() throws Exception {
        String body = disableBody(tomorrowAt(12, 0), tomorrowAt(13, 0));
        mockMvc.perform(post(disablePath(101)).with(csrf()).with(user("student01").roles("RESOURCE_ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mockMvc.perform(post(disablePath(101)).with(csrf()).with(anonymous())
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(disablePath(101)).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_INVALID"));
        jdbcTemplate.update("UPDATE app_user SET enabled = FALSE WHERE id = 4");
        try {
            disable(101, tomorrowAt(12, 0), tomorrowAt(13, 0)).andExpect(status().isUnauthorized());
        } finally {
            jdbcTemplate.update("UPDATE app_user SET enabled = TRUE WHERE id = 4");
        }
        assertThat(count("resource_block")).isZero();
    }

    @Test
    void validatesRangeAndResourceAndConvertsOffset() throws Exception {
        disable(101, now, now).andExpect(status().isBadRequest());
        disable(101, now, now.minusMinutes(1)).andExpect(status().isBadRequest());
        disable(99999, now, now.plusHours(1)).andExpect(status().isNotFound());
        for (String reason : List.of(" ", "x".repeat(501))) {
            mockMvc.perform(post(disablePath(101)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("startTime", iso(now), "endTime", iso(now.plusHours(1)), "reason", reason))))
                    .andExpect(status().isBadRequest());
        }
        mockMvc.perform(post(disablePath(101)).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("startTime", "2026-11-13T04:00:00Z", "endTime", "2026-11-13T05:00:00Z", "reason", "检修"))))
                .andExpect(status().isOk());
        assertThat(jdbcTemplate.queryForObject("SELECT start_at FROM resource_block", LocalDateTime.class))
                .isEqualTo(LocalDateTime.parse("2026-11-13T12:00:00"));
    }

    @Test
    void auditFailureRollsBackBlockAndAllCancellations() throws Exception {
        long first = booking(101, 12, 13, "RESERVED", null);
        long second = booking(101, 13, 14, "RESERVED", null);
        doThrow(new IllegalStateException("injected audit failure"))
                .when(mapper).insertEvent(eq(second), any(), any(), anyString(), anyString());
        disable(101, tomorrowAt(12, 0), tomorrowAt(14, 0)).andExpect(status().isInternalServerError());
        assertThat(state(first)).isEqualTo("RESERVED");
        assertThat(state(second)).isEqualTo("RESERVED");
        assertThat(count("resource_block")).isZero();
        assertThat(count("reservation_event")).isZero();
    }

    @Test
    void seatExpiresExactlyThirtyMinutesAfterAcceptanceAndOnlyOnce() throws Exception {
        long id = seat(101, "RESERVED", null);
        var candidate = new LifecycleMapper.Candidate(id, 101);
        at(now.minusNanos(1_000_000));
        assertThat(seatNoShow.expire(candidate)).isFalse();
        at(now);
        assertThat(seatNoShow.expire(candidate)).isTrue();
        assertThat(seatNoShow.expire(candidate)).isFalse();
        assertThat(state(id)).isEqualTo("CANCELLED");
        assertThat(jdbcTemplate.queryForObject("SELECT cancel_reason FROM reservation WHERE id = ?", String.class, id)).isEqualTo("NO_SHOW");
        assertThat(count("violation_record")).isEqualTo(1);
        assertThat(count("reservation_event")).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT operator_id FROM reservation_event", Long.class)).isNull();
        create(101, 14, 15, "released-slot").andExpect(status().isCreated());
    }

    @Test
    void arrivalAndTerminalStatesAndOtherResourceTypesAreNeverExpired() {
        long arrived = seat(101, "CHECKED_IN", now.minusSeconds(1));
        long arrivedButStatusLag = seat(101, "RESERVED", now.minusSeconds(1));
        long completed = seat(101, "COMPLETED", now.minusMinutes(10));
        long cancelled = seat(101, "CANCELLED", null);
        long sports = seat(201, "RESERVED", null);
        for (long id : List.of(arrived, arrivedButStatusLag, completed, cancelled))
            assertThat(seatNoShow.expire(new LifecycleMapper.Candidate(id, 101))).isFalse();
        assertThat(seatNoShow.expire(new LifecycleMapper.Candidate(sports, 201))).isFalse();
        assertThat(seatNoShow.expire(new LifecycleMapper.Candidate(arrived, 102))).isFalse();
        assertThat(count("violation_record")).isZero();
        assertThat(count("reservation_event")).isZero();
    }

    @Test
    void noShowDoesNotRemoveTeachingOrDisableBlocks() throws Exception {
        long id = seat(101, "RESERVED", null);
        seedTeachingBlock(101, tomorrowAt(14, 0), tomorrowAt(15, 0));
        assertThat(seatNoShow.expire(new LifecycleMapper.Candidate(id, 101))).isTrue();
        create(101, 14, 15, "still-teaching").andExpect(status().isConflict());
        disable(101, tomorrowAt(14, 0), tomorrowAt(15, 0)).andExpect(status().isOk());
        assertThat(count("resource_block")).isEqualTo(2);
        assertThat(count("reservation")).isEqualTo(1);
        create(101, 14, 15, "still-disabled").andExpect(status().isConflict());
    }

    @Test
    void violationFailureRollsBackCancellationAndEvent() {
        long id = seat(101, "RESERVED", null);
        doThrow(new IllegalStateException("injected violation failure")).when(mapper).insertViolation(eq(id), anyLong(), any());
        assertThatThrownBy(() -> seatNoShow.expire(new LifecycleMapper.Candidate(id, 101))).isInstanceOf(IllegalStateException.class);
        assertThat(state(id)).isEqualTo("RESERVED");
        assertThat(count("violation_record")).isZero();
        assertThat(count("reservation_event")).isZero();
    }

    @Test
    void scanPaginatesBeyondOneHundredAndRetriesFailedItemNextRun() {
        long failed = seat(101, "RESERVED", null);
        // Simulate a pre-existing conflicting row so just this transaction fails.
        mapper.insertViolation(failed, 1, now);
        for (int i = 0; i < 104; i++) seat(101, "RESERVED", null);
        SeatNoShowJob job = new SeatNoShowJob(mapper, seatNoShow, clock);
        job.scan();
        assertThat(state(failed)).isEqualTo("RESERVED");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reservation WHERE status = 'CANCELLED'", Integer.class)).isEqualTo(104);
        jdbcTemplate.update("DELETE FROM violation_record WHERE reservation_id = ?", failed);
        job.scan();
        job.scan();
        assertThat(state(failed)).isEqualTo("CANCELLED");
        assertThat(count("violation_record")).isEqualTo(105);
        assertThat(count("reservation_event")).isEqualTo(105);
    }

    @Test
    void concurrentWorkersProduceExactlyOneViolation() throws Exception {
        long id = seat(101, "RESERVED", null);
        var candidate = new LifecycleMapper.Candidate(id, 101);
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(() -> { start.await(); return seatNoShow.expire(candidate); });
            var second = pool.submit(() -> { start.await(); return seatNoShow.expire(candidate); });
            start.countDown();
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(true, false);
        }
        assertThat(count("violation_record")).isEqualTo(1);
        assertThat(count("reservation_event")).isEqualTo(1);
    }

    @Test
    void workerRechecksArrivalAfterWaitingForResourceLock() throws Exception {
        long id = seat(101, "RESERVED", null);
        try (var connection = jdbcTemplate.getDataSource().getConnection(); var pool = Executors.newSingleThreadExecutor()) {
            connection.setAutoCommit(false);
            try (var statement = connection.createStatement()) {
                statement.executeQuery("SELECT id FROM resource WHERE id = 101 FOR UPDATE").close();
                var started = new CountDownLatch(1);
                var worker = pool.submit(() -> { started.countDown(); return seatNoShow.expire(new LifecycleMapper.Candidate(id, 101)); });
                assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
                try (var update = connection.prepareStatement("UPDATE reservation SET status = 'CHECKED_IN', check_in_at = ? WHERE id = ?")) {
                    update.setObject(1, now.minusSeconds(1));
                    update.setLong(2, id);
                    update.executeUpdate();
                }
                connection.commit();
                assertThat(worker.get(10, TimeUnit.SECONDS)).isFalse();
            } finally { connection.rollback(); }
        }
        assertThat(state(id)).isEqualTo("CHECKED_IN");
        assertThat(count("violation_record")).isZero();
    }

    @Test
    void concurrentCreateAndDisableCannotLeaveAnActiveBookingInDisabledPeriod() throws Exception {
        var go = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var creation = pool.submit(() -> {
                go.await();
                return create(101, 12, 13, "create-disable-race").andReturn().getResponse().getStatus();
            });
            var disabling = pool.submit(() -> {
                go.await();
                return mockMvc.perform(post(disablePath(101)).with(user("admin01")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(disableBody(tomorrowAt(12, 0), tomorrowAt(13, 0))))
                        .andReturn().getResponse().getStatus();
            });
            go.countDown();
            assertThat(creation.get(10, TimeUnit.SECONDS)).isIn(201, 409);
            assertThat(disabling.get(10, TimeUnit.SECONDS)).isEqualTo(200);
        }
        assertThat(count("resource_block")).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reservation WHERE status IN ('RESERVED','CHECKED_IN')", Integer.class)).isZero();
        assertThat(count("violation_record")).isZero();
    }

    private long booking(long resourceId, int from, int to, String status, LocalDateTime checkIn) {
        return insertReservation(1, resourceId, java.util.UUID.randomUUID().toString(), tomorrowAt(from, 0), tomorrowAt(to, 0), status, null, checkIn);
    }
    private long seat(long resourceId, String status, LocalDateTime checkIn) {
        long id = booking(resourceId, 14, 15, status, checkIn);
        jdbcTemplate.update("UPDATE reservation SET accepted_at = ?, arrival_deadline = ? WHERE id = ?", now.minusMinutes(30), now, id);
        return id;
    }
    private String state(long id) { return jdbcTemplate.queryForObject("SELECT status FROM reservation WHERE id = ?", String.class, id); }
    private int count(String table) { return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class); }
    private String disablePath(long id) { return "/api/v1/admin/resources/" + id + "/disable-periods"; }
    private String disableBody(LocalDateTime start, LocalDateTime end) throws Exception {
        return objectMapper.writeValueAsString(Map.of("startTime", iso(start), "endTime", iso(end), "reason", "设备检修"));
    }
    private ResultActions disable(long id, LocalDateTime start, LocalDateTime end) throws Exception {
        return mockMvc.perform(post(disablePath(id)).with(csrf()).header("X-Request-Id", "disable-integration")
                .contentType(MediaType.APPLICATION_JSON).content(disableBody(start, end)));
    }
    private ResultActions create(long id, int from, int to, String key) throws Exception {
        return mockMvc.perform(post("/api/v1/reservations").with(csrf()).with(user("student02"))
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(Map.of(
                        "resourceId", Long.toString(id), "startTime", iso(tomorrowAt(from, 0)),
                        "endTime", iso(tomorrowAt(to, 0)), "clientRequestId", key))));
    }
}
