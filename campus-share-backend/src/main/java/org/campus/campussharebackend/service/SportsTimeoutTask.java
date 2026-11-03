package org.campus.campussharebackend.service;

import org.campus.campussharebackend.entity.Reservation;
import org.campus.campussharebackend.entity.ViolationRecord;
import org.campus.campussharebackend.mapper.ReservationMapper;
import org.campus.campussharebackend.mapper.ViolationRecordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SportsTimeoutTask {

    @Autowired
    private ReservationMapper reservationMapper;

    @Autowired
    private ViolationRecordMapper violationRecordMapper;

    // 每 10 秒执行一次
    @Scheduled(cron = "0/10 * * * * ?")
    public void checkTimeoutReservations() {
        System.out.println("⏰ 定时任务启动：正在扫描超时未签到的体育预约...");

        List<Reservation> timeoutList = reservationMapper.findTimeoutReservations();

        if (timeoutList != null && !timeoutList.isEmpty()) {
            for (Reservation r : timeoutList) {
                // 1. 自动取消预约
                reservationMapper.cancelReservationWithReason(r.getId(), "NO_SHOW");

                // 2. 记录违规
                ViolationRecord record = new ViolationRecord();
                record.setUserId(r.getUserId());
                record.setReservationId(r.getId());
                record.setViolationType("SPORTS_NO_SHOW");
                violationRecordMapper.insertViolation(record);

                System.out.println("❌ 已处理超时违规！预约ID: " + r.getId());
            }
        }
    }
}