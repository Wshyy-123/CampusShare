package com.campusshare.venue;

import com.campusshare.common.api.ApiResponse;
import com.campusshare.common.api.PageResponse;
import com.campusshare.venue.dto.ResourceRequest;
import com.campusshare.venue.dto.ResourceResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/resources")
@PreAuthorize("hasRole('RESOURCE_ADMIN')")
@Validated
public class AdminResourceController {

    private final VenueResourceService resourceService;

    public AdminResourceController(VenueResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping
    public ApiResponse<PageResponse<ResourceResponse>> list(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码必须从1开始") int page,
            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "每页条数至少为1")
            @Max(value = 100, message = "每页条数不能超过100") int size) {
        return ApiResponse.success(resourceService.list(page, size));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ResourceResponse> create(@Valid @RequestBody ResourceRequest request) {
        return ApiResponse.success(resourceService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ResourceResponse> update(@PathVariable Long id,
                                                @Valid @RequestBody ResourceRequest request) {
        return ApiResponse.success(resourceService.update(id, request));
    }
}
