package com.fang123.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("loupan_presale_permit")
public class LoupanPresalePermit implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long loupanId;
    private String projectName;
    private LocalDate publicityEndDate;
    private String developCompany;
    private String location;
    private String saleAddress;
    private String salePhone;
    private BigDecimal onlineSaleArea;
    private String permitNo;
    private String permitNoStr;
    @TableLogic(value = "0", delval = "1")
    @TableField("deleted")
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
