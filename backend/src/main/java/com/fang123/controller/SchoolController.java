package com.fang123.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fang123.common.Result;
import com.fang123.entity.School;
import com.fang123.service.CosService;
import com.fang123.service.SchoolService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class SchoolController {

    private final SchoolService schoolService;
    private final CosService cosService;
    private final ObjectMapper objectMapper;

    /** 公开-学校列表（地图展示，不分页，全部返回） */
    @GetMapping("/api/public/schools")
    public Result<List<School>> publicList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String schoolType,
            @RequestParam(required = false) String eduAdminDepartment,
            @RequestParam(required = false) Integer tier) {
        LambdaQueryWrapper<School> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            w.and(wr -> wr.like(School::getSchoolOrgName, keyword)
                    .or().like(School::getCampusName, keyword)
                    .or().like(School::getCampusCode, keyword)
                    .or().like(School::getCommunityNames, keyword));
        }
        if (StringUtils.hasText(schoolType)) {
            List<String> typeList = java.util.Arrays.stream(schoolType.split(","))
                    .map(String::trim).filter(StringUtils::hasText).toList();
            w.in(School::getSchoolType, typeList);
        }
        if (StringUtils.hasText(eduAdminDepartment)) w.eq(School::getEduAdminDepartment, eduAdminDepartment);
        if (tier != null) w.eq(School::getTier, tier);
        w.orderByAsc(School::getSchoolType).orderByAsc(School::getCampusCode);
        return Result.success(schoolService.list(w));
    }

    /** 公开-学校列表精简字段（地图标记用，减少数据传输） */
    @GetMapping("/api/public/schools/light")
    public Result<List<School>> publicLightList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String schoolType,
            @RequestParam(required = false) String eduAdminDepartment,
            @RequestParam(required = false) Integer tier) {
        LambdaQueryWrapper<School> w = new LambdaQueryWrapper<>();
        w.select(School::getCampusCode, School::getSchoolOrgName, School::getCampusName,
                School::getSchoolType, School::getTier, School::getLongitude, School::getLatitude,
                School::getEduAdminDepartment);
        if (StringUtils.hasText(keyword)) {
            w.and(wr -> wr.like(School::getSchoolOrgName, keyword)
                    .or().like(School::getCampusName, keyword)
                    .or().like(School::getCommunityNames, keyword));
        }
        if (StringUtils.hasText(schoolType)) {
            List<String> typeList = java.util.Arrays.stream(schoolType.split(","))
                    .map(String::trim).filter(StringUtils::hasText).toList();
            w.in(School::getSchoolType, typeList);
        }
        if (StringUtils.hasText(eduAdminDepartment)) w.eq(School::getEduAdminDepartment, eduAdminDepartment);
        if (tier != null) w.eq(School::getTier, tier);
        w.orderByAsc(School::getSchoolType).orderByAsc(School::getCampusCode);
        return Result.success(schoolService.list(w));
    }

    /** 公开-学校详情（点击标记后请求，返回完整数据含围栏） */
    @GetMapping("/api/public/schools/{campusCode}")
    public Result<School> publicDetail(@PathVariable String campusCode) {
        School entity = schoolService.getById(campusCode);
        if (entity == null) return Result.notFound("学校不存在");
        return Result.success(entity);
    }

    @GetMapping("/api/admin/schools")
    public Result<Page<School>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String schoolType,
            @RequestParam(required = false) String eduAdminDepartment,
            @RequestParam(required = false) Integer tier) {
        LambdaQueryWrapper<School> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            w.and(wr -> wr.like(School::getSchoolOrgName, keyword)
                    .or().like(School::getCampusName, keyword)
                    .or().like(School::getCampusCode, keyword)
                    .or().like(School::getCommunityNames, keyword));
        }
        if (StringUtils.hasText(schoolType)) w.eq(School::getSchoolType, schoolType);
        if (StringUtils.hasText(eduAdminDepartment)) w.eq(School::getEduAdminDepartment, eduAdminDepartment);
        if (tier != null) w.eq(School::getTier, tier);
        w.orderByAsc(School::getCampusCode);
        return Result.success(schoolService.page(new Page<>(page, size), w));
    }

    @GetMapping("/api/admin/schools/{campusCode}")
    public Result<School> detail(@PathVariable String campusCode) {
        School entity = schoolService.getById(campusCode);
        if (entity == null) return Result.notFound("学校不存在");
        return Result.success(entity);
    }

    /** 行政区下拉选项（去重） */
    @GetMapping("/api/admin/schools/departments")
    public Result<List<String>> departments() {
        LambdaQueryWrapper<School> w = new LambdaQueryWrapper<>();
        w.select(School::getEduAdminDepartment)
                .isNotNull(School::getEduAdminDepartment)
                .ne(School::getEduAdminDepartment, "")
                .groupBy(School::getEduAdminDepartment);
        List<String> list = schoolService.list(w).stream()
                .map(School::getEduAdminDepartment)
                .filter(StringUtils::hasText)
                .toList();
        return Result.success(list);
    }

    @PostMapping("/api/admin/schools")
    public Result<School> create(@RequestBody School entity) {
        if (!StringUtils.hasText(entity.getCampusCode())) return Result.badRequest("校区标识码不能为空");
        if (schoolService.getById(entity.getCampusCode()) != null) return Result.badRequest("该校区标识码已存在");
        schoolService.save(entity);
        return Result.success("创建成功", entity);
    }

    @PutMapping("/api/admin/schools/{campusCode}")
    public Result<School> update(@PathVariable String campusCode, @RequestBody School entity) {
        if (schoolService.getById(campusCode) == null) return Result.notFound("学校不存在");
        entity.setCampusCode(campusCode);
        schoolService.updateById(entity);
        return Result.success("更新成功", schoolService.getById(campusCode));
    }

    @DeleteMapping("/api/admin/schools/{campusCode}")
    public Result<Void> delete(@PathVariable String campusCode) {
        schoolService.removeById(campusCode);
        return Result.success();
    }

    /** 上传学校图片到 COS */
    @PostMapping("/api/admin/schools/upload")
    public Result<Map<String, String>> upload(@RequestParam(value = "file", required = false) MultipartFile file) {
        if (file == null || file.isEmpty()) return Result.badRequest("文件不能为空");
        String url = cosService.uploadFile(file, "school/hangzhou");
        return Result.success(Map.of("url", url));
    }

    /** 刷新学校小区数据：调用入学早知道 API，解析 result.appSchoolDistrictInfoEntityList 中 xqmc 拼接，更新 community_names */
    @PostMapping("/api/admin/schools/{campusCode}/refresh-community")
    public Result<Map<String, Object>> refreshCommunity(@PathVariable String campusCode) {
        School entity = schoolService.getById(campusCode);
        if (entity == null) return Result.notFound("学校不存在");
        try {
            String url = "https://rxyj.hzedu.gov.cn/hzjyAppServer/api/AppSchoolInfo/getSchoolInfo?year=2026&schoolName=" + campusCode;
            RestTemplate rt = new RestTemplate();
            String body = rt.getForObject(url, String.class);
            // 解析 JSON
            JsonNode root = objectMapper.readTree(body);
            JsonNode result = root.path("result");
            JsonNode list = result.path("appSchoolDistrictInfoEntityList");
            List<String> names = new java.util.ArrayList<>();
            if (list.isArray()) {
                for (JsonNode item : list) {
                    String xqmc = item.path("xqmc").asText("");
                    if (!xqmc.isBlank() && !names.contains(xqmc)) {
                        names.add(xqmc);
                    }
                }
            }
            String communityNames = String.join(",", names);
            entity.setCommunityNames(communityNames);
            // 解析 appSchoolInfoEntity：对口初中 + 学校简介
            JsonNode info = result.path("appSchoolInfoEntity");
            String directMiddleName = info.path("directMiddleSchoolName").asText("");
            String directMiddleCode = info.path("directMiddleSchoolCode").asText("");
            String schoolDetail = info.path("schoolDetail").asText("");
            if (!directMiddleName.isBlank()) entity.setTargetMiddleSchoolName(directMiddleName);
            if (!directMiddleCode.isBlank()) entity.setTargetMiddleSchoolCode(directMiddleCode);
            if (!schoolDetail.isBlank()) entity.setSchoolIntro(schoolDetail);
            schoolService.updateById(entity);
            Map<String, Object> resp = new java.util.HashMap<>();
            resp.put("communityNames", communityNames);
            resp.put("count", names.size());
            resp.put("directMiddleSchoolName", directMiddleName);
            resp.put("schoolDetail", schoolDetail);
            return Result.success("更新成功", resp);
        } catch (Exception e) {
            return Result.error(500, "刷新失败：" + e.getMessage());
        }
    }
}
