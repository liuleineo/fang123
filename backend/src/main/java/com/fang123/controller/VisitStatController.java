package com.fang123.controller;

import com.fang123.common.Result;
import com.fang123.service.VisitStatService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/** 管理端访问统计接口 */
@RestController
@RequestMapping("/api/admin/visit")
@RequiredArgsConstructor
public class VisitStatController {

    private final VisitStatService visitStatService;

    /**
     * 访问统计看板：概览 + 趋势 + 平台分布 + 热门页面。
     *
     * @param days 统计天数（1-90，默认 30）
     */
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard(@RequestParam(defaultValue = "30") int days) {
        return Result.success(visitStatService.dashboard(days));
    }

    /** 手动聚合某天数据（默认今天），定时任务漏跑时用于补数 */
    @PostMapping("/aggregate")
    public Result<Map<String, Object>> aggregate(@RequestParam(required = false) String date) {
        LocalDate target = StringUtils.hasText(date) ? LocalDate.parse(date) : LocalDate.now();
        visitStatService.aggregate(target);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("date", target.toString());
        data.put("msg", "聚合完成");
        return Result.success(data);
    }
}
