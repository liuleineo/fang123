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
 * 用户端今日跟进统计接口：返回今日跟进客户数/次数/每日目标，供客户页弹窗使用。
 */
@SpringBootTest
@AutoConfigureMockMvc
class UserCustomerFollowStatsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    @DisplayName("跟进统计：未登录拒绝，登录后返回 target=10")
    void followStats() throws Exception {
        mockMvc.perform(get("/api/user/customers/follow-stats"))
            .andExpect(status().is4xxClientError());

        String token = jwtUtil.generateToken(1L, "13800138000");
        mockMvc.perform(get("/api/user/customers/follow-stats")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.target").value(10))
            .andExpect(jsonPath("$.data.todayCustomerCount").exists())
            .andExpect(jsonPath("$.data.todayFollowCount").exists());
    }
}
