package org.campus.campussharebackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 体育预约规则的时间窗口。见 docs/api/sports-attendance.yaml 中的"待 PO 确认"口径。
 */
@Component
@ConfigurationProperties(prefix = "app.sports")
public class SportsProperties {

    /** 允许提前签到的分钟数，默认 0（从预约开始时间起）。 */
    private int checkInEarlyMinutes = 0;

    /** 允许签到的最晚分钟数，默认 15（预约开始后 15 分钟）。 */
    private int checkInGraceMinutes = 15;

    public int getCheckInEarlyMinutes() {
        return checkInEarlyMinutes;
    }

    public void setCheckInEarlyMinutes(int checkInEarlyMinutes) {
        this.checkInEarlyMinutes = checkInEarlyMinutes;
    }

    public int getCheckInGraceMinutes() {
        return checkInGraceMinutes;
    }

    public void setCheckInGraceMinutes(int checkInGraceMinutes) {
        this.checkInGraceMinutes = checkInGraceMinutes;
    }
}
