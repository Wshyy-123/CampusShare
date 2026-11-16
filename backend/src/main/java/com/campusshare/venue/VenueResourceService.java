package com.campusshare.venue;

import com.campusshare.common.api.PageResponse;
import com.campusshare.common.error.BusinessException;
import com.campusshare.common.error.ErrorCode;
import com.campusshare.venue.dto.ResourceRequest;
import com.campusshare.venue.dto.ResourceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VenueResourceService {

    private final VenueResourceRepository resourceRepository;

    public VenueResourceService(VenueResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ResourceResponse> list(int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.ASC, "id"));
        Page<VenueResource> result = resourceRepository.findAll(pageable);
        List<ResourceResponse> items = result.getContent().stream()
                .map(this::toResponse)
                .toList();
        return new PageResponse<>(items, result.getTotalElements(), page, size);
    }

    @Transactional
    public ResourceResponse create(ResourceRequest request) {
        validateBookingUnit(request);
        VenueResource resource = new VenueResource();
        apply(resource, request);
        return toResponse(resourceRepository.save(resource));
    }

    @Transactional
    public ResourceResponse update(Long id, ResourceRequest request) {
        VenueResource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "资源不存在"));
        validateBookingUnit(request);
        apply(resource, request);
        return toResponse(resourceRepository.save(resource));
    }

    private void validateBookingUnit(ResourceRequest request) {
        if (request.type().getBookingUnit() != request.bookingUnit()) {
            throw new BusinessException(ErrorCode.INVALID_BOOKING_UNIT,
                    request.type().getLabel() + "应按" + request.type().getBookingUnit().getLabel() + "预约");
        }
    }

    private void apply(VenueResource resource, ResourceRequest request) {
        resource.setName(request.name().trim());
        resource.setLocation(request.location().trim());
        resource.setType(request.type());
        resource.setBookingUnit(request.bookingUnit());
    }

    private ResourceResponse toResponse(VenueResource resource) {
        return new ResourceResponse(
                resource.getId(),
                resource.getName(),
                resource.getLocation(),
                resource.getType(),
                resource.getType().getLabel(),
                resource.getBookingUnit(),
                resource.getBookingUnit().getLabel(),
                resource.getCreatedAt(),
                resource.getUpdatedAt());
    }
}
