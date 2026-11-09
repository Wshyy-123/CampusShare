package com.campusshare.resource.repository;

import com.campusshare.resource.domain.CampusResource;
import com.campusshare.resource.domain.ResourceType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CampusResourceRepository extends JpaRepository<CampusResource, Long> {

    @Query("""
            select r
            from CampusResource r
            where (:keyword is null
                   or lower(r.name) like lower(concat('%', :keyword, '%'))
                   or lower(r.location) like lower(concat('%', :keyword, '%')))
              and (:type is null or r.type = :type)
            order by r.id
            """)
    Page<CampusResource> search(
            @Param("keyword") String keyword,
            @Param("type") ResourceType type,
            Pageable pageable);
}
