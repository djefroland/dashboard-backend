// src/main/java/com/dashboard/backend/entity/employee/Position.java
package com.dashboard.backend.entity.employee;

import com.dashboard.backend.entity.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "positions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Position extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @NotBlank(message = "Le titre du poste est obligatoire")
    @Size(max = 100, message = "Le titre ne peut pas dépasser 100 caractères")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "department_id")
    private Long departmentId;

    @Column(name = "min_salary")
    private Double minSalary;

    @Column(name = "max_salary")
    private Double maxSalary;

    @Enumerated(EnumType.STRING)
    @Column(name = "level")
    private PositionLevel level;

    @Builder.Default
    private Boolean active = true;

    public enum PositionLevel {
        JUNIOR,
        INTERMEDIATE,
        SENIOR,
        LEAD,
        MANAGER,
        DIRECTOR
    }
}