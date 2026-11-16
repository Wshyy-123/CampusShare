package org.campus.campussharebackend.controller;

import jakarta.validation.Valid;
import org.campus.campussharebackend.auth.CurrentUserProvider;
import org.campus.campussharebackend.common.ApiResponse;
import org.campus.campussharebackend.common.BusinessException;
import org.campus.campussharebackend.reservation.dto.CardSwipeRequest;
import org.campus.campussharebackend.reservation.service.ReservationService;
import org.campus.campussharebackend.reservation.vo.ReservationVO;
import org.campus.campussharebackend.service.CheckInService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/library/reservations")
public class CheckInController {
    private final CheckInService checkIn;
    private final ReservationService reservations;
    private final CurrentUserProvider users;

    public CheckInController(CheckInService checkIn, ReservationService reservations, CurrentUserProvider users) {
        this.checkIn = checkIn;
        this.reservations = reservations;
        this.users = users;
    }

    @PostMapping("/{reservationId}/check-in")
    public ApiResponse<ReservationVO> enter(@PathVariable String reservationId, @Valid @RequestBody CardSwipeRequest swipe) {
        long id = parseId(reservationId);
        checkIn.enter(users.require().userId(), id, parseId(swipe.resourceId()));
        return ApiResponse.ok("刷卡入场成功", reservations.detail(id));
    }

    @PostMapping("/{reservationId}/check-out")
    public ApiResponse<ReservationVO> leave(@PathVariable String reservationId, @Valid @RequestBody CardSwipeRequest swipe) {
        long id = parseId(reservationId);
        checkIn.leave(users.require().userId(), id, parseId(swipe.resourceId()));
        return ApiResponse.ok("刷卡离场成功，座位已释放", reservations.detail(id));
    }

    private static long parseId(String raw) {
        try {
            long id = Long.parseLong(raw);
            if (id > 0) return id;
        } catch (NumberFormatException ignored) { }
        throw new BusinessException(400, "INVALID_ARGUMENT", "预约或座位编号不合法");
    }
}
