package com.fang123.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fang123.entity.UserWechat;
import com.fang123.mapper.UserWechatMapper;
import com.fang123.service.UserWechatService;
import org.springframework.stereotype.Service;

@Service
public class UserWechatServiceImpl extends ServiceImpl<UserWechatMapper, UserWechat> implements UserWechatService {
}
