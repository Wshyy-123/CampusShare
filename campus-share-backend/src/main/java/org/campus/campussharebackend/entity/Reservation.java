package org.campus.campussharebackend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Reservation {
    private Long id;
    private Long userId;
    private Long resourceId;
    private LocalDateTime acceptedAt;
    private String status;
    private String cancelReason;
    private LocalDateTime checkInAt;
    private LocalDateTime checkOutAt;
    private LocalDateTime startAt; // 预约开始时间
    private LocalDateTime endAt;   // 预约结束时间
}