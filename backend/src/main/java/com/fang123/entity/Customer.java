package com.fang123.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("customers")
public class Customer implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 客户姓名 */
    private String name;
    /** 手机号 */
    private String phone;
    /** 备注 */
    private String remark;
    /** 意向：高/中/低 */
    private String intention;
    /** 所属用户id（关联 user_info.id） */
    private Long userId;
    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    /** 客户头像图片地址 */
    private String photo;
    /** 所属用户昵称（非表字段，列表展示用） */
    @TableField(exist = false)
    private String userNickname;
    /** 所属用户手机号（非表字段，列表展示用） */
    @TableField(exist = false)
    private String userPhone;
    /** 被分享人昵称列表，逗号分隔（非表字段，列表展示用） */
    @TableField(exist = false)
    private String sharedTo;
    /** 分享关系描述：分享给了谁 / 被谁分享（非表字段，C端列表展示用） */
    @TableField(exist = false)
    private String shareDesc;
    /** 最后一次跟进时间（非表字段，列表展示用） */
    @TableField(exist = false)
    private LocalDateTime lastFollowUpTime;
    /** 最后一次跟进内容（非表字段，列表展示用） */
    @TableField(exist = false)
    private String lastFollowUpContent;
}
