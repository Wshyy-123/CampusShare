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
import org.springframework.test.web.servlet.ResultActions;
import org.campus.campussharebackend.support.IntegrationTestBase;
import org.campus.campussharebackend.lifecycle.LifecycleMapper;
import org.campus.campussharebackend.reservation.mapper.ReservationRepository;
import org.campus.campussharebackend.reservation.service.SeatNoShowService;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithMockUser(username = "student01")
@Import(LibraryClassroomIntegrationTest.TimeConfig.class)
class LibraryClassroomIntegrationTest extends IntegrationTestBase {
    static final LocalDateTime NOW = LocalDate.now(ZONE).atTime(10, 0);
    @TestConfiguration static class TimeConfig {
        @Bean @Primary @Qualifier("lifecycleClock") Clock libraryClock() {
            return Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE);
        }
    }
    @Autowired CheckInService cards;
    @Autowired SeatNoShowService seats;
    @Autowired SportsNoShowService venues;
    @MockitoSpyBean ReservationRepository history;

    @AfterEach void restoreType() { jdbcTemplate.update("UPDATE resource SET resource_type='TENNIS' WHERE id=401"); }
    private long booking(long owner, long resource, String status, LocalDateTime start, LocalDateTime end) {
        long id = insertReservation(owner, resource, UUID.randomUUID().toString(), start, end, status, null,
                status.equals("CHECKED_IN") ? NOW.minusMinutes(1) : null);
        var accepted = NOW.minusMinutes(5);
        jdbcTemplate.update("UPDATE reservation SET accepted_at=?, arrival_deadline=? WHERE id=?", accepted, accepted.plusMinutes(30), id);
        return id;
    }
    private long seat(String status) { return booking(1, 101, status, NOW.minusMinutes(5), NOW.plusMinutes(55)); }
    private long classroom(String status, int start) {
        jdbcTemplate.update("UPDATE resource SET resource_type='CLASSROOM' WHERE id=401");
        return booking(1, 401, status, NOW.plusMinutes(start), NOW.plusMinutes(start+60));
    }
    private String state(long id) { return jdbcTemplate.queryForObject("SELECT status FROM reservation WHERE id=?", String.class, id); }
    private int events(long id) { return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reservation_event WHERE reservation_id=?", Integer.class, id); }
    private int violations() { return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM violation_record", Integer.class); }
    private String payload(long seat) { return "{\"resourceId\":\"" + seat + "\",\"source\":\"CARD_SIMULATION\"}"; }
    private ResultActions card(long id, String direction, long resource) throws Exception {
        return mockMvc.perform(post("/api/v1/library/reservations/" + id + "/" + direction).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(payload(resource)));
    }
    private ResultActions cancel(long id) throws Exception {
        return mockMvc.perform(post("/api/v1/reservations/" + id + "/cancel").with(csrf()));
    }

    @Test void entryAndRepeatSaveOneCardEventWithoutTogglingExit() throws Exception {
        long id = seat("RESERVED");
        card(id,"check-in",101).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("CHECKED_IN"));
        card(id,"check-in",101).andExpect(status().isOk());
        assertThat(events(id)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT reason FROM reservation_event WHERE reservation_id=?",String.class,id)).isEqualTo("CARD_SIMULATION_ENTRY:101");
        assertThat(jdbcTemplate.queryForObject("SELECT operator_id FROM reservation_event WHERE reservation_id=?",Long.class,id)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT check_out_at FROM reservation WHERE id=?",LocalDateTime.class,id)).isNull();
    }
    @Test void entryIsAllowedOneMillisecondBeforeDeadline() throws Exception {
        long id=seat("RESERVED"); var accepted=NOW.minusMinutes(30).plusNanos(1_000_000);
        jdbcTemplate.update("UPDATE reservation SET accepted_at=? WHERE id=?",accepted,id);
        card(id,"check-in",101).andExpect(status().isOk());
    }
    @Test void entryAtThirtyMinutesCannotBeatNoShowOrRestoreIt() throws Exception {
        long id=seat("RESERVED"); jdbcTemplate.update("UPDATE reservation SET accepted_at=? WHERE id=?",NOW.minusMinutes(30),id);
        card(id,"check-in",101).andExpect(status().isConflict());
        assertThat(seats.expire(new LifecycleMapper.Candidate(id,101))).isTrue();
        card(id,"check-in",101).andExpect(status().isConflict());
        assertThat(state(id)).isEqualTo("CANCELLED"); assertThat(events(id)).isEqualTo(1); assertThat(violations()).isEqualTo(1);
    }
    @Test void arrivalDeadlineUsesAcceptedTimeEvenIfStoredDeadlineIsWrong() throws Exception {
        long id=seat("RESERVED");
        jdbcTemplate.update("UPDATE reservation SET accepted_at=?, arrival_deadline=? WHERE id=?",NOW.minusMinutes(31),NOW.plusHours(1),id);
        card(id,"check-in",101).andExpect(status().isConflict()); assertThat(events(id)).isZero();
    }
    @Test void acceptedThirtyMinuteRuleDoesNotInventStartTimeRestriction() throws Exception {
        long id=booking(1,101,"RESERVED",NOW.plusMinutes(5),NOW.plusHours(1));
        card(id,"check-in",101).andExpect(status().isOk());
    }
    @Test void endedBookingCannotBeCheckedInEvenWithinThirtyMinutes() throws Exception {
        long id=booking(1,101,"RESERVED",NOW.minusMinutes(10),NOW);
        card(id,"check-in",101).andExpect(status().isConflict()); assertThat(events(id)).isZero();
    }
    @Test void ownershipAndSeatEvidenceAreRequired() throws Exception {
        long id=seat("RESERVED");
        card(id,"check-in",102).andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/library/reservations/"+id+"/check-in").with(user("student02")).with(csrf()).param("userId","1")
                .contentType(MediaType.APPLICATION_JSON).content(payload(101))).andExpect(status().isNotFound());
        assertThat(state(id)).isEqualTo("RESERVED");
    }
    @Test void payloadSourceSessionAndCsrfAreEnforced() throws Exception {
        long id=seat("RESERVED"); String path="/api/v1/library/reservations/"+id+"/check-in";
        mockMvc.perform(post(path).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
        mockMvc.perform(post(path).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"resourceId\":\"101\",\"source\":\"QR\"}")).andExpect(status().isBadRequest());
        mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(payload(101))).andExpect(status().isForbidden());
        mockMvc.perform(post(path).with(anonymous()).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(payload(101))).andExpect(status().isUnauthorized());
        assertThat(events(id)).isZero();
    }
    @Test void sportsAndClassroomsCannotUseCardEntryOrExit() throws Exception {
        long sports=booking(1,201,"RESERVED",NOW,NOW.plusHours(1)); long room=classroom("RESERVED",0);
        card(sports,"check-in",201).andExpect(status().isConflict());card(room,"check-in",401).andExpect(status().isConflict());
        card(sports,"check-out",201).andExpect(status().isConflict());
    }
    @Test void disabledAndBlockedSeatsCannotEnter() throws Exception {
        long id=seat("RESERVED");jdbcTemplate.update("UPDATE resource SET enabled=false WHERE id=101");
        card(id,"check-in",101).andExpect(status().isConflict());jdbcTemplate.update("UPDATE resource SET enabled=true WHERE id=101");
        seedTeachingBlock(101,NOW,NOW.plusHours(1));card(id,"check-in",101).andExpect(status().isConflict());
    }
    @Test void exitImmediatelyCompletesAndRepeatDoesNotTouchLaterBooking() throws Exception {
        long id=seat("CHECKED_IN");
        card(id,"check-out",101).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("COMPLETED"));
        long next=booking(2,101,"RESERVED",NOW.plusMinutes(1),NOW.plusHours(1));
        card(id,"check-out",101).andExpect(status().isOk());
        assertThat(events(id)).isEqualTo(1);assertThat(state(next)).isEqualTo("RESERVED"); assertThat(violations()).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT check_out_at FROM reservation WHERE id=?",LocalDateTime.class,id)).isEqualTo(NOW);
    }
    @Test void exitAllowsOverdueCheckedInUseButNotUnconfirmedOrCancelledBooking() throws Exception {
        long overdue=booking(1,101,"CHECKED_IN",NOW.minusHours(2),NOW.minusHours(1));
        card(overdue,"check-out",101).andExpect(status().isOk());
        long reserved=seat("RESERVED");card(reserved,"check-out",101).andExpect(status().isConflict());
        long cancelled=seat("CANCELLED");card(cancelled,"check-out",101).andExpect(status().isConflict());
    }
    @Test void libraryActiveCancellationAndOldTrustUserIdRoutesAreUnavailable() throws Exception {
        long id=seat("RESERVED");cancel(id).andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/library/check-in").param("userId","1").with(csrf())).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/library/cancel").param("id",""+id).with(csrf())).andExpect(status().isNotFound());
        assertThat(state(id)).isEqualTo("RESERVED"); assertThat(events(id)).isZero();
    }
    @Test void checkInProtectsAgainstTheSeatNoShowTask() throws Exception {
        long id=seat("RESERVED");card(id,"check-in",101).andExpect(status().isOk());
        jdbcTemplate.update("UPDATE reservation SET accepted_at=? WHERE id=?",NOW.minusHours(1),id);
        assertThat(seats.expire(new LifecycleMapper.Candidate(id,101))).isFalse();
        card(id,"check-in",101).andExpect(status().isOk()); assertThat(violations()).isZero();assertThat(events(id)).isEqualTo(1);
    }
    @Test void auditFailureRollsBackBothEntryAndExit() {
        long id=seat("RESERVED");
        doThrow(new IllegalStateException("audit unavailable")).when(history).insertEvent(eq(id),anyString(),any(),any(),anyString(),anyString());
        assertThatThrownBy(()->cards.enter(1,id,101)).isInstanceOf(IllegalStateException.class);assertThat(state(id)).isEqualTo("RESERVED");
        reset(history);cards.enter(1,id,101);
        doThrow(new IllegalStateException("audit unavailable")).when(history).insertEvent(eq(id),anyString(),any(),any(),anyString(),anyString());
        assertThatThrownBy(()->cards.leave(1,id,101)).isInstanceOf(IllegalStateException.class);assertThat(state(id)).isEqualTo("CHECKED_IN");
        reset(history);cards.leave(1,id,101);assertThat(events(id)).isEqualTo(2);
    }
    @Test void concurrentRepeatedEntryAndExitAreExactlyOnce() throws Exception {
        long id=seat("RESERVED");race(()->cards.enter(1,id,101),()->cards.enter(1,id,101));
        race(()->cards.leave(1,id,101),()->cards.leave(1,id,101));
        assertThat(state(id)).isEqualTo("COMPLETED");assertThat(events(id)).isEqualTo(2);assertThat(violations()).isZero();
    }
    @Test void deadlineEntryRacesSafelyWithNoShow() throws Exception {
        long id=seat("RESERVED");jdbcTemplate.update("UPDATE reservation SET accepted_at=? WHERE id=?",NOW.minusMinutes(30),id);
        race(()->{try{cards.enter(1,id,101);throw new AssertionError("late entry accepted");}catch(org.campus.campussharebackend.common.BusinessException ex){assertThat(ex.status()).isEqualTo(409);}},()->seats.expire(new LifecycleMapper.Candidate(id,101)));
        assertThat(state(id)).isEqualTo("CANCELLED");assertThat(events(id)).isEqualTo(1);assertThat(violations()).isEqualTo(1);
    }
    @Test void classroomCanCancelBeforeStartExactlyOnceWithoutPenalty() throws Exception {
        long id=classroom("RESERVED",1);cancel(id).andExpect(status().isOk()).andExpect(jsonPath("$.data.cancelReason").value("USER_CANCEL"));
        cancel(id).andExpect(status().isOk());assertThat(events(id)).isEqualTo(1);assertThat(violations()).isZero();
    }
    @Test void classroomCannotCancelAtStartOrAfterCheckIn() throws Exception {
        long at=classroom("RESERVED",0);cancel(at).andExpect(status().isConflict());
        long entered=classroom("CHECKED_IN",1);cancel(entered).andExpect(status().isConflict());assertThat(events(at)).isZero();assertThat(events(entered)).isZero();
    }
    @Test void classroomCannotCancelForAnotherAccountOrWithoutCsrf() throws Exception {
        long id=classroom("RESERVED",1);String path="/api/v1/reservations/"+id+"/cancel";
        mockMvc.perform(post(path).with(user("student02")).with(csrf())).andExpect(status().isNotFound());
        mockMvc.perform(post(path)).andExpect(status().isForbidden());assertThat(state(id)).isEqualTo("RESERVED");
    }
    @Test void classroomCancelledByNoShowOrDisableCannotChangeItsReason() throws Exception {
        long absent=classroom("RESERVED",-15);venues.handle(absent);cancel(absent).andExpect(status().isConflict());
        long disabled=classroom("CANCELLED",1);jdbcTemplate.update("UPDATE reservation SET cancel_reason='RESOURCE_DISABLED' WHERE id=?",disabled);
        cancel(disabled).andExpect(status().isConflict());assertThat(violations()).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT cancel_reason FROM reservation WHERE id=?",String.class,disabled)).isEqualTo("RESOURCE_DISABLED");
    }
    @Test void classroomCancellationKeepsOtherBookingsAndTeachingBlocks() throws Exception {
        long id=classroom("RESERVED",1);long other=booking(2,401,"RESERVED",NOW.plusHours(2),NOW.plusHours(3));
        seedTeachingBlock(401,NOW.plusHours(3),NOW.plusHours(4));cancel(id).andExpect(status().isOk());
        assertThat(state(other)).isEqualTo("RESERVED");assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM resource_block WHERE resource_id=401",Integer.class)).isEqualTo(1);
    }
    @Test void teacherCanCancelTheirClassroomAndEventFailureRollsBack() throws Exception {
        long id=classroom("RESERVED",1);jdbcTemplate.update("UPDATE reservation SET user_id=3 WHERE id=?",id);
        doThrow(new IllegalStateException("audit unavailable")).when(history).insertEvent(eq(id),anyString(),any(),any(),anyString(),anyString());
        mockMvc.perform(post("/api/v1/reservations/"+id+"/cancel").with(user("teacher01")).with(csrf())).andExpect(status().isInternalServerError());
        assertThat(state(id)).isEqualTo("RESERVED");reset(history);
        mockMvc.perform(post("/api/v1/reservations/"+id+"/cancel").with(user("teacher01")).with(csrf())).andExpect(status().isOk());
    }
    @Test void releasedSeatAndClassroomCanActuallyBeRebooked() throws Exception {
        long seat=booking(1,101,"CHECKED_IN",NOW.plusMinutes(1),NOW.plusHours(1));card(seat,"check-out",101).andExpect(status().isOk());
        long room=classroom("RESERVED",1);cancel(room).andExpect(status().isOk());
        for(long resource:List.of(101L,401L))mockMvc.perform(post("/api/v1/reservations").with(user("student02")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"resourceId\":\""+resource+"\",\"startTime\":\""+iso(NOW.plusMinutes(1))+"\",\"endTime\":\""+iso(NOW.plusHours(1))+"\",\"clientRequestId\":\""+UUID.randomUUID()+"\"}")).andExpect(status().isCreated());
    }
    private void race(Runnable first,Runnable second)throws Exception {
        var go=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var a=pool.submit(()->{go.await();first.run();return true;});var b=pool.submit(()->{go.await();second.run();return true;});
            go.countDown();a.get(10,TimeUnit.SECONDS);b.get(10,TimeUnit.SECONDS);
        }
    }
}
