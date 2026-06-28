package com.fang123.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("loupan")
public class Loupan implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long landId;
    /** 宗地编号 */
    private String landNo;
    private String coverImage;
    private String projectName;
    private String shortName;
    private String district;
    private String plate;
    private Integer avgUnitPrice;
    private Integer minTotalPrice;
    private Integer maxTotalPrice;
    private String priceTag;
    private String salesAddress;
    private Integer salesStatus;
    private String salesTel;
    private String projectAddress;
    private String showHouseDesc;
    private LocalDate deliveryDate;
    private BigDecimal floorHeightMin;
    private BigDecimal floorHeightMax;
    private Integer buildingTotal;
    private Integer floorMin;
    private Integer floorMax;
    private Integer areaMin;
    private Integer areaMax;
    private Integer decorateType;
    private Integer propertyRightYear;
    private Integer houseType;
    private String communityFacility;
    private Integer peopleCarSeparate;
    private BigDecimal propertyFeeHigh;
    private BigDecimal propertyFeeVilla;
    private String propertyCompany;
    private Integer parkTotal;
    private Integer parkSellNum;
    private String parkRatio;
    private String facadeMaterial;
    private BigDecimal selfHoldRate;
    private Long buildArea;
    private Long landArea;
    private Integer houseTotal;
    private BigDecimal plotRatio;
    private BigDecimal greenRate;
    private String projectCompany;
    private String brandList;
    private Long landPrice;
    private Integer landUnitPrice;
    private LocalDate landBuyDate;
    private String eduSupport;
    private String trafficSupport;
    private String medicalSupport;
    private String businessSupport;
    private String viewSupport;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer sort;
    @TableLogic(value = "0", delval = "1")
    @TableField("deleted")
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String encodedId;

    /** 字段完善比例 (0-100)，非数据库字段 */
    @TableField(exist = false)
    private Integer completionRatio;

    /**
     * 计算字段完善比例，遍历业务字段统计非空比例
     */
    public void computeCompletionRatio() {
        int total = 0, filled = 0;

        // 字符串字段
        filled += isFilled(projectName); total++;
        filled += isFilled(shortName); total++;
        filled += isFilled(district); total++;
        filled += isFilled(plate); total++;
        filled += isFilled(landNo); total++;
        filled += isFilled(coverImage); total++;
        filled += isFilled(priceTag); total++;
        filled += isFilled(salesAddress); total++;
        filled += isFilled(salesTel); total++;
        filled += isFilled(projectAddress); total++;
        filled += isFilled(showHouseDesc); total++;
        filled += isFilled(communityFacility); total++;
        filled += isFilled(propertyCompany); total++;
        filled += isFilled(parkRatio); total++;
        filled += isFilled(facadeMaterial); total++;
        filled += isFilled(projectCompany); total++;
        filled += isFilled(brandList); total++;
        filled += isFilled(eduSupport); total++;
        filled += isFilled(trafficSupport); total++;
        filled += isFilled(medicalSupport); total++;
        filled += isFilled(businessSupport); total++;
        filled += isFilled(viewSupport); total++;

        // 数值/日期字段 — 非 null 即算已填
        filled += isFilled(landId); total++;
        filled += isFilled(avgUnitPrice); total++;
        filled += isFilled(minTotalPrice); total++;
        filled += isFilled(maxTotalPrice); total++;
        filled += isFilled(salesStatus); total++;
        filled += isFilled(deliveryDate); total++;
        filled += isFilled(floorHeightMin); total++;
        filled += isFilled(floorHeightMax); total++;
        filled += isFilled(buildingTotal); total++;
        filled += isFilled(floorMin); total++;
        filled += isFilled(floorMax); total++;
        filled += isFilled(areaMin); total++;
        filled += isFilled(areaMax); total++;
        filled += isFilled(decorateType); total++;
        filled += isFilled(propertyRightYear); total++;
        filled += isFilled(houseType); total++;
        filled += isFilled(peopleCarSeparate); total++;
        filled += isFilled(propertyFeeHigh); total++;
        filled += isFilled(propertyFeeVilla); total++;
        filled += isFilled(parkTotal); total++;
        filled += isFilled(parkSellNum); total++;
        filled += isFilled(selfHoldRate); total++;
        filled += isFilled(buildArea); total++;
        filled += isFilled(landArea); total++;
        filled += isFilled(houseTotal); total++;
        filled += isFilled(plotRatio); total++;
        filled += isFilled(greenRate); total++;
        filled += isFilled(landPrice); total++;
        filled += isFilled(landUnitPrice); total++;
        filled += isFilled(landBuyDate); total++;
        filled += isFilled(longitude); total++;
        filled += isFilled(latitude); total++;
        filled += isFilled(sort); total++;

        this.completionRatio = total > 0 ? (int) Math.round(filled * 100.0 / total) : 0;
    }

    private static int isFilled(String s) { return s != null && !s.isEmpty() ? 1 : 0; }
    private static int isFilled(Object o) { return o != null ? 1 : 0; }
}
