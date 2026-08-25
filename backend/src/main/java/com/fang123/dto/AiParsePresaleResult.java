package com.fang123.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class AiParsePresaleResult {
    private List<String> imageUrls;
    private String ocrText;
    private PresaleFields fields;

    @Data
    public static class PresaleFields {
        private String projectName;
        private String permitNo;
        private String permitNoStr;
        private String developCompany;
        private String publicityDate;
        private String issueDate;
        private String location;
        private String saleAddress;
        private String salePhone;
        private BigDecimal onlineSaleArea;
    }
}
