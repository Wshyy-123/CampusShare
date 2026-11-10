package com.campusshare.resource.mapper;

import com.campusshare.resource.entity.BlockRecord;
import com.campusshare.resource.entity.OpeningRecord;
import com.campusshare.resource.entity.ReservationSlot;
import com.campusshare.resource.entity.ResourceRecord;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ResourceMapper {

    List<ResourceRecord> list(@Param("resourceType") String resourceType,
                              @Param("location") String location);

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
}
