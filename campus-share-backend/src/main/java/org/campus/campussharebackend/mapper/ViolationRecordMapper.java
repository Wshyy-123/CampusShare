package org.campus.campussharebackend.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.campus.campussharebackend.entity.ViolationRecord;

@Mapper
public interface ViolationRecordMapper {

    // 插入违规记录
    @Insert("INSERT INTO violation_record(user_id, reservation_id, violation_type) " +
            "VALUES(#{userId}, #{reservationId}, #{violationType})")
    void insertViolation(ViolationRecord record);
}