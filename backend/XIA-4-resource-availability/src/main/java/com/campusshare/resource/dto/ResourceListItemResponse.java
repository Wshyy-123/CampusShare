package com.campusshare.resource.dto;

import com.campusshare.resource.domain.ResourceStatus;
import com.campusshare.resource.domain.ResourceType;

public record ResourceListItemResponse(
        Long id,
        String name,
        String location,
        ResourceType type,
        ResourceStatus status,
        long unitCount) {
}
