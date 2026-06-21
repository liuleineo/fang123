package com.fang123.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fang123.entity.TaskUserOrder;
import com.fang123.mapper.TaskUserOrderMapper;
import com.fang123.service.TaskUserOrderService;
import org.springframework.stereotype.Service;

@Service
public class TaskUserOrderServiceImpl extends ServiceImpl<TaskUserOrderMapper, TaskUserOrder> implements TaskUserOrderService {}
