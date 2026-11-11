package org.campus.campussharebackend.reservation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.campus.campussharebackend.support.IntegrationTestBase;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class CrossResourceIdempotencyTest extends IntegrationTestBase {
    @Test
    void sameUserAndRequestKeyCannotCreateTwoDifferentResources() throws Exception {
        try (Connection connection = jdbcTemplate.getDataSource().getConnection()) {
            Assumptions.assumeTrue(connection.getMetaData().getDatabaseProductName().equalsIgnoreCase("MySQL"));
        }
        var ready = new CountDownLatch(2);
        var go = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> attempt(101, ready, go));
            var second = executor.submit(() -> attempt(201, ready, go));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue(); go.countDown();
            assertThat(List.of(first.get(15,TimeUnit.SECONDS),second.get(15,TimeUnit.SECONDS))).containsExactlyInAnyOrder(201,409);
        }
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reservation WHERE client_request_id='same-key-different-resources'",Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reservation_event",Integer.class)).isEqualTo(1);
    }

    private int attempt(long resourceId, CountDownLatch ready, CountDownLatch go) throws Exception {
        String json=objectMapper.writeValueAsString(Map.of("resourceId",Long.toString(resourceId),"startTime",iso(tomorrowAt(9,0)),"endTime",iso(tomorrowAt(10,0)),"clientRequestId","same-key-different-resources"));
        ready.countDown(); if (!go.await(5,TimeUnit.SECONDS)) return 0;
        return mockMvc.perform(post("/api/v1/reservations").with(user("student01")).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json)).andReturn().getResponse().getStatus();
    }
}
