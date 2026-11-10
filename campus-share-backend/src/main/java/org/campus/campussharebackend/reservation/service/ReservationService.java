package org.campus.campussharebackend.reservation.service;

import org.campus.campussharebackend.auth.CurrentUser;
import org.campus.campussharebackend.auth.CurrentUserProvider;
import org.campus.campussharebackend.common.BusinessException;
import org.campus.campussharebackend.common.PageResponse;
import org.campus.campussharebackend.common.RequestIdContext;
import org.campus.campussharebackend.common.TimeFormats;
import org.campus.campussharebackend.lifecycle.ResourceAvailabilityGuard;
import org.campus.campussharebackend.lifecycle.SeatArrivalDeadline;
import org.campus.campussharebackend.reservation.dto.CreateReservationRequest;
import org.campus.campussharebackend.reservation.entity.ReservationRecord;
import org.campus.campussharebackend.reservation.entity.ReservationRow;
import org.campus.campussharebackend.reservation.mapper.ReservationRepository;
import org.campus.campussharebackend.reservation.vo.ReservationVO;
import org.campus.campussharebackend.resource.entity.ResourceRecord;
import org.campus.campussharebackend.resource.mapper.ResourceMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReservationService {

    private static final Set<String> ALLOWED_STATUSES =
            Set.of("RESERVED", "CHECKED_IN", "COMPLETED", "CANCELLED");
    private static final int MAX_PAGE_SIZE = 100;

    private final ReservationRepository reservationMapper;
    private final ResourceMapper resourceMapper;
    private final ResourceAvailabilityGuard availabilityGuard;
    private final CurrentUserProvider currentUserProvider;
    private final Clock clock;

    public ReservationService(ReservationRepository reservationMapper,
                              ResourceMapper resourceMapper,
                              ResourceAvailabilityGuard availabilityGuard,
                              CurrentUserProvider currentUserProvider,
                              @Qualifier("lifecycleClock") Clock clock) {
        this.reservationMapper = reservationMapper;
        this.resourceMapper = resourceMapper;
        this.availabilityGuard = availabilityGuard;
        this.currentUserProvider = currentUserProvider;
        this.clock = clock;
    }

    public record CreateResult(ReservationVO reservation, boolean created) {
    }

    @Transactional
    public CreateResult create(CreateReservationRequest request) {
        CurrentUser user = currentUserProvider.require();
        long resourceId = parseResourceId(request.resourceId());
        LocalDateTime start = TimeFormats.toLocal(request.startTime());
        LocalDateTime end = TimeFormats.toLocal(request.endTime());
        if (!start.isBefore(end)) {
            throw new BusinessException(400, "INVALID_ARGUMENT", "开始时间必须早于结束时间");
        }

        ReservationRecord existing = reservationMapper.findByUserAndClientRequestId(
                user.userId(), request.clientRequestId());
        if (existing != null) {
            assertSameRequest(existing, resourceId, start, end);
            return new CreateResult(toVO(existing, requireResource(resourceId)), false);
        }

        // XIA-11/14 的共享检查：在同一事务内先锁 resource 行，检查启用状态和重叠阻塞安排。
        availabilityGuard.assertNotBlocked(resourceId, start, end);

        // 并发重复提交时，第一个请求可能在上面的资源锁释放前刚刚提交；
        // 拿到资源锁后必须用锁定读重新读取同一 clientRequestId。
        // MySQL REPEATABLE READ 下普通 SELECT 可能仍使用等待锁之前的旧快照，不能用于这一步。
        ReservationRecord afterLock = reservationMapper.lockByUserAndClientRequestId(
                user.userId(), request.clientRequestId());
        if (afterLock != null) {
            assertSameRequest(afterLock, resourceId, start, end);
            return new CreateResult(toVO(afterLock, requireResource(resourceId)), false);
        }

        ResourceRecord resource = requireResource(resourceId);
        if (!resource.isEnabled()) {
            throw new BusinessException(409, "RESOURCE_UNAVAILABLE", "资源已停用");
        }
        if (resourceMapper.lockCoveringOpening(resourceId, start, end) == null) {
            throw new BusinessException(409, "RESOURCE_UNAVAILABLE", "该时段不在资源开放安排内");
        }
        List<ReservationRecord> conflicts = reservationMapper.lockConflicts(resourceId, start, end);
        if (!conflicts.isEmpty()) {
            throw new BusinessException(409, "RESERVATION_CONFLICT", "该时段已被其他预约占用");
        }

        LocalDateTime now = TimeFormats.now(clock);
        ReservationRecord record = new ReservationRecord();
        record.setUserId(user.userId());
        record.setResourceId(resourceId);
        record.setClientRequestId(request.clientRequestId());
        record.setStartAt(start);
        record.setEndAt(end);
        record.setAcceptedAt(now);
        record.setArrivalDeadline(arrivalDeadline(resource.getResourceType(), start, now));
        record.setStatus("RESERVED");
        record.setCreatedAt(now);
        record.setUpdatedAt(now);

        try {
            reservationMapper.insert(record);
        } catch (DuplicateKeyException exception) {
            ReservationRecord duplicate = reservationMapper.findByUserAndClientRequestId(
                    user.userId(), request.clientRequestId());
            if (duplicate == null) {
                throw exception;
            }
            assertSameRequest(duplicate, resourceId, start, end);
            return new CreateResult(toVO(duplicate, resource), false);
        }

        reservationMapper.insertEvent(record.getId(), "CREATED", user.userId(), now,
                RequestIdContext.get(), "创建预约");
        return new CreateResult(toVO(record, resource), true);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReservationVO> mine(String status, OffsetDateTime from, OffsetDateTime to,
                                            int page, int size) {
        CurrentUser user = currentUserProvider.require();
        if (page < 1) {
            throw new BusinessException(400, "INVALID_ARGUMENT", "page 必须从1开始");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BusinessException(400, "INVALID_ARGUMENT", "size 必须在1到100之间");
        }
        String normalizedStatus = normalizeStatus(status);
        LocalDateTime fromLocal = from == null ? null : TimeFormats.toLocal(from);
        LocalDateTime toLocal = to == null ? null : TimeFormats.toLocal(to);
        if (fromLocal != null && toLocal != null && !fromLocal.isBefore(toLocal)) {
            throw new BusinessException(400, "INVALID_ARGUMENT", "开始筛选时间必须早于结束筛选时间");
        }

        long total = reservationMapper.countMine(user.userId(), normalizedStatus, fromLocal, toLocal);
        int offset = (page - 1) * size;
        List<ReservationVO> items = reservationMapper
                .listMine(user.userId(), normalizedStatus, fromLocal, toLocal, offset, size)
                .stream().map(this::toVO).toList();
        return new PageResponse<>(items, total, page, size);
    }

    @Transactional(readOnly = true)
    public ReservationVO detail(long reservationId) {
        CurrentUser user = currentUserProvider.require();
        if (reservationId <= 0) {
            throw new BusinessException(404, "RESERVATION_NOT_FOUND", "预约不存在或无权查看");
        }
        ReservationRow row = reservationMapper.findDetail(reservationId, user.userId());
        if (row == null) {
            // 不区分“不存在”和“不属于本人”，避免泄露他人预约是否存在。
            throw new BusinessException(404, "RESERVATION_NOT_FOUND", "预约不存在或无权查看");
        }
        return toVO(row);
    }

    private ResourceRecord requireResource(long resourceId) {
        ResourceRecord resource = resourceMapper.findById(resourceId);
        if (resource == null) {
            throw new BusinessException(404, "RESOURCE_NOT_FOUND", "资源不存在");
        }
        return resource;
    }

    private static long parseResourceId(String value) {
        try {
            long resourceId = Long.parseLong(value);
            if (resourceId <= 0) {
                throw new NumberFormatException();
            }
            return resourceId;
        } catch (NumberFormatException exception) {
            throw new BusinessException(400, "INVALID_ARGUMENT", "resourceId 不合法");
        }
    }

    private static void assertSameRequest(ReservationRecord existing, long resourceId,
                                          LocalDateTime start, LocalDateTime end) {
        boolean same = existing.getResourceId() == resourceId
                && existing.getStartAt().equals(start)
                && existing.getEndAt().equals(end);
        if (!same) {
            throw new BusinessException(409, "IDEMPOTENCY_CONFLICT",
                    "同一 clientRequestId 已对应不同的预约参数");
        }
    }

    private static String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalized = status.trim().toUpperCase();
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new BusinessException(400, "INVALID_ARGUMENT", "status 取值不合法");
        }
        return normalized;
    }

    private static LocalDateTime arrivalDeadline(String resourceType, LocalDateTime start,
                                                 LocalDateTime acceptedAt) {
        if ("LIBRARY_SEAT".equals(resourceType)) {
            return SeatArrivalDeadline.deadline(acceptedAt);
        }
        // 体育场地按预约开始时间起算15分钟；讨论室/教室的专项规则在 XIA-12/13 中另行确认。
        return start.plusMinutes(15);
    }

    private ReservationVO toVO(ReservationRow row) {
        return new ReservationVO(
                Long.toString(row.getId()),
                Long.toString(row.getResourceId()),
                row.getResourceName(),
                row.getResourceType(),
                row.getLocation(),
                row.getUnitLabel(),
                TimeFormats.format(row.getStartAt()),
                TimeFormats.format(row.getEndAt()),
                row.getStatus(),
                TimeFormats.format(row.getAcceptedAt()),
                TimeFormats.format(row.getArrivalDeadline()),
                TimeFormats.format(row.getCheckInAt()),
                TimeFormats.format(row.getCheckOutAt()),
                row.getCancelReason(),
                TimeFormats.format(row.getCreatedAt()));
    }

    private ReservationVO toVO(ReservationRecord record, ResourceRecord resource) {
        return new ReservationVO(
                Long.toString(record.getId()),
                Long.toString(record.getResourceId()),
                resource.getName(),
                resource.getResourceType(),
                resource.getLocation(),
                resource.getUnitLabel(),
                TimeFormats.format(record.getStartAt()),
                TimeFormats.format(record.getEndAt()),
                record.getStatus(),
                TimeFormats.format(record.getAcceptedAt()),
                TimeFormats.format(record.getArrivalDeadline()),
                TimeFormats.format(record.getCheckInAt()),
                TimeFormats.format(record.getCheckOutAt()),
                record.getCancelReason(),
                TimeFormats.format(record.getCreatedAt()));
    }
}
