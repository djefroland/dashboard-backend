// src/main/java/com/dashboard/backend/dto/employee/DepartmentDto.java
package com.dashboard.backend.dto.employee;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentDto {
    private Long id;
    private String name;
    private String description;
    private Long managerId;
    private String managerName;
    private Long parentDepartmentId;
    private String parentDepartmentName;
    private Double budget;
    private String location;
    private Boolean active;
    private Integer employeeCount;
    private LocalDateTime createdAt;
}