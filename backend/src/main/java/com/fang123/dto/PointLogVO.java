package com.fang123.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PointLogVO {

    private Long id;
    private Long userId;
    private String nickname;
    private String phone;
    private Integer point;
    private Integer balance;
    private Integer type;
    private String source;
    private String remark;
    private LocalDateTime createTime;
}
