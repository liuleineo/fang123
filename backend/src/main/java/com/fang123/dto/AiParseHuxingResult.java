package com.fang123.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * AI 解析户型资料图片的结果
 */
@Data
public class AiParseHuxingResult {

    /** 上传的图片 URL 列表 */
    private List<String> imageUrls;

    /** OCR 识别的原始文本 */
    private String ocrText;

    /** 解析出的户型列表 */
    private List<HuxingFields> huxings;

    @Data
    public static class HuxingFields {
        private String huxingName;
        /** 建筑面积㎡，可能是小数 */
        private BigDecimal area;
        private BigDecimal insideArea;
        private Integer roomNum;
        private Integer hallNum;
        private Integer toiletNum;
        private Integer balconyNum;
        private String orientation;
        private Integer floorType;
        /** 单价，可能是小数 */
        private BigDecimal unitPrice;
        private Integer totalPriceStart;
        private Integer totalPriceEnd;
        private Integer isShowHouse;
        private String tag;
    }
}
