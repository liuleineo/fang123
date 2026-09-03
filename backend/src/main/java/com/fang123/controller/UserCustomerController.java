package com.fang123.controller;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fang123.common.Result;
import com.fang123.dto.AiParseCustomerResult;
import com.fang123.entity.Customer;
import com.fang123.entity.CustomerShare;
import com.fang123.entity.FollowUp;
import com.fang123.entity.UserInfo;
import com.fang123.service.AiParseService;
import com.fang123.service.CustomerService;
import com.fang123.service.CustomerShareService;
import com.fang123.service.FollowUpService;
import com.fang123.service.UserInfoService;
import com.fang123.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user/customers")
@RequiredArgsConstructor
public class UserCustomerController {

    private final CustomerService customerService;
    private final FollowUpService followUpService;
    private final CustomerShareService customerShareService;
    private final UserInfoService userInfoService;
    private final AiParseService aiParseService;
    private final JwtUtil jwtUtil;

    private Long getUserId(String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        return jwtUtil.getUserIdFromToken(token);
    }

    /** 当前用户客户列表（本人 + 被分享给我的客户） */
    @GetMapping
    public Result<Page<Customer>> list(
            @RequestHeader("Authorization") String authHeader,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String intention,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortOrder) {
        Long userId = getUserId(authHeader);
        // 被分享给我的客户 id
        List<Long> sharedIds = customerShareService.list(
                new LambdaQueryWrapper<CustomerShare>()
                        .eq(CustomerShare::getSharedUserId, userId))
                .stream().map(CustomerShare::getCustomerId).toList();
        LambdaQueryWrapper<Customer> w = new LambdaQueryWrapper<>();
        if (sharedIds.isEmpty()) {
            w.eq(Customer::getUserId, userId);
        } else {
            w.and(wr -> wr.eq(Customer::getUserId, userId)
                    .or().in(Customer::getId, sharedIds));
        }
        // 关键词：姓名 / 手机号 / 需求备注 / 跟进记录内容
        if (StringUtils.hasText(keyword)) {
            List<Long> matchedIds = followUpService.list(
                            new LambdaQueryWrapper<FollowUp>()
                                    .like(FollowUp::getContent, keyword)
                                    .select(FollowUp::getCustomerId))
                    .stream().map(FollowUp::getCustomerId).distinct().toList();
            w.and(wr -> {
                wr.like(Customer::getName, keyword)
                        .or().like(Customer::getPhone, keyword)
                        .or().like(Customer::getRemark, keyword);
                if (!matchedIds.isEmpty()) wr.or().in(Customer::getId, matchedIds);
            });
        }
        // 意向筛选
        if (StringUtils.hasText(intention)) {
            w.eq(Customer::getIntention, intention);
        }
        // 排序：按最后跟进时间（无跟进记录的排最后）或默认按创建时间倒序
        if ("lastFollowUpTime".equals(sortField)) {
            String order = "asc".equalsIgnoreCase(sortOrder) ? "ASC" : "DESC";
            String sub = "(SELECT MAX(fu.follow_up_time) FROM follow_ups fu WHERE fu.customer_id = customers.id)";
            w.last("ORDER BY (" + sub + " IS NULL) ASC, " + sub + " " + order);
        } else {
            w.orderByDesc(Customer::getCreatedAt);
        }
        Page<Customer> result = customerService.page(new Page<>(page, size), w);
        fillLastFollowUp(result.getRecords());
        fillShareDesc(result.getRecords(), userId);
        return Result.success(result);
    }

    /** 为每个客户填充最后一次跟进时间和内容 */
    private void fillLastFollowUp(List<Customer> list) {
        if (list == null || list.isEmpty()) return;
        List<Long> ids = list.stream().map(Customer::getId).toList();
        List<FollowUp> all = followUpService.list(
                new LambdaQueryWrapper<FollowUp>()
                        .in(FollowUp::getCustomerId, ids)
                        .orderByDesc(FollowUp::getFollowUpTime)
                        .orderByDesc(FollowUp::getId));
        Map<Long, FollowUp> latest = new java.util.HashMap<>();
        for (FollowUp f : all) {
            latest.putIfAbsent(f.getCustomerId(), f);
        }
        list.forEach(c -> {
            FollowUp f = latest.get(c.getId());
            if (f != null) {
                c.setLastFollowUpTime(f.getFollowUpTime() != null ? f.getFollowUpTime() : f.getCreatedAt());
                c.setLastFollowUpContent(f.getContent());
            }
        });
    }

