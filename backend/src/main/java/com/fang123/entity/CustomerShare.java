package com.fang123.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("customer_shares")
public class CustomerShare implements Serializable {
    private static final long serialVersionUID = 1L;
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 客户ID */
    private Long customerId;
    /** 原拥有人 */
    private Long ownerUserId;
    /** 被共享人 */
    private Long sharedUserId;
    /** 共享时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
