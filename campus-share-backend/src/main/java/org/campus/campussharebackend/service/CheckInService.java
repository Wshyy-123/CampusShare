package org.campus.campussharebackend.service;

import org.campus.campussharebackend.entity.Reservation;
import org.campus.campussharebackend.mapper.ReservationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class CheckInService {

    @Autowired
    private ReservationMapper reservationMapper;

    public String swipeCard(Long userId) {
        // 1. 找到该用户待确认的预约
        Reservation reservation = reservationMapper.findPendingReservationByUserId(userId);
        if (reservation == null) {
            return "刷卡失败：没有找到待确认的座位预约";
        }

        // 2. 校验时间：预约成功后 30 分钟内
        LocalDateTime acceptedAt = reservation.getAcceptedAt();
        LocalDateTime now = LocalDateTime.now();
        long minutesElapsed = Duration.between(acceptedAt, now).toMinutes();

        if (minutesElapsed > 30) {
            // 验收标准 3：已超时释放的座位，后续刷卡不能恢复
            return "刷卡失败：预约已超时（超时 " + minutesElapsed + " 分钟）";
        }

        // 3. 校验通过，更新状态（到馆确认）
        reservationMapper.updateToCheckedIn(reservation.getId());
        return "刷卡成功：欢迎到馆！";
    }
}