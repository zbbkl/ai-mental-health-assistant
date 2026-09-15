package org.example.aispingboot.controller;

import jakarta.annotation.Resource;
import org.example.aispingboot.DTO.response.AnalyticsOverviewResponseDTO;
import org.example.aispingboot.common.Result;
import org.example.aispingboot.service.DataAnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/data-analytics")
public class DataAnalytics {

    @Resource
    private DataAnalyticsService dataAnalyticsService;

    // 管理端数据分析总览
    @GetMapping("/overview")
    public Result<AnalyticsOverviewResponseDTO> overview() {
        return Result.ok(dataAnalyticsService.overview());
    }
}
