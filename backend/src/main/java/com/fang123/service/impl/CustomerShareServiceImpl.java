package com.fang123.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fang123.entity.CustomerShare;
import com.fang123.mapper.CustomerShareMapper;
import com.fang123.service.CustomerShareService;
import org.springframework.stereotype.Service;

@Service
public class CustomerShareServiceImpl extends ServiceImpl<CustomerShareMapper, CustomerShare> implements CustomerShareService {
}
