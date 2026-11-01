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
    private LocalDateTime checkInAt;
}