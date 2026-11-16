package org.campus.campussharebackend.common;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public final class TimeFormats {

    public static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private TimeFormats() {
    }

    public static LocalDateTime toLocal(OffsetDateTime value) {
        return value.atZoneSameInstant(BUSINESS_ZONE).toLocalDateTime().truncatedTo(ChronoUnit.MILLIS);
    }

    public static String format(LocalDateTime value) {
        return value == null ? null : value.atZone(BUSINESS_ZONE).format(ISO_OFFSET);
    }

    public static LocalDateTime now(Clock clock) {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
    }
}
