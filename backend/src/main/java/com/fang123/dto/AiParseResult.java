package com.fang123.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * AI 解析楼盘资料图片的结果
 */
@Data
public class AiParseResult {

    /** 上传的图片 URL 列表 */
    private List<String> imageUrls;

    /** OCR 识别的原始文本 */
    private String ocrText;

    /** 解析出的楼盘字段 */
    private ParsedFields fields;

    @Data
    public static class ParsedFields {
        private String projectName;
        private String shortName;
        private String district;
        private String plate;
        private Integer avgUnitPrice;
        private Integer areaMin;
        private Integer areaMax;
        private Long buildArea;
        private Long landArea;
        private String projectCompany;
        private String brandList;
        private String projectAddress;
        private String salesAddress;
        private Integer houseType;
        private Integer decorateType;
        private Integer buildingTotal;
        private Integer houseTotal;
        private BigDecimal greenRate;
        private BigDecimal plotRatio;
        private String propertyCompany;
        private String deliveryDate;
        private String salesTel;
        private Integer propertyRightYear;
        private Integer minTotalPrice;
        private Integer maxTotalPrice;
        private String priceTag;
        private Long landPrice;
        private Integer landUnitPrice;
        private BigDecimal propertyFeeHigh;
        private BigDecimal propertyFeeVilla;
        private Integer parkTotal;
        private Integer parkSellNum;
        private String parkRatio;
        private String facadeMaterial;
        private BigDecimal selfHoldRate;
        private BigDecimal floorHeightMin;
        private BigDecimal floorHeightMax;
        private Integer floorMin;
        private Integer floorMax;
        private BigDecimal longitude;
        private BigDecimal latitude;
        private String eduSupport;
        private String trafficSupport;
        private String medicalSupport;
        private String businessSupport;
        private String viewSupport;
        private String communityFacility;
        private String showHouseDesc;
    }
}
