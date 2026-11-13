package org.campus.campussharebackend.lifecycle;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface LifecycleMapper {
    record Resource(long id, String resourceType, boolean enabled) {}
    record Reservation(long id, long userId, long resourceId, LocalDateTime startAt,
                       LocalDateTime endAt, LocalDateTime acceptedAt, String status,
                       LocalDateTime checkInAt) {}
    record Candidate(long id, long resourceId) {}
    record Admin(long id, String role, boolean enabled) {}

    @Select("SELECT id, user_id, resource_id, start_at, end_at, accepted_at, status, check_in_at FROM reservation WHERE resource_id = #{resourceId} AND status IN ('RESERVED','CHECKED_IN') ORDER BY id FOR UPDATE")
    List<Reservation> lockAllActive(long resourceId);

    @Select("SELECT id FROM reservation WHERE resource_id = #{resourceId} ORDER BY id FOR UPDATE")
    List<Long> lockHistoryIds(long resourceId);

    @Select("SELECT id, role, enabled FROM app_user WHERE account = #{account}")
    Admin findAdmin(String account);

    @Select("SELECT id, resource_type, enabled FROM resource WHERE id = #{id} FOR UPDATE")
    Resource lockResource(long id);

    @Select("""
            SELECT id, user_id, resource_id, start_at, end_at, accepted_at, status, check_in_at
            FROM reservation WHERE resource_id = #{resourceId}
              AND status IN ('RESERVED', 'CHECKED_IN')
              AND start_at < #{end} AND end_at > #{start}
            ORDER BY id FOR UPDATE
            """)
    List<Reservation> lockAffected(@Param("resourceId") long resourceId,
                                   @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select("""
            SELECT id, user_id, resource_id, start_at, end_at, accepted_at, status, check_in_at
            FROM reservation WHERE id = #{id} FOR UPDATE
            """)
    Reservation lockReservation(long id);

    @Insert("""
            INSERT INTO resource_block(resource_id, block_type, start_at, end_at, reason)
            VALUES(#{resourceId}, 'DISABLED', #{start}, #{end}, #{reason})
            """)
    void insertDisable(@Param("resourceId") long resourceId, @Param("start") LocalDateTime start,
                       @Param("end") LocalDateTime end, @Param("reason") String reason);

    // A locking read is required even if this transaction previously established a snapshot.
    @Select("""
            SELECT id FROM resource_block WHERE resource_id = #{resourceId}
              AND start_at < #{end} AND end_at > #{start}
            ORDER BY id LIMIT 1 FOR UPDATE
            """)
    Long lockOverlappingBlock(@Param("resourceId") long resourceId,
                               @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Update("""
            UPDATE reservation SET status = 'CANCELLED', cancel_reason = #{reason}, updated_at = #{now}
            WHERE id = #{id} AND status IN ('RESERVED', 'CHECKED_IN')
            """)
    int cancel(@Param("id") long id, @Param("reason") String reason, @Param("now") LocalDateTime now);

    @Insert("""
            INSERT INTO reservation_event(reservation_id, event_type, operator_id, occurred_at, request_id, reason)
            VALUES(#{id}, 'CANCELLED', #{operatorId}, #{now}, #{requestId}, #{reason})
            """)
    void insertEvent(@Param("id") long id, @Param("operatorId") Long operatorId,
                     @Param("now") LocalDateTime now, @Param("requestId") String requestId,
                     @Param("reason") String reason);

    @Insert("""
            INSERT INTO violation_record(reservation_id, user_id, violation_type, occurred_at)
            VALUES(#{id}, #{userId}, 'NO_SHOW', #{now})
            """)
    void insertViolation(@Param("id") long id, @Param("userId") long userId,
                         @Param("now") LocalDateTime now);

    @Select("""
            SELECT r.id, r.resource_id FROM reservation r JOIN resource s ON s.id = r.resource_id
            WHERE s.resource_type = 'LIBRARY_SEAT' AND r.status = 'RESERVED'
              AND r.check_in_at IS NULL AND r.accepted_at <= #{cutoff} AND r.id > #{afterId}
            ORDER BY r.id LIMIT #{limit}
            """)
    List<Candidate> findExpiredSeats(@Param("cutoff") LocalDateTime cutoff,
                                     @Param("afterId") long afterId, @Param("limit") int limit);
}
