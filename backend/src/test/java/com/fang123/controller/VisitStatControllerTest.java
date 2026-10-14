package com.fang123.controller;

import com.fang123.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 访问统计接口的 Web 层测试：确认上报接口匿名可访问、看板接口需要登录且返回结构稳定。
 */
@SpringBootTest
@AutoConfigureMockMvc
class VisitStatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    @DisplayName("上报接口匿名可访问，返回成功")
    void reportShouldBePubliclyAccessible() throws Exception {
        String body = "{\"events\":[{\"type\":\"page_view\",\"route\":\"/mockmvc-test\",\"title\":\"测试\","
            + "\"deviceId\":\"mockmvc-device\",\"sessionId\":\"mockmvc-session\",\"platform\":\"web\","
            + "\"ts\":" + System.currentTimeMillis() + "}]}";

        mockMvc.perform(post("/api/public/visit/report")
                .contentType(MediaType.APPLICATION_JSON)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/126.0")
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("看板接口未登录被拒，带管理员 token 返回完整结构")
    void dashboardShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/visit/dashboard"))
            .andExpect(status().is4xxClientError());

        String token = jwtUtil.generateAdminToken(1L, "admin");
        mockMvc.perform(get("/api/admin/visit/dashboard")
                .param("days", "7")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.overview.todayPv").exists())
            .andExpect(jsonPath("$.data.overview.online").exists())
            .andExpect(jsonPath("$.data.trend").isArray())
            .andExpect(jsonPath("$.data.trend.length()").value(7))
            .andExpect(jsonPath("$.data.platforms").isArray())
            .andExpect(jsonPath("$.data.topPages").isArray());
    }
}