    /** 填充分享关系描述：分享给了谁 / 被谁分享 */
    private void fillShareDesc(List<Customer> list, Long userId) {
        if (list == null || list.isEmpty()) return;
        List<Long> ids = list.stream().map(Customer::getId).toList();
        // 我分享出去的（被分享人）
        List<CustomerShare> out = customerShareService.list(
                new LambdaQueryWrapper<CustomerShare>()
                        .eq(CustomerShare::getOwnerUserId, userId)
                        .in(CustomerShare::getCustomerId, ids));
        // 分享给我的（原拥有人）
        List<CustomerShare> in = customerShareService.list(
                new LambdaQueryWrapper<CustomerShare>()
                        .eq(CustomerShare::getSharedUserId, userId)
                        .in(CustomerShare::getCustomerId, ids));
        if (out.isEmpty() && in.isEmpty()) return;

        Map<Long, String> nickMap = new java.util.HashMap<>();
        if (!out.isEmpty()) {
            List<Long> uids = out.stream().map(CustomerShare::getSharedUserId).filter(java.util.Objects::nonNull).distinct().toList();
            if (!uids.isEmpty()) {
                userInfoService.listByIds(uids).forEach(u -> nickMap.put(u.getId(), u.getNickname()));
            }
        }
        if (!in.isEmpty()) {
            List<Long> uids = in.stream().map(CustomerShare::getOwnerUserId).filter(java.util.Objects::nonNull).distinct().toList();
            if (!uids.isEmpty()) {
                userInfoService.listByIds(uids).forEach(u -> nickMap.put(u.getId(), u.getNickname()));
            }
        }
        Map<Long, java.util.List<String>> outNames = new java.util.HashMap<>();
        for (CustomerShare s : out) {
            String nick = nickMap.get(s.getSharedUserId());
            if (nick != null && !nick.isBlank()) {
                outNames.computeIfAbsent(s.getCustomerId(), k -> new java.util.ArrayList<>()).add(nick);
            }
        }
        Map<Long, String> inName = new java.util.HashMap<>();
        for (CustomerShare s : in) {
            inName.putIfAbsent(s.getCustomerId(), nickMap.get(s.getOwnerUserId()));
        }
        list.forEach(c -> {
            java.util.List<String> parts = new java.util.ArrayList<>();
            java.util.List<String> names = outNames.get(c.getId());
            if (names != null && !names.isEmpty()) {
                parts.add("分享给了 " + String.join("、", names));
            }
            String from = inName.get(c.getId());
            if (from != null && !from.isBlank()) {
                parts.add(from + " 分享给我");
            }
            if (!parts.isEmpty()) {
                c.setShareDesc(String.join(" · ", parts));
            }
        });
    }

    /** 搜索用户（按昵称/手机号，排除自己），用于分享客户 */
    @GetMapping("/user-search")
    public Result<List<UserInfo>> userSearch(@RequestHeader("Authorization") String authHeader,
                                             @RequestParam String keyword) {
        Long userId = getUserId(authHeader);
        if (!StringUtils.hasText(keyword)) return Result.success(List.of());
        List<UserInfo> list = userInfoService.list(
                new LambdaQueryWrapper<UserInfo>()
                        .and(w -> w.like(UserInfo::getNickname, keyword)
                                .or().like(UserInfo::getPhone, keyword))
                        .ne(UserInfo::getId, userId)
                        .last("LIMIT 20"));
        return Result.success(list);
    }

