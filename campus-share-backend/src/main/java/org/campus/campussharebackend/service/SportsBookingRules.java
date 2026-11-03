package org.campus.campussharebackend.service;

import org.campus.campussharebackend.entity.Reservation;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 体育预约的纯规则判断。不依赖数据库与 Spring，便于对边界时刻做单元测试。
 *
 * <p>口径来源见 docs/api/sports-attendance.yaml；其中标为"待 PO 确认"的窗口由配置调整。</p>
 */
public final class SportsBookingRules {

    public static final String STATUS_RESERVED = "RESERVED";
    public static final String STATUS_CHECKED_IN = "CHECKED_IN";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    public static final String REASON_USER_CANCEL = "USER_CANCEL";
    public static final String REASON_NO_SHOW = "NO_SHOW";

    private static final Set<String> SPORTS_RESOURCE_TYPES = Set.of("BADMINTON", "TABLE_TENNIS", "TENNIS");

    private SportsBookingRules() {
    }

    public static boolean isSportsResource(String resourceType) {
        return resourceType != null && SPORTS_RESOURCE_TYPES.contains(resourceType);
    }

    /**
     * 使用前取消：仅状态为 RESERVED 且当前时间早于开始时间。
     */
    public static boolean canCancel(Reservation reservation, LocalDateTime now) {
        return STATUS_RESERVED.equals(reservation.getStatus())
                && reservation.getStartAt() != null
                && now.isBefore(reservation.getStartAt());
    }

    /**
     * 可签到窗口：{@code [start - earlyMinutes, start + graceMinutes)}。
     */
    public static boolean inCheckInWindow(Reservation reservation, LocalDateTime now,
                                          int earlyMinutes, int graceMinutes) {
        LocalDateTime start = reservation.getStartAt();
        if (start == null) {
            return false;
        }
        return !now.isBefore(start.minusMinutes(earlyMinutes))
                && now.isBefore(start.plusMinutes(graceMinutes));
    }

    /**
     * 已过签到宽限：{@code now >= start + graceMinutes}。
     */
    public static boolean isNoShow(Reservation reservation, LocalDateTime now, int graceMinutes) {
        return reservation.getStartAt() != null
                && !now.isBefore(reservation.getStartAt().plusMinutes(graceMinutes));
    }
}
