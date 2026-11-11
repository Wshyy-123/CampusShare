package org.campus.campussharebackend.controller;

import org.campus.campussharebackend.service.CheckInService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/library")
@ConditionalOnProperty(name = "campusshare.legacy-checkin.enabled", havingValue = "true", matchIfMissing = false)
public class CheckInController {

    @Autowired
    private CheckInService checkInService;

    // 模拟图书馆刷卡机发来的请求
    @PostMapping("/check-in")
    public String checkIn(@RequestParam Long userId) {
        return checkInService.swipeCard(userId);
    }

    // 模拟用户主动取消预约
    @PostMapping("/cancel")
    public String cancel(@RequestParam Long id) {
        return checkInService.cancelReservation(id);
    }
}