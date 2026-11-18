package com.fang123.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fang123.common.Result;
import com.fang123.dto.AiParseHuxingResult;
import com.fang123.dto.HuxingBatchCreateRequest;
import com.fang123.entity.LoupanHuxing;
import com.fang123.service.AiParseService;
import com.fang123.service.LoupanHuxingService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        w.orderByDesc(LoupanHuxing::getId);
        return Result.success(huxingService.page(new Page<>(page, size), w));
    }

    /** 户型下拉选项（按楼盘过滤），供媒体素材等模块选择关联户型 */
    @GetMapping("/api/admin/huxings/options")
    public Result<List<Map<String, Object>>> options(@RequestParam(required = false) Long loupanId) {
        if (loupanId == null) return Result.success(List.of());
        LambdaQueryWrapper<LoupanHuxing> w = new LambdaQueryWrapper<>();
        w.eq(LoupanHuxing::getLoupanId, loupanId);
        w.orderByAsc(LoupanHuxing::getSort).orderByAsc(LoupanHuxing::getId);
        List<Map<String, Object>> list = new ArrayList<>();
        for (LoupanHuxing h : huxingService.list(w)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", h.getId());
            m.put("huxingName", h.getHuxingName());
            m.put("area", h.getArea());
            list.add(m);
        }
        return Result.success(list);
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

    /** AI 批量录入户型：一次请求保存多条，逐条返回失败明细 */
    @PostMapping("/api/admin/huxings/batch")
    public Result<Map<String, Object>> batchCreate(@RequestBody HuxingBatchCreateRequest req) {
        if (req == null || req.getLoupanId() == null) return Result.badRequest("请选择关联楼盘");
        if (req.getHuxings() == null || req.getHuxings().isEmpty()) return Result.badRequest("没有需要录入的户型");

        int created = 0;
        List<Map<String, Object>> errors = new ArrayList<>();
        List<AiParseHuxingResult.HuxingFields> list = req.getHuxings();
        for (int i = 0; i < list.size(); i++) {
            AiParseHuxingResult.HuxingFields fields = list.get(i);
            if (fields == null) continue;
            try {
                huxingService.save(toEntity(fields, req.getLoupanId()));
                created++;
            } catch (Exception e) {
                Map<String, Object> err = new LinkedHashMap<>();
                err.put("index", i);
                err.put("huxingName", fields.getHuxingName());
                err.put("msg", e.getMessage());
                errors.add(err);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("created", created);
        result.put("failed", errors.size());
        result.put("errors", errors);
        return Result.success("成功录入 " + created + " 个户型", result);
    }

    /** AI 字段 → 实体：数值四舍五入为整数，户型图留空（AI 不识别、不录入户型图） */
    private LoupanHuxing toEntity(AiParseHuxingResult.HuxingFields f, Long loupanId) {
        LoupanHuxing e = new LoupanHuxing();
        e.setLoupanId(loupanId);
        String name = StringUtils.hasText(f.getHuxingName()) ? f.getHuxingName().trim() : null;
        if (name == null) name = f.getArea() != null ? f.getArea().stripTrailingZeros().toPlainString() + "㎡户型" : "未命名户型";
        e.setHuxingName(name);
        e.setArea(toInt(f.getArea(), 0));
        e.setInsideArea(toInt(f.getInsideArea(), 0));
        e.setInsideAreaWithGift(f.getInsideAreaWithGift() == null ? null : toInt(f.getInsideAreaWithGift(), 0));
        e.setRoomNum(f.getRoomNum());
        e.setHallNum(f.getHallNum());
        e.setToiletNum(f.getToiletNum());
        e.setBalconyNum(f.getBalconyNum());
        e.setOrientation(StringUtils.hasText(f.getOrientation()) ? f.getOrientation().trim() : "");
        e.setFloorType(f.getFloorType() == null ? 1 : f.getFloorType());
        e.setUnitPrice(f.getUnitPrice() == null ? null : toInt(f.getUnitPrice(), 0));
        e.setTotalPriceStart(f.getTotalPriceStart());
        e.setTotalPriceEnd(f.getTotalPriceEnd());
        e.setIsShowHouse(f.getIsShowHouse() == null ? 0 : f.getIsShowHouse());
        e.setTag(StringUtils.hasText(f.getTag()) ? f.getTag().trim() : "");
        e.setSort(0);
        // 户型图不由 AI 识别，保存后由人工在列表中补充
        e.setHuxingImage("");
        e.setStandardHuxingImage("");
        return e;
    }

    private Integer toInt(BigDecimal v, int defaultVal) {
        if (v == null) return defaultVal;
        return v.setScale(0, java.math.RoundingMode.HALF_UP).intValue();
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
