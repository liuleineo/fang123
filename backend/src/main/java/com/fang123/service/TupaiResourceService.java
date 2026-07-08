package com.fang123.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fang123.entity.LoupanTupaiLand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 通过浙江自然资源交易平台 resourceId 拉取土拍地块信息
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TupaiResourceService {

    private final ObjectMapper objectMapper;
    private final LoupanTupaiLandService tupaiLandService;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String DETAIL_URL = "https://www.zjzrzyjy.com/trade/view/landbidding/queryResourceDetail?resourceId=%s";
    private static final String FILE_URL    = "https://www.zjzrzyjy.com/trade/view/landbidding/queryLandResourceUploadFile?resourceId=%s&fileType=QWT&currentPage=1&pageSize=100";

    /**
     * 批量拉取地块信息，每秒请求一次
     * @return { created: int, failed: int, errors: List<String> }
     */
    public Map<String, Object> fetchByResourceIds(List<String> resourceIds) {
        int created = 0, failed = 0;
        List<String> errors = new ArrayList<>();

        for (String resourceId : resourceIds) {
            try {
                boolean ok = fetchOne(resourceId.trim());
                if (ok) created++; else { failed++; errors.add(resourceId + ": 已存在"); }
            } catch (Exception e) {
                failed++;
                errors.add(resourceId + ": " + e.getMessage());
                log.error("Fetch tupai resource failed: {}", resourceId, e);
            }
            // 每秒一个，避免请求过快
            try { TimeUnit.SECONDS.sleep(1); } catch (InterruptedException ignored) {}
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("created", created);
        result.put("failed", failed);
        result.put("errors", errors);
        return result;
    }

    private boolean fetchOne(String resourceId) {
        // 1. 获取详情
        String detailUrl = String.format(DETAIL_URL, resourceId);
        String detailJson = restTemplate.getForObject(detailUrl, String.class);
        JsonNode root = parseJson(detailJson);
        JsonNode data = root.get("data");
        if (data == null || data.isNull()) throw new RuntimeException("接口返回无数据");

        // 检查 resourceId 是否已存在
        if (tupaiLandService.lambdaQuery().eq(LoupanTupaiLand::getResourceId, resourceId).count() > 0) {
            log.info("Tupai resource {} already exists, skip", resourceId);
            return false;
        }

        // 2. 获取位置示意图
        String imageUrl = null;
        try {
            String fileUrl = String.format(FILE_URL, resourceId);
            String fileJson = restTemplate.getForObject(fileUrl, String.class);
            JsonNode fileRoot = parseJson(fileJson);
            JsonNode records = fileRoot.at("/data/records");
            if (records != null && records.isArray() && records.size() > 0) {
                imageUrl = getText(records.get(0), "url");
            }
        } catch (Exception e) {
            log.warn("Fetch location image failed for {}: {}", resourceId, e.getMessage());
        }

        // 3. 解析坐标
        BigDecimal lng = null, lat = null;
        try {
            String coordStr = getText(data, "resourceCoordinate");
            if (coordStr != null && !coordStr.isEmpty()) {
                JsonNode coord = objectMapper.readTree(coordStr);
                JsonNode center = coord.get("center");
                if (center != null) {
                    if (center.has("lng") && !center.get("lng").isNull())
                        lng = BigDecimal.valueOf(center.get("lng").asDouble());
                    if (center.has("lat") && !center.get("lat").isNull())
                        lat = BigDecimal.valueOf(center.get("lat").asDouble());
                }
            }
        } catch (Exception e) {
            log.warn("Parse coordinate failed for {}: {}", resourceId, e.getMessage());
        }

        // 4. 构建实体
        LoupanTupaiLand entity = new LoupanTupaiLand();
        entity.setLandName(getText(data, "resourceName"));
        entity.setLandNo(getText(data, "resourceNumber"));
        entity.setDistrict(getText(data, "administrativeRegioncode"));
        entity.setPlate("");
        entity.setLandScope(getText(data, "resourceLocation"));
        entity.setLandStatus(1); // 已出让
        entity.setWinnerCompany(defaultIfNull(getText(data, "theUnit"), ""));
        entity.setLandUseType(defaultIfNull(getText(data, "primaryLandUse"), ""));
        entity.setLandYear(parseInt(data, "transferPeriodTo", 70));
        entity.setLandArea(parseBigDecimal(data, "assignmentArea"));
        entity.setBuildAreaLimit(null);
        entity.setPlotRatio(null);
        entity.setStartPrice(parseLong(data, "startPrice"));
        entity.setDealPrice(parseLong(data, "dealPrice"));
        entity.setFloorUnitPrice(null);
        entity.setPremiumRate(null);
        entity.setDealDate(parseDate(data, "endTime"));
        entity.setResourceId(resourceId);
        entity.setLocationImage(imageUrl);
        entity.setLongitude(lng);
        entity.setLatitude(lat);
        entity.setSort(0);

        tupaiLandService.save(entity);
        log.info("Tupai resource {} created: {}", resourceId, entity.getLandName());
        return true;
    }

    private JsonNode parseJson(String json) {
        try { return objectMapper.readTree(json); }
        catch (Exception e) { throw new RuntimeException("JSON解析失败: " + e.getMessage()); }
    }

    private String defaultIfNull(String val, String def) { return val != null ? val : def; }

    private String getText(JsonNode node, String field) {
        return node.has(field) && !node.get(field).isNull() ? node.get(field).asText() : null;
    }

    private Integer parseInt(JsonNode node, String field, int defaultVal) {
        try { return node.has(field) && !node.get(field).isNull() ? Integer.parseInt(node.get(field).asText()) : defaultVal; }
        catch (Exception e) { return defaultVal; }
    }

    private BigDecimal parseBigDecimal(JsonNode node, String field) {
        try { return node.has(field) && !node.get(field).isNull() ? new BigDecimal(node.get(field).asText()) : null; }
        catch (Exception e) { return null; }
    }

    private Long parseLong(JsonNode node, String field) {
        try { return node.has(field) && !node.get(field).isNull() ? Long.parseLong(node.get(field).asText()) : null; }
        catch (Exception e) { return null; }
    }

    private LocalDate parseDate(JsonNode node, String field) {
        try {
            String val = getText(node, field);
            if (val == null) return null;
            return LocalDateTime.parse(val.substring(0, 19), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).toLocalDate();
        } catch (Exception e) { return null; }
    }
}
