package com.campusshare.schedule.service;

import com.campusshare.common.BusinessException;
import com.campusshare.resource.repository.ResourceUnitRepository;
import com.campusshare.schedule.domain.SchedulePeriod;
import com.campusshare.schedule.dto.ScheduleCreateRequest;
import com.campusshare.schedule.dto.ScheduleResponse;
import com.campusshare.schedule.dto.ScheduleUpdateRequest;
import com.campusshare.schedule.repository.SchedulePeriodRepository;
import java.time.*;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScheduleService {

    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final SchedulePeriodRepository scheduleRepository;
    private final ResourceUnitRepository unitRepository;

    public ScheduleService(
            SchedulePeriodRepository scheduleRepository,
            ResourceUnitRepository unitRepository) {
        this.scheduleRepository = scheduleRepository;
        this.unitRepository = unitRepository;
    }

    @Transactional
    public List<ScheduleResponse> create(ScheduleCreateRequest request) {
        TimeRange range = normalizeAndValidate(request.startAt(), request.endAt());

        List<Long> distinctUnitIds = request.unitIds().stream().distinct().toList();
        if (distinctUnitIds.isEmpty()) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "UNIT_REQUIRED",
                    "至少选择一个预约单位");
        }

        long existingCount = unitRepository.findAllById(distinctUnitIds).size();
        if (existingCount != distinctUnitIds.size()) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "RESOURCE_UNIT_NOT_FOUND",
                    "存在不存在的预约单位，未保存任何时段");
        }

        List<SchedulePeriod> entities = distinctUnitIds.stream()
                .map(unitId -> new SchedulePeriod(
                        unitId,
                        request.type(),
                        range.startAt(),
                        range.endAt(),
                        cleanNote(request.note())))
                .toList();

        return scheduleRepository.saveAll(entities).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ScheduleResponse> list(Long unitId, LocalDate date) {
        if (!unitRepository.existsById(unitId)) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "RESOURCE_UNIT_NOT_FOUND",
                    "预约单位不存在");
        }

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

        return scheduleRepository.findOverlappingDay(unitId, dayStart, dayEnd)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ScheduleResponse update(Long id, ScheduleUpdateRequest request) {
        SchedulePeriod entity = scheduleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "SCHEDULE_NOT_FOUND",
                        "时段记录不存在"));

        TimeRange range = normalizeAndValidate(request.startAt(), request.endAt());
        entity.update(
                request.type(),
                range.startAt(),
                range.endAt(),
                cleanNote(request.note()));

        return toResponse(scheduleRepository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        if (!scheduleRepository.existsById(id)) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "SCHEDULE_NOT_FOUND",
                    "时段记录不存在");
        }
        scheduleRepository.deleteById(id);
    }

    private TimeRange normalizeAndValidate(OffsetDateTime start, OffsetDateTime end) {
        LocalDateTime startAt = start.atZoneSameInstant(BUSINESS_ZONE).toLocalDateTime();
        LocalDateTime endAt = end.atZoneSameInstant(BUSINESS_ZONE).toLocalDateTime();

        if (!startAt.isBefore(endAt)) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "INVALID_TIME_RANGE",
                    "开始时间必须早于结束时间");
        }

        return new TimeRange(startAt, endAt);
    }

    private String cleanNote(String note) {
        if (note == null) {
            return null;
        }
        String trimmed = note.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ScheduleResponse toResponse(SchedulePeriod entity) {
        return new ScheduleResponse(
                entity.getId(),
                entity.getResourceUnitId(),
                entity.getPeriodType(),
                entity.getStartAt().atZone(BUSINESS_ZONE).toOffsetDateTime(),
                entity.getEndAt().atZone(BUSINESS_ZONE).toOffsetDateTime(),
                entity.getNote());
    }

    private record TimeRange(LocalDateTime startAt, LocalDateTime endAt) {
    }
}
