package com.campusshare.schedule.repository;

import com.campusshare.schedule.domain.SchedulePeriod;
import com.campusshare.schedule.domain.SchedulePeriodType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SchedulePeriodRepository extends JpaRepository<SchedulePeriod, Long> {

    @Query("""
            select s
            from SchedulePeriod s
            where s.resourceUnitId = :unitId
              and s.startAt < :dayEnd
              and s.endAt > :dayStart
            order by s.startAt, s.id
            """)
    List<SchedulePeriod> findOverlappingDay(
            @Param("unitId") Long unitId,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd") LocalDateTime dayEnd);

    @Query("""
            select s
            from SchedulePeriod s
            where s.resourceUnitId = :unitId
              and s.periodType = :type
              and s.startAt < :dayEnd
              and s.endAt > :dayStart
            order by s.startAt, s.id
            """)
    List<SchedulePeriod> findOverlappingDayByType(
            @Param("unitId") Long unitId,
            @Param("type") SchedulePeriodType type,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd") LocalDateTime dayEnd);
}
