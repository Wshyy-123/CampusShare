package org.campus.campussharebackend.service;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.campus.campussharebackend.entity.Reservation;
import org.campus.campussharebackend.resource.entity.ResourceRecord;
import org.campus.campussharebackend.mapper.ReservationMapper;
import org.campus.campussharebackend.resource.mapper.ResourceMapper;
import org.campus.campussharebackend.reservation.mapper.ReservationRepository;
import org.campus.campussharebackend.config.SportsProperties;
import org.campus.campussharebackend.common.BusinessException;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.assertj.core.api.Assertions.*;
class SportsReviewRegressionTest {
 private final LocalDateTime now=LocalDateTime.of(2026,11,16,10,2);
 private final ReservationMapper reservations=mock(ReservationMapper.class);
 private final ResourceMapper resources=mock(ResourceMapper.class);
 private final ReservationRepository history=mock(ReservationRepository.class);
 private final Clock clock=Clock.fixed(now.atZone(ZoneId.of("Asia/Shanghai")).toInstant(),ZoneId.of("Asia/Shanghai"));
 private final SportsReservationService service=new SportsReservationService(reservations,resources,history,clock,new SportsProperties(),mock(org.campus.campussharebackend.lifecycle.LifecycleMapper.class));
 @Test void earlyCheckOutMustNotReleaseUnfinishedSportsSlot() {
  stub(booking("CHECKED_IN",now.minusMinutes(2),now.plusMinutes(58)));
  when(reservations.checkOutIfCheckedIn(eq(100L),any())).thenReturn(1);
  assertThatThrownBy(()->service.checkOut(1,100,201)).isInstanceOf(BusinessException.class);
  verify(reservations,never()).checkOutIfCheckedIn(any(),any());
 }
 @Test void endedShortBookingMustNotAcceptLateCheckInInsideFifteenMinuteGrace() {
  stub(booking("RESERVED",now.minusMinutes(12),now.minusMinutes(2)));
  when(reservations.checkInIfReserved(eq(100L),any())).thenReturn(1);
  assertThatThrownBy(()->service.checkIn(1,100,201)).isInstanceOf(BusinessException.class);
  verify(reservations,never()).checkInIfReserved(any(),any());
 }
 private Reservation booking(String status,LocalDateTime start,LocalDateTime end) {
  var v=new Reservation();v.setId(100L);v.setUserId(1L);v.setResourceId(201L);v.setStatus(status);v.setStartAt(start);v.setEndAt(end);return v;
 }
 private void stub(Reservation booking) {
  var resource=new ResourceRecord();resource.setId(201L);resource.setResourceType("BADMINTON");resource.setEnabled(true);
  when(reservations.findById(100L)).thenReturn(booking);when(reservations.findByIdForUpdate(100L)).thenReturn(booking);when(resources.lockById(201L)).thenReturn(resource);
 }
}
