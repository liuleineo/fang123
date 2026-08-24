package com.fang123.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fang123.common.Result;
import com.fang123.entity.LoupanDynamic;
import com.fang123.service.LoupanDynamicService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class LoupanDynamicController {

    private final LoupanDynamicService dynamicService;

    /** 列表 + 分页 + 搜索（楼盘ID / 类型 / 关键词标题） */
    @GetMapping("/api/admin/dynamics")
    public Result<Page<LoupanDynamic>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) Long loupanId,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<LoupanDynamic> w = new LambdaQueryWrapper<>();
        if (loupanId != null) w.eq(LoupanDynamic::getLoupanId, loupanId);
        if (type != null) w.eq(LoupanDynamic::getType, type);
        if (StringUtils.hasText(keyword)) w.like(LoupanDynamic::getTitle, keyword);
        w.orderByDesc(LoupanDynamic::getCreateTime);
        return Result.success(dynamicService.page(new Page<>(page, size), w));
    }

    @GetMapping("/api/admin/dynamics/{id}")
    public Result<LoupanDynamic> detail(@PathVariable Long id) {
        LoupanDynamic entity = dynamicService.getById(id);
        if (entity == null) return Result.notFound("动态不存在");
        return Result.success(entity);
    }

    @PostMapping("/api/admin/dynamics")
    public Result<LoupanDynamic> create(@RequestBody LoupanDynamic entity) {
        entity.setId(null);
        dynamicService.save(entity);
        return Result.success("创建成功", entity);
    }

    @PutMapping("/api/admin/dynamics/{id}")
    public Result<LoupanDynamic> update(@PathVariable Long id, @RequestBody LoupanDynamic entity) {
        if (dynamicService.getById(id) == null) return Result.notFound("动态不存在");
        entity.setId(id);
        dynamicService.updateById(entity);
        return Result.success("更新成功", dynamicService.getById(id));
    }

    @DeleteMapping("/api/admin/dynamics/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        dynamicService.removeById(id);
        return Result.success();
    }
}
