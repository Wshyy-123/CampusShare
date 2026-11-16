package com.campusshare.availability.dto;

import com.campusshare.resource.domain.ResourceStatus;
import com.campusshare.resource.domain.ResourceType;
import java.time.LocalDate;
import java.util.List;

public record ResourceAvailabilityResponse(
        Long resourceId,
        String resourceName,
        ResourceType resourceType,
        ResourceStatus resourceStatus,
        LocalDate date,
        List<UnitAvailabilityResponse> units) {
}
