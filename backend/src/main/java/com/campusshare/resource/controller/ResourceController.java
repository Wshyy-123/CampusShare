package com.campusshare.resource.controller;

import com.campusshare.common.ApiResponse;
import com.campusshare.resource.service.ResourceService;
import com.campusshare.resource.vo.AvailabilityVO;
import com.campusshare.resource.vo.ResourceVO;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/resources")
public class ResourceController {

    private final ResourceService resourceService;

    public ResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping
    public ApiResponse<List<ResourceVO>> list(@RequestParam(required = false) String type,
                                              @RequestParam(required = false) String location) {
        return ApiResponse.ok("OK", resourceService.list(type, location));
    }

    @GetMapping("/{resourceId}")
    public ApiResponse<ResourceVO> get(@PathVariable long resourceId) {
        return ApiResponse.ok("OK", resourceService.get(resourceId));
    }

    @GetMapping("/{resourceId}/availability")
    public ApiResponse<AvailabilityVO> availability(
            @PathVariable long resourceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok("OK", resourceService.availability(resourceId, date));
    }
}
