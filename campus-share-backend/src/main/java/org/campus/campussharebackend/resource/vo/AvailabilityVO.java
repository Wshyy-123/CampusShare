package org.campus.campussharebackend.resource.vo;

import java.util.List;

public record AvailabilityVO(ResourceVO resource, String date,
                             List<IntervalVO> openings, List<BlockVO> blocks,
                             List<ReservationSlotVO> reservations, List<IntervalVO> availablePeriods) {
}
