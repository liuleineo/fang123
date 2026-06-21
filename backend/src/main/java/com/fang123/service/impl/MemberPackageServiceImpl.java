package com.fang123.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fang123.entity.MemberPackage;
import com.fang123.mapper.MemberPackageMapper;
import com.fang123.service.MemberPackageService;
import org.springframework.stereotype.Service;

@Service
public class MemberPackageServiceImpl extends ServiceImpl<MemberPackageMapper, MemberPackage> implements MemberPackageService {}
