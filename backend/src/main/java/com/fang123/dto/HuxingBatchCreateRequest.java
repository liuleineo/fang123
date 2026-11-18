package com.fang123.dto;

import lombok.Data;

import java.util.List;

/**
 * AI 批量录入户型请求：一次提交多条（含前端手工修改后的结果）
 */
@Data
public class HuxingBatchCreateRequest {

    /** 所有户型统一关联的楼盘ID */
    private Long loupanId;

    /** 待录入的户型字段 */
    private List<AiParseHuxingResult.HuxingFields> huxings;
}
