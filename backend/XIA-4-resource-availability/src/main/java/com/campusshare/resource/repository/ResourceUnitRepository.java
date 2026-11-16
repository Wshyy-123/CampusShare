package com.campusshare.resource.repository;

import com.campusshare.resource.domain.ResourceUnit;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceUnitRepository extends JpaRepository<ResourceUnit, Long> {

    List<ResourceUnit> findByResourceIdOrderById(Long resourceId);

    long countByResourceId(Long resourceId);
}