    /** 当前用户客户详情 */
    @GetMapping("/{id}")
    public Result<Customer> detail(@RequestHeader("Authorization") String authHeader, @PathVariable Long id) {
        Long userId = getUserId(authHeader);
        Customer c = customerService.getById(id);
        if (c == null) return Result.notFound("客户不存在");
        if (!canAccess(userId, c)) return Result.notFound("客户不存在");
        return Result.success(c);
    }

    /** 录入客户（当前用户） */
    @PostMapping
    public Result<Customer> create(@RequestHeader("Authorization") String authHeader, @RequestBody Customer entity) {
        Long userId = getUserId(authHeader);
        entity.setId(null);
        entity.setUserId(userId);
        customerService.save(entity);
        return Result.success("录入成功", entity);
    }

    /** AI 识别客户资料图片（名片、登记表截图等），解析客户字段 */
    @PostMapping("/ai-parse")
    public Result<AiParseCustomerResult> aiParse(@RequestHeader("Authorization") String authHeader,
                                                 @RequestParam("files") MultipartFile[] files) {
        getUserId(authHeader);
        try {
            return Result.success(aiParseService.parseCustomer(files));
        } catch (Exception e) {
            return Result.error(500, "AI识别失败：" + e.getMessage());
        }
    }

    /** Excel 批量导入客户 */
    @PostMapping("/excel-import")
    public Result<Map<String, Object>> excelImport(@RequestHeader("Authorization") String authHeader,
                                                   @RequestParam("file") MultipartFile file) {
        Long userId = getUserId(authHeader);
        if (file == null || file.isEmpty()) return Result.badRequest("请上传Excel文件");
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!filename.endsWith(".xlsx") && !filename.endsWith(".xls")) {
            return Result.badRequest("仅支持 .xlsx / .xls 格式的Excel文件");
        }
        try {
            // 读取 Excel：第一行为表头，其余为数据行
            ExcelImportHolder holder = new ExcelImportHolder();
            EasyExcel.read(file.getInputStream())
                    .headRowNumber(1)
                    .registerReadListener(new AnalysisEventListener<Map<Integer, String>>() {
                        @Override
                        public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
                            holder.header = headMap;
                        }

                        @Override
                        public void invoke(Map<Integer, String> row, AnalysisContext context) {
                            holder.rows.add(row);
                        }

                        @Override
                        public void doAfterAllAnalysed(AnalysisContext context) {
                        }
                    })
                    .sheet()
                    .doRead();

            // 定位表头列
            int nameCol = -1, phoneCol = -1, intentionCol = -1, remarkCol = -1;
            if (holder.header != null) {
                for (Map.Entry<Integer, String> e : holder.header.entrySet()) {
                    String h = e.getValue() == null ? "" : e.getValue().trim();
                    if (h.isEmpty()) continue;
                    if (h.contains("姓名")) nameCol = e.getKey();
                    else if (h.contains("手机") || h.contains("电话")) phoneCol = e.getKey();
                    else if (h.contains("意向")) intentionCol = e.getKey();
                    else if (h.contains("备注") || h.contains("需求")) remarkCol = e.getKey();
                }
            }
            if (nameCol < 0) return Result.badRequest("未找到“姓名”列，请确保表头包含：姓名、手机号、意向、备注");

