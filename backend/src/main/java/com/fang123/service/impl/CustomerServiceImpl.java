package com.fang123.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fang123.entity.Customer;
import com.fang123.mapper.CustomerMapper;
import com.fang123.service.CustomerService;
import org.springframework.stereotype.Service;

@Service
public class CustomerServiceImpl extends ServiceImpl<CustomerMapper, Customer> implements CustomerService {
}
