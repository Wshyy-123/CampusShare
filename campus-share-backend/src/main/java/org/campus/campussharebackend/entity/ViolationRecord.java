package org.campus.campussharebackend.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ViolationRecord {
    private Long id;
    private Long userId;
    private Long reservationId;
    private String violationType;
    private LocalDateTime createdAt;
}