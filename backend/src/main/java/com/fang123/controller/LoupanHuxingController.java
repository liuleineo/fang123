package com.fang123.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fang123.common.Result;
import com.fang123.dto.AiParseHuxingResult;
import com.fang123.entity.LoupanHuxing;
import com.fang123.service.AiParseService;
import com.fang123.service.LoupanHuxingService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class LoupanHuxingController {

    private final LoupanHuxingService huxingService;
    private final AiParseService aiParseService;

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

    /** AI 解析户型资料图片，支持一次提取多个户型 */
    @PostMapping("/api/admin/huxings/ai-parse")
    public Result<AiParseHuxingResult> aiParse(@RequestParam("files") MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return Result.badRequest("请至少上传一张图片");
        }
        try {
            return Result.success("解析完成", aiParseService.parseHuxings(files));
        } catch (Exception e) {
            return Result.error(500, "AI解析失败：" + e.getMessage());
        }
    }
}
