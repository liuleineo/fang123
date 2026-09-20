package com.fang123.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.time.LocalDate;

/** 首页"预售公示 / 最新开盘"楼盘精简字段 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoupanSpotlightVO {
    private Long id;
    private String encodedId;
    private String projectName;
    private String coverImage;
    private String district;
    private String plate;
    private String brandList;
    private Integer avgUnitPrice;
    private Integer avgUnitPriceYangfang;
    private Integer avgUnitPriceDieshu;
    private Integer avgUnitPricePaiwu;
    private Integer areaMin;
    private Integer areaMax;
    private Integer minTotalPrice;
    private Integer maxTotalPrice;
    private Integer houseType;
    private Integer decorateType;
    private Integer salesStatus;
    private Integer buildingTotal;
    private String saleAddress;
    /** 楼盘自身开盘时间 */
    private LocalDate openingDate;
    /** 预售证-公示日期 */
    private LocalDate publicityDate;
    /** 预售证-核发日期 */
    private LocalDate issueDate;
    /** 预售证编号（STR 格式，如"杭房预许字(2026)第00564号"） */
    private String permitNoStr;
}
