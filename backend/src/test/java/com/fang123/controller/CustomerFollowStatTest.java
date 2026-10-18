package com.fang123.controller;

import com.fang123.util.JwtUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 今日跟进进度接口：确认路径不会被 /{id} 抢占，且返回结构与目标值正确。
 */
@SpringBootTest
@AutoConfigureMockMvc
class CustomerFollowStatTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    @DisplayName("今日跟进统计：未登录拒绝，登录后返回 count/target")
    void todayFollowStats() throws Exception {
        mockMvc.perform(get("/api/admin/customers/today-follow-stats"))
            .andExpect(status().is4xxClientError());

        String token = jwtUtil.generateAdminToken(1L, "admin");
        mockMvc.perform(get("/api/admin/customers/today-follow-stats")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.count").exists())
            .andExpect(jsonPath("$.data.target").value(10));
    }
}
