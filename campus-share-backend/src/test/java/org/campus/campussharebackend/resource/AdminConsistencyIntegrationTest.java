package org.campus.campussharebackend.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.concurrent.*;
import org.campus.campussharebackend.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

@WithMockUser(username = "admin01", roles = "RESOURCE_ADMIN")
class AdminConsistencyIntegrationTest extends IntegrationTestBase {
    @Test
    void sameNumericIdDoesNotCrossTablesOnUpdateOrDelete() throws Exception {
        jdbcTemplate.update("INSERT INTO resource_opening(id,resource_id,start_at,end_at) VALUES(9001,401,?,?)", tomorrowAt(20,0),tomorrowAt(21,0));
        jdbcTemplate.update("INSERT INTO resource_block(id,resource_id,block_type,start_at,end_at,reason) VALUES(9001,201,'TEACHING',?,?,'old')", tomorrowAt(10,0),tomorrowAt(11,0));
        mockMvc.perform(put("/api/v1/admin/schedules/9001").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(times("TEACHING", 12,13))).andExpect(status().isOk()).andExpect(jsonPath("$.data.resourceId").value("201"));
        mockMvc.perform(delete("/api/v1/admin/schedules/9001").param("type","TEACHING").with(csrf())).andExpect(status().isOk());
        assertThat(count("SELECT COUNT(*) FROM resource_opening WHERE id=9001")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM resource_block WHERE id=9001")).isZero();
        assertThat(count("SELECT COUNT(*) FROM resource_schedule_event WHERE schedule_type='TEACHING' AND schedule_id=9001")).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("SELECT before_json FROM resource_schedule_event WHERE operation='DELETE'",String.class)).contains("TEACHING");
    }
    @Test
    void deleteRequiresExplicitType() throws Exception {
        long id = opening();
        mockMvc.perform(delete("/api/v1/admin/schedules/"+id).with(csrf())).andExpect(status().isBadRequest());
        assertThat(count("SELECT COUNT(*) FROM resource_opening WHERE id="+id)).isEqualTo(1);
    }
    @Test
    void teachingCannotInvalidateAnAcceptedReservation() throws Exception {
        booking("RESERVED");
        mockMvc.perform(post("/api/v1/admin/resources/201/teaching-blocks").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("startTime",iso(tomorrowAt(9,0)),"endTime",iso(tomorrowAt(10,0)),"reason","课表"))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SCHEDULE_CONFLICT"));
        assertThat(count("SELECT COUNT(*) FROM resource_block")).isZero();
    }
    @Test
    void deletingOpeningWithBookingRollsBack() throws Exception {
        booking("RESERVED"); long id=opening();
        mockMvc.perform(delete("/api/v1/admin/schedules/"+id).param("type","OPEN").with(csrf())).andExpect(status().isConflict());
        assertThat(count("SELECT COUNT(*) FROM resource_opening WHERE id="+id)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM resource_schedule_event")).isZero();
    }
    @Test
    void shrinkingOpeningWithBookingRollsBack() throws Exception {
        booking("RESERVED"); long id=opening();
        mockMvc.perform(put("/api/v1/admin/schedules/"+id).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(times("OPEN",11,22))).andExpect(status().isConflict());
        assertThat(jdbcTemplate.queryForObject("SELECT start_at FROM resource_opening WHERE id=?",LocalDateTime.class,id)).isEqualTo(tomorrowAt(8,0));
    }
    @Test
    void harmlessOpeningEditPreservesBooking() throws Exception {
        long booking=booking("RESERVED");
        mockMvc.perform(put("/api/v1/admin/schedules/"+opening()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(times("OPEN",8,21))).andExpect(status().isOk());
        assertThat(state(booking)).isEqualTo("RESERVED");
    }
    @Test
    void fullDeactivationCancelsAndRecordsExactlyOnceAndDoesNotEraseBlocks() throws Exception {
        long id=booking("RESERVED"); seedTeachingBlock(201,tomorrowAt(12,0),tomorrowAt(13,0));
        save(false,"BADMINTON").andExpect(status().isOk());
        save(false,"BADMINTON").andExpect(status().isOk());
        assertThat(state(id)).isEqualTo("CANCELLED");
        assertThat(count("SELECT COUNT(*) FROM reservation_event WHERE reason='RESOURCE_DISABLED'")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM violation_record")).isZero();
        assertThat(count("SELECT COUNT(*) FROM resource_block")).isEqualTo(1);
        save(true,"BADMINTON").andExpect(status().isOk());
        assertThat(state(id)).isEqualTo("CANCELLED");
        assertThat(count("SELECT COUNT(*) FROM resource_block")).isEqualTo(1);
    }
    @Test
    void fullDeactivationWithInUseBookingRollsBackEverything() throws Exception {
        long future=booking("RESERVED");
        var now=LocalDateTime.now(ZONE);
        long used=insertReservation(2,201,"using",now.minusMinutes(1),now.plusHours(1),"CHECKED_IN",null,now.minusMinutes(1));
        save(false,"BADMINTON").andExpect(status().isConflict());
        assertThat(state(future)).isEqualTo("RESERVED"); assertThat(state(used)).isEqualTo("CHECKED_IN");
        assertThat(count("SELECT COUNT(*) FROM reservation_event")).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT enabled FROM resource WHERE id=201",Boolean.class)).isTrue();
    }
    @Test
    void typeCannotChangeWithHistoricalReservations() throws Exception {
        booking("COMPLETED");
        save(true,"LIBRARY_SEAT").andExpect(status().isConflict());
        assertThat(jdbcTemplate.queryForObject("SELECT resource_type FROM resource WHERE id=201",String.class)).isEqualTo("BADMINTON");
    }
    @Test
    void enormousAdminPageIsEmptyInsteadOfOverflowing() throws Exception {
        mockMvc.perform(get("/api/v1/admin/resources").param("page","2147483647").param("size","100"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items.length()").value(0));
    }
    @Test
    void staleAdminAuthorityCannotOverrideDatabaseRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/resources").with(user("student01").roles("RESOURCE_ADMIN")))
                .andExpect(status().isForbidden());
    }
    @Test
    void adminWritesStillRequireCsrf() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/schedules/"+opening()).param("type","OPEN"))
                .andExpect(status().isForbidden());
    }
    @Test
    void concurrentTeachingAndReservationCannotCreateConflict() throws Exception {
        var start=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var teaching=pool.submit(()->{start.await();return mockMvc.perform(post("/api/v1/admin/resources/201/teaching-blocks")
                    .with(user("admin01").roles("RESOURCE_ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("startTime",iso(tomorrowAt(9,0)),"endTime",iso(tomorrowAt(10,0)),"reason","race"))))
                    .andReturn().getResponse().getStatus();});
            var booking=pool.submit(()->{start.await();return mockMvc.perform(post("/api/v1/reservations")
                    .with(user("student01")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("resourceId","201","startTime",iso(tomorrowAt(9,0)),"endTime",iso(tomorrowAt(10,0)),"clientRequestId","schedule-race"))))
                    .andReturn().getResponse().getStatus();});
            start.countDown();
            assertThat(java.util.List.of(teaching.get(10,TimeUnit.SECONDS),booking.get(10,TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201,409);
        }
        assertThat(count("SELECT COUNT(*) FROM resource_block")+count("SELECT COUNT(*) FROM reservation")).isEqualTo(1);
    }
    @Test
    void concurrentCreateAndWholeResourceDisableCannotLeaveActiveBooking() throws Exception {
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var disabling = pool.submit(() -> { start.await(); return mockMvc.perform(put("/api/v1/admin/resources/201")
                    .with(user("admin01").roles("RESOURCE_ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"name\":\"体育馆1号羽毛球场\",\"location\":\"体育馆二楼\",\"resourceType\":\"BADMINTON\",\"unitLabel\":\"1号场\",\"enabled\":false}"))
                    .andReturn().getResponse().getStatus(); });
            var creation = pool.submit(() -> { start.await(); return mockMvc.perform(post("/api/v1/reservations")
                    .with(user("student01")).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(Map.of("resourceId","201","startTime",iso(tomorrowAt(9,0)),"endTime",iso(tomorrowAt(10,0)),"clientRequestId","full-disable-race"))))
                    .andReturn().getResponse().getStatus(); });
            start.countDown();
            assertThat(disabling.get(10,TimeUnit.SECONDS)).isEqualTo(200);
            assertThat(creation.get(10,TimeUnit.SECONDS)).isIn(201,409);
        }
        assertThat(count("SELECT COUNT(*) FROM reservation WHERE status IN ('RESERVED','CHECKED_IN')")).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT enabled FROM resource WHERE id=201",Boolean.class)).isFalse();
    }
    @Test
    void concurrentDuplicateTeachingCreatesOnlyOneRecord() throws Exception {
        var start = new CountDownLatch(1);
        var payload = objectMapper.writeValueAsString(Map.of("startTime",iso(tomorrowAt(9,0)),"endTime",iso(tomorrowAt(10,0)),"reason","课表"));
        Callable<Integer> creation = () -> { start.await(); return mockMvc.perform(post("/api/v1/admin/resources/201/teaching-blocks")
                .with(user("admin01").roles("RESOURCE_ADMIN")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(payload))
                .andReturn().getResponse().getStatus(); };
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(creation); var second = pool.submit(creation); start.countDown();
            assertThat(java.util.List.of(first.get(10,TimeUnit.SECONDS),second.get(10,TimeUnit.SECONDS))).containsExactlyInAnyOrder(201,409);
        }
        assertThat(count("SELECT COUNT(*) FROM resource_block")).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM resource_schedule_event")).isEqualTo(1);
    }
    private long booking(String status) { return insertReservation(1,201,java.util.UUID.randomUUID().toString(),tomorrowAt(9,0),tomorrowAt(10,0),status,null,null); }
    private long opening() { return jdbcTemplate.queryForObject("SELECT id FROM resource_opening WHERE resource_id=201 AND start_at=?",Long.class,tomorrowAt(8,0)); }
    private int count(String sql) { return jdbcTemplate.queryForObject(sql,Integer.class); }
    private String state(long id) { return jdbcTemplate.queryForObject("SELECT status FROM reservation WHERE id=?",String.class,id); }
    private String times(String type,int start,int end) throws Exception { return objectMapper.writeValueAsString(Map.of("type",type,"startTime",iso(tomorrowAt(start,0)),"endTime",iso(tomorrowAt(end,0)),"note","测试")); }
    private org.springframework.test.web.servlet.ResultActions save(boolean enabled,String type) throws Exception {
        return mockMvc.perform(put("/api/v1/admin/resources/201").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("name","体育馆1号羽毛球场","location","体育馆二楼","resourceType",type,"unitLabel","1号场","enabled",enabled))));
    }
}
