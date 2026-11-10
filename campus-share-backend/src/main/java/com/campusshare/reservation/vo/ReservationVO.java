package com.campusshare.reservation.vo;

public record ReservationVO(
        String reservationId,
        String resourceId,
        String resourceName,
        String resourceType,
        String location,
        String unitLabel,
        String startTime,
        String endTime,
        String status,
        String acceptedAt,
        String arrivalDeadline,
        String checkInAt,
        String checkOutAt,
        String cancelReason,
        String createdAt) {
}
