package com.fang123.service;

import com.fang123.config.CosProperties;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.region.Region;
import com.qcloud.cos.transfer.TransferManager;
import com.qcloud.cos.transfer.TransferManagerConfiguration;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CosService {

    /** 超过该大小改用分片并发上传（单连接 PUT 难以跑满带宽，大视频上传尤其明显） */
    private static final long MULTIPART_THRESHOLD = 20L * 1024 * 1024;
    /** 分片大小：10MB（默认 5MB，分片过碎会带来额外的请求开销） */
    private static final long PART_SIZE = 10L * 1024 * 1024;
    /** 分片上传并发数 */
    private static final int UPLOAD_CONCURRENCY = 8;
    /** 素材单文件上限 500MB（与 multipart、Nginx client_max_body_size 保持一致） */
    private static final long MAX_FILE_SIZE = 500L * 1024 * 1024;
    /** 直传预签名 URL 有效期（秒），过期后需重新申请 */
    private static final long PRESIGN_EXPIRE_SECONDS = 600;
    /** 允许直传的扩展名白名单（桶为公共读，避免被用来托管任意类型文件） */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        "jpg", "jpeg", "png", "gif", "webp", "bmp",
        "mp4", "mov", "webm", "avi", "mkv", "m4v"
    );

    private final CosProperties cosProperties;

    private COSClient cosClient;
    private TransferManager transferManager;

    @PostConstruct
    public void init() {
        log.info("COS 凭证检查: secretId 前缀={}, region={}", 
            cosProperties.getSecretId() != null ? cosProperties.getSecretId().substring(0, Math.min(8, cosProperties.getSecretId().length())) + "..." : "NULL",
            cosProperties.getRegion());
        COSCredentials cred = new BasicCOSCredentials(cosProperties.getSecretId(), cosProperties.getSecretKey());
        ClientConfig clientConfig = new ClientConfig(new Region(cosProperties.getRegion()));
        // 大文件上传耗时长，放宽单连接读写超时（SDK 默认 50s），避免长传过程中被判超时
        clientConfig.setConnectionTimeout(30_000);
        clientConfig.setSocketTimeout(120_000);
        this.cosClient = new COSClient(cred, clientConfig);

        this.transferManager = new TransferManager(cosClient, Executors.newFixedThreadPool(UPLOAD_CONCURRENCY, r -> {
            Thread t = new Thread(r, "cos-upload");
            t.setDaemon(true);
            return t;
        }));
        TransferManagerConfiguration tmConfig = new TransferManagerConfiguration();
        tmConfig.setMultipartUploadThreshold(MULTIPART_THRESHOLD);
        tmConfig.setMinimumUploadPartSize(PART_SIZE);
        this.transferManager.setConfiguration(tmConfig);
        log.info("COS client initialized for bucket: {}, multipartThreshold={}MB, partSize={}MB, concurrency={}",
            cosProperties.getBucket(), MULTIPART_THRESHOLD / 1024 / 1024, PART_SIZE / 1024 / 1024, UPLOAD_CONCURRENCY);
    }

    @PreDestroy
    public void destroy() {
        if (transferManager != null) {
            transferManager.shutdownNow(true);
            log.debug("COS transfer manager shutdown");
        }
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
        // 素材上限 500MB（与 multipart max-file-size、Nginx client_max_body_size 保持一致）
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件大小不能超过 500MB");
        }
        String ext = getUploadExtension(file.getOriginalFilename(), contentType);
        String key = folder + "/" + UUID.randomUUID().toString().replace("-", "") + "." + ext;

        long size = file.getSize();
        boolean multipart = size > MULTIPART_THRESHOLD;
        long started = System.currentTimeMillis();
        log.info("COS upload start: key={}, size={}MB, mode={}", key, String.format("%.1f", size / 1024d / 1024d),
            multipart ? "multipart" : "simple");

        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(contentType != null ? contentType : "application/octet-stream");
            metadata.setContentLength(size);

            if (multipart) {
                uploadByMultipart(file, key, metadata);
            } else {
                try (InputStream inputStream = file.getInputStream()) {
                    cosClient.putObject(new PutObjectRequest(cosProperties.getBucket(), key, inputStream, metadata));
                }
            }
        } catch (Exception e) {
            log.error("COS upload failed: key={}, size={}MB, cost={}ms", key, size / 1024 / 1024,
                System.currentTimeMillis() - started, e);
            throw new RuntimeException("文件上传失败，请稍后重试", e);
        }

        long cost = System.currentTimeMillis() - started;
        double mb = size / 1024d / 1024d;
        log.info("COS upload done: key={}, size={}MB, mode={}, cost={}ms, avg={}MB/s", key, String.format("%.1f", mb),
            multipart ? "multipart" : "simple", cost, cost > 0 ? String.format("%.2f", mb / (cost / 1000d)) : "-");
        return buildPublicUrl(key);
    }

    /**
     * 生成前端直传（浏览器 → COS）用的预签名 PUT 地址。
     * 直传不经过服务器中转：速度只取决于客户端上行带宽，也不占用服务器出口带宽，
     * 大视频上传通常比"浏览器→服务器→COS"两段串行快数倍。
     */
    public PresignInfo presignPut(String folder, String originalFilename, String contentType, long size) {
        if (size <= 0) {
            throw new IllegalArgumentException("文件大小无效");
        }
        if (size > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("文件大小不能超过 500MB");
        }
        String ext = resolveAllowedExtension(originalFilename, contentType);
        String key = folder + "/" + UUID.randomUUID().toString().replace("-", "") + "." + ext;

        GeneratePresignedUrlRequest req = new GeneratePresignedUrlRequest(
            cosProperties.getBucket(), key, HttpMethodName.PUT);
        req.setExpiration(new Date(System.currentTimeMillis() + PRESIGN_EXPIRE_SECONDS * 1000));
        // 签名绑定 Content-Type，前端 PUT 时必须带同一个 Content-Type，否则签名不匹配
        if (contentType != null && !contentType.isBlank()) {
            req.setContentType(contentType);
        }

        URL uploadUrl;
        try {
            uploadUrl = cosClient.generatePresignedUrl(req);
        } catch (Exception e) {
            log.error("COS presign failed: key={}, size={}MB", key, size / 1024 / 1024, e);
            throw new RuntimeException("获取上传地址失败，请稍后重试", e);
        }
        log.info("COS presign: key={}, size={}MB, expire={}s", key, size / 1024 / 1024, PRESIGN_EXPIRE_SECONDS);
        return new PresignInfo(key, uploadUrl.toString(), buildPublicUrl(key));
    }

    /** 扩展名白名单校验：桶为公共读，限制类型避免被用来托管任意文件 */
    private String resolveAllowedExtension(String filename, String contentType) {
        String ext = getUploadExtension(filename, contentType);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("不支持的文件类型（." + ext + "），仅支持常见图片与视频");
        }
        return ext;
    }

    private String buildPublicUrl(String key) {
        return "https://" + cosProperties.getBucket() + ".cos." + cosProperties.getRegion() + ".myqcloud.com/" + key;
    }

    /** 直传预签名信息：key 为对象键，uploadUrl 供前端 PUT，url 为最终可访问地址 */
    public record PresignInfo(String key, String uploadUrl, String url) {
    }

    /**
     * 大文件分片并发上传。
     * 分片要求内容可随机读取，MultipartFile 的流不可重置，因此先落到本地临时文件再交给 TransferManager。
     */
    private void uploadByMultipart(MultipartFile file, String key, ObjectMetadata metadata) throws Exception {
        Path temp = Files.createTempFile("cos-upload-", ".tmp");
        try {
            file.transferTo(temp.toFile());
            metadata.setContentLength(Files.size(temp));
            PutObjectRequest putRequest = new PutObjectRequest(cosProperties.getBucket(), key, temp.toFile());
            putRequest.setMetadata(metadata);
            transferManager.upload(putRequest).waitForUploadResult();
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    /**
     * 上传图片并压缩：宽2000px，高自适应，输出JPG格式
     */
    public String uploadAndCompress(MultipartFile file, String folder) {
        try {
            byte[] compressed = compressImage(file.getBytes(), file.getContentType());
            String key = folder + "/" + UUID.randomUUID() + ".jpg";
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(compressed.length);
            metadata.setContentType("image/jpeg");
            cosClient.putObject(cosProperties.getBucket(), key, new ByteArrayInputStream(compressed), metadata);
            return "https://" + cosProperties.getBucket() + ".cos." + cosProperties.getRegion() + ".myqcloud.com/" + key;
        } catch (Exception e) {
            log.error("Image compress failed, fallback to original upload", e);
            return uploadFile(file, folder);
        }
    }

    /** 图片压缩：宽2000px，JPG格式 */
    private byte[] compressImage(byte[] input, String contentType) throws Exception {
        String type = contentType != null && contentType.contains("/") ? contentType.substring(contentType.indexOf("/") + 1) : "jpg";
        if ("jpeg".equalsIgnoreCase(type)) type = "jpg";
        if ("png".equalsIgnoreCase(type)) type = "png";

        java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(input));
        if (img == null) throw new RuntimeException("无法解析图片");

        int w = img.getWidth(), h = img.getHeight();
        if (w > 2000) {
            h = h * 2000 / w;
            w = 2000;
            java.awt.image.BufferedImage scaled = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = scaled.createGraphics();
            g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(img, 0, 0, w, h, null);
            g.dispose();
            img = scaled;
        } else {
            // 原图宽度不大，但需要转 RGB（JPG 不支持透明）
            java.awt.image.BufferedImage rgb = new java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = rgb.createGraphics();
            g.drawImage(img, 0, 0, null);
            g.dispose();
            img = rgb;
        }

        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "jpg", out);
        return out.toByteArray();
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
