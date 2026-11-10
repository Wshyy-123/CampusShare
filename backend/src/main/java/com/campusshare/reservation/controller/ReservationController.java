package com.campusshare.reservation.controller;

import com.campusshare.common.ApiResponse;
import com.campusshare.common.BusinessException;
import com.campusshare.common.PageResponse;
import com.campusshare.reservation.dto.CreateReservationRequest;
import com.campusshare.reservation.service.ReservationService;
import com.campusshare.reservation.vo.ReservationVO;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ReservationVO>> create(
            @Valid @RequestBody CreateReservationRequest request) {
        ReservationService.CreateResult result = reservationService.create(request);
        return ResponseEntity.status(result.created() ? 201 : 200)
                .body(ApiResponse.ok(result.created() ? "预约成功" : "预约已存在", result.reservation()));
    }

    @GetMapping("/mine")
    public ApiResponse<PageResponse<ReservationVO>> mine(
            @RequestParam(required = false) String status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok("OK", reservationService.mine(status, from, to, page, size));
    }

    @GetMapping("/{reservationId}")
    public ApiResponse<ReservationVO> detail(@PathVariable String reservationId) {
        long id;
        try {
            id = Long.parseLong(reservationId);
        } catch (NumberFormatException exception) {
            throw new BusinessException(404, "RESERVATION_NOT_FOUND", "预约不存在或无权查看");
        }
        return ApiResponse.ok("OK", reservationService.detail(id));
    }
}
