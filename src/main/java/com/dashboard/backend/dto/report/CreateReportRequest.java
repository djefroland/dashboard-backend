// src/main/java/com/dashboard/backend/dto/report/CreateReportRequest.java
package com.dashboard.backend.dto.report;

import com.dashboard.backend.entity.report.Report;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateReportRequest {
    
    @NotBlank(message = "Le titre est obligatoire")
    private String title;
    
    @NotNull(message = "Le type de rapport est obligatoire")
    private Report.ReportType reportType;
    
    private String description;
    
    private String parameters;
    
    @NotNull(message = "La date de début est obligatoire")
    private LocalDate startDate;
    
    @NotNull(message = "La date de fin est obligatoire")
    private LocalDate endDate;
    
    private Long departmentId;
    private Long employeeId;
    private Long userId;
    
    @Builder.Default
    private Report.ReportFormat reportFormat = Report.ReportFormat.PDF;
    
    @Builder.Default
    private Report.AccessLevel accessLevel = Report.AccessLevel.AUTHOR_ONLY;
    
    @Builder.Default
    private Boolean isPublic = false;
    
    private LocalDate expiryDate;
}