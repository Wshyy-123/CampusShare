package com.campusshare.schedule.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "schedule_period",
        indexes = {
            @Index(
                    name = "idx_schedule_unit_time",
                    columnList = "resource_unit_id,start_at,end_at"),
            @Index(
                    name = "idx_schedule_unit_type_time",
                    columnList = "resource_unit_id,period_type,start_at,end_at")
        })
public class SchedulePeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resource_unit_id", nullable = false)
    private Long resourceUnitId;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", nullable = false, length = 20)
    private SchedulePeriodType periodType;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(length = 200)
    private String note;

    protected SchedulePeriod() {
    }

    public SchedulePeriod(
            Long resourceUnitId,
            SchedulePeriodType periodType,
            LocalDateTime startAt,
            LocalDateTime endAt,
            String note) {
        this.resourceUnitId = resourceUnitId;
        this.periodType = periodType;
        this.startAt = startAt;
        this.endAt = endAt;
        this.note = note;
    }

    public void update(
            SchedulePeriodType periodType,
            LocalDateTime startAt,
            LocalDateTime endAt,
            String note) {
        this.periodType = periodType;
        this.startAt = startAt;
        this.endAt = endAt;
        this.note = note;
    }

    public Long getId() {
        return id;
    }

    public Long getResourceUnitId() {
        return resourceUnitId;
    }

    public SchedulePeriodType getPeriodType() {
        return periodType;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public String getNote() {
        return note;
    }
}
