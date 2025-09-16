// src/main/java/com/dashboard/backend/dto/report/ReportDto.java
package com.dashboard.backend.dto.report;

import com.dashboard.backend.entity.report.Report;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportDto {
    private Long id;
    private String title;
    private Report.ReportType reportType;
    private String reportTypeDisplayName;
    private String description;
    private Long authorId;
    private String authorName;
    
    private String parameters;
    private LocalDate startDate;
    private LocalDate endDate;
    
    private Long departmentId;
    private String departmentName;
    private Long employeeId;
    private String employeeName;
    private Long userId;
    private String userName;
    
    private String reportPath;
    private Report.ReportFormat reportFormat;
    private String reportFormatDisplayName;
    private Long fileSize;
    private String fileSizeDisplay;
    
    private Report.ReportStatus status;
    private String statusDisplayName;
    private LocalDate generationDate;
    private LocalDate expiryDate;
    
    private Boolean isPublic;
    private Report.AccessLevel accessLevel;
    private String accessLevelDisplayName;
    private Integer downloadCount;
    
    private String reportData;
    private String errorMessage;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Propriétés calculées
    private Boolean isExpired;
    private Boolean canDownload;
    private Boolean canDelete;
    private Boolean canEdit;
    private String downloadUrl;
}