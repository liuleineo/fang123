package com.fang123.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fang123.entity.FollowUp;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface FollowUpMapper extends BaseMapper<FollowUp> {

    /**
     * 统计时间区间内被跟进过的客户数（按 customer_id 去重）。
     * 同一个客户一天跟进了 3 次只算 1 个；follow_up_time 为空的记录回落到 created_at，
     * 避免历史数据（未填跟进时间）被漏算。
     */
    @Select("""
        SELECT COUNT(DISTINCT customer_id) FROM follow_ups
        WHERE IFNULL(follow_up_time, created_at) >= #{start}
          AND IFNULL(follow_up_time, created_at) < #{end}
        """)
    Long countDistinctCustomersBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
