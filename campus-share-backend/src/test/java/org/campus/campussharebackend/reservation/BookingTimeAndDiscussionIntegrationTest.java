package org.campus.campussharebackend.reservation;

import java.time.*;
import java.util.Map;
import org.campus.campussharebackend.support.IntegrationTestBase;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WithMockUser(username = "student01")
class BookingTimeAndDiscussionIntegrationTest extends IntegrationTestBase {
    @MockitoBean(name = "lifecycleClock") Clock clock;
    private LocalDateTime now;

    @BeforeEach void fixedTime() {
        now = LocalDate.now(ZONE).atTime(10, 0);
        when(clock.getZone()).thenReturn(ZONE);
        when(clock.instant()).thenReturn(now.atZone(ZONE).toInstant());
        jdbcTemplate.update("UPDATE resource SET resource_type='DISCUSSION_ROOM' WHERE id=401");
    }
    @AfterEach void restoreType() {
        jdbcTemplate.update("UPDATE resource SET resource_type='TENNIS' WHERE id=401");
    }

    @ParameterizedTest
    @ValueSource(strings = {"LIBRARY_SEAT", "CLASSROOM", "BADMINTON", "TABLE_TENNIS", "TENNIS", "DISCUSSION_ROOM"})
    void rejectsPastStartEvenWhenEndIsInTheFuture(String type) throws Exception {
        jdbcTemplate.update("UPDATE resource SET resource_type=? WHERE id=401", type);
        create(now.minusMinutes(1), now.plusHours(1), "past-" + type)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
        assertThat(count("SELECT COUNT(*) FROM reservation")).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"LIBRARY_SEAT", "CLASSROOM", "BADMINTON", "TABLE_TENNIS", "TENNIS", "DISCUSSION_ROOM"})
    void availabilityNeverOffersEarlierThanServerNow(String type) throws Exception {
        jdbcTemplate.update("UPDATE resource SET resource_type=? WHERE id=401", type);
        mockMvc.perform(get("/api/v1/resources/401/availability").param("date", now.toLocalDate().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.availablePeriods[0].startTime").value(iso(now)));
        mockMvc.perform(get("/api/v1/resources/401/availability").param("date", now.toLocalDate().minusDays(1).toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.availablePeriods.length()").value(0));
        mockMvc.perform(get("/api/v1/resources/401/availability").param("date", now.toLocalDate().plusDays(1).toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.openings.length()").value(1))
                .andExpect(jsonPath("$.data.availablePeriods.length()").value(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"LIBRARY_SEAT", "CLASSROOM", "BADMINTON", "TABLE_TENNIS", "TENNIS", "DISCUSSION_ROOM"})
    void noResourceCanBeBookedForAnotherDay(String type) throws Exception {
        jdbcTemplate.update("UPDATE resource SET resource_type=? WHERE id=401",type);
        for(int days:new int[]{1,2}) create(now.plusDays(days),now.plusDays(days).plusHours(1),"future-"+type+days)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("SAME_DAY_ONLY"));
        assertThat(count("SELECT COUNT(*) FROM reservation")).isZero();
    }

    @Test void crossMidnightBookingIsRejectedButEndAtDayBoundaryIsAllowed() throws Exception {
        LocalDateTime midnight=now.toLocalDate().plusDays(1).atStartOfDay();
        jdbcTemplate.update("INSERT INTO resource_opening(resource_id,start_at,end_at) VALUES(401,?,?)",midnight.minusHours(1),midnight.plusHours(1));
        create(midnight.minusMinutes(30),midnight.plusMinutes(30),"cross-day")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("SAME_DAY_ONLY"));
        create(midnight.minusMinutes(30),midnight,"day-end").andExpect(status().isCreated());
    }

    @ParameterizedTest
    @ValueSource(strings = {"LIBRARY_SEAT", "CLASSROOM", "BADMINTON", "TABLE_TENNIS", "TENNIS", "DISCUSSION_ROOM"})
    void remainingCurrentDayIsStillBookableForEveryResourceType(String type) throws Exception {
        jdbcTemplate.update("UPDATE resource SET resource_type=? WHERE id=401",type);
        create(now.plusMinutes(20),now.plusHours(1),"today-"+type)
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.resourceType").value(type));
    }

    @Test void businessDayUsesShanghaiDateRegardlessOfInputTimezone() throws Exception {
        String payload=objectMapper.writeValueAsString(Map.of("resourceId","401","startTime",now.plusHours(1).atZone(ZONE).toInstant().toString(),
                "endTime",now.plusHours(2).atZone(ZONE).toInstant().toString(),"clientRequestId","utc-current-day"));
        mockMvc.perform(post("/api/v1/reservations").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated());
        payload=objectMapper.writeValueAsString(Map.of("resourceId","401","startTime",now.toLocalDate().plusDays(1).atTime(0,30).atZone(ZONE).toInstant().toString(),
                "endTime",now.toLocalDate().plusDays(1).atTime(1,0).atZone(ZONE).toInstant().toString(),"clientRequestId","utc-next-shanghai-day"));
        mockMvc.perform(post("/api/v1/reservations").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("SAME_DAY_ONLY"));
    }

    @Test void midnightRolloverRechecksDateAndPreservesIdempotentReplay() throws Exception {
        var start=now.plusHours(1);var end=now.plusHours(2);
        var result=create(start,end,"rollover-retry").andExpect(status().isCreated()).andReturn();
        String id=objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("reservationId").asText();
        when(clock.instant()).thenReturn(now.plusDays(1).atZone(ZONE).toInstant());
        create(start,end,"rollover-retry").andExpect(status().isOk()).andExpect(jsonPath("$.data.reservationId").value(id));
        create(start,end,"new-request-old-day").andExpect(status().isBadRequest());
        create(now.plusDays(1).plusHours(1),now.plusDays(1).plusHours(2),"new-business-day").andExpect(status().isCreated());
    }

    @Test void sameDayRoomCanBeCancelledAndRebookedByAnotherUser() throws Exception {
        var result = create(now.plusMinutes(30), now.plusHours(1), "room-same-day")
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.arrivalDeadline").isEmpty()).andReturn();
        String id = objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("reservationId").asText();
        mockMvc.perform(post("/api/v1/reservations/"+id+"/cancel").with(csrf()).with(user("student02")))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/reservations/"+id+"/cancel"))
                .andExpect(status().isForbidden());
        for (int i=0; i<2; i++) mockMvc.perform(post("/api/v1/reservations/"+id+"/cancel").with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancelReason").value("USER_CANCEL"));
        assertThat(count("SELECT COUNT(*) FROM reservation_event WHERE reservation_id="+id+" AND event_type='CANCELLED'")).isEqualTo(1);
        mockMvc.perform(post("/api/v1/reservations").with(csrf()).with(user("student02"))
                .contentType(MediaType.APPLICATION_JSON).content(body(now.plusMinutes(30), now.plusHours(1), "room-next-owner")))
                .andExpect(status().isCreated());
        assertThat(count("SELECT COUNT(*) FROM reservation")).isEqualTo(2);
    }

    @Test @WithMockUser(username = "admin01", roles = "RESOURCE_ADMIN")
    void adminCanCancelOwnRoomBooking() throws Exception {
        var result=create(now.plusHours(1), now.plusHours(2), "admin-room").andExpect(status().isCreated()).andReturn();
        String id=objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("reservationId").asText();
        mockMvc.perform(post("/api/v1/reservations/"+id+"/cancel").with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test void roomCannotBeCancelledAtStartOrAfterwardsOrWhenCheckedIn() throws Exception {
        for (var start : new LocalDateTime[]{now, now.minusMinutes(1), now.plusHours(1)}) {
            String state=start.isAfter(now)?"CHECKED_IN":"RESERVED";
            long id=insertReservation(1,401,"room-boundary-"+start,start,start.plusHours(1),state,null,
                    "CHECKED_IN".equals(state)?now:null);
            mockMvc.perform(post("/api/v1/reservations/"+id+"/cancel").with(csrf()))
                    .andExpect(status().isConflict());
        }
    }

    @Test void timezoneOffsetCannotBypassPastStartCheck() throws Exception {
        String data=objectMapper.writeValueAsString(Map.of("resourceId","401", "startTime",now.minusSeconds(1).atZone(ZONE).toInstant().toString(),
                "endTime",iso(now.plusHours(1)),"clientRequestId","offset-past"));
        mockMvc.perform(post("/api/v1/reservations").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(data))
                .andExpect(status().isBadRequest());
    }

    @Test void idempotentReplayAfterStartReturnsExistingBookingWithoutCreatingAnother() throws Exception {
        var result=create(now.plusMinutes(1),now.plusHours(1),"retry-existing").andExpect(status().isCreated()).andReturn();
        String id=objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("reservationId").asText();
        when(clock.instant()).thenReturn(now.plusMinutes(2).atZone(ZONE).toInstant());
        create(now.plusMinutes(1),now.plusHours(1),"retry-existing").andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reservationId").value(id));
        assertThat(count("SELECT COUNT(*) FROM reservation")).isEqualTo(1);
    }

    private ResultActions create(LocalDateTime start,LocalDateTime end,String key) throws Exception {
        return mockMvc.perform(post("/api/v1/reservations").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body(start,end,key)));
    }
    private String body(LocalDateTime start,LocalDateTime end,String key) throws Exception {
        return objectMapper.writeValueAsString(Map.of("resourceId","401","startTime",iso(start),"endTime",iso(end),"clientRequestId",key));
    }
    private long count(String sql) { return jdbcTemplate.queryForObject(sql, Long.class); }
}
