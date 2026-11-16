package org.campus.campussharebackend.reservation.mapper;

import org.campus.campussharebackend.reservation.entity.ReservationRecord;
import org.campus.campussharebackend.reservation.entity.ReservationRow;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReservationRepository {

    ReservationRecord findByUserAndClientRequestId(@Param("userId") long userId,
                                                   @Param("clientRequestId") String clientRequestId);

    ReservationRecord lockByUserAndClientRequestId(@Param("userId") long userId,
                                                   @Param("clientRequestId") String clientRequestId);

    List<ReservationRecord> lockConflicts(@Param("resourceId") long resourceId,
                                          @Param("start") LocalDateTime start,
                                          @Param("end") LocalDateTime end);

    int insert(ReservationRecord record);

    void insertEvent(@Param("reservationId") long reservationId,
                     @Param("eventType") String eventType,
                     @Param("operatorId") Long operatorId,
                     @Param("occurredAt") LocalDateTime occurredAt,
                     @Param("requestId") String requestId,
                     @Param("reason") String reason);

    ReservationRow findDetail(@Param("id") long id, @Param("userId") long userId);

    long countMine(@Param("userId") long userId,
                   @Param("status") String status,
                   @Param("from") LocalDateTime from,
                   @Param("to") LocalDateTime to);

    List<ReservationRow> listMine(@Param("userId") long userId,
                                  @Param("status") String status,
                                  @Param("from") LocalDateTime from,
                                  @Param("to") LocalDateTime to,
                                  @Param("offset") long offset,
                                  @Param("size") int size);
}
