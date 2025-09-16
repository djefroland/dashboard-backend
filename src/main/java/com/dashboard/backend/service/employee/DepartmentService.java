// src/main/java/com/dashboard/backend/service/employee/DepartmentService.java
package com.dashboard.backend.service.employee;

import com.dashboard.backend.dto.employee.DepartmentDto;
import com.dashboard.backend.entity.employee.Department;
import com.dashboard.backend.entity.user.User;
import com.dashboard.backend.exception.custom.ResourceNotFoundException;
import com.dashboard.backend.repository.DepartmentRepository;
import com.dashboard.backend.repository.EmployeeRepository;
import com.dashboard.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;

    /**
     * Obtient tous les départements actifs
     */
    public List<DepartmentDto> getAllDepartments() {
        List<Department> departments = departmentRepository.findByActiveTrue();
        return departments.stream()
                .map(this::mapToDepartmentDto)
                .collect(Collectors.toList());
    }

    /**
     * Obtient un département par son ID
     */
    public DepartmentDto getDepartmentById(Long departmentId) {
        Department department = departmentRepository.findById(departmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Département non trouvé"));
        
        return mapToDepartmentDto(department);
    }

    /**
     * Mappe Department vers DepartmentDto
     */
    private DepartmentDto mapToDepartmentDto(Department department) {
        String managerName = null;
        if (department.getManagerId() != null) {
            managerName = userRepository.findById(department.getManagerId())
                .map(User::getFullName).orElse("Manager inconnu");
        }

        String parentDepartmentName = null;
        if (department.getParentDepartmentId() != null) {
            parentDepartmentName = departmentRepository.findById(department.getParentDepartmentId())
                .map(Department::getName).orElse("Département parent inconnu");
        }

        int employeeCount = (int) employeeRepository.countByDepartmentId(department.getId());

        return DepartmentDto.builder()
                .id(department.getId())
                .name(department.getName())
                .description(department.getDescription())
                .managerId(department.getManagerId())
                .managerName(managerName)
                .parentDepartmentId(department.getParentDepartmentId())
                .parentDepartmentName(parentDepartmentName)
                .budget(department.getBudget())
                .location(department.getLocation())
                .active(department.getActive())
                .employeeCount(employeeCount)
                .createdAt(department.getCreatedAt())
                .build();
    }
}