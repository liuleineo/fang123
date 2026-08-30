package com.fang123.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fang123.entity.FollowUp;
import com.fang123.mapper.FollowUpMapper;
import com.fang123.service.FollowUpService;
import org.springframework.stereotype.Service;

@Service
public class FollowUpServiceImpl extends ServiceImpl<FollowUpMapper, FollowUp> implements FollowUpService {
}