            int successCount = 0, failedCount = 0;
            List<Map<String, Object>> errors = new ArrayList<>();
            int rowNo = 1; // 数据行号（不含表头）
            for (Map<Integer, String> row : holder.rows) {
                rowNo++;
                if (row == null || row.isEmpty()) continue;
                String name = cell(row, nameCol);
                if (name.isEmpty()) {
                    failedCount++;
                    errors.add(Map.of("row", rowNo, "msg", "姓名为空"));
                    continue;
                }
                String phone = phoneCol >= 0 ? cell(row, phoneCol) : "";
                if (phone.isEmpty() || !phone.matches("\\d{11}")) {
                    failedCount++;
                    errors.add(Map.of("row", rowNo, "msg", "手机号格式不正确（需11位数字）"));
                    continue;
                }
                String intention = intentionCol >= 0 ? cell(row, intentionCol) : "";
                if (!intention.isEmpty() && !List.of("高", "中", "低").contains(intention)) {
                    intention = intention.contains("高") ? "高" : intention.contains("低") ? "低" : "中";
                }
                String remark = remarkCol >= 0 ? cell(row, remarkCol) : "";

                Customer c = new Customer();
                c.setUserId(userId);
                c.setName(name);
                c.setPhone(phone);
                c.setIntention(intention.isEmpty() ? null : intention);
                c.setRemark(remark.isEmpty() ? null : remark);
                customerService.save(c);
                successCount++;
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("total", successCount + failedCount);
            result.put("success", successCount);
            result.put("failed", failedCount);
            result.put("errors", errors);
            return Result.success(result);
        } catch (IOException e) {
            return Result.error(500, "Excel 解析失败：" + e.getMessage());
        }
    }

    /** 读取单元格（Map 模式下标），空安全 */
    private String cell(Map<Integer, String> row, int col) {
        if (row == null || col < 0) return "";
        String v = row.get(col);
        return v == null ? "" : v.trim();
    }

    /** 承载 Excel 读取过程中的表头与数据行 */
    private static class ExcelImportHolder {
        Map<Integer, String> header;
        List<Map<Integer, String>> rows = new ArrayList<>();
    }

    /** 更新客户（仅本人） */
    @PutMapping("/{id}")
    public Result<Customer> update(@RequestHeader("Authorization") String authHeader,
                                   @PathVariable Long id, @RequestBody Customer entity) {
        Long userId = getUserId(authHeader);
        Customer c = customerService.getById(id);
        if (c == null || !userId.equals(c.getUserId())) return Result.notFound("客户不存在");
        entity.setId(id);
        entity.setUserId(userId);
        customerService.updateById(entity);
        return Result.success("更新成功", customerService.getById(id));
    }

    /** 删除客户（仅本人） */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@RequestHeader("Authorization") String authHeader, @PathVariable Long id) {
        Long userId = getUserId(authHeader);
        Customer c = customerService.getById(id);
        if (c == null || !userId.equals(c.getUserId())) return Result.notFound("客户不存在");
        customerService.removeById(id);
        return Result.success();
    }

    /** 跟进记录列表（按客户，需有访问权） */
    @GetMapping("/{id}/follow-ups")
    public Result<List<FollowUp>> followUps(@RequestHeader("Authorization") String authHeader, @PathVariable Long id) {
        Long userId = getUserId(authHeader);
        Customer c = customerService.getById(id);
        if (c == null || !canAccess(userId, c)) return Result.notFound("客户不存在");
        List<FollowUp> list = followUpService.list(
                new LambdaQueryWrapper<FollowUp>()
                        .eq(FollowUp::getCustomerId, id)
                        .orderByDesc(FollowUp::getFollowUpTime)
                        .orderByDesc(FollowUp::getId));
        // 填充操作人昵称
        if (!list.isEmpty()) {
            List<Long> uids = list.stream().map(FollowUp::getUserId).distinct().toList();
            Map<Long, String> nickMap = userInfoService.listByIds(uids).stream()
                    .collect(Collectors.toMap(UserInfo::getId, UserInfo::getNickname, (a, b) -> a));
            list.forEach(f -> f.setUserNickname(nickMap.getOrDefault(f.getUserId(), "未知")));
        }
        return Result.success(list);
    }

    /** 添加跟进记录 */
    @PostMapping("/{id}/follow-ups")
    public Result<FollowUp> addFollowUp(@RequestHeader("Authorization") String authHeader,
                                        @PathVariable Long id, @RequestBody FollowUp entity) {
        Long userId = getUserId(authHeader);
        Customer c = customerService.getById(id);
        if (c == null || !canAccess(userId, c)) return Result.notFound("客户不存在");
        entity.setId(null);
        entity.setCustomerId(id);
        entity.setUserId(userId);
        if (entity.getFollowUpTime() == null) entity.setFollowUpTime(java.time.LocalDateTime.now());
        followUpService.save(entity);
        return Result.success("跟进成功", entity);
    }

    /** AI 分析客户，生成销冠沟通文案 */
    @PostMapping("/{id}/ai-suggest")
    public Result<String> aiSuggest(@RequestHeader("Authorization") String authHeader, @PathVariable Long id) {
        Long userId = getUserId(authHeader);
        Customer c = customerService.getById(id);
        if (c == null || !canAccess(userId, c)) return Result.notFound("客户不存在");
        List<FollowUp> ups = followUpService.list(
                new LambdaQueryWrapper<FollowUp>()
                        .eq(FollowUp::getCustomerId, id)
                        .orderByDesc(FollowUp::getFollowUpTime)
                        .orderByDesc(FollowUp::getId));
        List<String> followTexts = ups.stream()
                .map(f -> {
                    String time = f.getFollowUpTime() != null ? f.getFollowUpTime().toString().replace('T', ' ') : "";
                    String method = f.getMethod() != null ? f.getMethod() : "";
                    String content = f.getContent() != null ? f.getContent() : "";
                    return (time + " [" + method + "] " + content).trim();
                })
                .filter(s -> !s.isEmpty())
                .toList();
        try {
            String copy = aiParseService.suggestCustomerCopy(c.getName(), c.getRemark(), followTexts);
            return Result.success(copy);
        } catch (Exception e) {
            return Result.error(500, "AI分析失败：" + e.getMessage());
        }
    }

    /** 分享客户给其他用户（支持传 userId 或手机号） */
    @PostMapping("/{id}/share")
    public Result<CustomerShare> share(@RequestHeader("Authorization") String authHeader,
                                       @PathVariable Long id, @RequestBody Map<String, String> body) {
        Long userId = getUserId(authHeader);
        String phone = body.get("phone");
        String userIdStr = body.get("userId");
        if (!StringUtils.hasText(userIdStr) && !StringUtils.hasText(phone)) {
            return Result.badRequest("请选择要分享的用户");
        }
        Customer c = customerService.getById(id);
        if (c == null || !canAccess(userId, c)) return Result.notFound("客户不存在");
        UserInfo target;
        if (StringUtils.hasText(userIdStr)) {
            target = userInfoService.getById(Long.valueOf(userIdStr));
        } else {
            target = userInfoService.getOne(
                    new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getPhone, phone));
        }
        if (target == null) return Result.notFound("未找到该用户");
        if (target.getId().equals(userId)) return Result.badRequest("不能分享给自己");
        // 去重
        CustomerShare exist = customerShareService.getOne(
                new LambdaQueryWrapper<CustomerShare>()
                        .eq(CustomerShare::getCustomerId, id)
                        .eq(CustomerShare::getOwnerUserId, userId)
                        .eq(CustomerShare::getSharedUserId, target.getId()));
        if (exist != null) return Result.badRequest("已分享给该用户");
        CustomerShare share = new CustomerShare();
        share.setCustomerId(id);
        share.setOwnerUserId(userId);
        share.setSharedUserId(target.getId());
        customerShareService.save(share);
        return Result.success("分享成功", share);
    }

    /** 分享给我（被共享人视角）：共享人列表 */
    @GetMapping("/{id}/shares")
    public Result<List<CustomerShare>> shares(@RequestHeader("Authorization") String authHeader, @PathVariable Long id) {
        Long userId = getUserId(authHeader);
        Customer c = customerService.getById(id);
        if (c == null || !canAccess(userId, c)) return Result.notFound("客户不存在");
        List<CustomerShare> list = customerShareService.list(
                new LambdaQueryWrapper<CustomerShare>().eq(CustomerShare::getCustomerId, id));
        return Result.success(list);
    }

    private boolean canAccess(Long userId, Customer c) {
        if (userId.equals(c.getUserId())) return true;
        return customerShareService.count(
                new LambdaQueryWrapper<CustomerShare>()
                        .eq(CustomerShare::getCustomerId, c.getId())
                        .eq(CustomerShare::getSharedUserId, userId)) > 0;
    }
}
