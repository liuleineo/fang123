package com.fang123.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fang123.common.IdObfuscator;
import com.fang123.common.Result;
import com.fang123.dto.LoupanSpotlightVO;
import com.fang123.entity.Loupan;
import com.fang123.entity.LoupanPresalePermit;
import com.fang123.service.LoupanPresalePermitService;
import com.fang123.service.LoupanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 公开-首页"预售公示 / 最新开盘"板块。
 *
 * <p>数据源：预售证信息表 {@code loupan_presale_permit}
 * <ul>
 *   <li>预售公示：公示日期 &gt;= 今天 且 公示日期 &lt; 核发日期</li>
 *   <li>最新开盘：核发日期在最近 7 天内（今天 - 7 天 &lt;= 核发日期 &lt;= 今天）</li>
 * </ul>
 * 同一楼盘存在多张预售证时只保留最靠前的一张，并过滤已删除/不存在的楼盘。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class LoupanSpotlightController {

    public static final String TYPE_PRESALE = "presale";
    public static final String TYPE_OPENING = "opening";
    /** 最新开盘：核发日期回溯统计的天数窗口（最近 7 天内取得预售证） */
    private static final int OPENING_WINDOW_DAYS = 7;
    /** 单页最大条数，避免一次返回过多数据 */
    private static final int MAX_PAGE_SIZE = 60;

    private final LoupanPresalePermitService permitService;
    private final LoupanService loupanService;

    /** 公开-两个板块的楼盘数量（首页按钮右上角角标） */
    @GetMapping("/api/public/loupan-spotlight/counts")
    public Result<Map<String, Integer>> counts() {
        LocalDate today = LocalDate.now();
        int presale = matchedLoupans(presalePermits(today)).size();
        int opening = matchedLoupans(openingPermits(today)).size();
        log.debug("[spotlight] counts today={} presale={} opening={}", today, presale, opening);
        return Result.success(Map.of(TYPE_PRESALE, presale, TYPE_OPENING, opening));
    }

    /** 公开-板块楼盘列表（type=presale|opening） */
    @GetMapping("/api/public/loupan-spotlight")
    public Result<Map<String, Object>> list(
            @RequestParam String type,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "12") Integer size) {
        if (!TYPE_PRESALE.equals(type) && !TYPE_OPENING.equals(type)) {
            return Result.badRequest("type 仅支持 presale / opening");
        }
        LocalDate today = LocalDate.now();
        List<LoupanPresalePermit> permits = TYPE_PRESALE.equals(type)
                ? presalePermits(today) : openingPermits(today);

        // 同一楼盘多张证时，按前面排好序的证保留第一条
        Map<Long, LoupanPresalePermit> permitByLoupan = new LinkedHashMap<>();
        permits.stream()
                .filter(p -> p.getLoupanId() != null)
                .forEach(p -> permitByLoupan.putIfAbsent(p.getLoupanId(), p));

        List<LoupanSpotlightVO> all = matchedLoupans(new ArrayList<>(permitByLoupan.values())).stream()
                .map(lp -> toVO(lp, permitByLoupan.get(lp.getId())))
                .toList();

        int pageNum = (page == null || page < 1) ? 1 : page;
        int pageSize = (size == null || size < 1) ? 12 : Math.min(size, MAX_PAGE_SIZE);
        int from = Math.min((pageNum - 1) * pageSize, all.size());
        int to = Math.min(from + pageSize, all.size());

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("type", type);
        data.put("total", all.size());
        data.put("page", pageNum);
        data.put("size", pageSize);
        data.put("records", all.subList(from, to));
        log.debug("[spotlight] list type={} total={} page={} size={}", type, all.size(), pageNum, pageSize);
        return Result.success(data);
    }

    /** 预售公示：公示日期 >= 今天 且 公示日期 < 核发日期 */
    private List<LoupanPresalePermit> presalePermits(LocalDate today) {
        return permitService.list(new LambdaQueryWrapper<LoupanPresalePermit>()
                .isNotNull(LoupanPresalePermit::getLoupanId)
                .isNotNull(LoupanPresalePermit::getPublicityDate)
                .ge(LoupanPresalePermit::getPublicityDate, today)
                .isNotNull(LoupanPresalePermit::getIssueDate)
                .apply("publicity_date < issue_date")
                .orderByAsc(LoupanPresalePermit::getPublicityDate)
                .orderByDesc(LoupanPresalePermit::getId));
    }

    /** 最新开盘：核发日期在最近 7 天内（今天 - 7 天 <= 核发日期 <= 今天） */
    private List<LoupanPresalePermit> openingPermits(LocalDate today) {
        return permitService.list(new LambdaQueryWrapper<LoupanPresalePermit>()
                .isNotNull(LoupanPresalePermit::getLoupanId)
                .isNotNull(LoupanPresalePermit::getIssueDate)
                .ge(LoupanPresalePermit::getIssueDate, today.minusDays(OPENING_WINDOW_DAYS))
                .le(LoupanPresalePermit::getIssueDate, today)
                .orderByDesc(LoupanPresalePermit::getIssueDate)
                .orderByDesc(LoupanPresalePermit::getId));
    }

    /** 按预售证顺序返回关联到的楼盘，过滤掉已删除/不存在的楼盘 */
    private List<Loupan> matchedLoupans(List<LoupanPresalePermit> permits) {
        if (permits.isEmpty()) return List.of();
        List<Long> ids = permits.stream()
                .map(LoupanPresalePermit::getLoupanId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) return List.of();
        Map<Long, Loupan> loupanMap = loupanService.listByIds(ids).stream()
                .collect(Collectors.toMap(Loupan::getId, Function.identity(), (a, b) -> a));
        List<Loupan> result = new ArrayList<>();
        for (LoupanPresalePermit p : permits) {
            Loupan lp = p.getLoupanId() == null ? null : loupanMap.get(p.getLoupanId());
            if (lp != null) result.add(lp);
        }
        return result;
    }

    private LoupanSpotlightVO toVO(Loupan lp, LoupanPresalePermit permit) {
        LoupanSpotlightVO vo = new LoupanSpotlightVO();
        BeanUtils.copyProperties(lp, vo);
        vo.setEncodedId(IdObfuscator.encode(lp.getId()));
        if (permit != null) {
            vo.setPublicityDate(permit.getPublicityDate());
            vo.setIssueDate(permit.getIssueDate());
            vo.setPermitNoStr(permit.getPermitNoStr());
            if (!StringUtils.hasText(vo.getSaleAddress())) vo.setSaleAddress(permit.getSaleAddress());
        }
        return vo;
    }
}
