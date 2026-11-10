package com.campusshare.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.campus.campussharebackend.CampusShareBackendApplication;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = CampusShareBackendApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    protected static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    protected static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected ObjectMapper objectMapper;

    @BeforeEach
    void resetData() {
        jdbcTemplate.update("DELETE FROM violation_record");
        jdbcTemplate.update("DELETE FROM reservation_event");
        jdbcTemplate.update("DELETE FROM reservation");
        jdbcTemplate.update("DELETE FROM resource_block");
        jdbcTemplate.update("DELETE FROM resource_opening");
        jdbcTemplate.update("UPDATE resource SET enabled = TRUE");
        seedOpenings();
    }

    protected void seedOpenings() {
        LocalDate today = LocalDate.now(ZONE);
        for (long resourceId : List.of(101L, 102L, 201L, 401L)) {
            for (int day = 0; day <= 6; day++) {
                LocalDate date = today.plusDays(day);
                jdbcTemplate.update(
                        "INSERT INTO resource_opening (resource_id, start_at, end_at) VALUES (?, ?, ?)",
                        resourceId, date.atTime(8, 0), date.atTime(22, 0));
            }
        }
    }

    protected LocalDateTime tomorrowAt(int hour, int minute) {
        return LocalDate.now(ZONE).plusDays(1).atTime(hour, minute);
    }

    protected String iso(LocalDateTime value) {
        return value.atZone(ZONE).format(ISO);
    }

    protected OffsetDateTime offset(LocalDateTime value) {
        return value.atZone(ZONE).toOffsetDateTime();
    }

    protected void seedTeachingBlock(long resourceId, LocalDateTime start, LocalDateTime end) {
        jdbcTemplate.update("""
                INSERT INTO resource_block (resource_id, block_type, start_at, end_at, reason)
                VALUES (?, 'TEACHING', ?, ?, '测试教学占用')
                """, resourceId, start, end);
    }

    protected long insertReservation(long userId, long resourceId, String clientRequestId,
                                     LocalDateTime start, LocalDateTime end, String status,
                                     String cancelReason, LocalDateTime checkInAt) {
        LocalDateTime accepted = start.minusHours(1);
        LocalDateTime deadline = start.plusMinutes(15);
        jdbcTemplate.update("""
                INSERT INTO reservation (user_id, resource_id, client_request_id, start_at, end_at,
                                         accepted_at, arrival_deadline, status, cancel_reason,
                                         check_in_at, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, userId, resourceId, clientRequestId, start, end, accepted, deadline,
                status, cancelReason, checkInAt, start.minusHours(2), start.minusHours(2));
        return jdbcTemplate.queryForObject(
                "SELECT id FROM reservation WHERE user_id = ? AND client_request_id = ?",
                Long.class, userId, clientRequestId);
    }
}
