package org.campus.campussharebackend.resource.vo;

import org.campus.campussharebackend.common.TimeFormats;
import org.campus.campussharebackend.resource.entity.OpeningRecord;

public record IntervalVO(String startTime, String endTime) {

    public static IntervalVO from(OpeningRecord record) {
        return new IntervalVO(TimeFormats.format(record.getStartAt()), TimeFormats.format(record.getEndAt()));
    }
}
