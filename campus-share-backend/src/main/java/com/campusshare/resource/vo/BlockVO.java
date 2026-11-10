package com.campusshare.resource.vo;

import com.campusshare.common.TimeFormats;
import com.campusshare.resource.entity.BlockRecord;

public record BlockVO(String startTime, String endTime, String blockType, String reason) {

    public static BlockVO from(BlockRecord record) {
        return new BlockVO(
                TimeFormats.format(record.getStartAt()),
                TimeFormats.format(record.getEndAt()),
                record.getBlockType(),
                record.getReason());
    }
}
