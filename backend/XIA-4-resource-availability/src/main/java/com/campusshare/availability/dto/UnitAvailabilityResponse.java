package com.campusshare.availability.dto;

import com.campusshare.resource.domain.ResourceStatus;
import com.campusshare.resource.domain.ResourceType;
import java.time.LocalDate;
import java.util.List;

public record UnitAvailabilityResponse(
        Long resourceId,
        String resourceName,
        ResourceType resourceType,
        ResourceStatus resourceStatus,
        Long unitId,
        String unitName,
        String unitCode,
        ResourceStatus unitStatus,
        LocalDate date,
        List<TimeRangeResponse> openPeriods,
        List<TimeRangeResponse> teachingPeriods,
        List<TimeRangeResponse> reservationPeriods,
        List<TimeRangeResponse> blockedPeriods,
        List<TimeRangeResponse> availablePeriods,
        boolean hasAvailablePeriod,
        String message) {
}
