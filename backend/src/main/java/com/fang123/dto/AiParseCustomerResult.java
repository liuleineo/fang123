package com.fang123.dto;

import lombok.Data;

/**
 * AI 识别客户资料图片的结果
 */
@Data
public class AiParseCustomerResult {

    /** OCR 识别的原始文本 */
    private String ocrText;

    /** 解析出的客户字段 */
    private CustomerFields fields;

    @Data
    public static class CustomerFields {
        /** 客户姓名 */
        private String name;
        /** 手机号 */
        private String phone;
        /** 意向：高/中/低 */
        private String intention;
        /** 备注/购房需求 */
        private String remark;
    }
}
