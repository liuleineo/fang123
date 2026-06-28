package com.fang123.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fang123.common.Result;
import com.fang123.dto.AiParseYfyjResult;
import com.fang123.entity.LoupanYfyj;
import com.fang123.service.AiParseService;
import com.fang123.service.LoupanYfyjService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class LoupanYfyjController {

    private final LoupanYfyjService yfyjService;
    private final AiParseService aiParseService;

    @GetMapping("/api/admin/yfyj")
    public Result<Page<LoupanYfyj>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long loupanId) {
        LambdaQueryWrapper<LoupanYfyj> w = new LambdaQueryWrapper<>();
        if (loupanId != null) w.eq(LoupanYfyj::getLoupanId, loupanId);
        if (StringUtils.hasText(keyword)) {
            w.like(LoupanYfyj::getBuildingNo, keyword)
             .or().like(LoupanYfyj::getRoomNo, keyword);
        }
        w.orderByDesc(LoupanYfyj::getSort).orderByDesc(LoupanYfyj::getCreateTime);
        return Result.success(yfyjService.page(new Page<>(page, size), w));
    }

    @GetMapping("/api/admin/yfyj/{id}")
    public Result<LoupanYfyj> detail(@PathVariable Long id) {
        LoupanYfyj entity = yfyjService.getById(id);
        if (entity == null) return Result.notFound("房源不存在");
        return Result.success(entity);
    }

    @PostMapping("/api/admin/yfyj")
    public Result<LoupanYfyj> create(@RequestBody LoupanYfyj entity) {
        entity.setId(null);
        yfyjService.save(entity);
        return Result.success("创建成功", entity);
    }

    @PutMapping("/api/admin/yfyj/{id}")
    public Result<LoupanYfyj> update(@PathVariable Long id, @RequestBody LoupanYfyj entity) {
        if (yfyjService.getById(id) == null) return Result.notFound("房源不存在");
        entity.setId(id);
        yfyjService.updateById(entity);
        return Result.success("更新成功", yfyjService.getById(id));
    }

    @DeleteMapping("/api/admin/yfyj/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        yfyjService.removeById(id);
        return Result.success();
    }

    @PostMapping("/api/admin/yfyj/ai-parse")
    public Result<AiParseYfyjResult> aiParse(@RequestParam("files") MultipartFile[] files) {
        if (files == null || files.length == 0) return Result.badRequest("请至少上传一张图片");
        try { return Result.success("解析完成", aiParseService.parseYfyj(files)); }
        catch (Exception e) { return Result.error(500, "AI解析失败：" + e.getMessage()); }
    }
}
