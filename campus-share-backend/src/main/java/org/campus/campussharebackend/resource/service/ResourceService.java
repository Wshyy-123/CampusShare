package org.campus.campussharebackend.resource.service;

import org.campus.campussharebackend.common.BusinessException;
import org.campus.campussharebackend.common.TimeFormats;
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

    private final Clock clock;
    public ResourceService(ResourceMapper resourceMapper, @Qualifier("lifecycleClock") Clock clock) {
        this.resourceMapper = resourceMapper;
        this.clock = clock;
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
