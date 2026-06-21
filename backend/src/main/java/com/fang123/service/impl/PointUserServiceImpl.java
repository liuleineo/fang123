package com.fang123.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fang123.entity.PointUser;
import com.fang123.mapper.PointUserMapper;
import com.fang123.service.PointUserService;
import org.springframework.stereotype.Service;

@Service
public class PointUserServiceImpl extends ServiceImpl<PointUserMapper, PointUser> implements PointUserService {
}
