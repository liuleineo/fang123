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
        private Integer floorNo;
        private String roomNo;
        private BigDecimal area;
        private Integer recordUnitPrice;
        private Integer recordTotalPrice;
        private Integer saleUnitPrice;
        private Integer saleTotalPrice;
        private Integer houseStatus;
        private String orientation;
        private String remark;
    }
}
