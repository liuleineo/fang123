package com.fang123.common;

import java.util.List;

/**
 * 杭州行政区展示顺序工具。
 * <p>列表、下拉等场景统一按官方区划顺序排列，避免直接按中文字符 collation 排序导致的乱序
 * （例如「上城、临平、临安、余杭……」）。</p>
 */
public final class DistrictOrder {

    /** 杭州市行政区顺序（2021 年区划调整后） */
    public static final List<String> HANGZHOU = List.of(
            "上城区", "拱墅区", "西湖区", "滨江区", "萧山区",
            "余杭区", "临平区", "钱塘区", "富阳区", "临安区");

    private DistrictOrder() {
    }

    /** 行政区排序权重，未知或空值返回最大值（排最后） */
    public static int weight(String district) {
        if (district == null || district.isBlank()) return HANGZHOU.size();
        String value = district.trim();
        for (int i = 0; i < HANGZHOU.size(); i++) {
            String name = HANGZHOU.get(i);
            // 兼容「余杭」「余杭区」两种写法
            if (value.equals(name) || value.equals(withoutSuffix(name))) return i;
        }
        return HANGZHOU.size();
    }

    /** 行政区列表按官方区划顺序排序，未知项排在最后并保持原有的字母序 */
    public static List<String> sorted(List<String> districts) {
        return districts.stream()
                .sorted((a, b) -> {
                    int c = Integer.compare(weight(a), weight(b));
                    return c != 0 ? c : String.valueOf(a).compareTo(String.valueOf(b));
                })
                .toList();
    }

    /**
     * 生成按行政区排序的 ORDER BY 片段（未知行政区、空值排最后）。
     *
     * @param column 行政区列名，仅允许字母、数字、下划线，防止拼接 SQL 注入
     * @param tail   行政区之后的次级排序，如 {@code "sort DESC, create_time DESC"}，可为空
     */
    public static String orderByClause(String column, String tail) {
        if (column == null || !column.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("非法的排序列名: " + column);
        }
        StringBuilder sb = new StringBuilder("ORDER BY CASE");
        for (int i = 0; i < HANGZHOU.size(); i++) {
            // 前缀匹配，兼容数据带/不带「区」后缀（余杭 / 余杭区）
            sb.append(" WHEN ").append(column).append(" LIKE '")
              .append(withoutSuffix(HANGZHOU.get(i))).append("%' THEN ").append(i);
        }
        sb.append(" ELSE ").append(HANGZHOU.size()).append(" END, ").append(column);
        if (tail != null && !tail.isBlank()) sb.append(", ").append(tail);
        return sb.toString();
    }

    private static String withoutSuffix(String district) {
        return district.endsWith("区") ? district.substring(0, district.length() - 1) : district;
    }
}
