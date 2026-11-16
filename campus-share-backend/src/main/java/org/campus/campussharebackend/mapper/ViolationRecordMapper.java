package org.campus.campussharebackend.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.campus.campussharebackend.entity.ViolationRecord;

import java.time.LocalDateTime;

@Mapper
public interface ViolationRecordMapper {

    // 插入违规记录（occurred_at 由数据库默认值补齐的场景保留）
    @Insert("INSERT INTO violation_record(user_id, reservation_id, violation_type) " +
            "VALUES(#{userId}, #{reservationId}, #{violationType})")
    void insertViolation(ViolationRecord record);

    /**
     * 同一预约同类违规只保留一条：依赖 003 中的唯一约束，后台重跑不会重复记违规。
     */
    @Insert("""
            INSERT INTO violation_record(user_id, reservation_id, violation_type, occurred_at)
            VALUES(#{userId}, #{reservationId}, #{violationType}, #{occurredAt})
            ON DUPLICATE KEY UPDATE violation_type = violation_type
            """)
    int insertViolationIfAbsent(@Param("userId") Long userId,
                                @Param("reservationId") Long reservationId,
                                @Param("violationType") String violationType,
                                @Param("occurredAt") LocalDateTime occurredAt);
}
