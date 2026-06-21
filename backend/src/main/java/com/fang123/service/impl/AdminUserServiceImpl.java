package com.fang123.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fang123.entity.AdminUser;
import com.fang123.mapper.AdminUserMapper;
import com.fang123.service.AdminUserService;
import org.springframework.stereotype.Service;

@Service
public class AdminUserServiceImpl extends ServiceImpl<AdminUserMapper, AdminUser> implements AdminUserService {
}
