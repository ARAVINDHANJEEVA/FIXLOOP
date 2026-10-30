package com.fixloop.controller;

import com.fixloop.domain.Department;
import com.fixloop.dto.DepartmentDto;
import com.fixloop.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {
    
    private final DepartmentRepository departmentRepo;

    @GetMapping
    public ResponseEntity<List<DepartmentDto>> getAllDepartments() {
        List<DepartmentDto> dtos = departmentRepo.findAll().stream().map(d -> DepartmentDto.builder()
            .id(d.getId())
            .name(d.getName())
            .code(d.getCode())
            .description(d.getDescription())
            .headOfDept(d.getHeadOfDept())
            .build()
        ).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepartmentDto> createDepartment(@RequestBody DepartmentDto dto) {
        Department dept = Department.builder()
            .name(dto.getName())
            .code(dto.getCode())
            .description(dto.getDescription())
            .headOfDept(dto.getHeadOfDept())
            .build();
        dept = departmentRepo.save(dept);
        dto.setId(dept.getId());
        return ResponseEntity.status(201).body(dto);
    }
}
