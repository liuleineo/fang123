package com.fang123.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 土拍地块公开接口精简字段 */
@Data
public class TupaiLandPublicVO {
    private Long id;
    private String landName;
    private String landNo;
    private String district;
    private String plate;
    private Integer landStatus;
    private String winnerCompany;
    private BigDecimal landArea;
    private Long dealPrice;
    /** 楼面价（元/㎡） */
    private Integer floorUnitPrice;
    /** 溢价率（%） */
    private BigDecimal premiumRate;
    private LocalDate dealDate;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String locationImage;
}
