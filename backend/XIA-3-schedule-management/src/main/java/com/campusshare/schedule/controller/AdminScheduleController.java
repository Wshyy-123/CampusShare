package com.campusshare.schedule.controller;

import com.campusshare.common.ApiResponse;
import com.campusshare.schedule.dto.ScheduleCreateRequest;
import com.campusshare.schedule.dto.ScheduleResponse;
import com.campusshare.schedule.dto.ScheduleUpdateRequest;
import com.campusshare.schedule.service.ScheduleService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/schedules")
@PreAuthorize("hasRole('RESOURCE_ADMIN')")
public class AdminScheduleController {

    private final ScheduleService scheduleService;

    public AdminScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostMapping
    public ApiResponse<List<ScheduleResponse>> create(
            @Valid @RequestBody ScheduleCreateRequest request) {
        return ApiResponse.success("时段保存成功", scheduleService.create(request));
    }

    @GetMapping
    public ApiResponse<List<ScheduleResponse>> list(
            @RequestParam Long unitId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {
        return ApiResponse.success(scheduleService.list(unitId, date));
    }

    @PutMapping("/{id}")
    public ApiResponse<ScheduleResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ScheduleUpdateRequest request) {
        return ApiResponse.success("时段修改成功", scheduleService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        scheduleService.delete(id);
        return ApiResponse.success("时段删除成功", null);
    }
}
