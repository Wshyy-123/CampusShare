package org.campus.campussharebackend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 体育预约规则的时间窗口。按统一业务规则：开始起15分钟，提前签到为0；配置不接受其他业务期限。
 */
@Component
@org.springframework.validation.annotation.Validated
@ConfigurationProperties(prefix = "app.sports")
public class SportsProperties {

    /** 允许提前签到的分钟数，默认 0（从预约开始时间起）。 */
    @jakarta.validation.constraints.Min(0) @jakarta.validation.constraints.Max(0)
    private int checkInEarlyMinutes = 0;

    /** 允许签到的最晚分钟数，默认 15（预约开始后 15 分钟）。 */
    @jakarta.validation.constraints.Min(15) @jakarta.validation.constraints.Max(15)
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
