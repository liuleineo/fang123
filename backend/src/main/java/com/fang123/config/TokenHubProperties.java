package com.fang123.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "tokenhub")
public class TokenHubProperties {

    /** API Key，从 TokenHub 控制台获取 */
    private String apiKey;
    /** TokenHub 基础地址 */
    private String baseUrl = "https://tokenhub.tencentmaas.com/v1";
    /** 模型 ID */
    private String model = "hy3-preview";
}
