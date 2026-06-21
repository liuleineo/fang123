package com.fang123.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fang123.common.Result;
import com.fang123.entity.LoupanTupaiLand;
import com.fang123.service.LoupanTupaiLandService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class LoupanTupaiLandController {

    private final LoupanTupaiLandService tupaiLandService;

    @GetMapping("/api/admin/tupai-lands")
    public Result<Page<LoupanTupaiLand>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<LoupanTupaiLand> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            w.like(LoupanTupaiLand::getLandName, keyword)
             .or().like(LoupanTupaiLand::getLandNo, keyword)
             .or().like(LoupanTupaiLand::getWinnerCompany, keyword);
        }
        w.orderByDesc(LoupanTupaiLand::getSort).orderByDesc(LoupanTupaiLand::getCreateTime);
        return Result.success(tupaiLandService.page(new Page<>(page, size), w));
    }

    @GetMapping("/api/admin/tupai-lands/{id}")
    public Result<LoupanTupaiLand> detail(@PathVariable Long id) {
        LoupanTupaiLand entity = tupaiLandService.getById(id);
        if (entity == null) return Result.notFound("地块不存在");
        return Result.success(entity);
    }

    @PostMapping("/api/admin/tupai-lands")
    public Result<LoupanTupaiLand> create(@RequestBody LoupanTupaiLand entity) {
        entity.setId(null);
        tupaiLandService.save(entity);
        return Result.success("创建成功", entity);
    }

    @PutMapping("/api/admin/tupai-lands/{id}")
    public Result<LoupanTupaiLand> update(@PathVariable Long id, @RequestBody LoupanTupaiLand entity) {
        if (tupaiLandService.getById(id) == null) return Result.notFound("地块不存在");
        entity.setId(id);
        tupaiLandService.updateById(entity);
        return Result.success("更新成功", tupaiLandService.getById(id));
    }

    @DeleteMapping("/api/admin/tupai-lands/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        tupaiLandService.removeById(id);
        return Result.success();
    }
}
