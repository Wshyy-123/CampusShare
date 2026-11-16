package org.campus.campussharebackend.resource.mapper;

import org.campus.campussharebackend.resource.entity.BlockRecord;
import org.campus.campussharebackend.resource.entity.OpeningRecord;
import org.campus.campussharebackend.resource.entity.ReservationSlot;
import org.campus.campussharebackend.resource.entity.ResourceRecord;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ResourceMapper {

    List<ResourceRecord> list(@Param("resourceType") String resourceType,
                              @Param("location") String location);

    List<OpeningRecord> lockOpenings(@Param("resourceId") long resourceId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    ResourceRecord lockById(@Param("id") long id);

    ResourceRecord findById(@Param("id") long id);

    OpeningRecord findCoveringOpening(@Param("resourceId") long resourceId,
                                      @Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end);

    OpeningRecord lockCoveringOpening(@Param("resourceId") long resourceId,
                                      @Param("start") LocalDateTime start,
                                      @Param("end") LocalDateTime end);

    List<OpeningRecord> listOpeningsForDate(@Param("resourceId") long resourceId,
                                            @Param("dayStart") LocalDateTime dayStart,
                                            @Param("dayEnd") LocalDateTime dayEnd);

    List<BlockRecord> listBlocksForDate(@Param("resourceId") long resourceId,
                                        @Param("dayStart") LocalDateTime dayStart,
                                        @Param("dayEnd") LocalDateTime dayEnd);

    List<ReservationSlot> listActiveReservationsForDate(@Param("resourceId") long resourceId,
                                                        @Param("dayStart") LocalDateTime dayStart,
                                                        @Param("dayEnd") LocalDateTime dayEnd);

    List<ResourceRecord> listAll(@Param("keyword") String keyword,
                                 @Param("resourceType") String resourceType,
                                 @Param("offset") long offset,
                                 @Param("limit") int limit);

    long countAll(@Param("keyword") String keyword,
                  @Param("resourceType") String resourceType);

    int insert(ResourceRecord record);

    int update(ResourceRecord record);
}
