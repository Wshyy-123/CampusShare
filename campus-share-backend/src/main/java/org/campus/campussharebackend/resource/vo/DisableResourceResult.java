package org.campus.campussharebackend.resource.vo;

import java.util.List;

public record DisableResourceResult(String resourceId, List<String> cancelledReservationIds) {
}
