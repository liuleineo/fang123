package com.fang123.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class AiParseYfyjResult {
    private List<String> imageUrls;
    private String ocrText;
    private List<YfyjFields> yfyjList;

    @Data
    public static class YfyjFields {
        private String buildingNo;
        private String unitNo;
        private String roomNo;
        private BigDecimal area;
        private Integer recordUnitPrice;
        private Integer recordTotalPrice;
        private Integer houseStatus;
        private String remark;
    }
}
