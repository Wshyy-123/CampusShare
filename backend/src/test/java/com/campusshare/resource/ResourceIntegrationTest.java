package com.campusshare.resource;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.campusshare.support.IntegrationTestBase;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;

@WithMockUser(username = "student01")
class ResourceIntegrationTest extends IntegrationTestBase {

    @Test
    void listsEnabledResourcesAndFiltersByType() throws Exception {
        mockMvc.perform(get("/api/v1/resources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4));
        mockMvc.perform(get("/api/v1/resources").param("type", "LIBRARY_SEAT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2));
        mockMvc.perform(get("/api/v1/resources").param("location", "体育馆"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void returnsAvailabilityForDate() throws Exception {
        String date = LocalDate.now(ZoneId.of("Asia/Shanghai")).plusDays(1).toString();
        mockMvc.perform(get("/api/v1/resources/101/availability").param("date", date))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resource.resourceId").value("101"))
                .andExpect(jsonPath("$.data.date").value(date))
                .andExpect(jsonPath("$.data.openings.length()").value(1))
                .andExpect(jsonPath("$.data.blocks.length()").value(0))
                .andExpect(jsonPath("$.data.reservations.length()").value(0));
    }

    @Test
    void availabilityShowsTeachingBlock() throws Exception {
        String date = LocalDate.now(ZoneId.of("Asia/Shanghai")).plusDays(1).toString();
        seedTeachingBlock(201, tomorrowAt(10, 0), tomorrowAt(12, 0));
        mockMvc.perform(get("/api/v1/resources/201/availability").param("date", date))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.blocks.length()").value(1))
                .andExpect(jsonPath("$.data.blocks[0].blockType").value("TEACHING"));
    }

    @Test
    void unknownResourceIsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/resources/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @WithAnonymousUser
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/resources"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
    }
}
