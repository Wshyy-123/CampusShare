package com.campusshare.lifecycle;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResourceDisableService {
    private final LifecycleMapper mapper;
    private final Clock clock;

    public ResourceDisableService(LifecycleMapper mapper, @Qualifier("lifecycleClock") Clock clock) {
        this.mapper = mapper;
        this.clock = clock;
    }

    public record Result(String resourceId, List<String> cancelledReservationIds) {}

    @Transactional
    public Result disable(long resourceId, OffsetDateTime startTime, OffsetDateTime endTime,
                          String reason, String requestId) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new LifecycleException(401, "AUTH_REQUIRED", "请先登录");
        }
        // Authentication#getName must be the server-authenticated app_user.account.
        var admin = mapper.findAdmin(auth.getName());
        if (admin == null || !admin.enabled() || !"RESOURCE_ADMIN".equals(admin.role())
                || auth.getAuthorities().stream().noneMatch(a -> "ROLE_RESOURCE_ADMIN".equals(a.getAuthority()))) {
            throw new LifecycleException(403, "FORBIDDEN", "需要资源管理员权限");
        }
        if (resourceId <= 0 || startTime == null || endTime == null
                || reason == null || reason.isBlank() || reason.length() > 500) {
            throw new LifecycleException(400, "INVALID_ARGUMENT", "请提供资源、起止时间和500字以内的停用原因");
        }
        var start = local(startTime);
        var end = local(endTime);
        if (!start.isBefore(end)) {
            throw new LifecycleException(400, "INVALID_ARGUMENT", "开始时间必须早于结束时间");
        }
        if (mapper.lockResource(resourceId) == null) {
            throw new LifecycleException(404, "RESOURCE_NOT_FOUND", "资源不存在");
        }
        var affected = mapper.lockAffected(resourceId, start, end);
        var now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
        if (affected.stream().anyMatch(r -> "CHECKED_IN".equals(r.status()) && !now.isBefore(r.startAt()))) {
            throw new LifecycleException(409, "INVALID_STATE", "停用范围内存在已开始使用的预约，请调整范围");
        }
        mapper.insertDisable(resourceId, start, end, reason.strip());
        for (var reservation : affected) {
            if (mapper.cancel(reservation.id(), "RESOURCE_DISABLED", now) != 1) {
                throw new IllegalStateException("Locked reservation could not be cancelled");
            }
            mapper.insertEvent(reservation.id(), admin.id(), now, requestId, "RESOURCE_DISABLED");
        }
        return new Result(Long.toString(resourceId), affected.stream().map(r -> Long.toString(r.id())).toList());
    }

    private static LocalDateTime local(OffsetDateTime value) {
        return value.atZoneSameInstant(ZoneId.of("Asia/Shanghai")).toLocalDateTime().truncatedTo(ChronoUnit.MILLIS);
    }
}
