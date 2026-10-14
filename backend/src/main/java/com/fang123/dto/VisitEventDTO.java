package com.fang123.dto;

import lombok.Data;

/**
 * 前端上报的单条访问事件。
 * <p>字段名与前端埋点脚本保持一致（ts 用毫秒时间戳，duration 用毫秒）。
 */
@Data
public class VisitEventDTO {

    /** 事件类型：page_view/click/login/order */
    private String type;

    /** 页面路径 */
    private String route;

    /** 页面标题 */
    private String title;

    /** 设备标识（前端 localStorage 生成） */
    private String deviceId;

    /** 会话 ID（前端 30 分钟滑窗生成） */
    private String sessionId;

    /** 平台：web/mini/android/ios/harmony */
    private String platform;

    /** 来源 URL */
    private String referrer;

    /** 渠道（前端优先判断：direct/internal/wechat/search，为空时后端按 referrer 兜底推导） */
    private String channel;

    /** 页面停留时长(ms) */
    private Long duration;

    /** 事件时间戳(ms) */
    private Long ts;
}
