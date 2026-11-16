package org.campus.campussharebackend.reservation.controller;

import java.time.Clock;
import java.util.Set;
import org.campus.campussharebackend.auth.CurrentUserProvider;
import org.campus.campussharebackend.common.ApiResponse;
import org.campus.campussharebackend.common.BusinessException;
import org.campus.campussharebackend.common.TimeFormats;
import org.campus.campussharebackend.mapper.ReservationMapper;
import org.campus.campussharebackend.resource.service.ResourceService;
import org.campus.campussharebackend.resource.vo.ResourceVO;
import org.campus.campussharebackend.reservation.service.ReservationService;
import org.campus.campussharebackend.reservation.vo.ReservationVO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/places")
public class PlaceController {
    private final ResourceService resources;
    private final ReservationService reservations;
    private final ReservationMapper mapper;
    private final CurrentUserProvider users;
    private final Clock clock;

    public PlaceController(ResourceService resources, ReservationService reservations, ReservationMapper mapper,
                           CurrentUserProvider users, @Qualifier("lifecycleClock") Clock clock) {
        this.resources = resources;
        this.reservations = reservations;
        this.mapper = mapper;
        this.users = users;
        this.clock = clock;
    }

    public record PlaceResult(ResourceVO place, ReservationVO reservation, String serverTime) {}

    @GetMapping("/{resourceId}")
    public ApiResponse<PlaceResult> get(@PathVariable long resourceId,
                                        @RequestParam(required = false) Long reservationId) {
        var user = users.require();
        var place = resources.get(resourceId);
        if (!Set.of("CLASSROOM", "BADMINTON", "TABLE_TENNIS", "TENNIS").contains(place.resourceType())) {
            throw new BusinessException(409, "INVALID_STATE", "该地点不支持扫码签到");
        }
        var now = TimeFormats.now(clock);
        Long id = reservationId == null ? mapper.findForPlace(user.userId(), resourceId, now) : reservationId;
        var booking = id == null ? null : reservations.detail(id);
        if (booking != null && !booking.resourceId().equals(Long.toString(resourceId))) {
            throw new BusinessException(409, "WRONG_PLACE", "预约与扫描地点不一致");
        }
        return ApiResponse.ok("OK", new PlaceResult(place, booking, TimeFormats.format(now)));
    }
}
