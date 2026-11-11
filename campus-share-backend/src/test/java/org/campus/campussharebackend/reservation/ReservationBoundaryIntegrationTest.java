package org.campus.campussharebackend.reservation;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import org.campus.campussharebackend.common.TimeFormats;
import org.campus.campussharebackend.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

@WithMockUser(username = "student01")
class ReservationBoundaryIntegrationTest extends IntegrationTestBase {
    @Test
    void adjacentOpeningsAllowTheSameRangeShownAsAvailable() throws Exception {
        jdbcTemplate.update("DELETE FROM resource_opening WHERE resource_id=101");
        jdbcTemplate.update("INSERT INTO resource_opening(resource_id,start_at,end_at) VALUES(101,?,?),(101,?,?)", tomorrowAt(9,0),tomorrowAt(9,30),tomorrowAt(9,30),tomorrowAt(10,0));
        create(101, tomorrowAt(9,0), tomorrowAt(10,0), "adjacent-openings").andExpect(status().isCreated());
    }
    @Test
    void rejectsPastStartEvenWithAnOpening() throws Exception {
        LocalDateTime start = LocalDate.now(ZONE).minusDays(1).atTime(10, 0);
        jdbcTemplate.update("INSERT INTO resource_opening(resource_id,start_at,end_at) VALUES(101,?,?)", start, start.plusHours(2));
        create(101, start, start.plusHours(1), "past-start")
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
    }

    @Test
    void discussionRoomRequiresPreviousCalendarDayAndHasNoSportsDeadline() throws Exception {
        jdbcTemplate.update("UPDATE resource SET resource_type='DISCUSSION_ROOM' WHERE id=401");
        try {
            LocalDateTime sameDay = LocalDate.now(ZONE).atTime(21, 0);
            create(401, sameDay, sameDay.plusMinutes(30), "room-today")
                    .andExpect(status().isBadRequest());
            LocalDateTime tomorrow = tomorrowAt(8, 0);
            create(401, tomorrow, tomorrow.plusHours(1), "room-tomorrow")
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.data.arrivalDeadline").isEmpty());
        } finally {
            jdbcTemplate.update("UPDATE resource SET resource_type='TENNIS' WHERE id=401");
        }
    }

    @Test
    void availabilityReturnsServerComputedFreePeriods() throws Exception {
        seedTeachingBlock(201, tomorrowAt(10, 0), tomorrowAt(12, 0));
        insertReservation(2, 201, "existing-slot", tomorrowAt(14, 0), tomorrowAt(15, 0), "RESERVED", null, null);
        mockMvc.perform(get("/api/v1/resources/201/availability").param("date", tomorrowAt(8, 0).toLocalDate().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.availablePeriods.length()").value(3))
                .andExpect(jsonPath("$.data.availablePeriods[0].endTime").value(iso(tomorrowAt(10, 0))))
                .andExpect(jsonPath("$.data.availablePeriods[1].startTime").value(iso(tomorrowAt(12, 0))))
                .andExpect(jsonPath("$.data.availablePeriods[2].startTime").value(iso(tomorrowAt(15, 0))));
        jdbcTemplate.update("UPDATE resource SET enabled=FALSE WHERE id=201");
        mockMvc.perform(get("/api/v1/resources/201/availability").param("date", tomorrowAt(8, 0).toLocalDate().toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.availablePeriods.length()").value(0));
    }

    @Test
    void hugePageReturnsEmptyRatherThanOverflowingSqlOffset() throws Exception {
        insertReservation(1, 101, "my-record", tomorrowAt(9, 0), tomorrowAt(10, 0), "RESERVED", null, null);
        mockMvc.perform(get("/api/v1/reservations/mine").param("page", "2147483647").param("size", "100"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items.length()").value(0));
    }

    private org.springframework.test.web.servlet.ResultActions create(long id, LocalDateTime start, LocalDateTime end, String key) throws Exception {
        return mockMvc.perform(post("/api/v1/reservations").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("resourceId", Long.toString(id), "startTime", iso(start), "endTime", iso(end), "clientRequestId", key))));
    }
}
