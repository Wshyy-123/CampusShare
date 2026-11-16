package com.campusshare.resource.dto;

import com.campusshare.resource.domain.ResourceStatus;

public record ResourceUnitSummaryResponse(
        Long id,
        String name,
        String unitCode,
        ResourceStatus status) {
}
