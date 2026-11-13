package org.campus.campussharebackend.schedule.mapper;

import org.campus.campussharebackend.resource.entity.BlockRecord;
import org.campus.campussharebackend.resource.entity.OpeningRecord;
import org.campus.campussharebackend.schedule.entity.ScheduleRow;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ScheduleRepository {
    @org.apache.ibatis.annotations.Insert("INSERT INTO resource_schedule_event(resource_id,schedule_type,schedule_id,operation,operator_id,occurred_at,request_id,before_json,after_json) VALUES(#{resourceId},#{type},#{id},#{operation},#{operatorId},#{now},#{requestId},#{before},#{after})")
    void audit(@Param("resourceId") long resourceId, @Param("type") String type, @Param("id") long id,
               @Param("operation") String operation, @Param("operatorId") long operatorId,
               @Param("now") java.time.LocalDateTime now, @Param("requestId") String requestId,
               @Param("before") String before, @Param("after") String after);


    List<ScheduleRow> list(@Param("resourceId") Long resourceId,
                           @Param("dayStart") LocalDateTime dayStart,
                           @Param("dayEnd") LocalDateTime dayEnd);

    OpeningRecord lockOpening(@Param("id") long id);

    BlockRecord lockTeachingBlock(@Param("id") long id);

    OpeningRecord findOpening(@Param("id") long id);

    BlockRecord findTeachingBlock(@Param("id") long id);

    int insertOpening(OpeningRecord record);

    int insertTeachingBlock(BlockRecord record);

    int updateOpening(@Param("id") long id,
                      @Param("startAt") LocalDateTime startAt,
                      @Param("endAt") LocalDateTime endAt,
                      @Param("note") String note);

    int updateTeachingBlock(@Param("id") long id,
                            @Param("startAt") LocalDateTime startAt,
                            @Param("endAt") LocalDateTime endAt,
                            @Param("reason") String reason);

    int deleteOpening(@Param("id") long id);

    int deleteTeachingBlock(@Param("id") long id);

    List<Long> lockOverlappingOpening(@Param("resourceId") long resourceId,
                                 @Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end,
                                 @Param("excludeId") Long excludeId);

    List<Long> lockOverlappingTeaching(@Param("resourceId") long resourceId,
                                  @Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end,
                                  @Param("excludeId") Long excludeId);
}
