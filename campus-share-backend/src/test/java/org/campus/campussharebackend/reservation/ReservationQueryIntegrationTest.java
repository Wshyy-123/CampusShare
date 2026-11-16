package org.campus.campussharebackend.reservation;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.campus.campussharebackend.support.IntegrationTestBase;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithMockUser;

@WithMockUser(username = "student01")
class ReservationQueryIntegrationTest extends IntegrationTestBase {

    private long ownSeatId;
    private long ownCancelledId;
    private long otherSeatId;

    @BeforeEach
    void seedReservations() {
        LocalDateTime seatStart = tomorrowAt(9, 0);
        LocalDateTime badmintonStart = tomorrowAt(14, 0);
        ownSeatId = insertReservation(1, 101, "query-own-seat", seatStart,
                seatStart.plusHours(1), "RESERVED", null, null);
        ownCancelledId = insertReservation(1, 201, "query-own-cancelled", badmintonStart,
                badmintonStart.plusHours(1), "CANCELLED", "USER_CANCEL", null);
        otherSeatId = insertReservation(2, 102, "query-other-seat", seatStart,
                seatStart.plusHours(1), "RESERVED", null, null);
    }

    @Test
    void listsOnlyOwnReservationsWithResourceStatusAndPaginationFields() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].reservationId").value(Long.toString(ownCancelledId)))
                .andExpect(jsonPath("$.data.items[0].status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.items[0].resourceName").value("体育馆1号羽毛球场"))
                .andExpect(jsonPath("$.data.items[1].reservationId").value(Long.toString(ownSeatId)));
    }

    @Test
    void filtersByStatus() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/mine").param("status", "CANCELLED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].reservationId").value(Long.toString(ownCancelledId)))
                .andExpect(jsonPath("$.data.items[0].cancelReason").value("USER_CANCEL"));
    }

    @Test
    void paginates() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/mine")
                        .param("page", "2").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.page").value(2))
                .andExpect(jsonPath("$.data.size").value(1))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].reservationId").value(Long.toString(ownSeatId)));
    }

    @Test
    void filtersByStartTimeRange() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/mine")
                        .param("from", iso(tomorrowAt(13, 0)))
                        .param("to", iso(tomorrowAt(16, 0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].reservationId").value(Long.toString(ownCancelledId)));
    }

    @Test
    void anotherUserSeesOnlyTheirOwnReservation() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/mine").with(user("student02")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.items[0].reservationId").value(Long.toString(otherSeatId)));
    }

    @Test
    void emptyStateReturnsEmptyItems() throws Exception {
        jdbcTemplate.update("DELETE FROM violation_record");
        jdbcTemplate.update("DELETE FROM reservation_event");
        jdbcTemplate.update("DELETE FROM reservation");
        mockMvc.perform(get("/api/v1/reservations/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.items.length()").value(0));
    }

    @Test
    void returnsOwnReservationDetail() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/{id}", ownSeatId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reservationId").value(Long.toString(ownSeatId)))
                .andExpect(jsonPath("$.data.resourceId").value("101"))
                .andExpect(jsonPath("$.data.resourceName").value("图书馆A区01座"))
                .andExpect(jsonPath("$.data.status").value("RESERVED"))
                .andExpect(jsonPath("$.data.acceptedAt").isString())
                .andExpect(jsonPath("$.data.arrivalDeadline").isString());
    }

    @Test
    void cannotReadAnotherUsersReservation() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/{id}", otherSeatId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESERVATION_NOT_FOUND"));
    }

    @Test
    void unknownReservationIsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESERVATION_NOT_FOUND"));
    }

    @Test
    void rejectsInvalidStatusAndPagination() throws Exception {
        mockMvc.perform(get("/api/v1/reservations/mine").param("status", "WAITING"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
        mockMvc.perform(get("/api/v1/reservations/mine").param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
        mockMvc.perform(get("/api/v1/reservations/mine").param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
    }
}
