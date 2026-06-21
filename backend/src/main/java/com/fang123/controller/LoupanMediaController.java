package com.fang123.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fang123.common.Result;
import com.fang123.entity.LoupanMedia;
import com.fang123.service.CosService;
import com.fang123.service.LoupanMediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class LoupanMediaController {

    private final LoupanMediaService mediaService;
    private final CosService cosService;

    @GetMapping("/api/admin/medias")
    public Result<Page<LoupanMedia>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long loupanId) {
        LambdaQueryWrapper<LoupanMedia> w = new LambdaQueryWrapper<>();
        if (loupanId != null) w.eq(LoupanMedia::getLoupanId, loupanId);
        if (StringUtils.hasText(keyword)) w.like(LoupanMedia::getMediaTitle, keyword);
        w.orderByDesc(LoupanMedia::getSort).orderByDesc(LoupanMedia::getCreateTime);
        return Result.success(mediaService.page(new Page<>(page, size), w));
    }

    @GetMapping("/api/admin/medias/{id}")
    public Result<LoupanMedia> detail(@PathVariable Long id) {
        LoupanMedia entity = mediaService.getById(id);
        if (entity == null) return Result.notFound("素材不存在");
        return Result.success(entity);
    }

    @PostMapping("/api/admin/medias")
    public Result<LoupanMedia> create(@RequestBody LoupanMedia entity) {
        entity.setId(null);
        mediaService.save(entity);
        return Result.success("创建成功", entity);
    }

    @PutMapping("/api/admin/medias/{id}")
    public Result<LoupanMedia> update(@PathVariable Long id, @RequestBody LoupanMedia entity) {
        if (mediaService.getById(id) == null) return Result.notFound("素材不存在");
        entity.setId(id);
        mediaService.updateById(entity);
        return Result.success("更新成功", mediaService.getById(id));
    }

    @DeleteMapping("/api/admin/medias/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        mediaService.removeById(id);
        return Result.success();
    }

    /** 上传素材文件到 COS */
    @PostMapping("/api/admin/medias/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        String url = cosService.uploadFile(file, "media/loupan");
        return Result.success(Map.of("url", url));
    }
}
