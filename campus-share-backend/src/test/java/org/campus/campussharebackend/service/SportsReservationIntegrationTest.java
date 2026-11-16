package org.campus.campussharebackend.service;

import org.campus.campussharebackend.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * XIA-7 / XIA-8 使用真实 MySQL 的联调测试（主工程 schema 001~007）。
 *
 * <p>默认跳过；设置 MYSQL_TEST=true 并让应用连到已应用全部迁移的数据库后执行。
 * 测试在事务内执行并回滚。</p>
 */
@SpringBootTest(properties = {
        "campusshare.sports-no-show.enabled=false",
        "campusshare.seat-no-show.enabled=false"
})
@EnabledIfEnvironmentVariable(named = "MYSQL_TEST", matches = "true")
@Transactional
class SportsReservationIntegrationTest {

    private static final long SEAT_RESOURCE_ID = 1L;
    private static final long SPORTS_RESOURCE_ID = 2L;
    private static final long STUDENT_USER_ID = 1L;
    private static final long OTHER_USER_ID = 2L;
    private static final AtomicLong ID_SEQUENCE = new AtomicLong(9_000_000L);

    @Autowired
    private SportsReservationService sportsReservationService;

    @Autowired
    private SportsNoShowService sportsNoShowService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void cancelReleasesOccupationAndRepeatedCancelDoesNotWriteAgain() {
        long id = insertReservation(STUDENT_USER_ID, SPORTS_RESOURCE_ID,
                LocalDateTime.now().plusHours(2), LocalDateTime.now().plusHours(3), "RESERVED", null);

        sportsReservationService.cancel(STUDENT_USER_ID, id);

        assertThat(statusOf(id)).isEqualTo("CANCELLED");
        assertThat(cancelReasonOf(id)).isEqualTo("USER_CANCEL");
        assertThat(countEvents(id, "CANCELLED")).isEqualTo(1);

        // 重复取消：不重复释放、不重复写事件
        sportsReservationService.cancel(STUDENT_USER_ID, id);

        assertThat(countEvents(id, "CANCELLED")).isEqualTo(1);
    }

    @Test
    void cancelAfterStartIsRejected() {
        long id = insertReservation(STUDENT_USER_ID, SPORTS_RESOURCE_ID,
                LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(59), "RESERVED", null);

        assertBusinessError(409, "INVALID_STATE", () -> sportsReservationService.cancel(STUDENT_USER_ID, id));
        assertThat(statusOf(id)).isEqualTo("RESERVED");
        assertThat(countEvents(id, "CANCELLED")).isZero();
    }

    @Test
    void seatReservationCannotUseSportsCancel() {
        long id = insertReservation(STUDENT_USER_ID, SEAT_RESOURCE_ID,
                LocalDateTime.now().plusHours(2), LocalDateTime.now().plusHours(3), "RESERVED", null);

        assertBusinessError(409, "INVALID_STATE", () -> sportsReservationService.cancel(STUDENT_USER_ID, id));
    }

    @Test
    void otherUsersReservationIsReportedAsNotFound() {
        long id = insertReservation(OTHER_USER_ID, SPORTS_RESOURCE_ID,
                LocalDateTime.now().plusHours(2), LocalDateTime.now().plusHours(3), "RESERVED", null);

        assertBusinessError(404, "RESERVATION_NOT_FOUND",
                () -> sportsReservationService.cancel(STUDENT_USER_ID, id));
    }

    @Test
    void checkInInsideWindowRecordsTimeAndStatus() {
        long id = insertReservation(STUDENT_USER_ID, SPORTS_RESOURCE_ID,
                LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(59), "RESERVED", null);

        sportsReservationService.checkIn(STUDENT_USER_ID, id);

        assertThat(statusOf(id)).isEqualTo("CHECKED_IN");
        assertThat(checkInAtOf(id)).isNotNull();
        assertThat(countEvents(id, "CHECKED_IN")).isEqualTo(1);
    }

    @Test
    void checkInBeforeStartIsRejected() {
        long id = insertReservation(STUDENT_USER_ID, SPORTS_RESOURCE_ID,
                LocalDateTime.now().plusMinutes(30), LocalDateTime.now().plusMinutes(90), "RESERVED", null);

        assertBusinessError(409, "INVALID_STATE", () -> sportsReservationService.checkIn(STUDENT_USER_ID, id));
        assertThat(statusOf(id)).isEqualTo("RESERVED");
    }

