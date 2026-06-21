package com.fang123.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fang123.entity.PointLog;
import com.fang123.mapper.PointLogMapper;
import com.fang123.service.PointLogService;
import org.springframework.stereotype.Service;

@Service
public class PointLogServiceImpl extends ServiceImpl<PointLogMapper, PointLog> implements PointLogService {
}
