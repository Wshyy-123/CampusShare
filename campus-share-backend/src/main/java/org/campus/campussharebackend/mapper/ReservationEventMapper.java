package org.campus.campussharebackend.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

@Mapper
public interface ReservationEventMapper {

    @Insert("""
            INSERT INTO reservation_event(reservation_id, event_type, operator_id, occurred_at, request_id, reason)
            VALUES(#{reservationId}, #{eventType}, #{operatorId}, #{occurredAt}, #{requestId}, #{reason})
            """)
    int insert(@Param("reservationId") Long reservationId,
               @Param("eventType") String eventType,
               @Param("operatorId") Long operatorId,
               @Param("occurredAt") LocalDateTime occurredAt,
               @Param("requestId") String requestId,
               @Param("reason") String reason);
}
