package com.fang123.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fang123.entity.MemberUser;
import com.fang123.mapper.MemberUserMapper;
import com.fang123.service.MemberUserService;
import org.springframework.stereotype.Service;

@Service
public class MemberUserServiceImpl extends ServiceImpl<MemberUserMapper, MemberUser> implements MemberUserService {}
