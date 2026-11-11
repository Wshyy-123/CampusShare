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

    List<ScheduleRow> list(@Param("resourceId") Long resourceId,
                           @Param("dayStart") LocalDateTime dayStart,
                           @Param("dayEnd") LocalDateTime dayEnd);

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

    long countOverlappingOpening(@Param("resourceId") long resourceId,
                                 @Param("start") LocalDateTime start,
                                 @Param("end") LocalDateTime end,
                                 @Param("excludeId") Long excludeId);

    long countOverlappingTeaching(@Param("resourceId") long resourceId,
                                  @Param("start") LocalDateTime start,
                                  @Param("end") LocalDateTime end,
                                  @Param("excludeId") Long excludeId);
}
