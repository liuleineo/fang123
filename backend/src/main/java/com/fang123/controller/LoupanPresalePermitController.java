package com.fang123.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fang123.common.Result;
import com.fang123.dto.AiParsePresaleResult;
import com.fang123.entity.LoupanPresalePermit;
import com.fang123.service.AiParseService;
import com.fang123.service.LoupanPresalePermitService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class LoupanPresalePermitController {

    private final LoupanPresalePermitService permitService;
    private final AiParseService aiParseService;

    @GetMapping("/api/admin/presale-permits")
    public Result<Page<LoupanPresalePermit>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long loupanId) {
        LambdaQueryWrapper<LoupanPresalePermit> w = new LambdaQueryWrapper<>();
        if (loupanId != null) w.eq(LoupanPresalePermit::getLoupanId, loupanId);
        if (StringUtils.hasText(keyword)) {
            w.like(LoupanPresalePermit::getProjectName, keyword)
             .or().like(LoupanPresalePermit::getPermitNo, keyword)
             .or().like(LoupanPresalePermit::getPermitNoStr, keyword)
             .or().like(LoupanPresalePermit::getDevelopCompany, keyword);
        }
        w.orderByDesc(LoupanPresalePermit::getCreateTime);
        return Result.success(permitService.page(new Page<>(page, size), w));
    }

    @GetMapping("/api/admin/presale-permits/{id}")
    public Result<LoupanPresalePermit> detail(@PathVariable Long id) {
        LoupanPresalePermit entity = permitService.getById(id);
        if (entity == null) return Result.notFound("预售证不存在");
        return Result.success(entity);
    }

    @PostMapping("/api/admin/presale-permits")
    public Result<LoupanPresalePermit> create(@RequestBody LoupanPresalePermit entity) {
        entity.setId(null);
        permitService.save(entity);
        return Result.success("创建成功", entity);
    }

    @PutMapping("/api/admin/presale-permits/{id}")
    public Result<LoupanPresalePermit> update(@PathVariable Long id, @RequestBody LoupanPresalePermit entity) {
        if (permitService.getById(id) == null) return Result.notFound("预售证不存在");
        entity.setId(id);
        permitService.updateById(entity);
        return Result.success("更新成功", permitService.getById(id));
    }

    @DeleteMapping("/api/admin/presale-permits/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        permitService.removeById(id);
        return Result.success();
    }

    @PostMapping("/api/admin/presale-permits/ai-parse")
    public Result<AiParsePresaleResult> aiParse(@RequestParam("files") MultipartFile[] files) {
        if (files == null || files.length == 0) return Result.badRequest("请至少上传一张图片");
        try { return Result.success("解析完成", aiParseService.parsePresale(files)); }
        catch (Exception e) { return Result.error(500, "AI解析失败：" + e.getMessage()); }
    }
}
