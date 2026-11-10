package com.campusshare;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthAndResourceApiTest {

    private static final String VALID_RESOURCE_JSON = """
            {"name":" 乒乓球桌A ","location":" 北体育馆 ","type":"PING_PONG_TABLE","bookingUnit":"TABLE"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticatedAccessIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/v1/admin/resources"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void invalidCredentialsDoNotCreateLoginState() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"stu2024001","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void studentCanLoginButCannotManageResources() throws Exception {
        MockHttpSession session = login("stu2024001", "Campus@123");

        mockMvc.perform(get("/api/v1/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("stu2024001"))
                .andExpect(jsonPath("$.data.role").value("STUDENT"))
                .andExpect(jsonPath("$.data.roleLabel").value("学生"));

        mockMvc.perform(post("/api/v1/admin/resources")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_RESOURCE_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void adminCanCreateAndListResources() throws Exception {
        MockHttpSession session = login("admin01", "Campus@123");

        mockMvc.perform(post("/api/v1/admin/resources")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_RESOURCE_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("乒乓球桌A"))
                .andExpect(jsonPath("$.data.location").value("北体育馆"))
                .andExpect(jsonPath("$.data.type").value("PING_PONG_TABLE"))
                .andExpect(jsonPath("$.data.typeLabel").value("乒乓球桌"))
                .andExpect(jsonPath("$.data.bookingUnit").value("TABLE"))
                .andExpect(jsonPath("$.data.bookingUnitLabel").value("桌"));

        mockMvc.perform(get("/api/v1/admin/resources").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.items[*].name", hasItem("乒乓球桌A")))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(10));
    }

    @Test
    void adminCanUpdateExistingResource() throws Exception {
        MockHttpSession session = login("admin01", "Campus@123");

        MvcResult created = mockMvc.perform(post("/api/v1/admin/resources")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"研讨室1","location":"图书馆3层","type":"DISCUSSION_ROOM","bookingUnit":"ROOM"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.bookingUnitLabel").value("整间房"))
                .andReturn();

        Number resourceId = JsonPath.read(created.getResponse().getContentAsString(), "$.data.id");

        mockMvc.perform(put("/api/v1/admin/resources/{id}", resourceId.longValue())
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"研讨室1-更新","location":"图书馆4层","type":"DISCUSSION_ROOM","bookingUnit":"ROOM"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("研讨室1-更新"))
                .andExpect(jsonPath("$.data.location").value("图书馆4层"));

        mockMvc.perform(put("/api/v1/admin/resources/{id}", 999999L)
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"不存在的资源","location":"图书馆","type":"DISCUSSION_ROOM","bookingUnit":"ROOM"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void mismatchedBookingUnitIsRejected() throws Exception {
        MockHttpSession session = login("admin01", "Campus@123");

        mockMvc.perform(post("/api/v1/admin/resources")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"乒乓球桌B","location":"北体育馆","type":"PING_PONG_TABLE","bookingUnit":"ROOM"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_BOOKING_UNIT"));
    }

    @Test
    void missingRequiredFieldsAreRejected() throws Exception {
        MockHttpSession session = login("admin01", "Campus@123");

        mockMvc.perform(post("/api/v1/admin/resources")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"  ","location":"","type":null,"bookingUnit":null}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.data.name").exists())
                .andExpect(jsonPath("$.data.location").exists())
                .andExpect(jsonPath("$.data.type").exists())
                .andExpect(jsonPath("$.data.bookingUnit").exists());
    }

    private MockHttpSession login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"%s","password":"%s"}
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        return session;
    }
}
