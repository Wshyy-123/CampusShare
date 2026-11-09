package com.campusshare.resource.service;

import com.campusshare.common.BusinessException;
import com.campusshare.resource.domain.CampusResource;
import com.campusshare.resource.domain.ResourceType;
import com.campusshare.resource.dto.PageResponse;
import com.campusshare.resource.dto.ResourceDetailResponse;
import com.campusshare.resource.dto.ResourceListItemResponse;
import com.campusshare.resource.dto.ResourceUnitSummaryResponse;
import com.campusshare.resource.repository.CampusResourceRepository;
import com.campusshare.resource.repository.ResourceUnitRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class ResourceService {

    private final CampusResourceRepository resourceRepository;
    private final ResourceUnitRepository unitRepository;

    public ResourceService(
            CampusResourceRepository resourceRepository,
            ResourceUnitRepository unitRepository) {
        this.resourceRepository = resourceRepository;
        this.unitRepository = unitRepository;
    }

    public PageResponse<ResourceListItemResponse> search(
            String keyword,
            ResourceType type,
            int page,
            int size) {

        if (page < 1) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST, "INVALID_PAGE", "page 必须从 1 开始");
        }
        if (size < 1 || size > 100) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST, "INVALID_SIZE", "size 必须在 1~100 之间");
        }

        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;
        var result = resourceRepository.search(
                normalizedKeyword, type, PageRequest.of(page - 1, size));

        List<ResourceListItemResponse> items = result.getContent().stream()
                .map(resource -> new ResourceListItemResponse(
                        resource.getId(),
                        resource.getName(),
                        resource.getLocation(),
                        resource.getType(),
                        resource.getStatus(),
                        unitRepository.countByResourceId(resource.getId())))
                .toList();

        return new PageResponse<>(items, result.getTotalElements(), page, size);
    }

    public ResourceDetailResponse getDetail(Long resourceId) {
        CampusResource resource = resourceRepository.findById(resourceId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "资源不存在"));

        List<ResourceUnitSummaryResponse> units = unitRepository
                .findByResourceIdOrderById(resourceId)
                .stream()
                .map(unit -> new ResourceUnitSummaryResponse(
                        unit.getId(),
                        unit.getName(),
                        unit.getUnitCode(),
                        unit.getStatus()))
                .toList();

        return new ResourceDetailResponse(
                resource.getId(),
                resource.getName(),
                resource.getLocation(),
                resource.getType(),
                resource.getStatus(),
                units);
    }
}
