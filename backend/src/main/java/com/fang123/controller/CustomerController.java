package com.fang123.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fang123.common.Result;
import com.fang123.entity.Customer;
import com.fang123.entity.CustomerShare;
import com.fang123.entity.FollowUp;
import com.fang123.entity.UserInfo;
import com.fang123.service.CustomerService;
import com.fang123.service.CustomerShareService;
import com.fang123.service.FollowUpService;
import com.fang123.service.UserInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;
    private final UserInfoService userInfoService;
    private final CustomerShareService customerShareService;
    private final FollowUpService followUpService;

    /** 客户列表（分页 + 姓名/手机号关键词 + 所属用户过滤） */
    @GetMapping("/api/admin/customers")
    public Result<Page<Customer>> list(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long userId) {
        LambdaQueryWrapper<Customer> w = new LambdaQueryWrapper<>();
        if (userId != null) w.eq(Customer::getUserId, userId);
        if (StringUtils.hasText(keyword)) {
            w.and(wr -> wr.like(Customer::getName, keyword)
                    .or().like(Customer::getPhone, keyword));
        }
        w.orderByDesc(Customer::getCreatedAt);
        Page<Customer> result = customerService.page(new Page<>(page, size), w);
        fillUserInfo(result.getRecords());
        fillSharedTo(result.getRecords());
        fillLastFollowUp(result.getRecords());
        return Result.success(result);
    }

    @GetMapping("/api/admin/customers/{id}")
    public Result<Customer> detail(@PathVariable Long id) {
        Customer entity = customerService.getById(id);
        if (entity == null) return Result.notFound("客户不存在");
        fillUserInfo(List.of(entity));
        fillSharedTo(List.of(entity));
        fillLastFollowUp(List.of(entity));
        return Result.success(entity);
    }

    /** 客户跟进记录列表（按跟进时间倒序，含操作人昵称） */
    @GetMapping("/api/admin/customers/{id}/follow-ups")
    public Result<List<FollowUp>> followUps(@PathVariable Long id) {
        Customer customer = customerService.getById(id);
        if (customer == null) return Result.notFound("客户不存在");
        List<FollowUp> list = followUpService.list(
                new LambdaQueryWrapper<FollowUp>()
                        .eq(FollowUp::getCustomerId, id)
                        .orderByDesc(FollowUp::getFollowUpTime)
                        .orderByDesc(FollowUp::getId));
        if (!list.isEmpty()) {
            List<Long> uids = list.stream().map(FollowUp::getUserId).filter(Objects::nonNull).distinct().toList();
            if (!uids.isEmpty()) {
                Map<Long, String> nickMap = userInfoService.listByIds(uids).stream()
                        .collect(Collectors.toMap(UserInfo::getId, UserInfo::getNickname, (a, b) -> a));
                list.forEach(f -> f.setUserNickname(nickMap.getOrDefault(f.getUserId(), "未知")));
            }
        }
        return Result.success(list);
    }

    @PostMapping("/api/admin/customers")
    public Result<Customer> create(@RequestBody Customer entity) {
        entity.setId(null);
        customerService.save(entity);
        return Result.success("创建成功", entity);
    }

    @PutMapping("/api/admin/customers/{id}")
    public Result<Customer> update(@PathVariable Long id, @RequestBody Customer entity) {
        if (customerService.getById(id) == null) return Result.notFound("客户不存在");
        entity.setId(id);
        customerService.updateById(entity);
        return Result.success("更新成功", customerService.getById(id));
    }

    @DeleteMapping("/api/admin/customers/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        customerService.removeById(id);
        return Result.success();
    }

    /** 批量填充所属用户昵称/手机号 */
    private void fillUserInfo(List<Customer> list) {
        if (list == null || list.isEmpty()) return;
        List<Long> userIds = list.stream().map(Customer::getUserId).filter(Objects::nonNull).distinct().toList();
        if (userIds.isEmpty()) return;
        Map<Long, UserInfo> userMap = userInfoService.listByIds(userIds).stream()
                .collect(Collectors.toMap(UserInfo::getId, u -> u, (a, b) -> a));
        list.forEach(c -> {
            UserInfo u = userMap.get(c.getUserId());
            if (u != null) {
                c.setUserNickname(u.getNickname());
                c.setUserPhone(u.getPhone());
            }
        });
    }

    /** 批量填充被分享人昵称（customer_shares 关联 user_info） */
    private void fillSharedTo(List<Customer> list) {
        if (list == null || list.isEmpty()) return;
        List<Long> ids = list.stream().map(Customer::getId).filter(Objects::nonNull).toList();
        List<CustomerShare> shares = customerShareService.list(
                new LambdaQueryWrapper<CustomerShare>().in(CustomerShare::getCustomerId, ids));
        if (shares.isEmpty()) return;
        List<Long> sharedUserIds = shares.stream().map(CustomerShare::getSharedUserId).filter(Objects::nonNull).distinct().toList();
        Map<Long, String> nickMap = sharedUserIds.isEmpty() ? Map.of()
                : userInfoService.listByIds(sharedUserIds).stream()
                        .collect(Collectors.toMap(UserInfo::getId, UserInfo::getNickname, (a, b) -> a));
        Map<Long, List<String>> customerToNames = new java.util.HashMap<>();
        for (CustomerShare s : shares) {
            String nick = nickMap.get(s.getSharedUserId());
            if (nick == null || nick.isBlank()) continue;
            customerToNames.computeIfAbsent(s.getCustomerId(), k -> new ArrayList<>()).add(nick);
        }
        list.forEach(c -> {
            List<String> names = customerToNames.get(c.getId());
            if (names != null && !names.isEmpty()) {
                c.setSharedTo(String.join("、", names));
            }
        });
    }

    /** 批量填充最后跟进时间（follow_ups 取最新一条） */
    private void fillLastFollowUp(List<Customer> list) {
        if (list == null || list.isEmpty()) return;
        List<Long> ids = list.stream().map(Customer::getId).filter(Objects::nonNull).toList();
        List<FollowUp> all = followUpService.list(
                new LambdaQueryWrapper<FollowUp>()
                        .in(FollowUp::getCustomerId, ids)
                        .orderByDesc(FollowUp::getFollowUpTime)
                        .orderByDesc(FollowUp::getId));
        if (all.isEmpty()) return;
        Map<Long, FollowUp> latest = new HashMap<>();
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
}
