package com.fang123.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("loupan_dynamic")
public class LoupanDynamic implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 楼盘ID */
    private Long loupanId;
    /** 动态标题 */
    private String title;
    /** 动态内容 */
    private String content;
    /** 动态类型：1建设动态 2销售动态 3优惠动态 */
    private Integer type;
    /** 动态图片URL列表（逗号分隔） */
    private String images;
    @TableLogic(value = "0", delval = "1")
    @TableField("deleted")
    private Integer deleted;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
