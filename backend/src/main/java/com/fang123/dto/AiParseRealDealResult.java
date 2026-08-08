package com.fang123.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AiParseRealDealResult {

    private RealDealFields fields;
    private String rawText;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RealDealFields {
        /** 小区名称 */
        private String communityName;
        /** 房号 */
        private String roomNo;
        /** 房源面积，㎡ */
        private Double houseArea;
        /** 成交价格，万元 */
        private Double dealPrice;
        /** 备注（是否带车位等） */
        private String remark;
        /** 成交日期，格式 YYYY-MM-DD */
        private String dealDate;
        /** 行政区 */
        private String district;
        /** 板块 */
        private String plate;
        /** 楼盘ID（若文本包含） */
        private Long loupanId;
        /** 一手买入价格，万元（可选） */
        private Double yfyj;
    }
}
