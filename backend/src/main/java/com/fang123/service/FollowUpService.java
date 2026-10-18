package com.fang123.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.fang123.entity.FollowUp;

import java.time.LocalDateTime;

public interface FollowUpService extends IService<FollowUp> {

    /** 统计时间区间内被跟进过的客户数（去重） */
    long countDistinctCustomersBetween(LocalDateTime start, LocalDateTime end);
}
