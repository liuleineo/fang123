package com.fang123.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "cos")
public class CosProperties {

    private String secretId;
    private String secretKey;
    private String region;
    private String bucket;

    /** 是否在启动时把 corsOrigins 同步到桶 CORS（需要 PutBucketCORS 权限，无权限时仅告警） */
    private boolean autoCors = true;

    /**
     * 浏览器直传（预签名 PUT）允许的跨域来源。
     * 注意：COS 的 origin 通配符 https://*.example.com 不匹配裸域 https://example.com，
     * 必须把裸域单独列出来，否则预检 403 会导致前端回退到"经服务器中转"。
     */
    private List<String> corsOrigins = new ArrayList<>();
}
