package com.campusshare.resource.dto;

import com.campusshare.resource.domain.ResourceStatus;
import com.campusshare.resource.domain.ResourceType;
import java.util.List;

public record ResourceDetailResponse(
        Long id,
        String name,
        String location,
        ResourceType type,
        ResourceStatus status,
        List<ResourceUnitSummaryResponse> units) {
}
