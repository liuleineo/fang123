package com.fang123.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fang123.entity.Loupan;
import com.fang123.mapper.LoupanMapper;
import com.fang123.service.LoupanService;
import org.springframework.stereotype.Service;

@Service
public class LoupanServiceImpl extends ServiceImpl<LoupanMapper, Loupan> implements LoupanService {
}
