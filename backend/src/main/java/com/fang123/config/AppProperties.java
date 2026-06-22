package com.fang123.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    /** 网站域名，如 https://niurouzhou.com */
    private String domain = "http://localhost:4000";
}
