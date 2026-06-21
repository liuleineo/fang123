package com.fang123.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fang123.entity.TaskPayLog;
import com.fang123.mapper.TaskPayLogMapper;
import com.fang123.service.TaskPayLogService;
import org.springframework.stereotype.Service;

@Service
public class TaskPayLogServiceImpl extends ServiceImpl<TaskPayLogMapper, TaskPayLog> implements TaskPayLogService {}
