package org.campus.campussharebackend.schedule;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.campus.campussharebackend.support.IntegrationTestBase;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;

@WithMockUser(username = "admin01")
class AdminScheduleIntegrationTest extends IntegrationTestBase {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private String tomorrowDate() {
        return LocalDate.now(ZONE).plusDays(1).toString();
    }

    private String body(Object payload) throws Exception {
        return objectMapper.writeValueAsString(payload);
    }

    @Test
    void adminCreatesOpeningAndTeachingBlockThenListsUpdatesAndDeletes() throws Exception {
        String date = tomorrowDate();

        // 新增开放安排（seed 开放为 08:00-22:00，取夜间时段避免重叠）
        LocalDateTime openStart = LocalDate.now(ZONE).plusDays(1).atTime(23, 0);
        LocalDateTime openEnd = LocalDate.now(ZONE).plusDays(1).atTime(23, 30);
        String createOpening = mockMvc.perform(post("/api/v1/admin/resources/101/openings")
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(java.util.Map.of(
                                "startTime", iso(openStart),
                                "endTime", iso(openEnd),
                                "note", "夜间临时开放"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.type").value("OPEN"))
                .andExpect(jsonPath("$.data.resourceId").value("101"))
                .andReturn().getResponse().getContentAsString();
        String openingId = objectMapper.readTree(createOpening).at("/data/scheduleId").asText();

        // 新增教学占用
        LocalDateTime teachStart = LocalDate.now(ZONE).plusDays(1).atTime(10, 0);
        LocalDateTime teachEnd = LocalDate.now(ZONE).plusDays(1).atTime(12, 0);
        String createTeaching = mockMvc.perform(post("/api/v1/admin/resources/201/teaching-blocks")
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(java.util.Map.of(
                                "startTime", iso(teachStart),
                                "endTime", iso(teachEnd),
                                "reason", "模拟教务课表"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.type").value("TEACHING"))
                .andExpect(jsonPath("$.data.resourceId").value("201"))
                .andReturn().getResponse().getContentAsString();
        String teachingId = objectMapper.readTree(createTeaching).at("/data/scheduleId").asText();

        // 列表应包含两条本次维护的安排
        mockMvc.perform(get("/api/v1/admin/schedules").param("date", date))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.type == 'OPEN' && @.resourceId == '101')]").exists())
                .andExpect(jsonPath("$.data[?(@.type == 'TEACHING' && @.resourceId == '201')]").exists());

        // 修改教学占用时段与备注
        LocalDateTime updatedStart = LocalDate.now(ZONE).plusDays(1).atTime(13, 0);
        LocalDateTime updatedEnd = LocalDate.now(ZONE).plusDays(1).atTime(14, 0);
        mockMvc.perform(put("/api/v1/admin/schedules/" + teachingId)
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(java.util.Map.of(
                                "type", "TEACHING",
                                "startTime", iso(updatedStart),
                                "endTime", iso(updatedEnd),
                                "note", "调整后的教务课表"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.startTime").value(iso(updatedStart)))
                .andExpect(jsonPath("$.data.note").value("调整后的教务课表"));

        // 删除开放安排
        mockMvc.perform(delete("/api/v1/admin/schedules/" + openingId).with(csrf()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/admin/schedules").param("date", date))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.scheduleId == '" + openingId + "')]").doesNotExist());
    }

    @Test
    @WithMockUser(username = "student01")
    void studentCannotMaintainSchedules() throws Exception {
        LocalDateTime start = LocalDate.now(ZONE).plusDays(1).atTime(9, 0);
        LocalDateTime end = LocalDate.now(ZONE).plusDays(1).atTime(10, 0);
        mockMvc.perform(post("/api/v1/admin/resources/101/openings")
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(java.util.Map.of(
                                "startTime", iso(start), "endTime", iso(end), "note", ""))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/v1/admin/schedules"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    @WithAnonymousUser
    void anonymousIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/admin/schedules"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
    }

    @Test
    void rejectsReversedTimeRange() throws Exception {
        LocalDateTime start = LocalDate.now(ZONE).plusDays(1).atTime(15, 0);
        LocalDateTime end = LocalDate.now(ZONE).plusDays(1).atTime(14, 0);
        mockMvc.perform(post("/api/v1/admin/resources/101/openings")
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(java.util.Map.of(
                                "startTime", iso(start), "endTime", iso(end), "note", ""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));
    }

    @Test
    void rejectsOverlappingTeachingBlock() throws Exception {
        LocalDateTime firstStart = LocalDate.now(ZONE).plusDays(1).atTime(10, 0);
        LocalDateTime firstEnd = LocalDate.now(ZONE).plusDays(1).atTime(12, 0);
        mockMvc.perform(post("/api/v1/admin/resources/201/teaching-blocks")
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(java.util.Map.of(
                                "startTime", iso(firstStart), "endTime", iso(firstEnd), "reason", "第一节"))))
                .andExpect(status().isCreated());

        LocalDateTime overlapStart = LocalDate.now(ZONE).plusDays(1).atTime(11, 0);
        LocalDateTime overlapEnd = LocalDate.now(ZONE).plusDays(1).atTime(13, 0);
        mockMvc.perform(post("/api/v1/admin/resources/201/teaching-blocks")
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(java.util.Map.of(
                                "startTime", iso(overlapStart), "endTime", iso(overlapEnd), "reason", "第二节"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SCHEDULE_CONFLICT"));
    }

    @Test
    void updateUnknownScheduleIsNotFound() throws Exception {
        LocalDateTime start = LocalDate.now(ZONE).plusDays(1).atTime(9, 0);
        LocalDateTime end = LocalDate.now(ZONE).plusDays(1).atTime(10, 0);
        mockMvc.perform(put("/api/v1/admin/schedules/999999")
                        .with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(body(java.util.Map.of(
                                "type", "OPEN",
                                "startTime", iso(start), "endTime", iso(end), "note", ""))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SCHEDULE_NOT_FOUND"));
    }
}
