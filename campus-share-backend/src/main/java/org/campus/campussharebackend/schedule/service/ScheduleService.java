package org.campus.campussharebackend.schedule.service;

import org.campus.campussharebackend.auth.CurrentUser;
import org.campus.campussharebackend.auth.CurrentUserProvider;
import org.campus.campussharebackend.common.BusinessException;
import org.campus.campussharebackend.common.TimeFormats;
import org.campus.campussharebackend.resource.entity.BlockRecord;
import org.campus.campussharebackend.resource.entity.OpeningRecord;
import org.campus.campussharebackend.resource.entity.ResourceRecord;
import org.campus.campussharebackend.resource.mapper.ResourceMapper;
import org.campus.campussharebackend.schedule.dto.OpeningCreateRequest;
import org.campus.campussharebackend.schedule.dto.ScheduleUpdateRequest;
import org.campus.campussharebackend.schedule.dto.TeachingBlockCreateRequest;
import org.campus.campussharebackend.schedule.entity.ScheduleRow;
import org.campus.campussharebackend.schedule.mapper.ScheduleRepository;
import org.campus.campussharebackend.schedule.vo.ScheduleVO;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScheduleService {

    public static final String TYPE_OPEN = "OPEN";
    public static final String TYPE_TEACHING = "TEACHING";

    private final ScheduleRepository scheduleRepository;
    private final ResourceMapper resourceMapper;
    private final CurrentUserProvider currentUserProvider;

    public ScheduleService(ScheduleRepository scheduleRepository,
                           ResourceMapper resourceMapper,
                           CurrentUserProvider currentUserProvider) {
        this.scheduleRepository = scheduleRepository;
        this.resourceMapper = resourceMapper;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional(readOnly = true)
    public List<ScheduleVO> list(Long resourceId, LocalDate date) {
        requireAdmin();
        LocalDate targetDate = date == null ? LocalDate.now(TimeFormats.BUSINESS_ZONE) : date;
        LocalDateTime dayStart = targetDate.atStartOfDay();
        LocalDateTime dayEnd = dayStart.plusDays(1);
        Long filteredResourceId = resourceId == null || resourceId <= 0 ? null : resourceId;
        return scheduleRepository.list(filteredResourceId, dayStart, dayEnd).stream()
                .map(ScheduleVO::from)
                .toList();
    }

    @Transactional
    public ScheduleVO createOpening(String resourceId, OpeningCreateRequest request) {
        requireAdmin();
        long parsedResourceId = parseResourceId(resourceId);
        LocalDateTime start = TimeFormats.toLocal(request.startTime());
        LocalDateTime end = TimeFormats.toLocal(request.endTime());
        validateRange(start, end);
        ResourceRecord resource = requireResource(parsedResourceId);
        if (scheduleRepository.countOverlappingOpening(parsedResourceId, start, end, null) > 0) {
            throw new BusinessException(409, "SCHEDULE_CONFLICT", "该时段与已有开放安排重叠");
        }
        OpeningRecord record = new OpeningRecord();
        record.setResourceId(parsedResourceId);
        record.setStartAt(start);
        record.setEndAt(end);
        record.setNote(normalizeNote(request.note()));
        scheduleRepository.insertOpening(record);
        return ScheduleVO.of(record.getId(), parsedResourceId, resource.getName(),
                TYPE_OPEN, start, end, record.getNote());
    }

    @Transactional
    public ScheduleVO createTeachingBlock(String resourceId, TeachingBlockCreateRequest request) {
        requireAdmin();
        long parsedResourceId = parseResourceId(resourceId);
        LocalDateTime start = TimeFormats.toLocal(request.startTime());
        LocalDateTime end = TimeFormats.toLocal(request.endTime());
        validateRange(start, end);
        ResourceRecord resource = requireResource(parsedResourceId);
        if (scheduleRepository.countOverlappingTeaching(parsedResourceId, start, end, null) > 0) {
            throw new BusinessException(409, "SCHEDULE_CONFLICT", "该时段与已有教学占用重叠");
        }
        BlockRecord record = new BlockRecord();
        record.setResourceId(parsedResourceId);
        record.setBlockType(TYPE_TEACHING);
        record.setStartAt(start);
        record.setEndAt(end);
        record.setReason(normalizeReason(request.reason()));
        scheduleRepository.insertTeachingBlock(record);
        return ScheduleVO.of(record.getId(), parsedResourceId, resource.getName(),
                TYPE_TEACHING, start, end, record.getReason());
    }

    @Transactional
    public ScheduleVO update(String scheduleId, ScheduleUpdateRequest request) {
        requireAdmin();
        long id = parseScheduleId(scheduleId);
        String type = normalizeType(request.type());
        LocalDateTime start = TimeFormats.toLocal(request.startTime());
        LocalDateTime end = TimeFormats.toLocal(request.endTime());
        validateRange(start, end);

        OpeningRecord opening = scheduleRepository.findOpening(id);
        if (opening != null) {
            requireTypeMatch(type, TYPE_OPEN);
            if (scheduleRepository.countOverlappingOpening(opening.getResourceId(), start, end, id) > 0) {
                throw new BusinessException(409, "SCHEDULE_CONFLICT", "该时段与已有开放安排重叠");
            }
            String note = normalizeNote(request.note());
            scheduleRepository.updateOpening(id, start, end, note);
            ResourceRecord resource = requireResource(opening.getResourceId());
            return ScheduleVO.of(id, opening.getResourceId(), resource.getName(),
                    TYPE_OPEN, start, end, note);
        }

        BlockRecord block = scheduleRepository.findTeachingBlock(id);
        if (block != null) {
            requireTypeMatch(type, TYPE_TEACHING);
            if (scheduleRepository.countOverlappingTeaching(block.getResourceId(), start, end, id) > 0) {
                throw new BusinessException(409, "SCHEDULE_CONFLICT", "该时段与已有教学占用重叠");
            }
            String reason = normalizeReason(request.note());
            scheduleRepository.updateTeachingBlock(id, start, end, reason);
            ResourceRecord resource = requireResource(block.getResourceId());
            return ScheduleVO.of(id, block.getResourceId(), resource.getName(),
                    TYPE_TEACHING, start, end, reason);
        }

        throw new BusinessException(404, "SCHEDULE_NOT_FOUND", "时段安排不存在或无权操作");
    }

    @Transactional
    public void delete(String scheduleId) {
        requireAdmin();
        long id = parseScheduleId(scheduleId);
        OpeningRecord opening = scheduleRepository.findOpening(id);
        if (opening != null) {
            scheduleRepository.deleteOpening(id);
            return;
        }
        BlockRecord block = scheduleRepository.findTeachingBlock(id);
        if (block != null) {
            scheduleRepository.deleteTeachingBlock(id);
            return;
        }
        throw new BusinessException(404, "SCHEDULE_NOT_FOUND", "时段安排不存在或无权操作");
    }

    private CurrentUser requireAdmin() {
        CurrentUser user = currentUserProvider.require();
        if (!"RESOURCE_ADMIN".equals(user.role())) {
            throw new BusinessException(403, "FORBIDDEN", "无权限维护时段安排");
        }
        return user;
    }

    private ResourceRecord requireResource(long resourceId) {
        ResourceRecord resource = resourceMapper.findById(resourceId);
        if (resource == null) {
            throw new BusinessException(404, "RESOURCE_NOT_FOUND", "资源不存在");
        }
        return resource;
    }

    private static void validateRange(LocalDateTime start, LocalDateTime end) {
        if (!start.isBefore(end)) {
            throw new BusinessException(400, "INVALID_ARGUMENT", "开始时间必须早于结束时间");
        }
    }

    private static String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            throw new BusinessException(400, "INVALID_ARGUMENT", "时段类型不合法");
        }
        String normalized = type.trim().toUpperCase();
        if (!TYPE_OPEN.equals(normalized) && !TYPE_TEACHING.equals(normalized)) {
            throw new BusinessException(400, "INVALID_ARGUMENT", "时段类型不合法");
        }
        return normalized;
    }

    private static void requireTypeMatch(String requested, String actual) {
        if (!actual.equals(requested)) {
            throw new BusinessException(400, "INVALID_ARGUMENT", "不能变更时段类型，请删除后重新创建");
        }
    }

    private static String normalizeNote(String note) {
        if (note == null) return null;
        String trimmed = note.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalizeReason(String reason) {
        String normalized = normalizeNote(reason);
        return normalized == null ? "教学占用" : normalized;
    }

    private static long parseResourceId(String value) {
        try {
            long resourceId = Long.parseLong(value);
            if (resourceId <= 0) throw new NumberFormatException();
            return resourceId;
        } catch (NumberFormatException exception) {
            throw new BusinessException(400, "INVALID_ARGUMENT", "resourceId 不合法");
        }
    }

    private static long parseScheduleId(String value) {
        try {
            long id = Long.parseLong(value);
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException exception) {
            throw new BusinessException(404, "SCHEDULE_NOT_FOUND", "时段安排不存在或无权操作");
        }
    }
}
