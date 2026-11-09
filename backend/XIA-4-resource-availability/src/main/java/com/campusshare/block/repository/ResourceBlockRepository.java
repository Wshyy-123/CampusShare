package com.campusshare.block.repository;

import com.campusshare.block.domain.ResourceBlock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResourceBlockRepository extends JpaRepository<ResourceBlock, Long> {

    @Query("""
            select b
            from ResourceBlock b
            where b.resourceUnitId = :unitId
              and b.active = true
              and b.startAt < :dayEnd
              and b.endAt > :dayStart
            order by b.startAt, b.id
            """)
    List<ResourceBlock> findActiveBlocks(
            @Param("unitId") Long unitId,
            @Param("dayStart") LocalDateTime dayStart,
            @Param("dayEnd") LocalDateTime dayEnd);
}
