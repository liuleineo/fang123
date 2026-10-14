package com.fang123.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fang123.entity.VisitStatDaily;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 访问统计日报。
 * <p>聚合统一走 {@code INSERT ... SELECT ... ON DUPLICATE KEY UPDATE}，天然幂等：
 * 重跑某一天只会覆盖该天的统计值，不会重复累加（当日数据可以每小时滚动刷新）。
 */
@Mapper
public interface VisitStatDailyMapper extends BaseMapper<VisitStatDaily> {

    /** 各平台当日汇总（platform=具体平台，dimension=total） */
    @Insert("""
        INSERT INTO visit_stat_daily (stat_date, platform, dimension, dim_value, pv, uv, session_cnt, new_uv, avg_duration_ms)
        SELECT DATE(event_time), platform, 'total', 'all',
               COUNT(*), COUNT(DISTINCT visitor_key), COUNT(DISTINCT session_id), 0,
               IFNULL(ROUND(AVG(duration_ms)), 0)
        FROM visit_event
        WHERE event_time >= #{start} AND event_time < #{end}
        GROUP BY DATE(event_time), platform
        ON DUPLICATE KEY UPDATE
            pv = VALUES(pv), uv = VALUES(uv), session_cnt = VALUES(session_cnt),
            avg_duration_ms = VALUES(avg_duration_ms), update_time = NOW()
        """)
    int upsertPlatformTotal(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** 跨平台汇总（platform=all，dimension=total） */
    @Insert("""
        INSERT INTO visit_stat_daily (stat_date, platform, dimension, dim_value, pv, uv, session_cnt, new_uv, avg_duration_ms)
        SELECT DATE(event_time), 'all', 'total', 'all',
               COUNT(*), COUNT(DISTINCT visitor_key), COUNT(DISTINCT session_id), 0,
               IFNULL(ROUND(AVG(duration_ms)), 0)
        FROM visit_event
        WHERE event_time >= #{start} AND event_time < #{end}
        GROUP BY DATE(event_time)
        ON DUPLICATE KEY UPDATE
            pv = VALUES(pv), uv = VALUES(uv), session_cnt = VALUES(session_cnt),
            avg_duration_ms = VALUES(avg_duration_ms), update_time = NOW()
        """)
    int upsertOverallTotal(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /** 页面维度（dimension=route） */
    @Insert("""
        INSERT INTO visit_stat_daily (stat_date, platform, dimension, dim_value, pv, uv, session_cnt, new_uv, avg_duration_ms)
        SELECT DATE(event_time), platform, 'route', route,
               COUNT(*), COUNT(DISTINCT visitor_key), COUNT(DISTINCT session_id), 0,
               IFNULL(ROUND(AVG(duration_ms)), 0)
        FROM visit_event
        WHERE event_time >= #{start} AND event_time < #{end} AND route <> ''
        GROUP BY DATE(event_time), platform, route
        ON DUPLICATE KEY UPDATE
            pv = VALUES(pv), uv = VALUES(uv), session_cnt = VALUES(session_cnt),
            avg_duration_ms = VALUES(avg_duration_ms), update_time = NOW()
        """)
    int upsertRoute(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    /**
     * 新访客数：访客首次出现落在统计日即为新访客。
     * <p>注意这里是全表按 visitor_key 取 MIN(event_time)，数据量到千万级后应改为维护
     * visitor_first_seen 表，目前日增十万以内可以接受。
     */
    @Insert("""
        INSERT INTO visit_stat_daily (stat_date, platform, dimension, dim_value, pv, uv, session_cnt, new_uv, avg_duration_ms)
        SELECT #{date}, 'all', 'total', 'all', 0, 0, 0, COUNT(*), 0
        FROM (
            SELECT visitor_key, MIN(event_time) AS first_time
            FROM visit_event
            WHERE visitor_key <> ''
            GROUP BY visitor_key
        ) t
        WHERE t.first_time >= #{start} AND t.first_time < #{end}
        ON DUPLICATE KEY UPDATE new_uv = VALUES(new_uv), update_time = NOW()
        """)
    int upsertNewVisitor(@Param("date") LocalDate date,
                         @Param("start") LocalDateTime start,
                         @Param("end") LocalDateTime end);

    // ---------- 报表查询（统一用小写下划线别名，由 Service 组装成对前端稳定的结构） ----------

    @Select("""
        SELECT stat_date, pv, uv, session_cnt, new_uv, avg_duration_ms
        FROM visit_stat_daily
        WHERE platform = 'all' AND dimension = 'total' AND stat_date >= #{fromDate}
        ORDER BY stat_date
        """)
    List<Map<String, Object>> selectTrend(@Param("fromDate") LocalDate fromDate);

    @Select("""
        SELECT stat_date, pv, uv, session_cnt, new_uv, avg_duration_ms
        FROM visit_stat_daily
        WHERE platform = 'all' AND dimension = 'total' AND stat_date = #{date}
        """)
    Map<String, Object> selectOneDay(@Param("date") LocalDate date);

    @Select("""
        SELECT platform, SUM(pv) AS pv, SUM(uv) AS uv
        FROM visit_stat_daily
        WHERE platform <> 'all' AND dimension = 'total' AND stat_date >= #{fromDate}
        GROUP BY platform
        ORDER BY pv DESC
        """)
    List<Map<String, Object>> selectPlatformStats(@Param("fromDate") LocalDate fromDate);

    @Select("""
        SELECT dim_value AS route, SUM(pv) AS pv, SUM(uv) AS uv,
               IFNULL(ROUND(AVG(NULLIF(avg_duration_ms, 0))), 0) AS avg_duration_ms
        FROM visit_stat_daily
        WHERE dimension = 'route' AND stat_date >= #{fromDate}
        GROUP BY dim_value
        ORDER BY pv DESC
        LIMIT #{limit}
        """)
    List<Map<String, Object>> selectTopRoutes(@Param("fromDate") LocalDate fromDate, @Param("limit") int limit);
}
