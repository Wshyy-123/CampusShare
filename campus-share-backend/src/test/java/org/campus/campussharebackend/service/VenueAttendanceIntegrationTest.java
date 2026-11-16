package org.campus.campussharebackend.service;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.campus.campussharebackend.support.IntegrationTestBase;
import org.campus.campussharebackend.job.VenueLifecycleJob;
import org.campus.campussharebackend.mapper.ReservationMapper;
import org.campus.campussharebackend.reservation.mapper.ReservationRepository;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WithMockUser(username="student01")
@Import(VenueAttendanceIntegrationTest.TimeConfig.class)
class VenueAttendanceIntegrationTest extends IntegrationTestBase {
 static final LocalDateTime NOW=LocalDateTime.of(2026,11,16,10,0);
 @TestConfiguration static class TimeConfig {
  @Bean @Primary @Qualifier("lifecycleClock") Clock venueTestClock(){return Clock.fixed(NOW.atZone(ZONE).toInstant(),ZONE);}
 }
 @Autowired SportsNoShowService lifecycle;
 @Autowired ReservationMapper mapper;
 @Autowired @Qualifier("lifecycleClock") Clock clock;
 @MockitoSpyBean ReservationRepository history;
 @AfterEach void restoreType(){jdbcTemplate.update("UPDATE resource SET resource_type='TENNIS' WHERE id=401");}
 private long booking(long resource,String status,int startMinutes,int endMinutes){return insertReservation(1,resource,UUID.randomUUID().toString(),NOW.plusMinutes(startMinutes),NOW.plusMinutes(endMinutes),status,null,status.equals("CHECKED_IN")?NOW.minusMinutes(1):null);}
 private String scan(long resource){return "{\"resourceId\":\""+resource+"\",\"source\":\"QR\"}";}
 private org.springframework.test.web.servlet.ResultActions action(long id,String name,long resource)throws Exception{return mockMvc.perform(post("/api/v1/reservations/"+id+"/"+name).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(scan(resource)));}
 private int count(String table){return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM "+table,Integer.class);}
 private String state(long id){return jdbcTemplate.queryForObject("SELECT status FROM reservation WHERE id=?",String.class,id);}
 @Test void placeGetIsReadOnlyAndOnlyReturnsOwnerBooking()throws Exception{
  long id=booking(201,"RESERVED",-1,59);
  mockMvc.perform(get("/api/v1/places/201")).andExpect(status().isOk()).andExpect(jsonPath("$.data.reservation.reservationId").value(""+id));
  mockMvc.perform(get("/api/v1/places/201").with(user("student02"))).andExpect(status().isOk()).andExpect(jsonPath("$.data.reservation").isEmpty());
  assertThat(state(id)).isEqualTo("RESERVED");assertThat(count("reservation_event")).isZero();
 }
 @Test void wrongPlaceCannotSignInOrLeakOwnerBooking()throws Exception{
  long id=booking(201,"RESERVED",-1,59);
  action(id,"check-in",401).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("WRONG_PLACE"));
  mockMvc.perform(get("/api/v1/places/401").param("reservationId",""+id)).andExpect(status().isConflict());
  mockMvc.perform(get("/api/v1/places/201").param("reservationId",""+id).with(user("student02"))).andExpect(status().isNotFound());
  assertThat(state(id)).isEqualTo("RESERVED");
 }
 @Test void correctScanIsIdempotentAndRecordsQrSource()throws Exception{
  long id=booking(201,"RESERVED",0,60);
  action(id,"check-in",201).andExpect(status().isOk());action(id,"check-in",201).andExpect(status().isOk());
  assertThat(state(id)).isEqualTo("CHECKED_IN");assertThat(count("reservation_event")).isEqualTo(1);
  assertThat(jdbcTemplate.queryForObject("SELECT reason FROM reservation_event",String.class)).isEqualTo("QR_CHECK_IN:201");
  assertThat(jdbcTemplate.queryForObject("SELECT operator_id FROM reservation_event",Long.class)).isEqualTo(1);
 }
 @Test void checkInRejectsWrongSourceAndMissingCsrf()throws Exception{
  long id=booking(201,"RESERVED",0,60);
  mockMvc.perform(post("/api/v1/reservations/"+id+"/check-in").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"resourceId\":\"201\",\"source\":\"BUTTON\"}")).andExpect(status().isBadRequest());
  mockMvc.perform(post("/api/v1/reservations/"+id+"/check-in").contentType(MediaType.APPLICATION_JSON).content(scan(201))).andExpect(status().isForbidden());
  assertThat(state(id)).isEqualTo("RESERVED");
 }
 @Test void disabledAndBlockedPlaceCannotBeCheckedIn()throws Exception{
  long id=booking(201,"RESERVED",0,60);jdbcTemplate.update("UPDATE resource SET enabled=false WHERE id=201");
  action(id,"check-in",201).andExpect(status().isConflict());jdbcTemplate.update("UPDATE resource SET enabled=true WHERE id=201");
  seedTeachingBlock(201,NOW,NOW.plusHours(1));action(id,"check-in",201).andExpect(status().isConflict());assertThat(state(id)).isEqualTo("RESERVED");
 }
 @Test void classroomScanWorksAndRepeatedScanDoesNotSignOut()throws Exception{
  jdbcTemplate.update("UPDATE resource SET resource_type='CLASSROOM' WHERE id=401");long id=booking(401,"RESERVED",0,60);
  action(id,"check-in",401).andExpect(status().isOk());action(id,"check-in",401).andExpect(status().isOk());
  action(id,"check-out",401).andExpect(status().isConflict());assertThat(state(id)).isEqualTo("CHECKED_IN");assertThat(count("reservation_event")).isEqualTo(1);
 }
 @Test void checkedInSportsCompletesAtEndWithoutInventingSignOut(){
  long id=booking(201,"CHECKED_IN",-60,0);assertThat(lifecycle.complete(id)).isTrue();assertThat(lifecycle.complete(id)).isFalse();
  assertThat(state(id)).isEqualTo("COMPLETED");assertThat(count("reservation_event")).isEqualTo(1);assertThat(count("violation_record")).isZero();
  assertThat(jdbcTemplate.queryForObject("SELECT check_out_at FROM reservation WHERE id=?",LocalDateTime.class,id)).isNull();
 }
 @Test void classroomCompletesAndNoShowDoesNotPenalizeClassroom(){
  jdbcTemplate.update("UPDATE resource SET resource_type='CLASSROOM' WHERE id=401");
  long used=booking(401,"CHECKED_IN",-60,0);long absent=booking(401,"RESERVED",-15,45);
  assertThat(lifecycle.complete(used)).isTrue();assertThat(lifecycle.handle(absent)).isTrue();assertThat(state(absent)).isEqualTo("CANCELLED");assertThat(count("violation_record")).isZero();
 }
 @Test void sportsNoShowAtExactlyFifteenMinutesPenalizesOnlyOnce(){
  long id=booking(201,"RESERVED",-15,45);assertThat(lifecycle.handle(id)).isTrue();assertThat(lifecycle.handle(id)).isFalse();
  assertThat(count("violation_record")).isEqualTo(1);assertThat(count("reservation_event")).isEqualTo(1);
 }
 @Test void manualSignOutAfterAutomaticEndRecordsTimeWithoutDoubleCompletion()throws Exception{
  long id=booking(201,"CHECKED_IN",-60,0);lifecycle.complete(id);
  action(id,"check-out",201).andExpect(status().isOk());action(id,"check-out",201).andExpect(status().isOk());
  assertThat(count("reservation_event")).isEqualTo(2);
  assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reservation_event WHERE event_type='COMPLETED'",Integer.class)).isEqualTo(1);
  assertThat(jdbcTemplate.queryForObject("SELECT check_out_at FROM reservation WHERE id=?",LocalDateTime.class,id)).isEqualTo(NOW);
 }
 @Test void venueJobNeverCompletesSeatsOrDiscussionRooms(){
  long seat=booking(101,"CHECKED_IN",-60,0);jdbcTemplate.update("UPDATE resource SET resource_type='DISCUSSION_ROOM' WHERE id=401");long room=booking(401,"CHECKED_IN",-60,0);
  new VenueLifecycleJob(mapper,lifecycle,clock).scan();assertThat(state(seat)).isEqualTo("CHECKED_IN");assertThat(state(room)).isEqualTo("CHECKED_IN");
 }
 @Test void paginatedJobCatchesUpMoreThanOneHundredRows(){
  for(int i=0;i<105;i++)booking(201,"RESERVED",-15,45);
  var job=new VenueLifecycleJob(mapper,lifecycle,clock);job.scan();job.scan();
  assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reservation WHERE status='CANCELLED'",Integer.class)).isEqualTo(105);
  assertThat(count("violation_record")).isEqualTo(105);assertThat(count("reservation_event")).isEqualTo(105);
 }
 @Test void eventFailureRollsBackCancellationAndNextScanRetries(){
  long id=booking(201,"RESERVED",-15,45);
  doThrow(new IllegalStateException("audit unavailable")).when(history).insertEvent(eq(id),anyString(),isNull(),any(),anyString(),anyString());
  new VenueLifecycleJob(mapper,lifecycle,clock).scan();assertThat(state(id)).isEqualTo("RESERVED");assertThat(count("violation_record")).isZero();
  reset(history);new VenueLifecycleJob(mapper,lifecycle,clock).scan();assertThat(state(id)).isEqualTo("CANCELLED");assertThat(count("violation_record")).isEqualTo(1);
 }
 @Test void concurrentEndAndSignOutProduceOneCompletion()throws Exception{
  long id=booking(201,"CHECKED_IN",-60,0);var go=new CountDownLatch(1);
  try(var pool=Executors.newFixedThreadPool(2)){
   var auto=pool.submit(()->{go.await();return lifecycle.complete(id);});
   var manual=pool.submit(()->{go.await();return mockMvc.perform(post("/api/v1/reservations/"+id+"/check-out").with(user("student01")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(scan(201))).andReturn().getResponse().getStatus();});
   go.countDown();auto.get(10,TimeUnit.SECONDS);assertThat(manual.get(10,TimeUnit.SECONDS)).isEqualTo(200);
  }
  assertThat(state(id)).isEqualTo("COMPLETED");assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reservation_event WHERE event_type='COMPLETED'",Integer.class)).isEqualTo(1);assertThat(count("violation_record")).isZero();
 }
 @Test void checkInAtDeadlineRacesSafelyWithNoShow()throws Exception{
  long id=booking(201,"RESERVED",-15,45);var go=new CountDownLatch(1);
  try(var pool=Executors.newFixedThreadPool(2)){
   var noShow=pool.submit(()->{go.await();return lifecycle.handle(id);});
   var scan=pool.submit(()->{go.await();return mockMvc.perform(post("/api/v1/reservations/"+id+"/check-in").with(user("student01")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(scan(201))).andReturn().getResponse().getStatus();});
   go.countDown();assertThat(noShow.get(10,TimeUnit.SECONDS)).isTrue();assertThat(scan.get(10,TimeUnit.SECONDS)).isEqualTo(409);
  }
  assertThat(state(id)).isEqualTo("CANCELLED");assertThat(count("violation_record")).isEqualTo(1);
 }
}
