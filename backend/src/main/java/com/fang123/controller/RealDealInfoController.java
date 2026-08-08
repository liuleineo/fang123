package com.fang123.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fang123.common.Result;
import com.fang123.dto.AiParseRealDealResult;
import com.fang123.entity.RealDealInfo;
import com.fang123.service.AiParseService;
import com.fang123.service.RealDealInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class RealDealInfoController {

    private final RealDealInfoService realDealService;
    private final AiParseService aiParseService;

    @GetMapping("/api/admin/real-deals")
    public Result<Page<RealDealInfo>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String plate,
            @RequestParam(required = false) Long loupanId) {
        LambdaQueryWrapper<RealDealInfo> w = new LambdaQueryWrapper<>();
        w.eq(RealDealInfo::getDeleted, 0);
        if (StringUtils.hasText(keyword)) {
            w.and(wr -> wr.like(RealDealInfo::getCommunityName, keyword)
                    .or().like(RealDealInfo::getRoomNo, keyword)
                    .or().like(RealDealInfo::getDistrict, keyword)
                    .or().like(RealDealInfo::getPlate, keyword));
        }
        if (StringUtils.hasText(district)) w.eq(RealDealInfo::getDistrict, district);
        if (StringUtils.hasText(plate)) w.eq(RealDealInfo::getPlate, plate);
        if (loupanId != null) w.eq(RealDealInfo::getLoupanId, loupanId);
        w.orderByDesc(RealDealInfo::getDealDate).orderByDesc(RealDealInfo::getId);
        return Result.success(realDealService.page(new Page<>(page, size), w));
    }

    @GetMapping("/api/admin/real-deals/{id}")
    public Result<RealDealInfo> detail(@PathVariable Long id) {
        RealDealInfo entity = realDealService.getById(id);
        if (entity == null) return Result.notFound("真实成交记录不存在");
        return Result.success(entity);
    }

    @PostMapping("/api/admin/real-deals")
    public Result<RealDealInfo> create(@RequestBody RealDealInfo entity) {
        if (entity.getDealDate() == null) return Result.badRequest("成交日期不能为空");
        if (!StringUtils.hasText(entity.getCommunityName())) return Result.badRequest("小区名称不能为空");
        entity.setId(null);
        entity.setDeleted(0);
        realDealService.save(entity);
        return Result.success("创建成功", entity);
    }

    @PutMapping("/api/admin/real-deals/{id}")
    public Result<RealDealInfo> update(@PathVariable Long id, @RequestBody RealDealInfo entity) {
        if (realDealService.getById(id) == null) return Result.notFound("真实成交记录不存在");
        entity.setId(id);
        realDealService.updateById(entity);
        return Result.success("更新成功", realDealService.getById(id));
    }

    @DeleteMapping("/api/admin/real-deals/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        // 软删除
        RealDealInfo entity = realDealService.getById(id);
        if (entity != null) {
            entity.setDeleted(1);
            realDealService.updateById(entity);
        }
        return Result.success();
    }

    /** AI 解析成交播报文本为结构化字段 */
    @PostMapping("/api/admin/real-deals/ai-parse")
    public Result<AiParseRealDealResult> aiParse(@RequestBody Map<String, String> body) {
        String text = body.get("text");
        if (!StringUtils.hasText(text)) {
            return Result.badRequest("请提供成交播报文本");
        }
        try {
            return Result.success("解析完成", aiParseService.parseRealDealFromText(text));
        } catch (Exception e) {
            return Result.error(500, "AI解析失败：" + e.getMessage());
        }
    }
}
