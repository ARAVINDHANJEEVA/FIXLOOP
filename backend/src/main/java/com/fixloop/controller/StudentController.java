package com.fixloop.controller;

import com.fixloop.domain.AppUser;
import com.fixloop.dto.student.StudentProfileDto;
import com.fixloop.dto.student.UpdateProfileRequest;
import com.fixloop.dto.complaint.ComplaintSummaryDto;
import com.fixloop.repository.AppUserRepository;
import com.fixloop.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final AppUserRepository userRepo;

    private AppUser getCurrentUser(Authentication auth) {
        return userRepo.findByEmail(auth.getName()).orElseThrow();
    }

    /**
     * GET /api/students/me — Student's own profile
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<StudentProfileDto> getMyProfile(Authentication auth) {
        AppUser user = getCurrentUser(auth);
        return ResponseEntity.ok(studentService.getMyProfile(user.getId()));
    }

    /**
     * PATCH /api/students/me — Update name / defaultLocation
     */
    @PatchMapping("/me")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<StudentProfileDto> updateMyProfile(
            @RequestBody UpdateProfileRequest request,
            Authentication auth) {
        AppUser user = getCurrentUser(auth);
        return ResponseEntity.ok(studentService.updateMyProfile(user.getId(), request));
    }

    /**
     * GET /api/students/me/complaints — Paginated, filterable complaint history for the student.
     * Server-scoped: never exposes other students' complaints.
     */
    @GetMapping("/me/complaints")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<Page<ComplaintSummaryDto>> getMyComplaints(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        AppUser user = getCurrentUser(auth);
        return ResponseEntity.ok(studentService.getMyComplaints(user.getId(), status, page, size));
    }
}
