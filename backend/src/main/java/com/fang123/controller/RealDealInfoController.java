package com.fang123.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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

    /** Excel 批量导入真实成交记录（第一行为表头） */
    @PostMapping("/api/admin/real-deals/excel-import")
    public Result<Map<String, Object>> excelImport(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) return Result.badRequest("请上传Excel文件");
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!filename.endsWith(".xlsx") && !filename.endsWith(".xls")) {
            return Result.badRequest("仅支持 .xlsx / .xls 格式的Excel文件");
        }
        try {
            ExcelHolder holder = new ExcelHolder();
            EasyExcel.read(file.getInputStream())
                    .headRowNumber(1)
                    .registerReadListener(new AnalysisEventListener<Map<Integer, String>>() {
                        @Override
                        public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
                            holder.header = headMap;
                        }

                        @Override
                        public void invoke(Map<Integer, String> row, AnalysisContext context) {
                            holder.rows.add(row);
                        }

                        @Override
                        public void doAfterAllAnalysed(AnalysisContext context) {
                        }
                    })
                    .sheet()
                    .doRead();

            // 定位表头列（容忍不同表头命名，取首个命中列）
            int dealDateCol = -1, districtCol = -1, plateCol = -1, communityCol = -1,
                    roomCol = -1, areaCol = -1, priceCol = -1, yfyjCol = -1, loupanCol = -1, remarkCol = -1;
            if (holder.header != null) {
                for (Map.Entry<Integer, String> e : holder.header.entrySet()) {
                    String h = e.getValue() == null ? "" : e.getValue().trim();
                    if (h.isEmpty()) continue;
                    int col = e.getKey();
                    if (dealDateCol < 0 && (h.contains("成交日期") || h.contains("成交时间"))) dealDateCol = col;
                    else if (districtCol < 0 && h.contains("行政区")) districtCol = col;
                    else if (plateCol < 0 && h.contains("板块")) plateCol = col;
                    else if (communityCol < 0 && h.contains("小区")) communityCol = col;
                    else if (roomCol < 0 && (h.contains("房号") || h.contains("门牌") || h.contains("室号"))) roomCol = col;
                    else if (areaCol < 0 && h.contains("面积")) areaCol = col;
                    else if (priceCol < 0 && (h.contains("成交价") || h.contains("成交总价") || h.contains("成交金额"))) priceCol = col;
                    else if (yfyjCol < 0 && (h.contains("一手") || h.contains("买入") || h.contains("一房一价"))) yfyjCol = col;
                    else if (loupanCol < 0 && (h.toLowerCase().contains("loupan") || h.contains("楼盘ID") || h.contains("楼盘id"))) loupanCol = col;
                    else if (remarkCol < 0 && h.contains("备注")) remarkCol = col;
                }
            }
            if (communityCol < 0 || dealDateCol < 0) {
                return Result.badRequest("未找到“小区名称 / 成交日期”列，模板表头示例：成交日期|行政区|板块|小区名称|房号|面积|成交价|一手买入价|楼盘ID|备注");
            }

            int successCount = 0, failedCount = 0;
            List<Map<String, Object>> errors = new ArrayList<>();
            int rowNo = 1; // 数据行号（不含表头）
            for (Map<Integer, String> row : holder.rows) {
                rowNo++;
                if (row == null || row.isEmpty()) continue;

                String community = cell(row, communityCol);
                if (community.isEmpty()) {
                    failedCount++;
                    errors.add(Map.of("row", rowNo, "msg", "小区名称为空"));
                    continue;
                }
                LocalDate dealDate = parseDealDate(cell(row, dealDateCol));
                if (dealDate == null) {
                    failedCount++;
                    errors.add(Map.of("row", rowNo, "msg", "成交日期不能为空或格式不正确（示例：2026-08-06）"));
                    continue;
                }
                // 可选数值列：非空但解析失败则整行记错
                String[] nums = {
                        cell(row, areaCol), cell(row, priceCol), cell(row, yfyjCol), cell(row, loupanCol)
                };
                BigDecimal area = parseNum(nums[0]), price = parseNum(nums[1]), yfyj = parseNum(nums[2]);
                Long loupanId = parseLongId(nums[3]);
                if ((!nums[0].isEmpty() && area == null)
                        || (!nums[1].isEmpty() && price == null)
                        || (!nums[2].isEmpty() && yfyj == null)
                        || (!nums[3].isEmpty() && loupanId == null)) {
                    failedCount++;
                    errors.add(Map.of("row", rowNo, "msg", "面积/成交价/一手价需为数字，楼盘ID需为整数"));
                    continue;
                }

                RealDealInfo d = new RealDealInfo();
                d.setDealDate(dealDate);
                d.setCommunityName(community);
                d.setRoomNo(opt(row, roomCol));
                d.setDistrict(opt(row, districtCol));
                d.setPlate(opt(row, plateCol));
                d.setHouseArea(area);
                d.setDealPrice(price);
                d.setYfyj(yfyj);
                d.setLoupanId(loupanId);
                d.setRemark(opt(row, remarkCol));
                d.setDeleted(0);
                realDealService.save(d);
                successCount++;
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("total", successCount + failedCount);
            result.put("success", successCount);
            result.put("failed", failedCount);
            result.put("errors", errors);
            return Result.success(result);
        } catch (IOException e) {
            return Result.error(500, "Excel 解析失败：" + e.getMessage());
        }
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

    // ===== Excel 导入辅助 =====

    private static String cell(Map<Integer, String> row, int col) {
        if (row == null || col < 0) return "";
        String v = row.get(col);
        return v == null ? "" : v.trim();
    }

    /** 可选文本列：空值返回 null 便于入库为空 */
    private static String opt(Map<Integer, String> row, int col) {
        String v = cell(row, col);
        return v.isEmpty() ? null : v;
    }

    /** 解析面积/价格等数值（兼容逗号、单位"万/元/㎡/平米"等字符） */
    private static BigDecimal parseNum(String v) {
        if (v == null || v.isEmpty()) return null;
        String s = v.replaceAll("[^0-9.\\-]", "");
        if (s.isEmpty() || "-".equals(s) || ".".equals(s)) return null;
        try {
            return new BigDecimal(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 楼盘ID 取整数部分 */
    private static Long parseLongId(String v) {
        if (v == null || v.isEmpty()) return null;
        String s = v.replaceAll("[^0-9]", "");
        if (s.isEmpty()) return null;
        try {
            return Long.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static final List<DateTimeFormatter> DEAL_DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("yyyy/M/d"),
            DateTimeFormatter.ofPattern("yyyy.M.d"),
            DateTimeFormatter.ofPattern("yyyy年M月d日"),
            DateTimeFormatter.ofPattern("yyyyMMdd"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy/M/d HH:mm:ss")
    );

    /** 解析成交日期：支持常见文本格式；若为 Excel 日期序列号（如 45234）则换算 */
    private static LocalDate parseDealDate(String v) {
        if (v == null) return null;
        String s = v.trim().replaceAll("\\s+", " ");
        if (s.isEmpty()) return null;
        String num = s.replaceAll("[^0-9]", "");
        if (!num.isEmpty() && num.length() == 5) {
            try {
                return LocalDate.of(1899, 12, 30).plusDays(Long.parseLong(num));
            } catch (Exception ignored) {
            }
        }
        for (DateTimeFormatter f : DEAL_DATE_FORMATS) {
            try {
                return LocalDate.parse(s, f);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /** 承载 Excel 读取过程中的表头与数据行 */
    private static class ExcelHolder {
        Map<Integer, String> header;
        List<Map<Integer, String>> rows = new ArrayList<>();
    }
}
