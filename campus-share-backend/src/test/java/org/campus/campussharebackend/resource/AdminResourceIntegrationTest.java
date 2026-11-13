package org.campus.campussharebackend.resource;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.campus.campussharebackend.support.IntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * XIA-2 资源维护接口：管理员新增/修改资源，其他角色被拒绝。
 * 新增的测试资源在每个用例后清理，避免影响其他测试类的列表断言。
 */
@WithMockUser(username = "admin01", roles = "RESOURCE_ADMIN")
class AdminResourceIntegrationTest extends IntegrationTestBase {

    private static final String CREATE_BODY = """
            {"name":"网球场2号","location":"室外运动场","resourceType":"TENNIS","unitLabel":"2号场","enabled":true}
            """;

    @AfterEach
    void removeCreatedResources() {
        jdbcTemplate.update("UPDATE resource SET name = ? WHERE id = 201", "体育馆1号羽毛球场");
        jdbcTemplate.update("DELETE FROM resource WHERE id NOT IN (101, 102, 201, 401)");
    }

    @Test
    void createsResourceAndListsIt() throws Exception {
        mockMvc.perform(post("/api/v1/admin/resources").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("OK"))
                .andExpect(jsonPath("$.data.resourceId").isNotEmpty())
                .andExpect(jsonPath("$.data.name").value("网球场2号"))
                .andExpect(jsonPath("$.data.unitLabel").value("2号场"))
                .andExpect(jsonPath("$.data.enabled").value(true));

        mockMvc.perform(get("/api/v1/admin/resources").param("keyword", "网球场2号"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.items[0].resourceType").value("TENNIS"));
    }

    @Test
    void updatesResourceAndHidesDisabledFromPublicList() throws Exception {
        mockMvc.perform(put("/api/v1/admin/resources/201").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"体育馆1号羽毛球场（检修）","location":"体育馆二楼",
                                 "resourceType":"BADMINTON","unitLabel":"1号场","enabled":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resourceId").value("201"))
                .andExpect(jsonPath("$.data.name").value("体育馆1号羽毛球场（检修）"))
                .andExpect(jsonPath("$.data.enabled").value(false));

        mockMvc.perform(get("/api/v1/resources"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3));
        mockMvc.perform(get("/api/v1/admin/resources").param("keyword", "检修"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1));
    }

    @Test
    void rejectsInvalidBodyAndUnknownResource() throws Exception {
        mockMvc.perform(post("/api/v1/admin/resources").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","location":"体育馆","resourceType":"NOT_A_TYPE",
                                 "unitLabel":"X","enabled":true}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));

        mockMvc.perform(put("/api/v1/admin/resources/999999").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void clampsPaginationToContractBounds() throws Exception {
        mockMvc.perform(get("/api/v1/admin/resources").param("page", "0").param("size", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(100))
                .andExpect(jsonPath("$.data.total").value(4));
    }

    @Test
    @WithMockUser(username = "student01", roles = "STUDENT")
    void rejectsNonAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/resources"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mockMvc.perform(post("/api/v1/admin/resources").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(CREATE_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/resources"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
    }
}
