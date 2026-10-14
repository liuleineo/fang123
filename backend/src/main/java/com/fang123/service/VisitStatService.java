package com.fang123.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fang123.entity.VisitEvent;
import com.fang123.mapper.VisitEventMapper;
import com.fang123.mapper.VisitStatDailyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 访问统计的聚合与报表查询。
 * <p>聚合走"明细 → 日报"两层：管理端只查 {@code visit_stat_daily}，明细表再大也不影响报表响应。
 * 聚合语句是幂等的（ON DUPLICATE KEY UPDATE），所以当天数据可以每小时滚动刷新。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisitStatService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int TOP_ROUTE_LIMIT = 20;
    private static final int MAX_RANGE_DAYS = 90;
    private static final int REALTIME_WINDOW_MINUTES = 5;
    /** 明细保留天数，超期每天凌晨清理（报表依赖日报，删明细不影响历史报表） */
    private static final int RETENTION_DAYS = 90;
    /** 启动时回补的天数：服务停机几天时明细仍在，日报可以补齐，避免图表出现空洞 */
    private static final int STARTUP_BACKFILL_DAYS = 7;
    private static final int CLEAN_BATCH_SIZE = 5000;
    private static final int CLEAN_MAX_ROUNDS = 40;

    private final VisitStatDailyMapper statMapper;
    private final VisitEventMapper eventMapper;

    /** 启动后回补最近 7 天，避免停机期间（定时任务未触发）图表出现空洞 */
    @EventListener(ApplicationReadyEvent.class)
    public void aggregateOnStartup() {
        try {
            LocalDate today = LocalDate.now();
            for (int i = STARTUP_BACKFILL_DAYS - 1; i >= 0; i--) {
                aggregate(today.minusDays(i));
            }
            log.info("访问统计启动回补完成：{} ~ {}", today.minusDays(STARTUP_BACKFILL_DAYS - 1L), today);
        } catch (Exception e) {
            log.warn("启动时访问统计聚合失败（不影响启动）：{}", e.getMessage());
        }
    }

    /** 每小时第 5 分钟滚动聚合昨天与今天 */
    @Scheduled(cron = "0 5 * * * ?")
    public void aggregateHourly() {
        aggregateRecentDays();
    }

    /** 清理过期明细（报表数据已在日报里，删除明细不影响历史趋势） */
    @Scheduled(cron = "0 30 3 * * ?")
    public void cleanExpiredEvents() {
        LocalDateTime before = LocalDate.now().minusDays(RETENTION_DAYS).atStartOfDay();
        int total = 0;
        try {
            for (int round = 0; round < CLEAN_MAX_ROUNDS; round++) {
                List<VisitEvent> expired = eventMapper.selectList(new LambdaQueryWrapper<VisitEvent>()
                    .select(VisitEvent::getId)
                    .lt(VisitEvent::getEventTime, before)
                    .last("LIMIT " + CLEAN_BATCH_SIZE));
                if (expired.isEmpty()) {
                    break;
                }
                List<Long> ids = expired.stream().map(VisitEvent::getId).toList();
                total += eventMapper.deleteByIds(ids);
            }
            if (total > 0) {
                log.info("访问统计明细清理完成：删除 {} 条（{} 之前）", total, before.toLocalDate());
            }
        } catch (Exception e) {
            log.warn("访问统计明细清理失败：{}", e.getMessage());
        }
    }

    public void aggregateRecentDays() {
        LocalDate today = LocalDate.now();
        aggregate(today.minusDays(1));
        aggregate(today);
    }

    /** 聚合某一天；重复执行只覆盖，不累加 */
    public void aggregate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        try {
            statMapper.upsertOverallTotal(start, end);
            statMapper.upsertPlatformTotal(start, end);
            statMapper.upsertRoute(start, end);
            statMapper.upsertNewVisitor(date, start, end);
        } catch (Exception e) {
            log.warn("访问统计聚合失败 date={}: {}", date, e.getMessage());
        }
    }

    /** 管理端看板：概览 + 趋势 + 平台分布 + 热门页面 */
    public Map<String, Object> dashboard(int days) {
        int range = Math.min(Math.max(days, 1), MAX_RANGE_DAYS);
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(range - 1L);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("overview", buildOverview(today));
        data.put("trend", buildTrend(from, today));
        data.put("platforms", buildPlatforms(from));
        data.put("topPages", buildTopPages(from));
        data.put("rangeDays", range);
        data.put("fromDate", from.format(DATE_FMT));
        data.put("toDate", today.format(DATE_FMT));
        return data;
    }

    private Map<String, Object> buildOverview(LocalDate today) {
        Map<String, Object> todayRow = statMapper.selectOneDay(today);
        Map<String, Object> yesterdayRow = statMapper.selectOneDay(today.minusDays(1));

        Map<String, Object> overview = new LinkedHashMap<>();
        overview.put("todayPv", intCell(todayRow, "pv"));
        overview.put("todayUv", intCell(todayRow, "uv"));
        overview.put("todaySessions", intCell(todayRow, "session_cnt"));
        overview.put("todayNewUv", intCell(todayRow, "new_uv"));
        overview.put("todayAvgDurationMs", intCell(todayRow, "avg_duration_ms"));
        overview.put("yesterdayPv", intCell(yesterdayRow, "pv"));
        overview.put("yesterdayUv", intCell(yesterdayRow, "uv"));
        overview.put("yesterdaySessions", intCell(yesterdayRow, "session_cnt"));
        try {
            overview.put("online", eventMapper.countOnlineSince(
                LocalDateTime.now().minusMinutes(REALTIME_WINDOW_MINUTES)));
        } catch (Exception e) {
            overview.put("online", 0);
        }
        return overview;
    }

    /** 趋势序列补齐缺失日期，避免前端折线断裂 */
    private List<Map<String, Object>> buildTrend(LocalDate from, LocalDate to) {
        Map<String, Map<String, Object>> byDate = new TreeMap<>();
        for (Map<String, Object> row : statMapper.selectTrend(from)) {
            byDate.put(strCell(row, "stat_date"), row);
        }

        List<Map<String, Object>> trend = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            String key = date.format(DATE_FMT);
            Map<String, Object> row = byDate.get(key);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", key);
            item.put("pv", intCell(row, "pv"));
            item.put("uv", intCell(row, "uv"));
            item.put("sessions", intCell(row, "session_cnt"));
            item.put("newUv", intCell(row, "new_uv"));
            item.put("avgDurationMs", intCell(row, "avg_duration_ms"));
            trend.add(item);
        }
        return trend;
    }

    private List<Map<String, Object>> buildPlatforms(LocalDate from) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> row : statMapper.selectPlatformStats(from)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("platform", strCell(row, "platform"));
            item.put("pv", intCell(row, "pv"));
            item.put("uv", intCell(row, "uv"));
            list.add(item);
        }
        return list;
    }

    private List<Map<String, Object>> buildTopPages(LocalDate from) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> row : statMapper.selectTopRoutes(from, TOP_ROUTE_LIMIT)) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("route", strCell(row, "route"));
            item.put("pv", intCell(row, "pv"));
            item.put("uv", intCell(row, "uv"));
            item.put("avgDurationMs", intCell(row, "avg_duration_ms"));
            list.add(item);
        }
        return list;
    }

    /** 取单元格，忽略列名大小写差异（不同驱动返回的 label 大小写不一致） */
    private Object cell(Map<String, Object> row, String key) {
        if (row == null) {
            return null;
        }
        Object value = row.get(key);
        if (value != null) {
            return value;
        }
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private int intCell(Map<String, Object> row, String key) {
        Object value = cell(row, key);
        return value instanceof Number number ? number.intValue() : 0;
    }

    private String strCell(Map<String, Object> row, String key) {
        Object value = cell(row, key);
        return value == null ? "" : String.valueOf(value);
    }
}
