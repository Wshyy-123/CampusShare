package org.campus.campussharebackend.controller;

import org.campus.campussharebackend.common.ApiException;
import org.campus.campussharebackend.common.ApiResponse;
import org.campus.campussharebackend.common.ErrorCode;
import org.campus.campussharebackend.dto.ReservationDetailResponse;
import org.campus.campussharebackend.security.CurrentUserProvider;
import org.campus.campussharebackend.service.SportsReservationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 体育预约取消、签到、签退与详情查询（XIA-7 / XIA-8）。
 * 契约见 docs/api/sports-attendance.yaml。
 */
@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {

    private final SportsReservationService sportsReservationService;
    private final CurrentUserProvider currentUserProvider;

    public ReservationController(SportsReservationService sportsReservationService,
                                 CurrentUserProvider currentUserProvider) {
        this.sportsReservationService = sportsReservationService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/{reservationId}")
    public ApiResponse<ReservationDetailResponse> detail(@PathVariable String reservationId) {
        long userId = currentUserProvider.requireUserId();
        return ApiResponse.ok("查询成功", sportsReservationService.get(userId, parseReservationId(reservationId)));
    }

    @PostMapping("/{reservationId}/cancel")
    public ApiResponse<ReservationDetailResponse> cancel(@PathVariable String reservationId) {
        long userId = currentUserProvider.requireUserId();
        return ApiResponse.ok("预约已取消", sportsReservationService.cancel(userId, parseReservationId(reservationId)));
    }

    @PostMapping("/{reservationId}/check-in")
    public ApiResponse<ReservationDetailResponse> checkIn(@PathVariable String reservationId) {
        long userId = currentUserProvider.requireUserId();
        return ApiResponse.ok("签到成功", sportsReservationService.checkIn(userId, parseReservationId(reservationId)));
    }

    @PostMapping("/{reservationId}/check-out")
    public ApiResponse<ReservationDetailResponse> checkOut(@PathVariable String reservationId) {
        long userId = currentUserProvider.requireUserId();
        return ApiResponse.ok("签退成功", sportsReservationService.checkOut(userId, parseReservationId(reservationId)));
    }

    private static long parseReservationId(String raw) {
        try {
            long reservationId = Long.parseLong(raw);
            if (reservationId <= 0) {
                throw new NumberFormatException("预约ID必须为正整数");
            }
            return reservationId;
        } catch (NumberFormatException exception) {
            throw new ApiException(ErrorCode.INVALID_ARGUMENT, "预约ID必须是正整数");
        }
    }
}
