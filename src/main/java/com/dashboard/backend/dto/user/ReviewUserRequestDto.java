// src/main/java/com/dashboard/backend/dto/user/ReviewUserRequestDto.java
package com.dashboard.backend.dto.user;

import com.dashboard.backend.entity.user.UserRequest;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewUserRequestDto {
    
    @NotNull(message = "La décision est obligatoire")
    private UserRequest.UserRequestStatus decision; // APPROVED ou REJECTED
    
    @Size(max = 1000, message = "Les commentaires ne peuvent pas dépasser 1000 caractères")
    private String comments;
}