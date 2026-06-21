package com.fang123.dto;

import lombok.Data;

@Data
public class UserUpdateRequest {

    private String nickname;

    private String avatar;

    private String phone;

    private Integer sex;

    private Integer status;
}
