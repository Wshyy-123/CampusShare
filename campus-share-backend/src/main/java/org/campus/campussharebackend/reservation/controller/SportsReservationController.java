package org.campus.campussharebackend.reservation.controller;

import org.campus.campussharebackend.auth.CurrentUserProvider;
import org.campus.campussharebackend.common.ApiResponse;
import org.campus.campussharebackend.common.BusinessException;
import org.campus.campussharebackend.reservation.service.ReservationService;
import org.campus.campussharebackend.reservation.vo.ReservationVO;
import org.campus.campussharebackend.service.SportsReservationService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 体育预约取消、扫码签到、扫码签退（XIA-7 / XIA-8）。
 * 契约见 docs/api/sports-attendance.yaml；身份取服务端会话，响应复用 ReservationVO。
 */
@RestController
@RequestMapping("/api/v1/reservations")
public class SportsReservationController {

    private final SportsReservationService sportsReservationService;
    private final ReservationService reservationService;
    private final CurrentUserProvider currentUserProvider;

    public SportsReservationController(SportsReservationService sportsReservationService,
                                       ReservationService reservationService,
                                       CurrentUserProvider currentUserProvider) {
        this.sportsReservationService = sportsReservationService;
        this.reservationService = reservationService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/{reservationId}/cancel")
    public ApiResponse<ReservationVO> cancel(@PathVariable String reservationId) {
        long id = parseReservationId(reservationId);
        sportsReservationService.cancel(currentUserProvider.require().userId(), id);
        return ApiResponse.ok("预约已取消", reservationService.detail(id));
    }

    @PostMapping("/{reservationId}/check-in")
    public ApiResponse<ReservationVO> checkIn(@PathVariable String reservationId) {
        long id = parseReservationId(reservationId);
        sportsReservationService.checkIn(currentUserProvider.require().userId(), id);
        return ApiResponse.ok("签到成功", reservationService.detail(id));
    }

    @PostMapping("/{reservationId}/check-out")
    public ApiResponse<ReservationVO> checkOut(@PathVariable String reservationId) {
        long id = parseReservationId(reservationId);
        sportsReservationService.checkOut(currentUserProvider.require().userId(), id);
        return ApiResponse.ok("签退成功", reservationService.detail(id));
    }

    private static long parseReservationId(String raw) {
        try {
            long id = Long.parseLong(raw);
            if (id <= 0) {
                throw new NumberFormatException("预约ID必须为正整数");
            }
            return id;
        } catch (NumberFormatException exception) {
            throw new BusinessException(404, "RESERVATION_NOT_FOUND", "预约不存在或无权操作");
        }
    }
}
