package com.campusshare.reservation.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "reservation",
        indexes = @Index(
                name = "idx_reservation_unit_time",
                columnList = "resource_unit_id,start_at,end_at,status"))
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resource_unit_id", nullable = false)
    private Long resourceUnitId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ReservationStatus status;

    protected Reservation() {
    }

    public Long getId() {
        return id;
    }

    public Long getResourceUnitId() {
        return resourceUnitId;
    }

    public Long getUserId() {
        return userId;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public ReservationStatus getStatus() {
        return status;
    }
}
