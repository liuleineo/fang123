package com.fang123.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("follow_ups")
public class FollowUp implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 客户id */
    private Long customerId;
    /** 跟进方式 */
    private String method;
    /** 跟进内容 */
    private String content;
    /** 跟进时间 */
    private LocalDateTime followUpTime;
    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /** 跟进图片数组，存图片url列表 */
    private String photos;
    /** 操作人id */
    private Long userId;
    /** 操作人昵称（非表字段） */
    @TableField(exist = false)
    private String userNickname;
}
