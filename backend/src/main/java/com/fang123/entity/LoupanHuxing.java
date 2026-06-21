package com.fang123.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("loupan_huxing")
public class LoupanHuxing implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long loupanId;
    private String huxingName;
    private Integer area;
    private Integer insideArea;
    private Integer roomNum;
    private Integer hallNum;
    private Integer toiletNum;
    private Integer balconyNum;
    private String orientation;
    private Integer floorType;
    private Integer unitPrice;
    private Integer totalPriceStart;
    private Integer totalPriceEnd;
    private Integer isShowHouse;
    private String tag;
    private Integer sort;
    @TableLogic(value = "0", delval = "1")
    @TableField("deleted")
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
