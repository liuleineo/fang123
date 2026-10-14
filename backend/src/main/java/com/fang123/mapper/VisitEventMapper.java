package com.fang123.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fang123.entity.VisitEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface VisitEventMapper extends BaseMapper<VisitEvent> {

    /** 实时在线：最近一段时间内有事件的访客数 */
    @Select("SELECT COUNT(DISTINCT visitor_key) FROM visit_event WHERE event_time >= #{since}")
    int countOnlineSince(@Param("since") LocalDateTime since);
}
