package com.fang123.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fang123.entity.TaskInfo;
import com.fang123.mapper.TaskInfoMapper;
import com.fang123.service.TaskInfoService;
import org.springframework.stereotype.Service;

@Service
public class TaskInfoServiceImpl extends ServiceImpl<TaskInfoMapper, TaskInfo> implements TaskInfoService {}
