package com.fang123.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class AiParseTupaiLandResult {
    private List<String> imageUrls;
    private String ocrText;
    private TupaiLandFields fields;

    @Data
    public static class TupaiLandFields {
        private String landName;
        private String landNo;
        private String district;
        private String plate;
        private String landScope;
        private Integer landStatus;
        private String winnerCompany;
        private String landUseType;
        private Integer landYear;
        private BigDecimal landArea;
        private BigDecimal buildAreaLimit;
        private BigDecimal plotRatio;
        private Long startPrice;
        private Long dealPrice;
        private Integer floorUnitPrice;
        private BigDecimal premiumRate;
        private String dealDate;
        private BigDecimal longitude;
        private BigDecimal latitude;
    }
}
