package com.fang123.service;

import com.fang123.config.CosProperties;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.region.Region;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CosService {

    private final CosProperties cosProperties;

    private COSClient cosClient;

    @PostConstruct
    public void init() {
        log.info("COS 凭证检查: secretId 前缀={}, region={}", 
            cosProperties.getSecretId() != null ? cosProperties.getSecretId().substring(0, Math.min(8, cosProperties.getSecretId().length())) + "..." : "NULL",
            cosProperties.getRegion());
        COSCredentials cred = new BasicCOSCredentials(cosProperties.getSecretId(), cosProperties.getSecretKey());
        ClientConfig clientConfig = new ClientConfig(new Region(cosProperties.getRegion()));
        this.cosClient = new COSClient(cred, clientConfig);
        log.debug("COS client initialized for bucket: {}", cosProperties.getBucket());
    }

    @PreDestroy
    public void destroy() {
        if (cosClient != null) {
            cosClient.shutdown();
            log.debug("COS client shutdown");
        }
    }

    /**
     * 上传头像图片到 COS
     * @param file  上传的图片文件
     * @param userId 用户ID，用作目录前缀
     * @return COS 访问 URL
     */
    public String uploadAvatar(MultipartFile file, Long userId) {
        // 校验文件类型
        String contentType = file.getContentType();
        if (contentType == null || !isAllowedImageType(contentType)) {
            throw new IllegalArgumentException("不支持的图片格式，仅支持 JPG、PNG、GIF、WebP");
        }

        // 校验文件大小（最大 5MB）
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("图片大小不能超过 5MB");
        }

        // 生成对象 Key: avatar/{userId}/{uuid}.{ext}
        String ext = getExtension(file.getOriginalFilename(), contentType);
        String key = "avatar/" + userId + "/" + UUID.randomUUID().toString().replace("-", "") + "." + ext;

        // 上传
        try (InputStream inputStream = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType);
            metadata.setContentLength(file.getSize());

            PutObjectRequest putRequest = new PutObjectRequest(
                cosProperties.getBucket(), key, inputStream, metadata
            );
            cosClient.putObject(putRequest);

            log.debug("Avatar uploaded: key={}, size={}", key, file.getSize());
        } catch (Exception e) {
            log.error("COS upload failed for user: {}", userId, e);
            throw new RuntimeException("头像上传失败，请稍后重试", e);
        }

        // 返回完整访问 URL
        return "https://" + cosProperties.getBucket() + ".cos." + cosProperties.getRegion() + ".myqcloud.com/" + key;
    }

    private boolean isAllowedImageType(String contentType) {
        return "image/jpeg".equals(contentType)
            || "image/png".equals(contentType)
            || "image/gif".equals(contentType)
            || "image/webp".equals(contentType);
    }

    /**
     * 通用文件上传到 COS
     * @param file   上传文件
     * @param folder 存储目录，如 media/loupan
     * @return COS 访问 URL
     */
    public String uploadFile(MultipartFile file, String folder) {
        String contentType = file.getContentType();
        if (file.getSize() > 20 * 1024 * 1024) {
            throw new IllegalArgumentException("文件大小不能超过 20MB");
        }
        String ext = getUploadExtension(file.getOriginalFilename(), contentType);
        String key = folder + "/" + UUID.randomUUID().toString().replace("-", "") + "." + ext;

        try (InputStream inputStream = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType != null ? contentType : "application/octet-stream");
            metadata.setContentLength(file.getSize());

            PutObjectRequest putRequest = new PutObjectRequest(
                cosProperties.getBucket(), key, inputStream, metadata
            );
            cosClient.putObject(putRequest);
            log.debug("File uploaded: key={}, size={}", key, file.getSize());
        } catch (Exception e) {
            log.error("COS upload failed: {}", folder, e);
            throw new RuntimeException("文件上传失败，请稍后重试", e);
        }
        return "https://" + cosProperties.getBucket() + ".cos." + cosProperties.getRegion() + ".myqcloud.com/" + key;
    }

    /**
     * 从 COS URL 提取 key 并删除文件
     */
    public void deleteByUrl(String url) {
        if (url == null || url.isEmpty()) return;
        try {
            String prefix = "https://" + cosProperties.getBucket() + ".cos." + cosProperties.getRegion() + ".myqcloud.com/";
            if (!url.startsWith(prefix)) return;
            String key = url.substring(prefix.length());
            cosClient.deleteObject(cosProperties.getBucket(), key);
            log.debug("COS file deleted: {}", key);
        } catch (Exception e) {
            log.error("COS delete failed for url: {}", url, e);
        }
    }

    private String getUploadExtension(String filename, String contentType) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        }
        if (contentType == null) return "bin";
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            case "video/mp4" -> "mp4";
            case "video/webm" -> "webm";
            default -> "bin";
        };
    }

    private String getExtension(String filename, String contentType) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        }
        return switch (contentType) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
    }
}
