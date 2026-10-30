package com.fixloop.controller;

import com.fixloop.domain.AppUser;
import com.fixloop.domain.Department;
import com.fixloop.domain.Skill;
import com.fixloop.domain.StaffProfile;
import com.fixloop.dto.staff.StaffCreateRequest;
import com.fixloop.dto.staff.StaffDto;
import com.fixloop.dto.staff.StaffUpdateRequest;
import com.fixloop.dto.staff.WorkloadDto;
import com.fixloop.repository.AppUserRepository;
import com.fixloop.repository.ComplaintRepository;
import com.fixloop.repository.DepartmentRepository;
import com.fixloop.repository.SkillRepository;
import com.fixloop.repository.StaffProfileRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffController {

    private final StaffProfileRepository staffRepo;
    private final AppUserRepository userRepo;
    private final DepartmentRepository departmentRepo;
    private final SkillRepository skillRepo;
    private final PasswordEncoder passwordEncoder;
    private final ComplaintRepository complaintRepo;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<StaffDto>> getAllStaff() {
        List<StaffDto> dtoList = staffRepo.findAllWithDetails().stream()
                .map(this::toStaffDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtoList);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<StaffDto> createStaff(@Valid @RequestBody StaffCreateRequest request) {
        if (userRepo.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().build();
        }

        String[] parts = request.getName() != null ? request.getName().trim().split("\\s+") : new String[]{""};
        String initials = parts.length >= 2
                ? (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase()
                : (parts[0].isEmpty() ? "ST" : parts[0].substring(0, 1).toUpperCase());

        AppUser user = AppUser.builder()
                .email(request.getEmail())
                .name(request.getName())
                .passwordHash(passwordEncoder.encode(request.getPassword() != null ? request.getPassword() : "staff123"))
                .role("STAFF")
                .restrictedAccess(false)
                .avatarInitials(initials)
                .build();
        user = userRepo.save(user);

        Department dept = null;
        if (request.getDepartmentId() != null) {
            dept = departmentRepo.findById(request.getDepartmentId()).orElse(null);
        }

        Set<Skill> skills = new HashSet<>();
        if (request.getSkillIds() != null && !request.getSkillIds().isEmpty()) {
            skills.addAll(skillRepo.findAllById(request.getSkillIds()));
        }

        StaffProfile profile = StaffProfile.builder()
                .userId(user.getId())
                .user(user)
                .department(dept)
                .jobTitle(request.getJobTitle())
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .currentWeightedLoad(BigDecimal.ZERO)
                .skills(skills)
                .build();

        staffRepo.save(profile);
        return ResponseEntity.status(HttpStatus.CREATED).body(toStaffDto(profile));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<StaffDto> updateStaff(@PathVariable Integer id, @RequestBody StaffUpdateRequest request) {
        StaffProfile profile = staffRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (request.getName() != null && profile.getUser() != null) {
            profile.getUser().setName(request.getName());
            userRepo.save(profile.getUser());
        }
        if (request.getJobTitle() != null) {
            profile.setJobTitle(request.getJobTitle());
        }
        if (request.getStatus() != null) {
            profile.setStatus(request.getStatus());
        }
        if (request.getDepartmentId() != null) {
            Department dept = departmentRepo.findById(request.getDepartmentId()).orElse(null);
            profile.setDepartment(dept);
        }
        if (request.getSkillIds() != null) {
            Set<Skill> skills = new HashSet<>(skillRepo.findAllById(request.getSkillIds()));
            profile.setSkills(skills);
        }

        staffRepo.save(profile);
        return ResponseEntity.ok(toStaffDto(profile));
    }

    @GetMapping("/workload")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<WorkloadDto> getWorkload(Authentication auth) {
        AppUser user = userRepo.findByEmail(auth.getName()).orElseThrow();
        Integer staffId = user.getId();
        StaffProfile sp = staffRepo.findById(staffId).orElseThrow();

        long openCount = complaintRepo.findByAssignedStaff(staffId, PageRequest.of(0, 1)).getTotalElements();

        LocalDateTime weekStart = LocalDateTime.now().minusDays(7);
        List<Object[]> resolved = complaintRepo.countResolvedSince(weekStart);
        int resolvedThisWeek = resolved.stream()
                .filter(r -> staffId.equals(r[0]))
                .mapToInt(r -> ((Long) r[1]).intValue())
                .sum();

        WorkloadDto dto = WorkloadDto.builder()
                .userId(staffId)
                .name(user.getName())
                .openTickets((int) openCount)
                .currentWeightedLoad(sp.getCurrentWeightedLoad() != null ? sp.getCurrentWeightedLoad() : BigDecimal.ZERO)
                .resolvedThisWeek(resolvedThisWeek)
                .avgResolutionHours(0.0)
                .build();

        return ResponseEntity.ok(dto);
    }

    private StaffDto toStaffDto(StaffProfile sp) {
        List<String> skillNames = sp.getSkills() != null
                ? sp.getSkills().stream().map(Skill::getName).collect(Collectors.toList())
                : Collections.emptyList();

        return StaffDto.builder()
                .id(sp.getUserId())
                .name(sp.getUser() != null ? sp.getUser().getName() : null)
                .email(sp.getUser() != null ? sp.getUser().getEmail() : null)
                .jobTitle(sp.getJobTitle())
                .departmentId(sp.getDepartment() != null ? sp.getDepartment().getId() : null)
                .departmentName(sp.getDepartment() != null ? sp.getDepartment().getName() : null)
                .skillTags(skillNames)
                .status(sp.getStatus())
                .currentWeightedLoad(sp.getCurrentWeightedLoad() != null ? sp.getCurrentWeightedLoad() : BigDecimal.ZERO)
                .avatarInitials(sp.getUser() != null ? sp.getUser().getAvatarInitials() : "ST")
                .build();
    }
}
