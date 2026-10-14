package com.fang123.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 访问统计日报（预聚合）。
 * <p>维度约定：
 * <ul>
 *   <li>platform=all + dimension=total：跨平台汇总行，报表的"总计"取这一行</li>
 *   <li>platform=web 等 + dimension=total：各平台当日汇总</li>
 *   <li>platform=web 等 + dimension=route：各页面当日汇总</li>
 * </ul>
 */
@Data
@TableName("visit_stat_daily")
public class VisitStatDaily implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private LocalDate statDate;

    private String platform;

    private String dimension;

    private String dimValue;

    private Integer pv;

    private Integer uv;

    private Integer sessionCnt;

    /** 新访客数，仅 platform=all 的 total 行有效 */
    private Integer newUv;

    private Integer avgDurationMs;

    private LocalDateTime updateTime;
}
