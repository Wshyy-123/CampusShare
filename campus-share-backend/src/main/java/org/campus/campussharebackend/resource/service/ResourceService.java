package org.campus.campussharebackend.resource.service;

import org.campus.campussharebackend.common.BusinessException;
import org.campus.campussharebackend.common.PageResponse;
import org.campus.campussharebackend.common.TimeFormats;
import org.campus.campussharebackend.resource.dto.ResourceSaveRequest;
import org.campus.campussharebackend.resource.entity.ResourceRecord;
import org.campus.campussharebackend.resource.mapper.ResourceMapper;
import org.campus.campussharebackend.resource.vo.AvailabilityVO;
import org.campus.campussharebackend.resource.vo.BlockVO;
import org.campus.campussharebackend.resource.vo.IntervalVO;
import org.campus.campussharebackend.resource.vo.ReservationSlotVO;
import org.campus.campussharebackend.resource.vo.ResourceVO;
import java.time.LocalDate;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Qualifier;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Minimal read model needed by the XIA-5 page. XIA-4 owns the final resource
 * search and availability contract; this class only exposes what the reservation
 * form needs and must be replaced rather than duplicated when XIA-4 lands.
 */
@Service
public class ResourceService {

    private final ResourceMapper resourceMapper;
    private final org.campus.campussharebackend.auth.CurrentUserProvider currentUserProvider;
    private final org.campus.campussharebackend.lifecycle.LifecycleMapper lifecycleMapper;
    private final ResourceDisableService disableService;

    private final Clock clock;
    public ResourceService(ResourceMapper resourceMapper, @Qualifier("lifecycleClock") Clock clock,
        org.campus.campussharebackend.auth.CurrentUserProvider currentUserProvider,
        org.campus.campussharebackend.lifecycle.LifecycleMapper lifecycleMapper, ResourceDisableService disableService) {
        this.resourceMapper = resourceMapper;
        this.clock = clock;
        this.currentUserProvider = currentUserProvider;
        this.lifecycleMapper = lifecycleMapper;
        this.disableService = disableService;
    }

    @Transactional(readOnly = true)
    public List<ResourceVO> list(String resourceType, String location) {
        return resourceMapper.list(resourceType, location).stream().map(ResourceVO::from).toList();
    }

    @Transactional(readOnly = true)
    public ResourceVO get(long resourceId) {
        return ResourceVO.from(requireResource(resourceId));
    }

    @Transactional(readOnly = true)
    public AvailabilityVO availability(long resourceId, LocalDate date) {
        ResourceRecord resource = requireResource(resourceId);
        LocalDate targetDate = date == null ? LocalDate.now(clock) : date;
        LocalDateTime dayStart = targetDate.atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);
        var openings = resourceMapper.listOpeningsForDate(resourceId, dayStart, dayEnd);
        var blocks = resourceMapper.listBlocksForDate(resourceId, dayStart, dayEnd);
        var reservations = resourceMapper.listActiveReservationsForDate(resourceId, dayStart, dayEnd);
        LocalDateTime now = TimeFormats.now(clock);
        var available = resource.isEnabled() && !("DISCUSSION_ROOM".equals(resource.getResourceType()) && !targetDate.isAfter(now.toLocalDate()))
                ? AvailabilityCalculator.free(openings, blocks, reservations, dayStart.isAfter(now) ? dayStart : now, dayEnd)
                : List.<IntervalVO>of();
        return new AvailabilityVO(
                ResourceVO.from(resource),
                targetDate.toString(),
                openings.stream().map(IntervalVO::from).toList(),
                blocks.stream().map(BlockVO::from).toList(),
                reservations.stream().map(ReservationSlotVO::from).toList(), available);
    }

    @Transactional(readOnly = true)
    public PageResponse<ResourceVO> adminList(String keyword, String resourceType, int page, int size) {
        requireAdmin();
        int safePage = Math.max(1, page);
        int safeSize = Math.min(100, Math.max(1, size));
        String normalizedKeyword = blankToNull(keyword);
        String normalizedType = blankToNull(resourceType);
        long total = resourceMapper.countAll(normalizedKeyword, normalizedType);
        long offset = ((long) safePage - 1) * safeSize;
        List<ResourceVO> items = offset >= total ? List.of() : resourceMapper
                .listAll(normalizedKeyword, normalizedType, offset, safeSize)
                .stream().map(ResourceVO::from).toList();
        return new PageResponse<>(items, total, safePage, safeSize);
    }

    @Transactional
    public ResourceVO create(ResourceSaveRequest request) {
        requireAdmin();
        ResourceRecord record = toRecord(request);
        resourceMapper.insert(record);
        return ResourceVO.from(requireResource(record.getId()));
    }

    @Transactional
    public ResourceVO update(long resourceId, ResourceSaveRequest request) {
        requireAdmin();
        ResourceRecord existing = resourceId <= 0 ? null : resourceMapper.lockById(resourceId);
        if (existing == null) throw new BusinessException(404, "RESOURCE_NOT_FOUND", "资源不存在");
        if (!existing.getResourceType().equals(request.resourceType()) && !lifecycleMapper.lockHistoryIds(resourceId).isEmpty()) {
            throw new BusinessException(409, "INVALID_STATE", "资源已有预约记录，不能变更资源类型；请新建对应资源");
        }
        if (!request.enabled()) disableService.deactivateReservations(resourceId);
        ResourceRecord record = toRecord(request);
        record.setId(existing.getId());
        resourceMapper.update(record);
        return ResourceVO.from(requireResource(resourceId));
    }

    private void requireAdmin() {
        if (!"RESOURCE_ADMIN".equals(currentUserProvider.require().role()))
            throw new BusinessException(403, "FORBIDDEN", "需要资源管理员权限");
    }

    private static ResourceRecord toRecord(ResourceSaveRequest request) {
        ResourceRecord record = new ResourceRecord();
        record.setName(request.name().trim());
        record.setLocation(request.location().trim());
        record.setResourceType(request.resourceType());
        record.setUnitLabel(request.unitLabel().trim());
        record.setEnabled(request.enabled());
        return record;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ResourceRecord requireResource(long resourceId) {
        if (resourceId <= 0) {
            throw new BusinessException(400, "INVALID_ARGUMENT", "资源ID不合法");
        }
        ResourceRecord resource = resourceMapper.findById(resourceId);
        if (resource == null) {
            throw new BusinessException(404, "RESOURCE_NOT_FOUND", "资源不存在");
        }
        return resource;
    }
}
