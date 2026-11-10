package com.campusshare.lifecycle;

import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** The reservation owner calls this inside the SAME transaction as reservation creation. */
@Service
public class ResourceAvailabilityGuard {
    private final LifecycleMapper mapper;

    public ResourceAvailabilityGuard(LifecycleMapper mapper) { this.mapper = mapper; }

    @Transactional(propagation = Propagation.MANDATORY)
    public void assertNotBlocked(long resourceId, LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || !start.isBefore(end)) {
            throw new LifecycleException(400, "INVALID_ARGUMENT", "开始时间必须早于结束时间");
        }
        var resource = mapper.lockResource(resourceId);
        if (resource == null) {
            throw new LifecycleException(404, "RESOURCE_NOT_FOUND", "资源不存在");
        }
        if (!resource.enabled() || mapper.lockOverlappingBlock(resourceId, start, end) != null) {
            throw new LifecycleException(409, "RESOURCE_UNAVAILABLE", "该时段存在停用或占用安排");
        }
    }
}
