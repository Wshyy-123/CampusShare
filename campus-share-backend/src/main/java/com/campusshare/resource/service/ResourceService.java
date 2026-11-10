package com.campusshare.resource.service;

import com.campusshare.common.BusinessException;
import com.campusshare.common.TimeFormats;
import com.campusshare.resource.entity.ResourceRecord;
import com.campusshare.resource.mapper.ResourceMapper;
import com.campusshare.resource.vo.AvailabilityVO;
import com.campusshare.resource.vo.BlockVO;
import com.campusshare.resource.vo.IntervalVO;
import com.campusshare.resource.vo.ReservationSlotVO;
import com.campusshare.resource.vo.ResourceVO;
import java.time.LocalDate;
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

    public ResourceService(ResourceMapper resourceMapper) {
        this.resourceMapper = resourceMapper;
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
        LocalDate targetDate = date == null ? LocalDate.now(TimeFormats.BUSINESS_ZONE) : date;
        LocalDateTime dayStart = targetDate.atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);
        return new AvailabilityVO(
                ResourceVO.from(resource),
                targetDate.toString(),
                resourceMapper.listOpeningsForDate(resourceId, dayStart, dayEnd).stream().map(IntervalVO::from).toList(),
                resourceMapper.listBlocksForDate(resourceId, dayStart, dayEnd).stream().map(BlockVO::from).toList(),
                resourceMapper.listActiveReservationsForDate(resourceId, dayStart, dayEnd)
                        .stream().map(ReservationSlotVO::from).toList());
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
