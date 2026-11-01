package org.campus.campussharebackend.service;

import org.campus.campussharebackend.entity.Reservation;
import org.campus.campussharebackend.mapper.ReservationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
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

    // 用户尝试主动取消预约
    public String cancelReservation(Long reservationId) {
        // 1. 查询预约记录
        Reservation r = reservationMapper.findById(reservationId);
        if (r == null) {
            return "取消失败：预约不存在";
        }

        // 2. 状态校验：只有 RESERVED（待确认）的才能取消
        if (!"RESERVED".equals(r.getStatus())) {
            return "取消失败：当前状态不可取消";
        }

        // 3. 时间规则校验 (核心)
        // 注意：这里假设实体类里有 startAt，如果你们用 acceptedAt 代替预约时间，请换成 r.getAcceptedAt()
        LocalDate today = LocalDate.now();
        LocalDate startDate = r.getStartAt().toLocalDate();
        LocalDateTime now = LocalDateTime.now();

        // 规则一：当天的预约无法取消
        if (startDate.equals(today)) {
            return "取消失败：当天的预约无法取消";
        }

        // 规则二：预约当天的早上 7:30 之后，无法取消
        LocalDateTime deadline = startDate.atTime(7, 30);
        if (now.isAfter(deadline)) {
            return "取消失败：已过取消截止时间（预约当天早上7:30）";
        }

        // 4. 校验通过，执行取消操作
        reservationMapper.cancelReservationWithReason(r.getId(), "USER_CANCEL");

        return "取消成功";
    }
}