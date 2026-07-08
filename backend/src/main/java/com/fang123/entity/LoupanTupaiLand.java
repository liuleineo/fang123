package com.fang123.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("loupan_tupai_land")
public class LoupanTupaiLand implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long loupanId;
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
    private LocalDate dealDate;
    /** 经度 */
    private BigDecimal longitude;
    /** 纬度 */
    private BigDecimal latitude;
    /** 资源ID */
    private String resourceId;
    /** 位置示意图URL */
    private String locationImage;
    private Integer sort;
    @TableLogic(value = "0", delval = "1")
    @TableField("deleted")
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
