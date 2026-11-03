package org.campus.campussharebackend.service;

import org.campus.campussharebackend.common.ApiException;
import org.campus.campussharebackend.common.ErrorCode;
import org.campus.campussharebackend.dto.ReservationDetailResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * XIA-7 / XIA-8 使用真实 MySQL 的联调测试。
 *
 * <p>默认跳过；需要设置环境变量 MYSQL_TEST=true，并让应用连到已应用
 * 001~004 脚本的数据库。运行方式见 docs/verification/XIA-7-XIA-8.md。</p>
 *
 * <p>测试在事务内执行并回滚，不留下数据。</p>
 */
@SpringBootTest(properties = "app.sports.no-show-cron=-")
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

    // ===================== XIA-7 =====================

    @Test
    void cancelReleasesOccupationAndRepeatedCancelDoesNotWriteAgain() {
        long id = insertReservation(STUDENT_USER_ID, SPORTS_RESOURCE_ID,
                LocalDateTime.now().plusHours(2), LocalDateTime.now().plusHours(3), "RESERVED", null);

        ReservationDetailResponse first = sportsReservationService.cancel(STUDENT_USER_ID, id);

        assertThat(first.status()).isEqualTo("CANCELLED");
        assertThat(first.cancelReason()).isEqualTo("USER_CANCEL");
        assertThat(statusOf(id)).isEqualTo("CANCELLED");
        assertThat(countEvents(id, "CANCEL")).isEqualTo(1);

        // AC3：同一取消重复提交，不重复释放、不重复写事件
        ReservationDetailResponse second = sportsReservationService.cancel(STUDENT_USER_ID, id);

        assertThat(second.status()).isEqualTo("CANCELLED");
        assertThat(countEvents(id, "CANCEL")).isEqualTo(1);
    }

    @Test
    void cancelAfterStartIsRejectedAndKeepingReservation() {
        long id = insertReservation(STUDENT_USER_ID, SPORTS_RESOURCE_ID,
                LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(59), "RESERVED", null);

        assertThatThrownBy(() -> sportsReservationService.cancel(STUDENT_USER_ID, id))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATE));
        assertThat(statusOf(id)).isEqualTo("RESERVED");
        assertThat(countEvents(id, "CANCEL")).isZero();
    }

    @Test
    void seatReservationCannotUseSportsCancel() {
        long id = insertReservation(STUDENT_USER_ID, SEAT_RESOURCE_ID,
                LocalDateTime.now().plusHours(2), LocalDateTime.now().plusHours(3), "RESERVED", null);

        assertThatThrownBy(() -> sportsReservationService.cancel(STUDENT_USER_ID, id))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATE));
    }

    @Test
    void otherUsersReservationIsReportedAsNotFound() {
        long id = insertReservation(OTHER_USER_ID, SPORTS_RESOURCE_ID,
                LocalDateTime.now().plusHours(2), LocalDateTime.now().plusHours(3), "RESERVED", null);

        assertThatThrownBy(() -> sportsReservationService.cancel(STUDENT_USER_ID, id))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.RESERVATION_NOT_FOUND));
    }

    // ===================== XIA-8 =====================

    @Test
    void checkInInsideWindowRecordsTimeAndStatus() {
        long id = insertReservation(STUDENT_USER_ID, SPORTS_RESOURCE_ID,
                LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusMinutes(59), "RESERVED", null);

        ReservationDetailResponse response = sportsReservationService.checkIn(STUDENT_USER_ID, id);

        assertThat(response.status()).isEqualTo("CHECKED_IN");
        assertThat(response.checkInAt()).isNotNull();
        assertThat(statusOf(id)).isEqualTo("CHECKED_IN");
        assertThat(countEvents(id, "CHECK_IN")).isEqualTo(1);
    }

    @Test
    void checkInBeforeStartIsRejected() {
        long id = insertReservation(STUDENT_USER_ID, SPORTS_RESOURCE_ID,
                LocalDateTime.now().plusMinutes(30), LocalDateTime.now().plusMinutes(90), "RESERVED", null);

        assertThatThrownBy(() -> sportsReservationService.checkIn(STUDENT_USER_ID, id))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATE));
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

        // AC3：超时取消后再扫码，不恢复原占用
        assertThatThrownBy(() -> sportsReservationService.checkIn(OTHER_USER_ID, id))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATE));
        assertThat(statusOf(id)).isEqualTo("CANCELLED");
        assertThat(countEvents(id, "CHECK_IN")).isZero();

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

        ReservationDetailResponse response = sportsReservationService.checkOut(STUDENT_USER_ID, id);

        assertThat(response.status()).isEqualTo("COMPLETED");
        assertThat(response.checkOutAt()).isNotNull();
        assertThat(statusOf(id)).isEqualTo("COMPLETED");
        assertThat(countEvents(id, "CHECK_OUT")).isEqualTo(1);
    }

    // ===================== 工具方法 =====================

    private long insertReservation(long userId, long resourceId,
                                   LocalDateTime startAt, LocalDateTime endAt,
                                   String status, LocalDateTime checkInAt) {
        long id = ID_SEQUENCE.incrementAndGet();
        jdbcTemplate.update("""
                        INSERT INTO reservation (id, user_id, resource_id, accepted_at, start_at, end_at, status, check_in_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                id, userId, resourceId,
                java.sql.Timestamp.valueOf(startAt), java.sql.Timestamp.valueOf(startAt),
                java.sql.Timestamp.valueOf(endAt), status,
                checkInAt == null ? null : java.sql.Timestamp.valueOf(checkInAt));
        return id;
    }

    private String statusOf(long id) {
        return jdbcTemplate.queryForObject("SELECT status FROM reservation WHERE id = ?", String.class, id);
    }

    private String cancelReasonOf(long id) {
        return jdbcTemplate.queryForObject("SELECT cancel_reason FROM reservation WHERE id = ?", String.class, id);
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
