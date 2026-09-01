package com.fang123.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 宽松的 LocalDate 反序列化器：
 * 兼容 "2026-08"（自动补日）、"2026年8月"、"2026-08-01"、空字符串等格式，
 * 无法解析时返回 null 而不是抛异常，避免接口 500。
 */
public class LenientLocalDateDeserializer extends JsonDeserializer<LocalDate> {

    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4})[-年./](\\d{1,2})(?:[-月./](\\d{1,2}))?");

    @Override
    public LocalDate deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String s = p.getValueAsString();
        if (s == null || s.isBlank()) return null;
        s = s.trim();

        Matcher m = DATE_PATTERN.matcher(s);
        if (m.find()) {
            try {
                int year = Integer.parseInt(m.group(1));
                int month = Integer.parseInt(m.group(2));
                int day = m.group(3) != null ? Integer.parseInt(m.group(3)) : 1;
                return LocalDate.of(year, month, day);
            } catch (Exception ignored) {
                // 非法日期（如 2026-13），继续尝试其他解析
            }
        }
        try {
            return LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception ignored) {
            return null;
        }
    }
}