    @Test
    void noShowCancellationCannotBeRestoredByLateCheckIn() {
        long id = insertReservation(OTHER_USER_ID, SPORTS_RESOURCE_ID,
                LocalDateTime.now().minusMinutes(20), LocalDateTime.now().plusMinutes(40), "RESERVED", null);

        assertThat(sportsNoShowService.handle(id)).isTrue();
        assertThat(statusOf(id)).isEqualTo("CANCELLED");
        assertThat(cancelReasonOf(id)).isEqualTo("NO_SHOW");
        assertThat(countViolations(id)).isEqualTo(1);

        assertBusinessError(409, "INVALID_STATE",
                () -> sportsReservationService.checkIn(OTHER_USER_ID, id));
        assertThat(statusOf(id)).isEqualTo("CANCELLED");
        assertThat(countEvents(id, "CHECKED_IN")).isZero();

        // 后台重跑不重复记违规
        assertThat(sportsNoShowService.handle(id)).isFalse();
        assertThat(countViolations(id)).isEqualTo(1);
    }

    @Test
    void noShowJobLeavesSeatReservationsUntouched() {
        long id = insertReservation(STUDENT_USER_ID, SEAT_RESOURCE_ID,
                LocalDateTime.now().minusMinutes(20), LocalDateTime.now().plusMinutes(40), "RESERVED", null);

        assertThat(sportsNoShowService.handle(id)).isFalse();
        assertThat(statusOf(id)).isEqualTo("RESERVED");
    }

    @Test
    void checkOutCompletesReservation() {
        long id = insertReservation(STUDENT_USER_ID, SPORTS_RESOURCE_ID,
                LocalDateTime.now().minusMinutes(30), LocalDateTime.now().plusMinutes(30), "CHECKED_IN",
                LocalDateTime.now().minusMinutes(30));

        sportsReservationService.checkOut(STUDENT_USER_ID, id);

        assertThat(statusOf(id)).isEqualTo("COMPLETED");
        assertThat(checkOutAtOf(id)).isNotNull();
        assertThat(countEvents(id, "COMPLETED")).isEqualTo(1);
    }

    // ===================== 工具方法 =====================

    private void assertBusinessError(int status, String code, Runnable action) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.status()).isEqualTo(status);
            assertThat(exception.code()).isEqualTo(code);
        });
    }

    private long insertReservation(long userId, long resourceId,
                                   LocalDateTime startAt, LocalDateTime endAt,
                                   String status, LocalDateTime checkInAt) {
        long id = ID_SEQUENCE.incrementAndGet();
        jdbcTemplate.update("""
                        INSERT INTO reservation
                            (id, user_id, resource_id, client_request_id, accepted_at, arrival_deadline,
                             start_at, end_at, status, check_in_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                id, userId, resourceId, "it-" + id,
                Timestamp.valueOf(startAt), Timestamp.valueOf(startAt.plusMinutes(15)),
                Timestamp.valueOf(startAt), Timestamp.valueOf(endAt), status,
                checkInAt == null ? null : Timestamp.valueOf(checkInAt));
        return id;
    }

    private String statusOf(long id) {
        return jdbcTemplate.queryForObject("SELECT status FROM reservation WHERE id = ?", String.class, id);
    }

    private String cancelReasonOf(long id) {
        return jdbcTemplate.queryForObject("SELECT cancel_reason FROM reservation WHERE id = ?", String.class, id);
    }

    private Timestamp checkInAtOf(long id) {
        return jdbcTemplate.queryForObject("SELECT check_in_at FROM reservation WHERE id = ?", Timestamp.class, id);
    }

    private Timestamp checkOutAtOf(long id) {
        return jdbcTemplate.queryForObject("SELECT check_out_at FROM reservation WHERE id = ?", Timestamp.class, id);
    }

    private int countEvents(long reservationId, String eventType) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reservation_event WHERE reservation_id = ? AND event_type = ?",
                Integer.class, reservationId, eventType);
        return count == null ? 0 : count;
    }

    private int countViolations(long reservationId) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM violation_record WHERE reservation_id = ?",
                Integer.class, reservationId);
        return count == null ? 0 : count;
    }
}
