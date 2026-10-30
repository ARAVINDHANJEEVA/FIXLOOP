package com.fixloop.controller;

import com.fixloop.domain.Skill;
import com.fixloop.domain.Vendor;
import com.fixloop.dto.VendorDto;
import com.fixloop.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/vendors")
@RequiredArgsConstructor
public class VendorController {

    private final VendorRepository vendorRepo;

    @GetMapping
    public ResponseEntity<List<VendorDto>> getVendors(@RequestParam(required = false) List<Integer> skillIds) {
        List<Vendor> vendors;
        if (skillIds != null && !skillIds.isEmpty()) {
            vendors = vendorRepo.findBySkillIds(skillIds);
        } else {
            vendors = vendorRepo.findAll();
        }
        
        List<VendorDto> dtos = vendors.stream().map(v -> VendorDto.builder()
            .id(v.getId())
            .name(v.getName())
            .phone(v.getPhone())
            .email(v.getEmail())
            .contactPerson(v.getContactPerson())
            .skillTags(v.getSkills() != null
                ? v.getSkills().stream().map(Skill::getName).collect(Collectors.toList())
                : Collections.emptyList())
            .build()
        ).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}
