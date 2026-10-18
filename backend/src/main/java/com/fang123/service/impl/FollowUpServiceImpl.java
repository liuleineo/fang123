package com.fang123.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fang123.entity.FollowUp;
import com.fang123.mapper.FollowUpMapper;
import com.fang123.service.FollowUpService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class FollowUpServiceImpl extends ServiceImpl<FollowUpMapper, FollowUp> implements FollowUpService {

    @Override
    public long countDistinctCustomersBetween(LocalDateTime start, LocalDateTime end) {
        Long count = baseMapper.countDistinctCustomersBetween(start, end);
        return count == null ? 0L : count;
    }
}
