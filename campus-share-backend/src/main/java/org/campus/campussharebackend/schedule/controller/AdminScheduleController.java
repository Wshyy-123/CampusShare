package org.campus.campussharebackend.schedule.controller;

import org.campus.campussharebackend.common.ApiResponse;
import org.campus.campussharebackend.schedule.dto.OpeningCreateRequest;
import org.campus.campussharebackend.schedule.dto.ScheduleUpdateRequest;
import org.campus.campussharebackend.schedule.dto.TeachingBlockCreateRequest;
import org.campus.campussharebackend.schedule.service.ScheduleService;
import org.campus.campussharebackend.schedule.vo.ScheduleVO;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminScheduleController {

    private final ScheduleService scheduleService;

    public AdminScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping("/schedules")
    public ApiResponse<List<ScheduleVO>> list(
            @RequestParam(required = false) Long resourceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.ok("OK", scheduleService.list(resourceId, date));
    }

    @PostMapping("/resources/{resourceId}/openings")
    public ResponseEntity<ApiResponse<ScheduleVO>> createOpening(
            @PathVariable String resourceId,
            @Valid @RequestBody OpeningCreateRequest request) {
        ScheduleVO schedule = scheduleService.createOpening(resourceId, request);
        return ResponseEntity.status(201).body(ApiResponse.ok("开放安排已保存", schedule));
    }

    @PostMapping("/resources/{resourceId}/teaching-blocks")
    public ResponseEntity<ApiResponse<ScheduleVO>> createTeachingBlock(
            @PathVariable String resourceId,
            @Valid @RequestBody TeachingBlockCreateRequest request) {
        ScheduleVO schedule = scheduleService.createTeachingBlock(resourceId, request);
        return ResponseEntity.status(201).body(ApiResponse.ok("教学占用已保存", schedule));
    }

    @PutMapping("/schedules/{scheduleId}")
    public ApiResponse<ScheduleVO> update(
            @PathVariable String scheduleId,
            @Valid @RequestBody ScheduleUpdateRequest request) {
        return ApiResponse.ok("时段安排已更新", scheduleService.update(scheduleId, request));
    }

    @DeleteMapping("/schedules/{scheduleId}")
    public ApiResponse<Void> delete(@PathVariable String scheduleId) {
        scheduleService.delete(scheduleId);
        return ApiResponse.ok("时段安排已删除", null);
    }
}
