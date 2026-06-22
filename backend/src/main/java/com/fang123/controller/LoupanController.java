package com.fang123.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fang123.common.Result;
import com.fang123.dto.AiParseResult;
import com.fang123.entity.Loupan;
import com.fang123.service.AiParseService;
import com.fang123.service.LoupanService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class LoupanController {

    private final LoupanService loupanService;
    private final AiParseService aiParseService;

    @GetMapping("/api/admin/loupans")
    public Result<Page<Loupan>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<Loupan> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            w.like(Loupan::getProjectName, keyword)
             .or().like(Loupan::getDistrict, keyword)
             .or().like(Loupan::getPlate, keyword)
             .or().like(Loupan::getProjectCompany, keyword);
        }
        w.orderByDesc(Loupan::getSort).orderByDesc(Loupan::getCreateTime);
        return Result.success(loupanService.page(new Page<>(page, size), w));
    }

    @GetMapping("/api/admin/loupans/{id}")
    public Result<Loupan> detail(@PathVariable Long id) {
        Loupan entity = loupanService.getById(id);
        if (entity == null) return Result.notFound("楼盘不存在");
        return Result.success(entity);
    }

    @PostMapping("/api/admin/loupans")
    public Result<Loupan> create(@RequestBody Loupan entity) {
        entity.setId(null);
        loupanService.save(entity);
        return Result.success("创建成功", entity);
    }

    @PutMapping("/api/admin/loupans/{id}")
    public Result<Loupan> update(@PathVariable Long id, @RequestBody Loupan entity) {
        if (loupanService.getById(id) == null) return Result.notFound("楼盘不存在");
        entity.setId(id);
        loupanService.updateById(entity);
        return Result.success("更新成功", loupanService.getById(id));
    }

    @DeleteMapping("/api/admin/loupans/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        loupanService.removeById(id);
        return Result.success();
    }

    /** AI 解析楼盘资料图片，返回结构化字段 */
    @PostMapping("/api/admin/loupans/ai-parse")
    public Result<AiParseResult> aiParse(@RequestParam("files") MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return Result.badRequest("请至少上传一张图片");
        }
        try {
            return Result.success("解析完成", aiParseService.parse(files));
        } catch (Exception e) {
            return Result.error(500, "AI解析失败：" + e.getMessage());
        }
    }
}
