package org.campus.campussharebackend.schedule.entity;

import java.time.LocalDateTime;

/**
 * 管理视图中的一行时段安排：OPEN 来自 resource_opening，TEACHING 来自 resource_block。
 */
public class ScheduleRow {

    private long id;
    private long resourceId;
    private String type;
    private LocalDateTime startAt;
    private LocalDateTime endAt;
    private String note;
    private String resourceName;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getResourceId() {
        return resourceId;
    }

    public void setResourceId(long resourceId) {
        this.resourceId = resourceId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startAt = startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public void setEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }
}
