package com.campusshare.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusshare.common.BusinessException;
import com.campusshare.reservation.dto.CreateReservationRequest;
import com.campusshare.reservation.service.ReservationService;
import com.campusshare.support.IntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MvcResult;

@WithMockUser(username = "student01")
class ReservationCreateIntegrationTest extends IntegrationTestBase {

    @Autowired
    private ReservationService reservationService;

    @Test
    void createsReservationAndWritesEvent() throws Exception {
        LocalDateTime start = tomorrowAt(9, 0);
        LocalDateTime end = start.plusHours(1);

        mockMvc.perform(post("/api/v1/reservations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(101, start, end, "create-ok-001")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.reservationId").isString())
                .andExpect(jsonPath("$.data.resourceId").value("101"))
                .andExpect(jsonPath("$.data.status").value("RESERVED"))
                .andExpect(jsonPath("$.data.startTime").value(iso(start)))
                .andExpect(jsonPath("$.data.endTime").value(iso(end)))
                .andExpect(jsonPath("$.requestId").isString());

        assertThat(count("SELECT COUNT(*) FROM reservation")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM reservation_event WHERE event_type = 'CREATED'")).isEqualTo(1);
    }

    @Test
    void seatDeadlineIsThirtyMinutesFromAcceptedTime() throws Exception {
        LocalDateTime start = tomorrowAt(9, 0);
        MvcResult result = mockMvc.perform(post("/api/v1/reservations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(101, start, start.plusHours(1), "seat-deadline-001")))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        OffsetDateTime acceptedAt = OffsetDateTime.parse(data.path("acceptedAt").asText());
        OffsetDateTime deadline = OffsetDateTime.parse(data.path("arrivalDeadline").asText());
        assertThat(Duration.between(acceptedAt, deadline)).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void sportsDeadlineIsFifteenMinutesFromStart() throws Exception {
        LocalDateTime start = tomorrowAt(13, 0);
        MvcResult result = mockMvc.perform(post("/api/v1/reservations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(201, start, start.plusHours(1), "sports-deadline-001")))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        assertThat(OffsetDateTime.parse(data.path("arrivalDeadline").asText()))
                .isEqualTo(offset(start.plusMinutes(15)));
    }

    @Test
    void rejectsOverlappingReservationFromAnotherUser() throws Exception {
        LocalDateTime start = tomorrowAt(9, 0);
        LocalDateTime end = start.plusHours(1);
        mockMvc.perform(post("/api/v1/reservations").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(101, start, end, "overlap-owner-001")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/reservations").with(csrf())
                        .with(user("student02"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(101, start, end, "overlap-other-001")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESERVATION_CONFLICT"));
    }

    @Test
    void idempotentReplayReturnsSameReservation() throws Exception {
        LocalDateTime start = tomorrowAt(10, 0);
        LocalDateTime end = start.plusHours(1);
        String body = body(101, start, end, "idempotent-001");

        MvcResult first = mockMvc.perform(post("/api/v1/reservations").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        String firstId = objectMapper.readTree(first.getResponse().getContentAsString())
                .path("data").path("reservationId").asText();

        mockMvc.perform(post("/api/v1/reservations").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reservationId").value(firstId));

        assertThat(count("SELECT COUNT(*) FROM reservation")).isEqualTo(1);
    }

    @Test
    void sameClientRequestIdWithDifferentParametersIsConflict() throws Exception {
        LocalDateTime start = tomorrowAt(10, 0);
        String clientRequestId = "idempotent-different-001";
        mockMvc.perform(post("/api/v1/reservations").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(101, start, start.plusHours(1), clientRequestId)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/reservations").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(101, start, start.plusHours(2), clientRequestId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));
    }

    @Test
    void rejectsTeachingBlock() throws Exception {
        LocalDateTime start = tomorrowAt(10, 30);
        seedTeachingBlock(201, tomorrowAt(10, 0), tomorrowAt(12, 0));

        mockMvc.perform(post("/api/v1/reservations").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(201, start, start.plusHours(1), "blocked-001")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESOURCE_UNAVAILABLE"));

        assertThat(count("SELECT COUNT(*) FROM reservation")).isZero();
    }

    @Test
    void rejectsTimeOutsideOpening() throws Exception {
        LocalDateTime start = tomorrowAt(6, 0);
        mockMvc.perform(post("/api/v1/reservations").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(101, start, start.plusHours(1), "outside-opening-001")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESOURCE_UNAVAILABLE"));
    }

    @Test
    void rejectsDisabledResource() throws Exception {
        jdbcTemplate.update("UPDATE resource SET enabled = FALSE WHERE id = 101");
        LocalDateTime start = tomorrowAt(9, 0);
        mockMvc.perform(post("/api/v1/reservations").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(101, start, start.plusHours(1), "disabled-001")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESOURCE_UNAVAILABLE"));
    }

    @Test
    void rejectsInvalidTimeRange() throws Exception {
        LocalDateTime start = tomorrowAt(9, 0);
        mockMvc.perform(post("/api/v1/reservations").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(101, start, start.minusMinutes(1), "invalid-time-001")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
    }

    @Test
    void rejectsEqualStartAndEnd() throws Exception {
        LocalDateTime start = tomorrowAt(9, 0);
        mockMvc.perform(post("/api/v1/reservations").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(101, start, start, "equal-time-001")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
    }

    @Test
    void rejectsMissingClientRequestId() throws Exception {
        LocalDateTime start = tomorrowAt(9, 0);
        String request = objectMapper.writeValueAsString(Map.of(
                "resourceId", "101",
                "startTime", iso(start),
                "endTime", iso(start.plusHours(1))));
        mockMvc.perform(post("/api/v1/reservations").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
    }

    @Test
    void convertsUtcOffsetToShanghaiBusinessTime() throws Exception {
        LocalDateTime start = tomorrowAt(9, 0);
        LocalDateTime end = start.plusHours(1);
        String startUtc = start.atZone(ZONE).withZoneSameInstant(java.time.ZoneOffset.UTC)
                .toOffsetDateTime().toString();
        String endUtc = end.atZone(ZONE).withZoneSameInstant(java.time.ZoneOffset.UTC)
                .toOffsetDateTime().toString();
        String request = objectMapper.writeValueAsString(Map.of(
                "resourceId", "101",
                "startTime", startUtc,
                "endTime", endUtc,
                "clientRequestId", "utc-offset-001"));

        mockMvc.perform(post("/api/v1/reservations").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.startTime").value(iso(start)))
                .andExpect(jsonPath("$.data.endTime").value(iso(end)));
    }

    @Test
    @WithAnonymousUser
    void requiresAuthentication() throws Exception {
        LocalDateTime start = tomorrowAt(9, 0);
        mockMvc.perform(post("/api/v1/reservations")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(101, start, start.plusHours(1), "anonymous-001")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
    }

    @Test
    void concurrentOverlappingRequestsLeaveAtMostOneActiveReservation() throws Exception {
        Assumptions.assumeTrue(isMysql(), "MySQL row-lock concurrency test; H2 only covers service logic");
        LocalDateTime start = tomorrowAt(15, 0);
        LocalDateTime end = start.plusHours(1);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<String> first = pool.submit(() -> attempt(
                    "student01", 101, start, end, "concurrent-001", ready, go));
            Future<String> second = pool.submit(() -> attempt(
                    "student02", 101, start, end, "concurrent-002", ready, go));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            go.countDown();
            List<String> results = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
            assertThat(results).containsExactlyInAnyOrder("OK", "RESERVATION_CONFLICT");
        } finally {
            pool.shutdownNow();
        }
        assertThat(count("""
                SELECT COUNT(*) FROM reservation
                WHERE resource_id = 101 AND status IN ('RESERVED', 'CHECKED_IN')
                """)).isEqualTo(1);
    }

    @Test
    void concurrentSameClientRequestIdReturnsOneReservation() throws Exception {
        Assumptions.assumeTrue(isMysql(), "MySQL row-lock concurrency test; H2 only covers service logic");
        LocalDateTime start = tomorrowAt(16, 0);
        LocalDateTime end = start.plusHours(1);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<String> first = pool.submit(() -> attemptSameId(
                    "student01", 101, start, end, "concurrent-idem-001", ready, go));
            Future<String> second = pool.submit(() -> attemptSameId(
                    "student01", 101, start, end, "concurrent-idem-001", ready, go));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            go.countDown();
            List<String> results = List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
            assertThat(results).anyMatch(result -> result.startsWith("NEW:"));
            assertThat(results).anyMatch(result -> result.startsWith("REPLAY:"));
            String newId = results.stream().filter(result -> result.startsWith("NEW:"))
                    .findFirst().orElseThrow().substring(4);
            String replayId = results.stream().filter(result -> result.startsWith("REPLAY:"))
                    .findFirst().orElseThrow().substring(7);
            assertThat(replayId).isEqualTo(newId);
        } finally {
            pool.shutdownNow();
        }
        assertThat(count("SELECT COUNT(*) FROM reservation")).isEqualTo(1);
    }

    private String attemptSameId(String account, long resourceId, LocalDateTime start, LocalDateTime end,
                                 String clientRequestId, CountDownLatch ready, CountDownLatch go) throws Exception {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                account, null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))));
        SecurityContextHolder.setContext(context);
        try {
            ready.countDown();
            if (!go.await(5, TimeUnit.SECONDS)) {
                return "GO_TIMEOUT";
            }
            ReservationService.CreateResult result = reservationService.create(new CreateReservationRequest(
                    Long.toString(resourceId), offset(start), offset(end), clientRequestId));
            return (result.created() ? "NEW:" : "REPLAY:") + result.reservation().reservationId();
        } catch (BusinessException exception) {
            return "ERROR:" + exception.code();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private String attempt(String account, long resourceId, LocalDateTime start, LocalDateTime end,
                           String clientRequestId, CountDownLatch ready, CountDownLatch go) throws Exception {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(
                account, null, List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))));
        SecurityContextHolder.setContext(context);
        try {
            ready.countDown();
            if (!go.await(5, TimeUnit.SECONDS)) {
                return "GO_TIMEOUT";
            }
            reservationService.create(new CreateReservationRequest(
                    Long.toString(resourceId), offset(start), offset(end), clientRequestId));
            return "OK";
        } catch (BusinessException exception) {
            return exception.code();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private boolean isMysql() {
        if (jdbcTemplate.getDataSource() == null) {
            return false;
        }
        try (Connection connection = jdbcTemplate.getDataSource().getConnection()) {
            return connection.getMetaData().getDatabaseProductName().toLowerCase().contains("mysql");
        } catch (SQLException exception) {
            return false;
        }
    }

    private int count(String sql) {
        Integer value = jdbcTemplate.queryForObject(sql, Integer.class);
        return value == null ? 0 : value;
    }

    private String body(long resourceId, LocalDateTime start, LocalDateTime end, String clientRequestId)
            throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "resourceId", Long.toString(resourceId),
                "startTime", iso(start),
                "endTime", iso(end),
                "clientRequestId", clientRequestId));
    }
}
