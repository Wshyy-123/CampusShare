package org.campus.campussharebackend.schedule.vo;

import org.campus.campussharebackend.common.TimeFormats;
import org.campus.campussharebackend.schedule.entity.ScheduleRow;
import java.time.LocalDateTime;

public record ScheduleVO(String scheduleId, String resourceId, String resourceName,
                         String type, String startTime, String endTime, String note) {

    public static ScheduleVO from(ScheduleRow row) {
        return new ScheduleVO(
                Long.toString(row.getId()),
                Long.toString(row.getResourceId()),
                row.getResourceName(),
                row.getType(),
                TimeFormats.format(row.getStartAt()),
                TimeFormats.format(row.getEndAt()),
                row.getNote());
    }

    public static ScheduleVO of(long id, long resourceId, String resourceName, String type,
                               LocalDateTime startAt, LocalDateTime endAt, String note) {
        ScheduleRow row = new ScheduleRow();
        row.setId(id);
        row.setResourceId(resourceId);
        row.setResourceName(resourceName);
        row.setType(type);
        row.setStartAt(startAt);
        row.setEndAt(endAt);
        row.setNote(note);
        return from(row);
    }
}
