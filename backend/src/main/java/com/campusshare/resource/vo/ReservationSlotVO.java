package com.campusshare.resource.vo;

import com.campusshare.common.TimeFormats;
import com.campusshare.resource.entity.ReservationSlot;

public record ReservationSlotVO(String reservationId, String startTime, String endTime, String status) {

    public static ReservationSlotVO from(ReservationSlot slot) {
        return new ReservationSlotVO(
                Long.toString(slot.getId()),
                TimeFormats.format(slot.getStartAt()),
                TimeFormats.format(slot.getEndAt()),
                slot.getStatus());
    }
}
