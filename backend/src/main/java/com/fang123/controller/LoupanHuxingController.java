package com.fang123.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fang123.common.Result;
import com.fang123.entity.LoupanHuxing;
import com.fang123.service.LoupanHuxingService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class LoupanHuxingController {

    private final LoupanHuxingService huxingService;

    @GetMapping("/api/admin/huxings")
    public Result<Page<LoupanHuxing>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long loupanId) {
        LambdaQueryWrapper<LoupanHuxing> w = new LambdaQueryWrapper<>();
        if (loupanId != null) w.eq(LoupanHuxing::getLoupanId, loupanId);
        if (StringUtils.hasText(keyword)) w.like(LoupanHuxing::getHuxingName, keyword);
        w.orderByDesc(LoupanHuxing::getSort).orderByDesc(LoupanHuxing::getCreateTime);
        return Result.success(huxingService.page(new Page<>(page, size), w));
    }

    @GetMapping("/api/admin/huxings/{id}")
    public Result<LoupanHuxing> detail(@PathVariable Long id) {
        LoupanHuxing entity = huxingService.getById(id);
        if (entity == null) return Result.notFound("户型不存在");
        return Result.success(entity);
    }

    @PostMapping("/api/admin/huxings")
    public Result<LoupanHuxing> create(@RequestBody LoupanHuxing entity) {
        entity.setId(null);
        huxingService.save(entity);
        return Result.success("创建成功", entity);
    }

    @PutMapping("/api/admin/huxings/{id}")
    public Result<LoupanHuxing> update(@PathVariable Long id, @RequestBody LoupanHuxing entity) {
        if (huxingService.getById(id) == null) return Result.notFound("户型不存在");
        entity.setId(id);
        huxingService.updateById(entity);
        return Result.success("更新成功", huxingService.getById(id));
    }

    @DeleteMapping("/api/admin/huxings/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        huxingService.removeById(id);
        return Result.success();
    }
}
