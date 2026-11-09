package com.campusshare.resource.controller;

import com.campusshare.availability.dto.ResourceAvailabilityResponse;
import com.campusshare.availability.dto.UnitAvailabilityResponse;
import com.campusshare.availability.service.AvailabilityService;
import com.campusshare.common.ApiResponse;
import com.campusshare.resource.domain.ResourceType;
import com.campusshare.resource.dto.PageResponse;
import com.campusshare.resource.dto.ResourceDetailResponse;
import com.campusshare.resource.dto.ResourceListItemResponse;
import com.campusshare.resource.service.ResourceService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/resources")
public class ResourceController {

    private final ResourceService resourceService;
    private final AvailabilityService availabilityService;

    public ResourceController(
            ResourceService resourceService,
            AvailabilityService availabilityService) {
        this.resourceService = resourceService;
        this.availabilityService = availabilityService;
    }

    @GetMapping
    public ApiResponse<PageResponse<ResourceListItemResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ResourceType type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ApiResponse.success(resourceService.search(keyword, type, page, size));
    }

    @GetMapping("/{resourceId}")
    public ApiResponse<ResourceDetailResponse> detail(@PathVariable Long resourceId) {
        return ApiResponse.success(resourceService.getDetail(resourceId));
    }

    @GetMapping("/{resourceId}/availability")
    public ApiResponse<ResourceAvailabilityResponse> resourceAvailability(
            @PathVariable Long resourceId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {

        return ApiResponse.success(
                availabilityService.getResourceAvailability(resourceId, date));
    }

    @GetMapping("/units/{unitId}/availability")
    public ApiResponse<UnitAvailabilityResponse> unitAvailability(
            @PathVariable Long unitId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {

        return ApiResponse.success(
                availabilityService.getUnitAvailability(unitId, date));
    }
}
