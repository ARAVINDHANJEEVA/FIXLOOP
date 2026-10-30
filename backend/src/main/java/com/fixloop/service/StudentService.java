package com.fixloop.service;

import com.fixloop.domain.*;
import com.fixloop.dto.student.StudentProfileDto;
import com.fixloop.dto.student.UpdateProfileRequest;
import com.fixloop.dto.complaint.ComplaintSummaryDto;
import com.fixloop.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentService {

    private final AppUserRepository userRepo;
    private final StudentProfileRepository studentRepo;
    private final LocationRepository locationRepo;
    private final ComplaintRepository complaintRepo;
    private final ComplaintService complaintService;

    @Transactional(readOnly = true)
    public StudentProfileDto getMyProfile(Integer userId) {
        StudentProfile sp = studentRepo.findById(userId)
            .orElseThrow(() -> new RuntimeException("Student profile not found for userId=" + userId));
        return toDto(sp);
    }

    public StudentProfileDto updateMyProfile(Integer userId, UpdateProfileRequest request) {
        StudentProfile sp = studentRepo.findById(userId)
            .orElseThrow(() -> new RuntimeException("Student profile not found for userId=" + userId));

        if (request.getName() != null && !request.getName().isBlank()) {
            sp.getUser().setName(request.getName().trim());
            // Regenerate initials
            String[] parts = request.getName().trim().split("\\s+");
            String initials = parts.length >= 2
                ? String.valueOf(parts[0].charAt(0)).toUpperCase() + String.valueOf(parts[parts.length - 1].charAt(0)).toUpperCase()
                : String.valueOf(parts[0].charAt(0)).toUpperCase();
            sp.getUser().setAvatarInitials(initials);
            userRepo.save(sp.getUser());
        }

        if (request.getDefaultLocationId() != null) {
            Location loc = locationRepo.findById(request.getDefaultLocationId())
                .orElseThrow(() -> new RuntimeException("Location not found: " + request.getDefaultLocationId()));
            sp.setDefaultLocation(loc);
        }

        return toDto(studentRepo.save(sp));
    }

    @Transactional(readOnly = true)
    public Page<ComplaintSummaryDto> getMyComplaints(Integer userId, String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<com.fixloop.domain.Complaint> complaints;
        if (status != null && !status.equalsIgnoreCase("ALL")) {
            complaints = complaintRepo.findByStudentUserIdAndStatus(userId, status.toUpperCase(), pageable);
        } else {
            complaints = complaintRepo.findByStudentUserId(userId, pageable);
        }
        return complaints.map(complaintService::toSummaryDto);
    }

    private StudentProfileDto toDto(StudentProfile sp) {
        AppUser u = sp.getUser();
        Department dept = sp.getDepartment();
        Location loc = sp.getDefaultLocation();
        return StudentProfileDto.builder()
            .userId(sp.getUserId())
            .name(u != null ? u.getName() : null)
            .email(u != null ? u.getEmail() : null)
            .rollNo(sp.getRollNo())
            .avatarInitials(u != null ? u.getAvatarInitials() : null)
            .departmentId(dept != null ? dept.getId() : null)
            .departmentName(dept != null ? dept.getName() : null)
            .defaultLocationId(loc != null ? loc.getId() : null)
            .defaultLocationBuilding(loc != null ? loc.getBuilding() : null)
            .defaultLocationRoom(loc != null ? loc.getRoom() : null)
            .createdAt(u != null && u.getCreatedAt() != null ? u.getCreatedAt().toString() : null)
            .build();
    }
}
