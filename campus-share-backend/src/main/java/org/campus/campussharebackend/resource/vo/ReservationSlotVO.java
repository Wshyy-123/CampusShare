package org.campus.campussharebackend.resource.vo;

import org.campus.campussharebackend.common.TimeFormats;
import org.campus.campussharebackend.resource.entity.ReservationSlot;

public record ReservationSlotVO(String reservationId, String startTime, String endTime, String status) {

    public static ReservationSlotVO from(ReservationSlot slot) {
        return new ReservationSlotVO(
                Long.toString(slot.getId()),
                TimeFormats.format(slot.getStartAt()),
                TimeFormats.format(slot.getEndAt()),
                slot.getStatus());
    }
}
