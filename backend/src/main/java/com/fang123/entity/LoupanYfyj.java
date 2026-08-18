package com.fang123.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("loupan_yfyj")
public class LoupanYfyj implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long loupanId;
    private Long huxingId;
    /** 预售许可证编号 */
    private String permitNo;
    /** 房屋编码 */
    private String fwcode;
    /** 楼栋号 */
    private String buildingNo;
    /** 单元号 */
    private String unitNo;
    private String roomNo;
    private BigDecimal area;
    private Integer recordUnitPrice;
    private Integer recordTotalPrice;
    private Integer houseStatus;
    private String remark;
    private Integer sort;
    @TableLogic(value = "0", delval = "1")
    @TableField("deleted")
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
