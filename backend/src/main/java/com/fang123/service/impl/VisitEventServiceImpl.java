package com.fang123.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fang123.dto.VisitEventDTO;
import com.fang123.entity.VisitEvent;
import com.fang123.mapper.VisitEventMapper;
import com.fang123.service.VisitEventService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 访问事件写入服务。
 * <p>设计要点：
 * <ul>
 *   <li>上报接口只入队，独立线程按 500 条 / 1 秒批量写库，统计写入不占用业务请求线程；</li>
 *   <li>队列有界（2 万条），打满时丢弃而不阻塞业务；</li>
 *   <li>关闭时把队列剩余数据写完，避免重启丢数据；</li>
 *   <li>爬虫 UA 直接丢弃，避免监控/搜索引擎把 PV 刷高。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisitEventServiceImpl extends ServiceImpl<VisitEventMapper, VisitEvent> implements VisitEventService {

    /** 单次上报最多接受的事件数（防止被刷接口塞垃圾数据） */
    private static final int MAX_EVENTS_PER_REPORT = 50;
    private static final int QUEUE_CAPACITY = 20000;
    private static final int BATCH_SIZE = 500;
    private static final long FLUSH_INTERVAL_MS = 1000L;
    /** 客户端时间与服务器时间偏差超过该范围就认为不可信（时区错误、改系统时间） */
    private static final Duration MAX_CLOCK_SKEW = Duration.ofHours(12);

    private static final Pattern BOT_UA = Pattern.compile(
        "bot|spider|crawl|slurp|bingpreview|python-requests|curl/|wget|headless|lighthouse|phantomjs",
        Pattern.CASE_INSENSITIVE);

    private final BlockingQueue<VisitEvent> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);

    private volatile boolean running = true;
    private Thread worker;

    @PostConstruct
    public void start() {
        worker = new Thread(this::consumeLoop, "visit-event-writer");
        worker.setDaemon(true);
        worker.start();
        log.info("访问统计写入线程已启动：队列容量={}, 批量={}条/{}ms", QUEUE_CAPACITY, BATCH_SIZE, FLUSH_INTERVAL_MS);
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (worker != null) {
            worker.interrupt();
            try {
                worker.join(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        List<VisitEvent> rest = new ArrayList<>();
        queue.drainTo(rest);
        if (!rest.isEmpty()) {
            log.info("访问统计退出前写入剩余 {} 条事件", rest.size());
            flush(rest);
        }
    }

    @Override
    public void report(List<VisitEventDTO> events, Long userId, String ip, String userAgent) {
        if (events == null || events.isEmpty()) {
            return;
        }
        if (isBot(userAgent)) {
            return;
        }

        String ipHash = md5(ip);
        String uaBrief = briefUserAgent(userAgent);
        int limit = Math.min(events.size(), MAX_EVENTS_PER_REPORT);
        int accepted = 0;

        for (int i = 0; i < limit; i++) {
            VisitEvent event = convert(events.get(i), userId, ipHash, uaBrief);
            if (event == null) {
                continue;
            }
            if (!queue.offer(event)) {
                log.warn("访问统计队列已满（{}），本次剩余 {} 条事件被丢弃", QUEUE_CAPACITY, limit - i);
                break;
            }
            accepted++;
        }
        if (accepted > 0 && log.isDebugEnabled()) {
            log.debug("访问统计入队 {} 条，当前队列 {} 条", accepted, queue.size());
        }
    }

    /** 消费线程：攒够一批或超过 1 秒就落库 */
    private void consumeLoop() {
        List<VisitEvent> buffer = new ArrayList<>(BATCH_SIZE);
        while (running) {
            try {
                VisitEvent first = queue.poll(FLUSH_INTERVAL_MS, TimeUnit.MILLISECONDS);
                if (first == null) {
                    continue;
                }
                buffer.clear();
                buffer.add(first);
                queue.drainTo(buffer, BATCH_SIZE - 1);
                flush(buffer);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                log.warn("访问统计批量写入失败，已丢弃 {} 条：{}", buffer.size(), e.getMessage());
            }
        }
    }

    private void flush(List<VisitEvent> events) {
        try {
            saveBatch(new ArrayList<>(events), BATCH_SIZE);
        } catch (Exception e) {
            log.warn("访问统计批量写入异常（{} 条）：{}", events.size(), e.getMessage());
        }
    }

    private VisitEvent convert(VisitEventDTO dto, Long userId, String ipHash, String uaBrief) {
        if (dto == null) {
            return null;
        }
        String route = trim(dto.getRoute(), 255);
        String type = StringUtils.hasText(dto.getType()) ? trim(dto.getType(), 32) : "page_view";
        String deviceId = trim(dto.getDeviceId(), 64);
        // 页面浏览必须有路由（无路由的 page_view 是脏数据），其余事件允许没有
        if (route.isEmpty() && "page_view".equals(type)) {
            return null;
        }

        VisitEvent event = new VisitEvent();
        event.setUserId(userId);
        event.setDeviceId(deviceId);
        event.setSessionId(trim(dto.getSessionId(), 64));
        event.setPlatform(StringUtils.hasText(dto.getPlatform()) ? trim(dto.getPlatform(), 16) : "web");
        event.setEventType(type);
        event.setRoute(route);
        event.setPageTitle(trim(dto.getTitle(), 128));
        event.setReferrer(trim(dto.getReferrer(), 500));
        event.setChannel(resolveChannel(dto.getChannel(), dto.getReferrer()));
        event.setIpHash(ipHash);
        event.setUaBrief(uaBrief);
        event.setDurationMs(dto.getDuration() == null || dto.getDuration() < 0
            ? 0 : (int) Math.min(dto.getDuration(), Integer.MAX_VALUE));
        event.setEventTime(resolveEventTime(dto.getTs()));
        // 登录用户按 user_id 去重，匿名按 device_id 去重
        event.setVisitorKey(userId != null ? "u:" + userId : (deviceId.isEmpty() ? "" : "d:" + deviceId));
        return event;
    }

    private LocalDateTime resolveEventTime(Long ts) {
        LocalDateTime now = LocalDateTime.now();
        if (ts == null || ts <= 0) {
            return now;
        }
        LocalDateTime client = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault());
        // 客户端时间与服务器时间偏差过大时不可信，回落到服务端时间
        if (client.isAfter(now.plus(MAX_CLOCK_SKEW)) || client.isBefore(now.minus(MAX_CLOCK_SKEW))) {
            return now;
        }
        return client;
    }

    /** 渠道：前端已判断的优先，否则按 referrer 兜底推导 */
    private String resolveChannel(String explicit, String referrer) {
        if (StringUtils.hasText(explicit)) {
            return trim(explicit, 64);
        }
        if (!StringUtils.hasText(referrer)) {
            return "direct";
        }
        try {
            String host = URI.create(referrer).getHost();
            if (host == null) {
                return "direct";
            }
            String lower = host.toLowerCase();
            if (lower.contains("weixin") || lower.contains("wechat") || lower.endsWith("qq.com")) {
                return "wechat";
            }
            if (lower.contains("baidu") || lower.contains("google") || lower.contains("bing")
                || lower.contains("sogou") || lower.contains("so.com") || lower.contains("sm.cn")) {
                return "search";
            }
            return trim(lower, 64);
        } catch (Exception e) {
            return "direct";
        }
    }

    private boolean isBot(String userAgent) {
        return userAgent == null || BOT_UA.matcher(userAgent).find();
    }

    /** UA 只留"系统/浏览器"两位，避免存储完整指纹 */
    private String briefUserAgent(String ua) {
        if (!StringUtils.hasText(ua)) {
            return "";
        }
        String os;
        if (ua.contains("Android")) {
            os = "Android";
        } else if (ua.contains("iPhone") || ua.contains("iPad") || ua.contains("iOS")) {
            os = "iOS";
        } else if (ua.contains("Windows")) {
            os = "Windows";
        } else if (ua.contains("Mac OS")) {
            os = "macOS";
        } else if (ua.contains("Linux")) {
            os = "Linux";
        } else {
            os = "其他";
        }

        String browser;
        if (ua.contains("MicroMessenger")) {
            browser = "微信";
        } else if (ua.contains("Edg/")) {
            browser = "Edge";
        } else if (ua.contains("Firefox")) {
            browser = "Firefox";
        } else if (ua.contains("Chrome")) {
            browser = "Chrome";
        } else if (ua.contains("Safari")) {
            browser = "Safari";
        } else {
            browser = "其他";
        }
        return os + "/" + browser;
    }

    private String md5(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private String trim(String text, int maxLength) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String value = text.trim();
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }
}
