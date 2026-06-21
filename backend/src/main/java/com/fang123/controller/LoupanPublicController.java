package com.fang123.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fang123.common.IdObfuscator;
import com.fang123.common.Result;
import com.fang123.entity.Loupan;
import com.fang123.entity.LoupanHuxing;
import com.fang123.entity.LoupanMedia;
import com.fang123.service.LoupanService;
import com.fang123.service.LoupanHuxingService;
import com.fang123.service.LoupanMediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class LoupanPublicController {

    private final LoupanService loupanService;
    private final LoupanHuxingService huxingService;
    private final LoupanMediaService mediaService;

    /** 公开-楼盘列表（搜索+分页） */
    @GetMapping("/api/public/loupans")
    public Result<Page<Loupan>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "12") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String plate,
            @RequestParam(required = false) Integer houseType,
            @RequestParam(required = false) Integer decorateType) {
        LambdaQueryWrapper<Loupan> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            w.and(wr -> wr.like(Loupan::getProjectName, keyword)
                    .or().like(Loupan::getProjectCompany, keyword)
                    .or().like(Loupan::getBrandList, keyword));
        }
        if (StringUtils.hasText(district)) w.eq(Loupan::getDistrict, district);
        if (StringUtils.hasText(plate)) w.eq(Loupan::getPlate, plate);
        if (houseType != null) w.eq(Loupan::getHouseType, houseType);
        if (decorateType != null) w.eq(Loupan::getDecorateType, decorateType);
        w.orderByDesc(Loupan::getSort).orderByDesc(Loupan::getCreateTime);
        Page<Loupan> result = loupanService.page(new Page<>(page, size), w);
        result.getRecords().forEach(lp -> lp.setEncodedId(IdObfuscator.encode(lp.getId())));
        return Result.success(result);
    }

    /** 公开-楼盘详情（encodedId 为混淆后的 ID） */
    @GetMapping("/api/public/loupans/{encodedId}")
    public Result<Loupan> detail(@PathVariable String encodedId) {
        Long id = IdObfuscator.decode(encodedId);
        if (id == null) return Result.notFound("楼盘不存在");
        Loupan entity = loupanService.getById(id);
        if (entity == null) return Result.notFound("楼盘不存在");
        entity.setEncodedId(encodedId);
        return Result.success(entity);
    }

    /** 公开-楼盘户型列表 */
    @GetMapping("/api/public/loupans/{encodedId}/huxings")
    public Result<List<LoupanHuxing>> huxings(@PathVariable String encodedId) {
        Long id = IdObfuscator.decode(encodedId);
        LambdaQueryWrapper<LoupanHuxing> w = new LambdaQueryWrapper<>();
        w.eq(LoupanHuxing::getLoupanId, id).orderByAsc(LoupanHuxing::getSort);
        return Result.success(huxingService.list(w));
    }

    /** 公开-楼盘素材列表 */
    @GetMapping("/api/public/loupans/{encodedId}/medias")
    public Result<List<LoupanMedia>> medias(@PathVariable String encodedId) {
        Long id = IdObfuscator.decode(encodedId);
        LambdaQueryWrapper<LoupanMedia> w = new LambdaQueryWrapper<>();
        w.eq(LoupanMedia::getLoupanId, id).orderByAsc(LoupanMedia::getSort);
        return Result.success(mediaService.list(w));
    }

    /** 公开-筛选选项（行政区、板块列表） */
    @GetMapping("/api/public/loupan-filters")
    public Result<Map<String, Object>> filters() {
        List<Loupan> list = loupanService.list();
        List<String> districts = list.stream().map(Loupan::getDistrict).filter(StringUtils::hasText).distinct().sorted().toList();
        List<String> plates = list.stream().map(Loupan::getPlate).filter(StringUtils::hasText).distinct().sorted().toList();
        return Result.success(Map.of("districts", districts, "plates", plates));
    }
}
