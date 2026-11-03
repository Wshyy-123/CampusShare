package org.campus.campussharebackend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.campus.campussharebackend.entity.Reservation;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ReservationMapper {

    // ================= 图书馆座位（XIA-10 / XIA-11 沿用，本次未改动） =================

    @Select("SELECT * FROM reservation WHERE user_id = #{userId} AND status = 'RESERVED' LIMIT 1")
    Reservation findPendingReservationByUserId(@Param("userId") Long userId);

    @Update("UPDATE reservation SET status = 'CHECKED_IN', check_in_at = NOW() WHERE id = #{id}")
    void updateToCheckedIn(@Param("id") Long id);

    @Select("SELECT * FROM reservation WHERE id = #{id}")
    Reservation findById(@Param("id") Long id);

    @Update("UPDATE reservation SET status = 'CANCELLED', cancel_reason = #{reason}, updated_at = NOW() WHERE id = #{id}")
    void cancelReservationWithReason(@Param("id") Long id, @Param("reason") String reason);

    // ================= 体育取消 / 签到 / 签退（XIA-7 / XIA-8） =================

    /**
     * 按共同锁顺序读取预约行：调用方必须先锁定对应 resource 行。
     */
    @Select("SELECT * FROM reservation WHERE id = #{id} FOR UPDATE")
    Reservation findByIdForUpdate(@Param("id") Long id);

    /**
     * 仅在仍为 RESERVED 时取消，返回影响行数；0 表示状态已被并发修改。
     */
    @Update("""
            UPDATE reservation
            SET status = 'CANCELLED', cancel_reason = #{reason}, updated_at = #{occurredAt}
            WHERE id = #{id} AND status = 'RESERVED'
            """)
    int cancelIfReserved(@Param("id") Long id,
                         @Param("reason") String reason,
                         @Param("occurredAt") LocalDateTime occurredAt);

    /**
     * 仅在仍为 RESERVED 时签到，避免覆盖已取消或已完成的预约。
     */
    @Update("""
            UPDATE reservation
            SET status = 'CHECKED_IN', check_in_at = #{occurredAt}, updated_at = #{occurredAt}
            WHERE id = #{id} AND status = 'RESERVED'
            """)
    int checkInIfReserved(@Param("id") Long id, @Param("occurredAt") LocalDateTime occurredAt);

    /**
     * 仅在已签到时签退。
     */
    @Update("""
            UPDATE reservation
            SET status = 'COMPLETED', check_out_at = #{occurredAt}, updated_at = #{occurredAt}
            WHERE id = #{id} AND status = 'CHECKED_IN'
            """)
    int checkOutIfCheckedIn(@Param("id") Long id, @Param("occurredAt") LocalDateTime occurredAt);

    /**
     * 体育未到候选：状态仍为 RESERVED，且已过"开始时间 + 签到宽限"。
     * 只扫体育资源，避免把图书馆座位当作体育预约处理。
     */
    @Select("""
            SELECT r.* FROM reservation r
            JOIN resource res ON res.id = r.resource_id
            WHERE r.status = 'RESERVED'
              AND r.start_at IS NOT NULL
              AND r.start_at <= #{deadline}
              AND res.resource_type IN ('BADMINTON', 'TABLE_TENNIS', 'TENNIS')
            """)
    List<Reservation> findSportsNoShowCandidates(@Param("deadline") LocalDateTime deadline);
}
