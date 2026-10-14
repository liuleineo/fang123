package com.fang123.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fang123.entity.VisitEvent;
import com.fang123.mapper.VisitEventMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 访问统计的聚合与看板查询集成测试。
 * <p>依赖本地 MySQL（dev profile），测试数据用独立 device_id 标记，跑完即清理。
 */
@SpringBootTest
class VisitStatServiceTest {

    private static final String TEST_DEVICE = "junit-visit-device";

    @Autowired
    private VisitStatService visitStatService;

    @Autowired
    private VisitEventMapper visitEventMapper;

    @BeforeEach
    void setUp() {
        cleanTestData();
        insertEvent("/junit-test-page", "junit-session-1", 1000, LocalDateTime.now());
        insertEvent("/junit-test-page", "junit-session-1", 3000, LocalDateTime.now());
        insertEvent("/junit-other-page", "junit-session-2", 2000, LocalDateTime.now().minusMinutes(5));
    }

    @AfterEach
    void tearDown() {
        cleanTestData();
        // 清理后重聚合一次，把测试数据从日报里抹掉（聚合是幂等的）
        visitStatService.aggregate(LocalDate.now());
    }

    @Test
    @DisplayName("聚合 + 看板：结构完整、趋势按天补齐、热门页面可查")
    void dashboardShouldReturnStructuredData() {
        visitStatService.aggregate(LocalDate.now());

        Map<String, Object> data = visitStatService.dashboard(7);

        Map<String, Object> overview = castMap(data.get("overview"));
        assertThat(overview).containsKeys("todayPv", "todayUv", "todaySessions", "todayNewUv",
            "yesterdayPv", "yesterdayUv", "online");
        assertThat((Integer) overview.get("todayPv")).isGreaterThanOrEqualTo(3);
        assertThat((Integer) overview.get("todayUv")).isGreaterThanOrEqualTo(2);
        // 三个事件分属两个会话
        assertThat((Integer) overview.get("todaySessions")).isGreaterThanOrEqualTo(2);

        List<Map<String, Object>> trend = castList(data.get("trend"));
        assertThat(trend).hasSize(7);
        assertThat(trend.get(6).get("date")).isEqualTo(LocalDate.now().toString());
        // 缺失日期补 0，保证前端折线连续
        assertThat(trend).allSatisfy(item -> assertThat(item).containsKeys("pv", "uv", "sessions", "newUv"));

        List<Map<String, Object>> topPages = castList(data.get("topPages"));
        assertThat(topPages).anySatisfy(item -> assertThat(item.get("route")).isEqualTo("/junit-test-page"));
        Map<String, Object> testPage = topPages.stream()
            .filter(item -> "/junit-test-page".equals(item.get("route")))
            .findFirst()
            .orElseThrow();
        assertThat((Integer) testPage.get("pv")).isEqualTo(2);

        List<Map<String, Object>> platforms = castList(data.get("platforms"));
        assertThat(platforms).anySatisfy(item -> assertThat(item.get("platform")).isEqualTo("web"));
    }

    private void insertEvent(String route, String sessionId, int durationMs, LocalDateTime eventTime) {
        VisitEvent event = new VisitEvent();
        event.setVisitorKey("d:" + TEST_DEVICE);
        event.setDeviceId(TEST_DEVICE);
        event.setSessionId(sessionId);
        event.setPlatform("web");
        event.setEventType("page_view");
        event.setRoute(route);
        event.setPageTitle("集成测试页面");
        event.setReferrer("");
        event.setChannel("direct");
        event.setIpHash("");
        event.setUaBrief("Windows/Chrome");
        event.setDurationMs(durationMs);
        event.setEventTime(eventTime);
        visitEventMapper.insert(event);
    }

    private void cleanTestData() {
        visitEventMapper.delete(new LambdaQueryWrapper<VisitEvent>()
            .eq(VisitEvent::getDeviceId, TEST_DEVICE));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object value) {
        return (List<Map<String, Object>>) value;
    }
}
