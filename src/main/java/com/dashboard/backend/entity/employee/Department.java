// src/main/java/com/dashboard/backend/entity/employee/Department.java
package com.dashboard.backend.entity.employee;

import com.dashboard.backend.entity.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "departments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    @NotBlank(message = "Le nom du département est obligatoire")
    @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères")
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "manager_id")
    private Long managerId;

    @Column(name = "parent_department_id")
    private Long parentDepartmentId;

    @Column(name = "budget")
    private Double budget;

    @Column(name = "location")
    private String location;

    @Builder.Default
    private Boolean active = true;

    // Relations
    @OneToMany(mappedBy = "departmentId", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Employee> employees;
}