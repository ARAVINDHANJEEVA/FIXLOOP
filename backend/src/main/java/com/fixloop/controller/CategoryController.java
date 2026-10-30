package com.fixloop.controller;

import com.fixloop.domain.Category;
import com.fixloop.domain.Skill;
import com.fixloop.dto.CategoryDto;
import com.fixloop.repository.CategoryRepository;
import com.fixloop.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryRepository categoryRepo;
    private final SkillRepository skillRepo;

    @GetMapping
    public ResponseEntity<List<CategoryDto>> getAllCategories() {
        return ResponseEntity.ok(
            categoryRepo.findAll().stream().map(this::toDto).collect(Collectors.toList())
        );
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<CategoryDto> updateCategory(@PathVariable Integer id, @RequestBody CategoryDto dto) {
        Category category = categoryRepo.findById(id).orElseThrow();
        if (dto.getName() != null) category.setName(dto.getName());
        if (dto.getBaseSeverityWeight() != null) category.setBaseSeverityWeight(dto.getBaseSeverityWeight());
        if (dto.getDefaultSlaHours() != null) category.setDefaultSlaHours(dto.getDefaultSlaHours());
        if (dto.getFormSchema() != null) category.setFormSchema(dto.getFormSchema());
        if (dto.getRequiredSkillIds() != null) {
            category.setRequiredSkills(new HashSet<>(skillRepo.findAllById(dto.getRequiredSkillIds())));
        }
        category = categoryRepo.save(category);
        return ResponseEntity.ok(toDto(category));
    }

    private CategoryDto toDto(Category c) {
        List<Integer> skillIds = c.getRequiredSkills() != null
            ? c.getRequiredSkills().stream().map(Skill::getId).collect(Collectors.toList())
            : Collections.emptyList();
        List<String> skillNames = c.getRequiredSkills() != null
            ? c.getRequiredSkills().stream().map(Skill::getName).collect(Collectors.toList())
            : Collections.emptyList();
        return CategoryDto.builder()
            .id(c.getId())
            .name(c.getName())
            .icon(c.getIcon())
            .color(c.getColor())
            .sensitive(c.isSensitive())
            .baseSeverityWeight(c.getBaseSeverityWeight())
            .defaultSlaHours(c.getDefaultSlaHours())
            .departmentId(c.getDepartment() != null ? c.getDepartment().getId() : null)
            .formSchema(c.getFormSchema())
            .requiredSkillIds(skillIds)
            .requiredSkillNames(skillNames)
            .filterGroup(c.getFilterGroup())
            .build();
    }
}
