package org.campus.campussharebackend.service;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.campus.campussharebackend.support.IntegrationTestBase;
import org.springframework.security.test.context.support.WithMockUser;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@WithMockUser(username="student01")
class SportsReviewHttpTest extends IntegrationTestBase {
 @Test void rejectsEarlyCheckoutRatherThanCompletingAndReleasing() throws Exception {
  var now=LocalDateTime.now(ZONE);
  var id=insertReservation(1,201,"review-early-out",now.minusMinutes(2),now.plusMinutes(58),"CHECKED_IN",null,now.minusMinutes(2));
  mockMvc.perform(post("/api/v1/reservations/"+id+"/check-out").with(csrf()).contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{\"resourceId\":\"201\",\"source\":\"QR\"}")).andExpect(status().isConflict());
 }
 @Test void rejectsCheckInAfterShortBookingEnded() throws Exception {
  var now=LocalDateTime.now(ZONE);
  var id=insertReservation(1,201,"review-ended-in",now.minusMinutes(12),now.minusMinutes(2),"RESERVED",null,null);
  mockMvc.perform(post("/api/v1/reservations/"+id+"/check-in").with(csrf()).contentType(org.springframework.http.MediaType.APPLICATION_JSON).content("{\"resourceId\":\"201\",\"source\":\"QR\"}")).andExpect(status().isConflict());
 }
 @Test void requiresScannedLocationEvidenceForCheckIn() throws Exception {
  var now=LocalDateTime.now(ZONE);
  var id=insertReservation(1,201,"review-no-place",now.minusMinutes(1),now.plusMinutes(59),"RESERVED",null,null);
  mockMvc.perform(post("/api/v1/reservations/"+id+"/check-in").with(csrf())).andExpect(status().isBadRequest());
 }
}
