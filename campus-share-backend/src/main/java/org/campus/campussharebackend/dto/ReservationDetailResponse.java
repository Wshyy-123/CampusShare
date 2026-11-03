package org.campus.campussharebackend.dto;

import org.campus.campussharebackend.entity.Reservation;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 预约详情。JSON 中的 ID 用字符串，时间用带 +08:00 偏移的 ISO 8601。
 */
public record ReservationDetailResponse(
        String reservationId,
        String resourceId,
        String resourceType,
        String status,
        String cancelReason,
        String startTime,
        String endTime,
        String checkInAt,
        String checkOutAt) {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    public static ReservationDetailResponse of(Reservation reservation, String resourceType) {
        return new ReservationDetailResponse(
                String.valueOf(reservation.getId()),
                String.valueOf(reservation.getResourceId()),
                resourceType,
                reservation.getStatus(),
                reservation.getCancelReason(),
                format(reservation.getStartAt()),
                format(reservation.getEndAt()),
                format(reservation.getCheckInAt()),
                format(reservation.getCheckOutAt()));
    }

    private static String format(LocalDateTime time) {
        return time == null ? null : time.atZone(BUSINESS_ZONE).format(FORMATTER);
    }
}
