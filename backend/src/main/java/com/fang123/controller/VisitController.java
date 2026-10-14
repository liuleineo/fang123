package com.fang123.controller;

import com.fang123.common.Result;
import com.fang123.dto.VisitReportRequest;
import com.fang123.service.VisitEventService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 前端埋点上报入口。
 * <p>路径放在 {@code /api/public/**} 下（安全配置已放行），未登录也能上报；
 * 若请求带了有效 token，JwtAuthenticationFilter 会注入用户身份，这里就能把事件关联到用户。
 */
@RestController
@RequiredArgsConstructor
public class VisitController {

    private final VisitEventService visitEventService;

    @PostMapping("/api/public/visit/report")
    public Result<Void> report(@RequestBody(required = false) VisitReportRequest request,
                               HttpServletRequest httpRequest) {
        if (request != null && request.getEvents() != null && !request.getEvents().isEmpty()) {
            visitEventService.report(
                request.getEvents(),
                currentUserId(),
                clientIp(httpRequest),
                httpRequest.getHeader("User-Agent"));
        }
        return Result.success();
    }

    /** 登录用户 ID；匿名返回 null */
    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long userId) {
            return userId;
        }
        return null;
    }

    /** 取真实客户端 IP（生产环境在 Nginx 后面，优先读转发头） */
    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
