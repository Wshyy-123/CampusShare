package org.campus.campussharebackend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.campus.campussharebackend.entity.Reservation;

@Mapper
public interface ReservationMapper {

    // 根据用户ID查询当前状态为 RESERVED（未到馆）的预约
    @Select("SELECT * FROM reservation WHERE user_id = #{userId} AND status = 'RESERVED' LIMIT 1")
    Reservation findPendingReservationByUserId(@Param("userId") Long userId);

    // 更新预约状态为已到馆
    @Update("UPDATE reservation SET status = 'CHECKED_IN', check_in_at = NOW() WHERE id = #{id}")
    void updateToCheckedIn(@Param("id") Long id);

    // 根据 ID 查询预约详情
    @Select("SELECT * FROM reservation WHERE id = #{id}")
    Reservation findById(@Param("id") Long id);

    // 取消预约并记录原因
    @Update("UPDATE reservation SET status = 'CANCELLED', cancel_reason = #{reason}, updated_at = NOW() WHERE id = #{id}")
    void cancelReservationWithReason(@Param("id") Long id, @Param("reason") String reason);
}