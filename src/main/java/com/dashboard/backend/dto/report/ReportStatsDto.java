// src/main/java/com/dashboard/backend/dto/report/ReportStatsDto.java
package com.dashboard.backend.dto.report;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportStatsDto {
    private Integer totalReports;
    private Integer pendingReports;
    private Integer completedReports;
    private Integer failedReports;
    
    private Map<String, Integer> reportsByType;
    private Map<String, Integer> reportsByFormat;
    private Map<String, Integer> reportsByStatus;
    
    private Integer totalDownloads;
    private Long totalFileSize;
    private String totalFileSizeDisplay;
    
    private Integer reportsThisMonth;
    private Integer reportsThisWeek;
}