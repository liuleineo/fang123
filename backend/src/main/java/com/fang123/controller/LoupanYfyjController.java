package com.fang123.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
            @RequestParam(required = false) Long loupanId,
            @RequestParam(required = false) String buildingNo,
            @RequestParam(required = false) String unitNo,
            @RequestParam(required = false) String roomNo,
            @RequestParam(required = false) String permitNo) {
        LambdaQueryWrapper<LoupanYfyj> w = new LambdaQueryWrapper<>();
        if (loupanId != null) w.eq(LoupanYfyj::getLoupanId, loupanId);
        if (StringUtils.hasText(buildingNo)) w.eq(LoupanYfyj::getBuildingNo, buildingNo);
        if (StringUtils.hasText(unitNo)) w.eq(LoupanYfyj::getUnitNo, unitNo);
        if (StringUtils.hasText(roomNo)) w.like(LoupanYfyj::getRoomNo, roomNo);
        if (StringUtils.hasText(permitNo)) w.like(LoupanYfyj::getPermitNo, permitNo);
        w.orderByDesc(LoupanYfyj::getId);
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

    /** 批量设置：按筛选条件对符合条件的房源批量设置 loupan_id / huxing_id */
    @PostMapping("/api/admin/yfyj/batch-update")
    public Result<Integer> batchUpdate(@RequestBody java.util.Map<String, Object> body) {
        // 筛选条件（where）
        LambdaUpdateWrapper<LoupanYfyj> uw = new LambdaUpdateWrapper<>();
        Object fLoupan = body.get("loupanId");
        if (fLoupan != null && StringUtils.hasText(String.valueOf(fLoupan))) uw.eq(LoupanYfyj::getLoupanId, Long.valueOf(String.valueOf(fLoupan)));
        if (StringUtils.hasText(asStr(body.get("buildingNo")))) uw.eq(LoupanYfyj::getBuildingNo, asStr(body.get("buildingNo")));
        if (StringUtils.hasText(asStr(body.get("unitNo")))) uw.eq(LoupanYfyj::getUnitNo, asStr(body.get("unitNo")));
        if (StringUtils.hasText(asStr(body.get("roomNo")))) uw.like(LoupanYfyj::getRoomNo, asStr(body.get("roomNo")));
        if (StringUtils.hasText(asStr(body.get("permitNo")))) uw.like(LoupanYfyj::getPermitNo, asStr(body.get("permitNo")));

        // 要设置的值（set）
        boolean hasLoupan = body.containsKey("setLoupanId");
        boolean hasHuxing = body.containsKey("setHuxingId");
        boolean hasPermit = body.containsKey("setPermitNo");
        if (!hasLoupan && !hasHuxing && !hasPermit) return Result.badRequest("请指定要批量设置的字段（setLoupanId / setHuxingId / setPermitNo）");
        if (hasLoupan) uw.set(LoupanYfyj::getLoupanId, toLongOrNull(body.get("setLoupanId")));
        if (hasHuxing) uw.set(LoupanYfyj::getHuxingId, toLongOrNull(body.get("setHuxingId")));
        if (hasPermit) uw.set(LoupanYfyj::getPermitNo, asStr(body.get("setPermitNo")));

        int affected = yfyjService.getBaseMapper().update(null, uw);
        return Result.success("批量设置成功", affected);
    }

    @PostMapping("/api/admin/yfyj/ai-parse")
    public Result<AiParseYfyjResult> aiParse(@RequestParam("files") MultipartFile[] files) {
        if (files == null || files.length == 0) return Result.badRequest("请至少上传一张图片");
        try { return Result.success("解析完成", aiParseService.parseYfyj(files)); }
        catch (Exception e) { return Result.error(500, "AI解析失败：" + e.getMessage()); }
    }

    private static String asStr(Object v) {
        return v == null ? null : String.valueOf(v).trim();
    }

    private static Long toLongOrNull(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        if (s.isEmpty()) return null;
        try { return Long.valueOf(s); } catch (NumberFormatException e) { return null; }
    }
}
