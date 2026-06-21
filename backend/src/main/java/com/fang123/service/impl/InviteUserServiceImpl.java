package com.fang123.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fang123.entity.InviteUser;
import com.fang123.mapper.InviteUserMapper;
import com.fang123.service.InviteUserService;
import org.springframework.stereotype.Service;

@Service
public class InviteUserServiceImpl extends ServiceImpl<InviteUserMapper, InviteUser> implements InviteUserService {
}
