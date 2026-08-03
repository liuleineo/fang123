package com.fang123.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("school")
public class School implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "campus_code", type = IdType.INPUT)
    private String campusCode;

    private String schoolOrgCode;
    private String schoolOrgName;
    private String campusName;
    private String schoolAddress;
    private String contactPhone;
    private String schoolType;
    private String sponsorType;
    private Integer tier;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String eduAdminDepartment;
    private String schoolDistrictScope;
    private String schoolIntro;
    private String targetMiddleSchoolName;
    private String targetMiddleSchoolCode;
    private String communityNames;
    private String districtMapImage;
    private String mapFence;
    private String photos;
    private String schoolLogo;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
