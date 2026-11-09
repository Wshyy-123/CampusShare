package com.campusshare.reservation.repository;

import com.campusshare.reservation.domain.Reservation;
import com.campusshare.reservation.domain.ReservationStatus;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("""
            select r
            from Reservation r
            where r.resourceUnitId = :unitId
              and r.status in :statuses
              and r.startAt < :dayEnd
              and r.endAt > :dayStart
            order by r.startAt, r.id
            """)
    List<Reservation> findBlockingReservations(
            @Param("unitId") Long unitId,
            @Param("statuses") Collection<ReservationStatus> statuses,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd") LocalDateTime dayEnd);
}
