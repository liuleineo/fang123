package com.fang123.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fang123.dto.VisitEventDTO;
import com.fang123.entity.VisitEvent;

import java.util.List;

public interface VisitEventService extends IService<VisitEvent> {

    /**
     * 接收前端上报的访问事件。
     * <p>方法只做校验与入队，立即返回；落库由独立线程批量完成，不占用业务请求线程。
     *
     * @param events    事件列表（超出上限只取前 N 条）
     * @param userId    登录用户 ID，匿名为 null
     * @param ip        客户端 IP（内部做哈希，不存明文）
     * @param userAgent 客户端 UA（用于过滤爬虫、生成摘要）
     */
    void report(List<VisitEventDTO> events, Long userId, String ip, String userAgent);
}
