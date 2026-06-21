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
}
