package org.campus.campussharebackend.resource.service;

import java.time.Clock;
import org.campus.campussharebackend.auth.CurrentUserProvider;
import org.campus.campussharebackend.common.BusinessException;
import org.campus.campussharebackend.common.RequestIdContext;
import org.campus.campussharebackend.common.TimeFormats;
import org.campus.campussharebackend.lifecycle.LifecycleMapper;
import org.campus.campussharebackend.resource.dto.DisableResourceRequest;
import org.campus.campussharebackend.resource.vo.DisableResourceResult;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResourceDisableService {
    private final LifecycleMapper mapper;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public ResourceDisableService(LifecycleMapper mapper, CurrentUserProvider currentUserProvider,
                                  @Qualifier("lifecycleClock") Clock clock) {
        this.mapper = mapper;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    @Transactional
    public DisableResourceResult disable(long resourceId, DisableResourceRequest request) {
        var admin = currentUserProvider.require();
        if (!"RESOURCE_ADMIN".equals(admin.role())) {
            throw new BusinessException(403, "FORBIDDEN", "需要资源管理员权限");
        }
        if (resourceId <= 0 || request == null || request.startTime() == null || request.endTime() == null
                || request.reason() == null || request.reason().isBlank() || request.reason().length() > 500) {
            throw new BusinessException(400, "INVALID_ARGUMENT", "请提供资源、起止时间和500字以内的停用原因");
        }
        var start = TimeFormats.toLocal(request.startTime());
        var end = TimeFormats.toLocal(request.endTime());
        if (!start.isBefore(end)) {
            throw new BusinessException(400, "INVALID_ARGUMENT", "开始时间必须早于结束时间");
        }
        // 与创建预约和超时任务共用资源行锁；锁内复核后一次提交。
        if (mapper.lockResource(resourceId) == null) {
            throw new BusinessException(404, "RESOURCE_NOT_FOUND", "资源不存在");
        }
        var affected = mapper.lockAffected(resourceId, start, end);
        var now = TimeFormats.now(clock);
        if (affected.stream().anyMatch(r -> "CHECKED_IN".equals(r.status()) && !now.isBefore(r.startAt()))) {
            throw new BusinessException(409, "INVALID_STATE", "停用范围内存在已开始使用的预约，请调整范围");
        }
        mapper.insertDisable(resourceId, start, end, request.reason().strip());
        String requestId = RequestIdContext.get();
        for (var reservation : affected) {
            if (mapper.cancel(reservation.id(), "RESOURCE_DISABLED", now) != 1) {
                throw new IllegalStateException("Locked reservation could not be cancelled");
            }
            mapper.insertEvent(reservation.id(), admin.userId(), now, requestId, "RESOURCE_DISABLED");
        }
        return new DisableResourceResult(Long.toString(resourceId),
                affected.stream().map(r -> Long.toString(r.id())).toList());
    }
}
