package org.campus.campussharebackend.schedule.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.campus.campussharebackend.auth.CurrentUserProvider;
import org.campus.campussharebackend.common.BusinessException;
import org.campus.campussharebackend.common.TimeFormats;
import org.campus.campussharebackend.lifecycle.LifecycleMapper;
import org.campus.campussharebackend.resource.entity.BlockRecord;
import org.campus.campussharebackend.resource.entity.OpeningRecord;
import org.campus.campussharebackend.resource.entity.ResourceRecord;
import org.campus.campussharebackend.resource.mapper.ResourceMapper;
import org.campus.campussharebackend.resource.service.AvailabilityCalculator;
import org.campus.campussharebackend.schedule.dto.*;
import org.campus.campussharebackend.schedule.mapper.ScheduleRepository;
import org.campus.campussharebackend.schedule.vo.ScheduleVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScheduleService {
    private final ScheduleRepository schedules;
    private final ResourceMapper resources;
    private final CurrentUserProvider users;
    private final LifecycleMapper lifecycle;
    private final com.fasterxml.jackson.databind.ObjectMapper json;
    private final java.time.Clock clock;

    public ScheduleService(ScheduleRepository schedules, ResourceMapper resources,
                           CurrentUserProvider users, LifecycleMapper lifecycle,
                           com.fasterxml.jackson.databind.ObjectMapper json,
                           @org.springframework.beans.factory.annotation.Qualifier("lifecycleClock") java.time.Clock clock) {
        this.schedules = schedules; this.resources = resources; this.users = users; this.lifecycle = lifecycle; this.json = json; this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<ScheduleVO> list(Long resourceId, LocalDate date) {
        requireAdmin();
        if (resourceId != null && resourceId <= 0) throw invalid("resourceId 不合法");
        LocalDate day = date == null ? LocalDate.now(TimeFormats.BUSINESS_ZONE) : date;
        return schedules.list(resourceId, day.atStartOfDay(), day.plusDays(1).atStartOfDay())
                .stream().map(ScheduleVO::from).toList();
    }

    @Transactional
    public ScheduleVO createOpening(String resourceId, OpeningCreateRequest request) {
        requireAdmin();
        long id = parseId(resourceId);
        var start = TimeFormats.toLocal(request.startTime()); var end = TimeFormats.toLocal(request.endTime());
        validateRange(start, end);
        var resource = lockResource(id);
        openingConflict(id, start, end, null);
        var record = new OpeningRecord();
        record.setResourceId(id); record.setStartAt(start); record.setEndAt(end); record.setNote(note(request.note()));
        schedules.insertOpening(record);
        var result = ScheduleVO.of(record.getId(), id, resource.getName(), "OPEN", start, end, record.getNote());
        audit("CREATE", null, result);
        return result;
    }

    @Transactional
    public ScheduleVO createTeachingBlock(String resourceId, TeachingBlockCreateRequest request) {
        requireAdmin();
        long id = parseId(resourceId);
        var start = TimeFormats.toLocal(request.startTime()); var end = TimeFormats.toLocal(request.endTime());
        validateRange(start, end);
        var resource = lockResource(id);
        teachingConflict(id, start, end, null);
        var record = new BlockRecord();
        record.setResourceId(id); record.setBlockType("TEACHING"); record.setStartAt(start); record.setEndAt(end);
        record.setReason(note(request.reason()) == null ? "教学占用" : note(request.reason()));
        schedules.insertTeachingBlock(record);
        var result = ScheduleVO.of(record.getId(), id, resource.getName(), "TEACHING", start, end, record.getReason());
        audit("CREATE", null, result);
        return result;
    }

    @Transactional
    public ScheduleVO update(String scheduleId, ScheduleUpdateRequest request) {
        requireAdmin();
        long id = parseId(scheduleId); String type = type(request.type());
        var start = TimeFormats.toLocal(request.startTime()); var end = TimeFormats.toLocal(request.endTime());
        validateRange(start, end);
        // type 与 id 联合定位。先普通读取资源编号，再按统一顺序锁资源和重新读取目标。
        if ("OPEN".equals(type)) {
            var hint = schedules.findOpening(id);
            if (hint == null) throw missing();
            var resource = lockResource(hint.getResourceId());
            var opening = schedules.lockOpening(id);
            if (opening == null) throw missing();
            openingConflict(resource.getId(), start, end, id);
            schedules.updateOpening(id, start, end, note(request.note()));
            preserveBookings(resource.getId(), opening.getStartAt(), opening.getEndAt());
            var result = ScheduleVO.of(id, resource.getId(), resource.getName(), type, start, end, note(request.note()));
            audit("UPDATE", ScheduleVO.of(id, resource.getId(), resource.getName(), type, opening.getStartAt(), opening.getEndAt(), opening.getNote()), result);
            return result;
        }
        var hint = schedules.findTeachingBlock(id);
        if (hint == null) throw missing();
        var resource = lockResource(hint.getResourceId());
        var block = schedules.lockTeachingBlock(id);
        if (block == null) throw missing();
        teachingConflict(resource.getId(), start, end, id);
        String reason = note(request.note()) == null ? "教学占用" : note(request.note());
        schedules.updateTeachingBlock(id, start, end, reason);
        var result = ScheduleVO.of(id, resource.getId(), resource.getName(), type, start, end, reason);
        audit("UPDATE", ScheduleVO.of(id, resource.getId(), resource.getName(), type, block.getStartAt(), block.getEndAt(), block.getReason()), result);
        return result;
    }

    @Transactional
    public void delete(String scheduleId, String requestedType) {
        requireAdmin();
        long id = parseId(scheduleId); String type = type(requestedType);
        if ("OPEN".equals(type)) {
            var hint = schedules.findOpening(id);
            if (hint == null) throw missing();
            var resource = lockResource(hint.getResourceId());
            var opening = schedules.lockOpening(id);
            if (opening == null) throw missing();
            audit("DELETE", ScheduleVO.of(id, resource.getId(), resource.getName(), type, opening.getStartAt(), opening.getEndAt(), opening.getNote()), null);
            schedules.deleteOpening(id);
            preserveBookings(opening.getResourceId(), opening.getStartAt(), opening.getEndAt());
        } else {
            var hint = schedules.findTeachingBlock(id);
            if (hint == null) throw missing();
            var resource = lockResource(hint.getResourceId());
            var block = schedules.lockTeachingBlock(id);
            if (block == null) throw missing();
            audit("DELETE", ScheduleVO.of(id, resource.getId(), resource.getName(), type, block.getStartAt(), block.getEndAt(), block.getReason()), null);
            schedules.deleteTeachingBlock(id);
        }
    }

    private void audit(String operation, ScheduleVO before, ScheduleVO after) {
        var value = after == null ? before : after;
        try {
            schedules.audit(Long.parseLong(value.resourceId()), value.type(), Long.parseLong(value.scheduleId()),
                    operation, users.require().userId(), TimeFormats.now(clock), org.campus.campussharebackend.common.RequestIdContext.get(),
                    before == null ? null : json.writeValueAsString(before), after == null ? null : json.writeValueAsString(after));
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new IllegalStateException("无法记录时段维护历史", exception);
        }
    }

    private void openingConflict(long id, LocalDateTime start, LocalDateTime end, Long exclude) {
        if (!schedules.lockOverlappingOpening(id, start, end, exclude).isEmpty())
            throw conflict("该时段与已有开放安排重叠");
    }
    private void teachingConflict(long id, LocalDateTime start, LocalDateTime end, Long exclude) {
        // 维护课表不能静默侵占已接受的预约；需要取消时使用有历史记录的停用流程。
        if (!lifecycle.lockAffected(id, start, end).isEmpty()) throw conflict("该教学时段与有效预约冲突，请先处理受影响预约");
        if (!schedules.lockOverlappingTeaching(id, start, end, exclude).isEmpty()) throw conflict("该时段与已有教学占用重叠");
    }
    private void preserveBookings(long id, LocalDateTime start, LocalDateTime end) {
        for (var booking : lifecycle.lockAffected(id, start, end)) {
            if (!AvailabilityCalculator.covers(resources.lockOpenings(id, booking.startAt(), booking.endAt()), booking.startAt(), booking.endAt()))
                throw conflict("调整后开放时间不能覆盖已有有效预约，请先处理受影响预约");
        }
    }
    private ResourceRecord lockResource(long id) {
        var resource = resources.lockById(id);
        if (resource == null) throw new BusinessException(404, "RESOURCE_NOT_FOUND", "资源不存在");
        return resource;
    }
    private void requireAdmin() {
        if (!"RESOURCE_ADMIN".equals(users.require().role())) throw new BusinessException(403, "FORBIDDEN", "无权限维护时段安排");
    }
    private static void validateRange(LocalDateTime start, LocalDateTime end) {
        if (!start.isBefore(end)) throw invalid("开始时间必须早于结束时间");
    }
    private static String type(String value) {
        if (!"OPEN".equals(value) && !"TEACHING".equals(value)) throw invalid("维护类型必须是OPEN或TEACHING");
        return value;
    }
    private static String note(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private static long parseId(String value) {
        try { long id = Long.parseLong(value); if (id <= 0) throw new NumberFormatException(); return id; }
        catch (NumberFormatException e) { throw invalid("编号不合法"); }
    }
    private static BusinessException missing() { return new BusinessException(404, "SCHEDULE_NOT_FOUND", "时段安排不存在"); }
    private static BusinessException invalid(String message) { return new BusinessException(400, "INVALID_ARGUMENT", message); }
    private static BusinessException conflict(String message) { return new BusinessException(409, "SCHEDULE_CONFLICT", message); }
}
