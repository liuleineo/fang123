package com.fang123.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fang123.common.Result;
import com.fang123.dto.AiParseTupaiLandResult;
import com.fang123.entity.LoupanTupaiLand;
import com.fang123.service.AiParseService;
import com.fang123.service.LoupanTupaiLandService;
import com.fang123.service.TupaiResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class LoupanTupaiLandController {

    private final LoupanTupaiLandService tupaiLandService;
    private final AiParseService aiParseService;
    private final TupaiResourceService tupaiResourceService;

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

    @PostMapping("/api/admin/tupai-lands/ai-parse")
    public Result<AiParseTupaiLandResult> aiParse(@RequestParam("files") MultipartFile[] files) {
        if (files == null || files.length == 0) return Result.badRequest("请至少上传一张图片");
        try { return Result.success("解析完成", aiParseService.parseTupaiLand(files)); }
        catch (Exception e) { return Result.error(500, "AI解析失败：" + e.getMessage()); }
    }

    /** 通过 resourceId 从浙江自然资源交易平台拉取地块信息 */
    @PostMapping("/api/admin/tupai-lands/fetch-by-resource")
    public Result<Map<String, Object>> fetchByResourceId(@RequestBody Map<String, List<String>> body) {
        List<String> ids = body.get("resourceIds");
        if (ids == null || ids.isEmpty()) return Result.badRequest("请至少输入一个resourceId");
        return Result.success(tupaiResourceService.fetchByResourceIds(ids));
    }
}
