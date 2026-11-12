package org.campus.campussharebackend.resource.controller;

import org.campus.campussharebackend.common.ApiResponse;
import org.campus.campussharebackend.common.PageResponse;
import org.campus.campussharebackend.resource.dto.ResourceSaveRequest;
import org.campus.campussharebackend.resource.service.ResourceService;
import org.campus.campussharebackend.resource.vo.ResourceVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * XIA-2 资源维护入口。访问权限由 SecurityConfig 的 /api/v1/admin/** 规则校验，
 * 停用和开放安排分别属于 XIA-14、XIA-3，不在此重复实现。
 */
@RestController
@RequestMapping("/api/v1/admin/resources")
public class AdminResourceController {

    private final ResourceService resourceService;

    public AdminResourceController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping
    public ApiResponse<PageResponse<ResourceVO>> list(@RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "20") int size,
                                                      @RequestParam(required = false) String keyword,
                                                      @RequestParam(required = false) String type) {
        return ApiResponse.ok("OK", resourceService.adminList(keyword, type, page, size));
    }

    @PostMapping
    public ApiResponse<ResourceVO> create(@Valid @RequestBody ResourceSaveRequest request) {
        return ApiResponse.ok("资源已创建", resourceService.create(request));
    }

    @PutMapping("/{resourceId}")
    public ApiResponse<ResourceVO> update(@PathVariable long resourceId,
                                          @Valid @RequestBody ResourceSaveRequest request) {
        return ApiResponse.ok("资源已更新", resourceService.update(resourceId, request));
    }
}
