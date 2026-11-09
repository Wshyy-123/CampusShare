package com.campusshare.block.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "resource_block",
        indexes = @Index(
                name = "idx_block_unit_time",
                columnList = "resource_unit_id,start_at,end_at,active"))
public class ResourceBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resource_unit_id", nullable = false)
    private Long resourceUnitId;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 200)
    private String reason;

    protected ResourceBlock() {
    }

    public Long getId() {
        return id;
    }

    public Long getResourceUnitId() {
        return resourceUnitId;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public boolean isActive() {
        return active;
    }

    public String getReason() {
        return reason;
    }
}
