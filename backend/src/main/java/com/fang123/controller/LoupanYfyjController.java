package com.fang123.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fang123.common.Result;
import com.fang123.dto.AiParseYfyjResult;
import com.fang123.entity.LoupanYfyj;
import com.fang123.service.AiParseService;
import com.fang123.service.LoupanYfyjService;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    /** Excel 批量导入一房一价房源 */
    @PostMapping("/api/admin/yfyj/excel-import")
    public Result<Map<String, Object>> excelImport(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) return Result.badRequest("请上传Excel文件");
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!filename.endsWith(".xlsx") && !filename.endsWith(".xls")) {
            return Result.badRequest("仅支持 .xlsx / .xls 格式的Excel文件");
        }
        try {
            YfyjExcelHolder holder = new YfyjExcelHolder();
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

            // 定位表头列
            int loupanCol = -1, huxingCol = -1, permitCol = -1, fwcodeCol = -1,
                    buildingCol = -1, unitCol = -1, roomCol = -1, areaCol = -1,
                    unitPriceCol = -1, totalPriceCol = -1, statusCol = -1, remarkCol = -1;
            if (holder.header != null) {
                for (Map.Entry<Integer, String> e : holder.header.entrySet()) {
                    String h = e.getValue() == null ? "" : e.getValue().trim();
                    if (h.isEmpty()) continue;
                    int col = e.getKey();
                    String lower = h.toLowerCase();
                    if (loupanCol < 0 && (lower.contains("loupan") || h.contains("楼盘"))) loupanCol = col;
                    else if (huxingCol < 0 && (lower.contains("huxing") || h.contains("户型"))) huxingCol = col;
                    else if (permitCol < 0 && (h.contains("预售证") || h.contains("备案证"))) permitCol = col;
                    else if (fwcodeCol < 0 && h.contains("房屋编码")) fwcodeCol = col;
                    else if (buildingCol < 0 && h.contains("楼栋")) buildingCol = col;
                    else if (unitCol < 0 && h.contains("单元")) unitCol = col;
                    else if (roomCol < 0 && (h.contains("房号") || h.contains("房间号") || h.contains("室号"))) roomCol = col;
                    else if (areaCol < 0 && h.contains("面积")) areaCol = col;
                    else if (unitPriceCol < 0 && (h.contains("单价") || h.contains("均价") || h.contains("备案单价"))) unitPriceCol = col;
                    else if (totalPriceCol < 0 && (h.contains("总价") || h.contains("总房价"))) totalPriceCol = col;
                    else if (statusCol < 0 && h.contains("状态")) statusCol = col;
                    else if (remarkCol < 0 && h.contains("备注")) remarkCol = col;
                }
            }
            if (loupanCol < 0 || roomCol < 0) {
                return Result.badRequest("未找到“楼盘 / 房号”列，模板表头示例：楼盘|户型|预售证号|楼栋|单元|房号|面积|备案单价|备案总价|状态|备注");
            }

            int successCount = 0, failedCount = 0;
            List<Map<String, Object>> errors = new ArrayList<>();
            int rowNo = 1; // 数据行号（不含表头）
            for (Map<Integer, String> row : holder.rows) {
                rowNo++;
                if (row == null || row.isEmpty()) continue;
                Long loupanId = parseLong(cell(row, loupanCol));
                if (loupanId == null) {
                    failedCount++;
                    errors.add(Map.of("row", rowNo, "msg", "楼盘ID为空或非数字"));
                    continue;
                }
                String roomNo = cell(row, roomCol);
                if (roomNo.isEmpty()) {
                    failedCount++;
                    errors.add(Map.of("row", rowNo, "msg", "房号为空"));
                    continue;
                }
                Long huxingId = parseLong(cell(row, huxingCol));
                BigDecimal area = parseNum(cell(row, areaCol));
                Integer unitPrice = parseInt(cell(row, unitPriceCol));
                Integer totalPrice = parseInt(cell(row, totalPriceCol));
                String[] nums = { cell(row, huxingCol), cell(row, areaCol), cell(row, unitPriceCol), cell(row, totalPriceCol) };
                if ((!nums[0].isEmpty() && huxingId == null)
                        || (!nums[1].isEmpty() && area == null)
                        || (!nums[2].isEmpty() && unitPrice == null)
                        || (!nums[3].isEmpty() && totalPrice == null)) {
                    failedCount++;
                    errors.add(Map.of("row", rowNo, "msg", "户型ID/面积/单价/总价需为数字"));
                    continue;
                }

                LoupanYfyj y = new LoupanYfyj();
                y.setLoupanId(loupanId);
                y.setHuxingId(huxingId);
                y.setPermitNo(opt(row, permitCol));
                y.setFwcode(opt(row, fwcodeCol));
                y.setBuildingNo(opt(row, buildingCol));
                y.setUnitNo(opt(row, unitCol));
                y.setRoomNo(roomNo);
                y.setArea(area);
                y.setRecordUnitPrice(unitPrice);
                y.setRecordTotalPrice(totalPrice);
                y.setHouseStatus(parseStatus(cell(row, statusCol)));
                y.setRemark(opt(row, remarkCol));
                yfyjService.save(y);
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

    @PostMapping("/api/admin/yfyj/ai-parse")
    public Result<AiParseYfyjResult> aiParse(@RequestParam("files") MultipartFile[] files) {
        if (files == null || files.length == 0) return Result.badRequest("请至少上传一张图片");
        try { return Result.success("解析完成", aiParseService.parseYfyj(files)); }
        catch (Exception e) { return Result.error(500, "AI解析失败：" + e.getMessage()); }
    }

    // ===== Excel 导入辅助 =====

    private static String cell(Map<Integer, String> row, int col) {
        if (row == null || col < 0) return "";
        String v = row.get(col);
        return v == null ? "" : v.trim();
    }

    private static String opt(Map<Integer, String> row, int col) {
        String v = cell(row, col);
        return v.isEmpty() ? null : v;
    }

    private static Long parseLong(String v) {
        if (v == null || v.isEmpty()) return null;
        String s = v.replaceAll("[^0-9]", "");
        if (s.isEmpty()) return null;
        try {
            return Long.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

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

    /** 单价/总价整数解析（有小数按四舍五入） */
    private static Integer parseInt(String v) {
        BigDecimal b = parseNum(v);
        if (b == null) return null;
        try {
            return b.setScale(0, RoundingMode.HALF_UP).intValueExact();
        } catch (ArithmeticException e) {
            return b.setScale(0, RoundingMode.HALF_UP).intValue();
        }
    }

    /** 房源状态：支持文字（未售/认购/已售/抵押/保留）与数字 */
    private static Integer parseStatus(String v) {
        if (v == null || v.isEmpty()) return null;
        String s = v.trim();
        if (s.contains("未售")) return 0;
        if (s.contains("认购")) return 1;
        if (s.contains("已售") || s.contains("成交")) return 2;
        if (s.contains("抵押")) return 3;
        if (s.contains("保留")) return 4;
        try {
            return Integer.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 承载 Excel 读取过程中的表头与数据行 */
    private static class YfyjExcelHolder {
        Map<Integer, String> header;
        List<Map<Integer, String>> rows = new ArrayList<>();
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
