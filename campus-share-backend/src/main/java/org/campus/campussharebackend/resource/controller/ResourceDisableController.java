package org.campus.campussharebackend.resource.controller;

import jakarta.validation.Valid;
import org.campus.campussharebackend.common.ApiResponse;
import org.campus.campussharebackend.resource.dto.DisableResourceRequest;
import org.campus.campussharebackend.resource.service.ResourceDisableService;
import org.campus.campussharebackend.resource.vo.DisableResourceResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/resources")
public class ResourceDisableController {
    private final ResourceDisableService service;

    public ResourceDisableController(ResourceDisableService service) {
        this.service = service;
    }

    @PostMapping("/{resourceId}/disable-periods")
    public ApiResponse<DisableResourceResult> disable(@PathVariable long resourceId,
                                                     @Valid @RequestBody DisableResourceRequest request) {
        return ApiResponse.ok("停用成功", service.disable(resourceId, request));
    }
}
