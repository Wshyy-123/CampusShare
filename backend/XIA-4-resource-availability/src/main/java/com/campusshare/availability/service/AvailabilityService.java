package com.campusshare.availability.service;

import com.campusshare.availability.dto.ResourceAvailabilityResponse;
import com.campusshare.availability.dto.TimeRangeResponse;
import com.campusshare.availability.dto.UnitAvailabilityResponse;
import com.campusshare.block.domain.ResourceBlock;
import com.campusshare.block.repository.ResourceBlockRepository;
import com.campusshare.common.BusinessException;
import com.campusshare.reservation.domain.Reservation;
import com.campusshare.reservation.domain.ReservationStatus;
import com.campusshare.reservation.repository.ReservationRepository;
import com.campusshare.resource.domain.CampusResource;
import com.campusshare.resource.domain.ResourceStatus;
import com.campusshare.resource.domain.ResourceUnit;
import com.campusshare.resource.repository.CampusResourceRepository;
import com.campusshare.resource.repository.ResourceUnitRepository;
import com.campusshare.schedule.domain.SchedulePeriod;
import com.campusshare.schedule.domain.SchedulePeriodType;
import com.campusshare.schedule.repository.SchedulePeriodRepository;
import java.time.*;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AvailabilityService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");
    private static final EnumSet<ReservationStatus> BLOCKING_RESERVATION_STATUSES =
            EnumSet.of(ReservationStatus.RESERVED, ReservationStatus.CHECKED_IN);

    private final CampusResourceRepository resourceRepository;
    private final ResourceUnitRepository unitRepository;
    private final SchedulePeriodRepository scheduleRepository;
    private final ReservationRepository reservationRepository;
    private final ResourceBlockRepository blockRepository;
    private final AvailabilityCalculator calculator = new AvailabilityCalculator();

    public AvailabilityService(
            CampusResourceRepository resourceRepository,
            ResourceUnitRepository unitRepository,
            SchedulePeriodRepository scheduleRepository,
            ReservationRepository reservationRepository,
            ResourceBlockRepository blockRepository) {
        this.resourceRepository = resourceRepository;
        this.unitRepository = unitRepository;
        this.scheduleRepository = scheduleRepository;
        this.reservationRepository = reservationRepository;
        this.blockRepository = blockRepository;
    }

    public ResourceAvailabilityResponse getResourceAvailability(
            Long resourceId,
            LocalDate date) {

        CampusResource resource = findResource(resourceId);

        List<UnitAvailabilityResponse> units = unitRepository
                .findByResourceIdOrderById(resourceId)
                .stream()
                .map(unit -> buildUnitAvailability(resource, unit, date))
                .toList();

        return new ResourceAvailabilityResponse(
                resource.getId(),
                resource.getName(),
                resource.getType(),
                resource.getStatus(),
                date,
                units);
    }

    public UnitAvailabilityResponse getUnitAvailability(
            Long unitId,
            LocalDate date) {

        ResourceUnit unit = unitRepository.findById(unitId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "RESOURCE_UNIT_NOT_FOUND",
                        "预约单位不存在"));

        CampusResource resource = findResource(unit.getResourceId());
        return buildUnitAvailability(resource, unit, date);
    }

    private UnitAvailabilityResponse buildUnitAvailability(
            CampusResource resource,
            ResourceUnit unit,
            LocalDate date) {

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

        List<TimeInterval> open = scheduleRepository
                .findOverlappingDayByType(
                        unit.getId(),
                        SchedulePeriodType.OPEN,
                        dayStart,
                        dayEnd)
                .stream()
                .map(this::intervalOfSchedule)
                .map(interval -> interval.clip(dayStart, dayEnd))
                .filter(interval -> interval != null)
                .toList();

        List<TimeInterval> teaching = scheduleRepository
                .findOverlappingDayByType(
                        unit.getId(),
                        SchedulePeriodType.TEACHING,
                        dayStart,
                        dayEnd)
                .stream()
                .map(this::intervalOfSchedule)
                .map(interval -> interval.clip(dayStart, dayEnd))
                .filter(interval -> interval != null)
                .toList();

        List<TimeInterval> reservations = reservationRepository
                .findBlockingReservations(
                        unit.getId(),
                        BLOCKING_RESERVATION_STATUSES,
                        dayStart,
                        dayEnd)
                .stream()
                .map(this::intervalOfReservation)
                .map(interval -> interval.clip(dayStart, dayEnd))
                .filter(interval -> interval != null)
                .toList();

        List<TimeInterval> blocked = blockRepository
                .findActiveBlocks(unit.getId(), dayStart, dayEnd)
                .stream()
                .map(this::intervalOfBlock)
                .map(interval -> interval.clip(dayStart, dayEnd))
                .filter(interval -> interval != null)
                .toList();

        boolean globallyDisabled =
                resource.getStatus() == ResourceStatus.DISABLED
                        || unit.getStatus() == ResourceStatus.DISABLED;

        List<TimeInterval> available;
        String message;

        if (globallyDisabled) {
            available = List.of();
            message = "资源已停用，当前不可预约";
        } else {
            List<TimeInterval> busy = new ArrayList<>();
            busy.addAll(teaching);
            busy.addAll(reservations);
            busy.addAll(blocked);

            available = calculator.subtract(open, busy);
            message = available.isEmpty() ? "暂无可预约时段" : "存在可预约时段";
        }

        return new UnitAvailabilityResponse(
                resource.getId(),
                resource.getName(),
                resource.getType(),
                resource.getStatus(),
                unit.getId(),
                unit.getName(),
                unit.getUnitCode(),
                unit.getStatus(),
                date,
                toResponse(calculator.merge(open)),
                toResponse(calculator.merge(teaching)),
                toResponse(calculator.merge(reservations)),
                toResponse(calculator.merge(blocked)),
                toResponse(available),
                !available.isEmpty(),
                message);
    }

    private CampusResource findResource(Long resourceId) {
        return resourceRepository.findById(resourceId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "RESOURCE_NOT_FOUND",
                        "资源不存在"));
    }

    private TimeInterval intervalOfSchedule(SchedulePeriod period) {
        return new TimeInterval(period.getStartAt(), period.getEndAt());
    }

    private TimeInterval intervalOfReservation(Reservation reservation) {
        return new TimeInterval(reservation.getStartAt(), reservation.getEndAt());
    }

    private TimeInterval intervalOfBlock(ResourceBlock block) {
        return new TimeInterval(block.getStartAt(), block.getEndAt());
    }

    private List<TimeRangeResponse> toResponse(List<TimeInterval> intervals) {
        return intervals.stream()
                .map(interval -> new TimeRangeResponse(
                        toOffset(interval.startAt()),
                        toOffset(interval.endAt())))
                .toList();
    }

    private OffsetDateTime toOffset(LocalDateTime value) {
        return value.atZone(BUSINESS_ZONE).toOffsetDateTime();
    }
}
