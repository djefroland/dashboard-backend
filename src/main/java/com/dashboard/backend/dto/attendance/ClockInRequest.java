// src/main/java/com/dashboard/backend/dto/attendance/ClockInRequest.java
package com.dashboard.backend.dto.attendance;

import com.dashboard.backend.entity.attendance.AttendanceStatus;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClockInRequest {
    
    private String location;
    
    private AttendanceStatus status;
    
    @Size(max = 500, message = "Les notes ne peuvent pas dépasser 500 caractères")
    private String notes;
    
    @Builder.Default
    private Boolean isRemote = false;
}