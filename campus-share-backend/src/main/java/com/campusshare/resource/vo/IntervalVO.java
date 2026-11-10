package com.campusshare.resource.vo;

import com.campusshare.common.TimeFormats;
import com.campusshare.resource.entity.OpeningRecord;

public record IntervalVO(String startTime, String endTime) {

    public static IntervalVO from(OpeningRecord record) {
        return new IntervalVO(TimeFormats.format(record.getStartAt()), TimeFormats.format(record.getEndAt()));
    }
}
