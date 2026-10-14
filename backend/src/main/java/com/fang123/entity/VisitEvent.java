package com.fang123.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 访问事件明细。
 * <p>只追加、不更新；报表不查这张表（走 visit_stat_daily），避免明细膨胀拖慢主库。
 * 不使用 BaseEntity 是因为这里没有逻辑删除语义，且 create_time 由数据库默认值填充。
 */
@Data
@TableName("visit_event")
public class VisitEvent implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 访客标识：登录为 u:{userId}，匿名为 d:{deviceId}，用于 UV 去重 */
    private String visitorKey;

    private Long userId;

    private String deviceId;

    private String sessionId;

    /** web/mini/android/ios/harmony */
    private String platform;

    /** page_view/click/login/order */
    private String eventType;

    private String route;

    private String pageTitle;

    private String referrer;

    private String channel;

    /** IP 的 MD5，不落明文 */
    private String ipHash;

    private String uaBrief;

    private Integer durationMs;

    /** 事件发生时间（客户端时间经服务端范围校正） */
    private LocalDateTime eventTime;

    private LocalDateTime createTime;
}
