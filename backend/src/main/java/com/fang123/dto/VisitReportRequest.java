package com.fang123.dto;

import lombok.Data;

import java.util.List;

/** 前端批量上报请求体 */
@Data
public class VisitReportRequest {

    private List<VisitEventDTO> events;
}
