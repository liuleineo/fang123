package com.fang123.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fang123.common.Result;
import com.fang123.entity.School;
import com.fang123.service.CosService;
import com.fang123.service.SchoolService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class SchoolController {

    private final SchoolService schoolService;
    private final CosService cosService;

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
        if (StringUtils.hasText(schoolType)) w.eq(School::getSchoolType, schoolType);
        if (StringUtils.hasText(eduAdminDepartment)) w.eq(School::getEduAdminDepartment, eduAdminDepartment);
        if (tier != null) w.eq(School::getTier, tier);
        w.orderByAsc(School::getSchoolType).orderByAsc(School::getCampusCode);
        return Result.success(schoolService.list(w));
    }

    @GetMapping("/api/admin/schools")
    public Result<Page<School>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String schoolType,
            @RequestParam(required = false) String eduAdminDepartment) {
        LambdaQueryWrapper<School> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            w.and(wr -> wr.like(School::getSchoolOrgName, keyword)
                    .or().like(School::getCampusName, keyword)
                    .or().like(School::getCampusCode, keyword)
                    .or().like(School::getCommunityNames, keyword));
        }
        if (StringUtils.hasText(schoolType)) w.eq(School::getSchoolType, schoolType);
        if (StringUtils.hasText(eduAdminDepartment)) w.eq(School::getEduAdminDepartment, eduAdminDepartment);
        w.orderByAsc(School::getCampusCode);
        return Result.success(schoolService.page(new Page<>(page, size), w));
    }

    @GetMapping("/api/admin/schools/{campusCode}")
    public Result<School> detail(@PathVariable String campusCode) {
        School entity = schoolService.getById(campusCode);
        if (entity == null) return Result.notFound("学校不存在");
        return Result.success(entity);
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
}
