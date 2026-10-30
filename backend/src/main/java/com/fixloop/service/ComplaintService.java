package com.fixloop.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixloop.domain.*;
import com.fixloop.dto.complaint.*;
import com.fixloop.dto.complaint.ClusterMemberDto;
import com.fixloop.dto.complaint.IncidentSummaryDto;
import com.fixloop.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ComplaintService {
    private final ComplaintRepository complaintRepo;
    private final StaffProfileRepository staffRepo;
    private final IncidentRepository incidentRepo;
    private final StatusHistoryRepository statusHistoryRepo;
    private final EscalationLogRepository escalationLogRepo;
    private final AttachmentRepository attachmentRepo;
    private final ReviewRepository reviewRepo;
    private final ComplaintVendorRepository complaintVendorRepo;
    private final VendorRepository vendorRepo;
    private final CategoryRepository categoryRepo;
    private final LocationRepository locationRepo;
    private final SlaRuleRepository slaRuleRepo;
    private final IncidentClusterService clusterService;
    private final AutoAssignService autoAssignService;
    private final PriorityScoreService priorityScoreService;
    private final NotificationService notificationService;
    private final StudentProfileRepository studentProfileRepo;
    private final AppUserRepository userRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Admin dispatches complaint to a department.
     * Sensitive categories (Ragging/Harassment/Malpractice) skip staff assignment entirely.
     * For non-sensitive complaints, triggers auto-assignment within the department,
     * or assigns a vendor if EXTERNAL_VENDOR dispatch type.
     */
    public ComplaintDetailDto dispatch(Integer complaintId, DispatchRequest request, AppUser adminUser) {
        Complaint complaint = complaintRepo.findById(complaintId)
            .orElseThrow(() -> new RuntimeException("Complaint not found: " + complaintId));

        // ── Sensitive bypass: never route to staff pool ─────────────
        if (complaint.getCategory() != null && complaint.getCategory().isSensitive()) {
            complaint.setStatus("UNDER_ADMIN_REVIEW");
            if (complaint.getCategory().getDepartment() != null) {
                complaint.setRoutedDepartment(complaint.getCategory().getDepartment());
            }
            addStatusHistory(complaint, "UNDER_ADMIN_REVIEW",
                adminUser.getId(), adminUser.getName(), "ADMIN",
                "Sensitive complaint — restricted to admin review only, no staff assignment");
            Complaint saved = complaintRepo.save(complaint);
            notificationService.broadcastStatusChange(saved.getId(), saved.getStatus());
            return toDetailDto(saved);
        }

        complaint.setDispatchType(request.getDispatchType());
        complaint.setStatus("UNDER_ADMIN_REVIEW");
        
        // Look up the admin profile from the AppUser
        // (AdminProfile has same ID as AppUser)
        // complaint.setAssignedByAdmin(adminProfile); // set this
        
        if ("INTERNAL_STAFF".equals(request.getDispatchType())) {
            // Set routed department
            Department dept = new Department();
            dept.setId(request.getDepartmentId());
            complaint.setRoutedDepartment(dept);
            
            // Auto-assign
            Set<Skill> requiredSkills = complaint.getCategory().getRequiredSkills();
            Optional<StaffProfile> bestStaff = autoAssignService.findBestStaff(request.getDepartmentId(), requiredSkills);
            
            if (bestStaff.isPresent()) {
                autoAssignService.assignComplaint(complaint, bestStaff.get());
                addStatusHistory(complaint, "ASSIGNED", adminUser.getId(), adminUser.getName(), "ADMIN",
                    "Auto-assigned to " + bestStaff.get().getUser().getName());
                // Notify the assigned staff
                notificationService.broadcastAssignment(bestStaff.get().getUserId(), toSummaryDto(complaint));
            } else {
                complaint.setStatus("UNDER_ADMIN_REVIEW"); // stays in queue for manual
                addStatusHistory(complaint, "UNDER_ADMIN_REVIEW", adminUser.getId(), adminUser.getName(), "ADMIN",
                    "No matching staff found — manual assignment required");
                notificationService.broadcastAdminAlert(java.util.Map.of(
                    "type", "MANUAL_ASSIGNMENT_REQUIRED",
                    "complaintId", complaint.getId(),
                    "token", complaint.getToken(),
                    "message", "No matching staff found in department — manual assignment required"
                ));
            }
        } else if ("EXTERNAL_VENDOR".equals(request.getDispatchType())) {
            if (request.getVendorId() != null) {
                Vendor vendor = vendorRepo.findById(request.getVendorId())
                    .orElseThrow(() -> new RuntimeException("Vendor not found"));
                ComplaintVendorId cvId = new ComplaintVendorId(complaint.getId(), vendor.getId());
                ComplaintVendor cv = ComplaintVendor.builder()
                    .complaintId(complaint.getId())
                    .vendorId(vendor.getId())
                    .complaint(complaint)
                    .vendor(vendor)
                    .contactStatus("CONTACTED")
                    .notes(request.getNote())
                    .updatedAt(LocalDateTime.now())
                    .build();
                complaintVendorRepo.save(cv);
                complaint.setStatus("VENDOR_ASSIGNED");
                addStatusHistory(complaint, "VENDOR_ASSIGNED", adminUser.getId(), adminUser.getName(), "ADMIN",
                    "Dispatched to vendor: " + vendor.getName());
            }
        }
        
        Complaint saved = complaintRepo.save(complaint);
        ComplaintDetailDto detailDto = toDetailDto(saved);
        notificationService.broadcastStatusChange(saved.getId(), saved.getStatus());
        notificationService.broadcastComplaintUpdate(detailDto);
        notificationService.broadcastNewComplaint(toSummaryDto(saved));
        return detailDto;
    }
    
    /**
     * Manual reassignment by admin (with mandatory reason → escalation_log).
     */
    public ComplaintDetailDto reassign(Integer complaintId, ReassignRequest request, AppUser adminUser) {
        Complaint complaint = complaintRepo.findById(complaintId)
            .orElseThrow(() -> new RuntimeException("Complaint not found"));
        
        StaffProfile oldStaff = complaint.getAssignedToStaff();
        StaffProfile newStaff = staffRepo.findById(request.getStaffId())
            .orElseThrow(() -> new RuntimeException("Staff not found"));
        
        // Release old staff load
        if (oldStaff != null) autoAssignService.releaseLoad(complaint);
        
        // Assign new staff
        autoAssignService.assignComplaint(complaint, newStaff);
        
        // Log to escalation_log (manual reassignment is audited here)
        EscalationLog log = EscalationLog.builder()
            .complaint(complaint)
            .fromStaff(oldStaff)
            .toStaff(newStaff)
            .previousLevel(complaint.getEscalationLevel())
            .newLevel(complaint.getEscalationLevel())
            .reason(request.getReason())
            .triggeredBy("MANUAL")
            .build();
        escalationLogRepo.save(log);
        
        addStatusHistory(complaint, "ASSIGNED", adminUser.getId(), adminUser.getName(), "ADMIN",
            "Manually reassigned to " + newStaff.getUser().getName() + " — Reason: " + request.getReason());
        
        Complaint saved = complaintRepo.save(complaint);
        ComplaintDetailDto detailDto = toDetailDto(saved);
        ComplaintSummaryDto summaryDto = toSummaryDto(saved);
        notificationService.broadcastAssignment(newStaff.getUserId(), summaryDto);
        notificationService.broadcastStatusChange(saved.getId(), saved.getStatus());
        notificationService.broadcastComplaintUpdate(detailDto);
        notificationService.broadcastNewComplaint(summaryDto);
        return detailDto;
    }
    
    /**
     * Update complaint status (staff or admin).
     * If RESOLVED: release staff load, set resolvedAt.
     * If REOPENED: add back to queue (status back to ASSIGNED).
     */
    public ComplaintDetailDto updateStatus(Integer complaintId, StatusUpdateRequest request, AppUser actor) {
        Complaint complaint = complaintRepo.findById(complaintId)
            .orElseThrow(() -> new RuntimeException("Complaint not found"));
        
        String oldStatus = complaint.getStatus();
        String newStatus = request.getStatus();
        
        complaint.setStatus(newStatus);
        
        if ("RESOLVED".equals(newStatus) || "CLOSED".equals(newStatus)) {
            complaint.setResolvedAt(LocalDateTime.now());
            autoAssignService.releaseLoad(complaint);
            complaint.setAtRisk(false);
            complaint.setBreached(false);
        }
        
        addStatusHistory(complaint, newStatus, actor.getId(), actor.getName(),
            actor.getRole(), request.getNote());
        
        Complaint saved = complaintRepo.save(complaint);
        
        if (saved.getStudentUser() != null && saved.getStudentUser().getUser() != null) {
            String note = (request.getNote() != null && !request.getNote().isBlank()) ? ": " + request.getNote() : "";
            notificationService.persistAndBroadcast(
                saved.getStudentUser().getUser(),
                saved,
                "STATUS_CHANGE",
                "Status Updated: " + saved.getToken(),
                "Your complaint status is now " + newStatus + note
            );
        }
        
        ComplaintDetailDto detailDto = toDetailDto(saved);
        ComplaintSummaryDto summaryDto = toSummaryDto(saved);
        if (saved.getAssignedToStaff() != null) {
            notificationService.broadcastAssignment(saved.getAssignedToStaff().getUserId(), summaryDto);
        }
        notificationService.broadcastStatusChange(saved.getId(), newStatus);
        notificationService.broadcastNewComplaint(summaryDto);
        notificationService.broadcastComplaintUpdate(detailDto);
        return detailDto;
    }
    
    /**
     * Staff requests more info from student.
     * Sets status to AWAITING_STUDENT_INFO and logs message as note.
     */
    public ComplaintDetailDto requestInfo(Integer complaintId, RequestInfoRequest request, AppUser staffUser) {
        return requestInfo(String.valueOf(complaintId), request, staffUser);
    }

    public ComplaintDetailDto requestInfo(String idOrToken, RequestInfoRequest request, AppUser staffUser) {
        Complaint complaint = findByIdOrToken(idOrToken);
        complaint.setStatus("AWAITING_STUDENT_INFO");
        addStatusHistory(complaint, "AWAITING_STUDENT_INFO", staffUser.getId(), staffUser.getName(), "STAFF",
            request.getMessage());
        Complaint saved = complaintRepo.save(complaint);
        
        // Notify student: creates notification DB row for Alerts tab + broadcasts to /topic/student/{userId}
        if (saved.getStudentUser() != null && saved.getStudentUser().getUser() != null) {
            notificationService.persistAndBroadcast(
                saved.getStudentUser().getUser(),
                saved,
                "AWAITING_INFO",
                "Action Required: " + saved.getToken(),
                staffUser.getName() + " asked: " + request.getMessage()
            );
        }
        
        ComplaintDetailDto detailDto = toDetailDto(saved);
        ComplaintSummaryDto summaryDto = toSummaryDto(saved);
        if (saved.getAssignedToStaff() != null) {
            notificationService.broadcastAssignment(saved.getAssignedToStaff().getUserId(), summaryDto);
        }
        notificationService.broadcastStatusChange(saved.getId(), saved.getStatus());
        notificationService.broadcastNewComplaint(summaryDto);
        notificationService.broadcastComplaintUpdate(detailDto);
        return detailDto;
    }
    
    /**
     * Assign/update vendor for a complaint.
     */
    public ComplaintDetailDto updateVendor(Integer complaintId, VendorUpdateRequest request, AppUser staffUser) {
        Complaint complaint = complaintRepo.findById(complaintId)
            .orElseThrow(() -> new RuntimeException("Complaint not found"));
        Vendor vendor = vendorRepo.findById(request.getVendorId())
            .orElseThrow(() -> new RuntimeException("Vendor not found"));
        
        Optional<ComplaintVendor> existing = complaintVendorRepo.findByComplaintIdAndVendorId(complaintId, request.getVendorId());
        ComplaintVendor cv;
        if (existing.isPresent()) {
            cv = existing.get();
            cv.setContactStatus(request.getContactStatus());
            cv.setNotes(request.getNotes());
            cv.setUpdatedAt(LocalDateTime.now());
        } else {
            cv = ComplaintVendor.builder()
                .complaintId(complaintId)
                .vendorId(request.getVendorId())
                .complaint(complaint)
                .vendor(vendor)
                .contactStatus(request.getContactStatus())
                .notes(request.getNotes())
                .updatedAt(LocalDateTime.now())
                .build();
        }
        complaintVendorRepo.save(cv);
        
        if (complaint.getStatus().equals("ASSIGNED") || complaint.getStatus().equals("IN_PROGRESS")) {
            complaint.setStatus("VENDOR_ASSIGNED");
            addStatusHistory(complaint, "VENDOR_ASSIGNED", staffUser.getId(), staffUser.getName(), "STAFF",
                "Vendor assigned: " + vendor.getName());
        }
        Complaint saved = complaintRepo.save(complaint);
        ComplaintDetailDto detailDto = toDetailDto(saved);
        ComplaintSummaryDto summaryDto = toSummaryDto(saved);
        notificationService.broadcastStatusChange(complaintId, "VENDOR_ASSIGNED");
        notificationService.broadcastNewComplaint(summaryDto);
        notificationService.broadcastComplaintUpdate(detailDto);
        if (saved.getAssignedToStaff() != null) {
            notificationService.broadcastAssignment(saved.getAssignedToStaff().getUserId(), summaryDto);
        }
        return detailDto;
    }
    
    /**
     * Get paginated complaint list for admin queue with optional filters.
     * restricted=true filters to only sensitive categories.
     */
    @Transactional(readOnly = true)
    public Page<ComplaintSummaryDto> getAdminQueue(String status, Integer categoryId, Integer departmentId,
                                                    String dateFrom, String dateTo,
                                                    Boolean restricted, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "priorityScore"));
        
        Specification<Complaint> spec = Specification.where(null);
        
        if (status != null && !status.equals("ALL")) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status));
        }
        if (categoryId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("category").get("id"), categoryId));
        }
        if (departmentId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("routedDepartment").get("id"), departmentId));
        }
        if (Boolean.TRUE.equals(restricted)) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("category").get("sensitive"), true));
        } else {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("category").get("sensitive"), false));
        }
        if (dateFrom != null) {
            LocalDateTime from = LocalDate.parse(dateFrom).atStartOfDay();
            spec = spec.and((root, q, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), from));
        }
        if (dateTo != null) {
            LocalDateTime to = LocalDate.parse(dateTo).atTime(23, 59, 59);
            spec = spec.and((root, q, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), to));
        }
        
        return complaintRepo.findAll(spec, pageable).map(this::toSummaryDto);
    }
    
    /**
     * Get complaint detail by ID with security access check for sensitive categories.
     */
    @Transactional(readOnly = true)
    public ComplaintDetailDto getById(Integer id, AppUser requester) {
        Complaint c = complaintRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Complaint not found: " + id));
        if (c.getCategory() != null && c.getCategory().isSensitive()) {
            if (requester == null || !requester.isRestrictedAccess()) {
                throw new org.springframework.security.access.AccessDeniedException(
                    "Access Denied: Restricted access privilege required to view sensitive complaints"
                );
            }
        }
        return toDetailDto(c);
    }

    @Transactional(readOnly = true)
    public ComplaintDetailDto getById(Integer id) {
        Complaint c = complaintRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Complaint not found: " + id));
        return toDetailDto(c);
    }
    
    @Transactional(readOnly = true)
    public ComplaintDetailDto getByToken(String token, AppUser requester) {
        Complaint c = complaintRepo.findByToken(token)
            .orElseThrow(() -> new RuntimeException("Complaint not found: " + token));
        if (c.getCategory() != null && c.getCategory().isSensitive()) {
            if (requester == null || !requester.isRestrictedAccess()) {
                throw new org.springframework.security.access.AccessDeniedException(
                    "Access Denied: Restricted access privilege required to view sensitive complaints"
                );
            }
        }
        return toDetailDto(c);
    }

    @Transactional(readOnly = true)
    public ComplaintDetailDto getByToken(String token) {
        Complaint c = complaintRepo.findByToken(token)
            .orElseThrow(() -> new RuntimeException("Complaint not found: " + token));
        return toDetailDto(c);
    }
    
    /**
     * Staff personal queue - scoped server-side.
     */
    @Transactional(readOnly = true)
    public Page<ComplaintSummaryDto> getStaffQueue(Integer staffUserId, String status, Integer page, Integer size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "priorityScore"));
        if (status != null && !status.equals("ALL")) {
            return complaintRepo.findByAssignedStaffAndStatus(staffUserId, status, pageable).map(this::toSummaryDto);
        }
        return complaintRepo.findByAssignedStaff(staffUserId, pageable).map(this::toSummaryDto);
    }
    
    // ========== Private helpers ==========
    
    private void addStatusHistory(Complaint complaint, String status, Integer actorId,
                                   String actorName, String actorRole, String note) {
        StatusHistory sh = StatusHistory.builder()
            .complaint(complaint)
            .status(status)
            .changedById(actorId)
            .changedByName(actorName)
            .changedByRole(actorRole)
            .note(note)
            .build();
        statusHistoryRepo.save(sh);
    }
    
    public ComplaintSummaryDto toSummaryDto(Complaint c) {
        List<String> siblingTokens = (c.getIncident() != null)
            ? complaintRepo.findSiblingTokens(c.getIncident().getId(), c.getId())
            : Collections.emptyList();
        List<Integer> siblingIds = (c.getIncident() != null)
            ? complaintRepo.findSiblingIds(c.getIncident().getId(), c.getId())
            : Collections.emptyList();
        return ComplaintSummaryDto.builder()
            .id(c.getId())
            .token(c.getToken())
            .categoryId(c.getCategory() != null ? c.getCategory().getId() : null)
            .categoryName(c.getCategory() != null ? c.getCategory().getName() : null)
            .locationId(c.getLocation() != null ? c.getLocation().getId() : null)
            .location(toLocationDto(c.getLocation()))
            .description(c.getDescription())
            .severity(c.getSeverity())
            .studentsAffected(c.getStudentsAffected())
            .status(c.getStatus())
            .priorityScore(c.getPriorityScore())
            .isAnonymous(c.isAnonymous())
            .incidentId(c.getIncident() != null ? c.getIncident().getId() : null)
            .incidentCount(c.getIncident() != null ? c.getIncident().getComplaintCount() : null)
            .recurrenceCountLast90Days(c.getRecurrenceCount90d())
            .assignedToStaffId(c.getAssignedToStaff() != null ? c.getAssignedToStaff().getUserId() : null)
            .dispatchType(c.getDispatchType())
            .escalationLevel(c.getEscalationLevel())
            .isAtRisk(c.isAtRisk())
            .isBreached(c.isBreached())
            .createdAt(c.getCreatedAt() != null ? c.getCreatedAt().toString() : null)
            .slaDeadline(c.getSlaDeadline() != null ? c.getSlaDeadline().toString() : null)
            .resolvedAt(c.getResolvedAt() != null ? c.getResolvedAt().toString() : null)
            .siblingTokens(siblingTokens)
            .siblingIds(siblingIds)
            .clusterExcluded(c.isClusterExcluded())
            .clusterScore(c.getClusterScore())
            .build();
    }
    
    public ComplaintDetailDto toDetailDto(Complaint c) {
        ComplaintSummaryDto summary = toSummaryDto(c);
        
        // Status history
        List<StatusHistoryEntryDto> history = statusHistoryRepo
            .findByComplaintIdOrderByTimestampAsc(c.getId()).stream()
            .map(sh -> StatusHistoryEntryDto.builder()
                .id(sh.getId())
                .status(sh.getStatus())
                .changedBy(sh.getChangedByName())
                .changedByRole(sh.getChangedByRole())
                .note(sh.getNote())
                .timestamp(sh.getTimestamp() != null ? sh.getTimestamp().toString() : null)
                .build())
            .collect(Collectors.toList());
        
        // Attachments
        List<AttachmentDto> attachments = attachmentRepo.findByComplaintId(c.getId()).stream()
            .map(a -> AttachmentDto.builder()
                .id(a.getId())
                .fileName(a.getFileName())
                .fileUrl("/api/attachments/" + a.getId())
                .mimeType(a.getMimeType())
                .build())
            .collect(Collectors.toList());
        
        // Review
        ReviewDto reviewDto = reviewRepo.findByComplaintId(c.getId())
            .map(r -> ReviewDto.builder()
                .id(r.getId())
                .rating(r.getRating())
                .comment(r.getComment())
                .createdAt(r.getCreatedAt() != null ? r.getCreatedAt().toString() : null)
                .build())
            .orElse(null);
        
        // Vendor assignment
        ComplaintVendorDto vendorDto = complaintVendorRepo.findByComplaintId(c.getId()).stream()
            .findFirst()
            .map(cv -> ComplaintVendorDto.builder()
                .vendorId(cv.getVendorId())
                .vendorName(cv.getVendor() != null ? cv.getVendor().getName() : null)
                .assignedAt(cv.getAssignedAt() != null ? cv.getAssignedAt().toString() : null)
                .contactStatus(cv.getContactStatus())
                .notes(cv.getNotes())
                .build())
            .orElse(null);
        
        // Student info
        StudentDto studentDto = null;
        if (!c.isAnonymous() && c.getStudentUser() != null) {
            AppUser su = c.getStudentUser().getUser();
            studentDto = StudentDto.builder()
                .id(su.getId())
                .name(su.getName())
                .rollNo(c.getStudentUser().getRollNo())
                .contact(su.getEmail())
                .build();
        }
        
        // Assigned staff
        StaffSummaryDto staffDto = null;
        if (c.getAssignedToStaff() != null) {
            StaffProfile sp = c.getAssignedToStaff();
            staffDto = StaffSummaryDto.builder()
                .id(sp.getUserId())
                .name(sp.getUser() != null ? sp.getUser().getName() : null)
                .jobTitle(sp.getJobTitle())
                .departmentId(sp.getDepartment() != null ? sp.getDepartment().getId() : null)
                .departmentName(sp.getDepartment() != null ? sp.getDepartment().getName() : null)
                .currentWeightedLoad(sp.getCurrentWeightedLoad())
                .avatarInitials(sp.getUser() != null ? sp.getUser().getAvatarInitials() : null)
                .email(sp.getUser() != null ? sp.getUser().getEmail() : null)
                .build();
        }
        
        return ComplaintDetailDto.builder()
            // Copy all summary fields
            .id(summary.getId())
            .token(summary.getToken())
            .categoryId(summary.getCategoryId())
            .categoryName(summary.getCategoryName())
            .locationId(summary.getLocationId())
            .location(summary.getLocation())
            .description(summary.getDescription())
            .severity(summary.getSeverity())
            .studentsAffected(summary.getStudentsAffected())
            .status(summary.getStatus())
            .priorityScore(summary.getPriorityScore())
            .isAnonymous(summary.isAnonymous())
            .incidentId(summary.getIncidentId())
            .incidentCount(summary.getIncidentCount())
            .recurrenceCountLast90Days(summary.getRecurrenceCountLast90Days())
            .assignedToStaffId(summary.getAssignedToStaffId())
            .dispatchType(summary.getDispatchType())
            .escalationLevel(summary.getEscalationLevel())
            .isAtRisk(summary.isAtRisk())
            .isBreached(summary.isBreached())
            .createdAt(summary.getCreatedAt())
            .slaDeadline(summary.getSlaDeadline())
            .resolvedAt(summary.getResolvedAt())
            .siblingTokens(summary.getSiblingTokens())
            .siblingIds(summary.getSiblingIds())
            .clusterExcluded(summary.isClusterExcluded())
            .clusterScore(summary.getClusterScore())
            // Detail-only fields
            .student(studentDto)
            .assignedToStaff(staffDto)
            .vendorAssignment(vendorDto)
            .statusHistory(history)
            .attachments(attachments)
            .review(reviewDto)
            .extraData(c.getExtraData())
            .incident(buildIncidentSummaryDto(c))
            .build();
    }
    
    // =========================================================================
    // NEW: Student-facing complaint submission
    // =========================================================================

    /**
     * POST /api/complaints — full pipeline:
     * validate → SLA → cluster → token → priority → history → broadcast
     */
    public ComplaintCreatedDto createComplaint(ComplaintCreateRequest request, AppUser actor) {
        // 1. Resolve student profile
        StudentProfile studentProfile = studentProfileRepo.findById(actor.getId())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Only students may submit complaints"));

        // 2. Look up category and location (with fallback to first available if ID sequence shifted)
        Category category = categoryRepo.findById(request.getCategoryId())
            .orElseGet(() -> categoryRepo.findAll().stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No categories available")));

        Location location = locationRepo.findById(request.getLocationId())
            .orElseGet(() -> locationRepo.findAll().stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No locations available")));

        // 3. Validate extraData against category.formSchema (server-side guard)
        String extraDataJson = validateAndSerializeExtraData(request.getExtraData(), category.getFormSchema());

        // 4. Map severity int → severityTier string
        String severityTier = switch (request.getSeverity()) {
            case 1 -> "LOW";
            case 2 -> "MEDIUM";
            case 3 -> "HIGH";
            default -> "CRITICAL";
        };

        // 5. Compute SLA deadline
        LocalDateTime slaDeadline = null;
        SlaRule slaRule = slaRuleRepo.findByCategoryIdAndSeverityTier(category.getId(), severityTier).orElse(null);
        int slaHours = (slaRule != null) ? slaRule.getResolutionHours() : category.getDefaultSlaHours();
        slaDeadline = LocalDateTime.now().plusHours(slaHours);

        // 6. Count recurrence
        LocalDateTime since90 = LocalDateTime.now().minusDays(90);
        long recurrenceCount = complaintRepo.countRecurrence(category.getId(), location.getId(), since90, -1);

        int studentsAffected = (request.getStudentsAffected() != null && request.getStudentsAffected() > 0)
            ? request.getStudentsAffected() : 1;

        // 7. Insert complaint (temporary token to satisfy NOT NULL + UNIQUE)
        String tempToken = "TMP-" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        Complaint complaint = Complaint.builder()
            .token(tempToken)
            .studentUser(request.isAnonymous() ? null : studentProfile)
            .isAnonymous(request.isAnonymous())
            .category(category)
            .location(location)
            .description(extractDescription(request.getExtraData()))
            .severity(request.getSeverity().shortValue())
            .studentsAffected(studentsAffected)
            .recurrenceCount90d((int) Math.min(recurrenceCount, Integer.MAX_VALUE))
            .status("SUBMITTED")
            .priorityScore(java.math.BigDecimal.ZERO)
            .extraData(extraDataJson)
            .escalationLevel((short) 0)
            .slaDeadline(slaDeadline)
            .build();

        complaint = complaintRepo.save(complaint);

        // 8. Generate token SNT-{YEAR}-{zero-padded-6-digit-id}
        String token = String.format("SNT-%d-%06d", LocalDateTime.now().getYear(), complaint.getId());
        complaint.setToken(token);

        // 9. Incident clustering (skip for sensitive categories)
        if (!category.isSensitive()) {
            Incident incident = clusterService.clusterComplaint(complaint);
            complaint.setIncident(incident);
        }

        // 10. Compute priority score
        long hoursElapsed = 0; // brand new complaint
        java.math.BigDecimal score = priorityScoreService.compute(
            request.getSeverity(), studentsAffected,
            (int) Math.min(recurrenceCount, Integer.MAX_VALUE), hoursElapsed, slaHours);
        complaint.setPriorityScore(score);

        complaint = complaintRepo.save(complaint);

        // 11. Initial status history
        String actorName = request.isAnonymous() ? "Anonymous" : actor.getName();
        addStatusHistory(complaint, "SUBMITTED", actor.getId(), actorName, "STUDENT",
            "Complaint submitted" + (request.isAnonymous() ? " anonymously" : ""));

        // 12. Broadcast to admin queue and general complaints topic
        ComplaintSummaryDto summaryDto = toSummaryDto(complaint);
        notificationService.broadcastNewComplaint(summaryDto);
        notificationService.broadcastComplaintUpdate(summaryDto);

        return ComplaintCreatedDto.builder()
            .id(complaint.getId())
            .token(complaint.getToken())
            .categoryId(category.getId())
            .categoryName(category.getName())
            .status(complaint.getStatus())
            .priorityScore(complaint.getPriorityScore())
            .slaDeadline(complaint.getSlaDeadline() != null ? complaint.getSlaDeadline().toString() : null)
            .createdAt(complaint.getCreatedAt() != null ? complaint.getCreatedAt().toString() : null)
            .build();
    }

    /**
     * POST /api/complaints/{id}/reply — student answers AWAITING_STUDENT_INFO
     */
    public ComplaintDetailDto replyToInfoRequest(Integer complaintId, String replyText, AppUser student) {
        return replyToInfoRequest(String.valueOf(complaintId), replyText, student);
    }

    public ComplaintDetailDto replyToInfoRequest(String idOrToken, String replyText, AppUser student) {
        Complaint complaint = findByIdOrToken(idOrToken);

        assertStudentOwns(complaint, student);

        if (!"AWAITING_STUDENT_INFO".equals(complaint.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Complaint is not awaiting student info (current status: " + complaint.getStatus() + ")");
        }

        complaint.setStatus("IN_PROGRESS");
        addStatusHistory(complaint, "IN_PROGRESS", student.getId(), student.getName(), "STUDENT",
            "Student reply: " + replyText);

        Complaint saved = complaintRepo.save(complaint);

        // Notify assigned staff if present
        if (saved.getAssignedToStaff() != null) {
            AppUser staffUser = saved.getAssignedToStaff().getUser();
            if (staffUser != null) {
                notificationService.persistAndBroadcast(staffUser, saved, "STATUS_CHANGE",
                    "Student replied on " + saved.getToken(),
                    "The student has provided information: \"" + replyText + "\". Complaint is now IN_PROGRESS.");
            }
        }

        // Also notify the student (creates Alert row + WebSocket update)
        if (saved.getStudentUser() != null && saved.getStudentUser().getUser() != null) {
            notificationService.persistAndBroadcast(
                saved.getStudentUser().getUser(),
                saved,
                "STATUS_CHANGE",
                "Reply Received: " + saved.getToken(),
                "Your response has been sent to staff: \"" + replyText + "\""
            );
        }

        ComplaintDetailDto detailDto = toDetailDto(saved);
        ComplaintSummaryDto summaryDto = toSummaryDto(saved);
        if (saved.getAssignedToStaff() != null) {
            notificationService.broadcastAssignment(saved.getAssignedToStaff().getUserId(), summaryDto);
        }
        notificationService.broadcastStatusChange(saved.getId(), saved.getStatus());
        notificationService.broadcastNewComplaint(summaryDto);
        notificationService.broadcastComplaintUpdate(detailDto);
        return detailDto;
    }

    /**
     * POST /api/complaints/{id}/review — student submits rating after RESOLVED
     */
    public ComplaintDetailDto submitReview(Integer complaintId, Integer rating, String comment, AppUser student) {
        return submitReview(String.valueOf(complaintId), rating, comment, student);
    }

    public ComplaintDetailDto submitReview(String idOrToken, Integer rating, String comment, AppUser student) {
        Complaint complaint = findByIdOrToken(idOrToken);

        assertStudentOwns(complaint, student);

        if (!"RESOLVED".equals(complaint.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Reviews can only be submitted for RESOLVED complaints");
        }
        if (reviewRepo.findByComplaintId(complaint.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A review already exists for this complaint");
        }

        Review review = Review.builder()
            .complaint(complaint)
            .rating(rating.shortValue())
            .comment(comment)
            .build();
        reviewRepo.save(review);

        complaint.setStatus("CLOSED");
        addStatusHistory(complaint, "CLOSED", student.getId(), student.getName(), "STUDENT",
            "Student submitted review (rating: " + rating + "/5). Complaint closed.");

        Complaint saved = complaintRepo.save(complaint);
        
        if (saved.getStudentUser() != null && saved.getStudentUser().getUser() != null) {
            notificationService.persistAndBroadcast(
                saved.getStudentUser().getUser(),
                saved,
                "STATUS_CHANGE",
                "Complaint Closed: " + saved.getToken(),
                "Thank you for your rating (" + rating + "/5). This complaint is now closed."
            );
        }

        ComplaintDetailDto detailDto = toDetailDto(saved);
        ComplaintSummaryDto summaryDto = toSummaryDto(saved);
        if (saved.getAssignedToStaff() != null) {
            notificationService.broadcastAssignment(saved.getAssignedToStaff().getUserId(), summaryDto);
        }
        notificationService.broadcastStatusChange(saved.getId(), saved.getStatus());
        notificationService.broadcastNewComplaint(summaryDto);
        notificationService.broadcastComplaintUpdate(detailDto);
        return detailDto;
    }

    /**
     * POST /api/complaints/{id}/reopen — student reopens RESOLVED or CLOSED complaint
     */
    public ComplaintDetailDto reopenComplaint(Integer complaintId, String reason, AppUser student) {
        return reopenComplaint(String.valueOf(complaintId), reason, student);
    }

    public ComplaintDetailDto reopenComplaint(String idOrToken, String reason, AppUser student) {
        Complaint complaint = findByIdOrToken(idOrToken);

        assertStudentOwns(complaint, student);

        if (!Set.of("RESOLVED", "CLOSED").contains(complaint.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Only RESOLVED or CLOSED complaints can be reopened");
        }

        complaint.setStatus("REOPENED");
        complaint.setResolvedAt(null);
        complaint.setAtRisk(false);
        complaint.setBreached(false);

        // Re-add to staff load if previously assigned
        if (complaint.getAssignedToStaff() != null && complaint.getPriorityScore() != null) {
            staffRepo.adjustLoad(complaint.getAssignedToStaff().getUserId(), complaint.getPriorityScore());
        }

        addStatusHistory(complaint, "REOPENED", student.getId(), student.getName(), "STUDENT",
            reason != null && !reason.isBlank() ? "Reopen reason: " + reason : "Complaint reopened by student");

        Complaint saved = complaintRepo.save(complaint);

        if (saved.getStudentUser() != null && saved.getStudentUser().getUser() != null) {
            notificationService.persistAndBroadcast(
                saved.getStudentUser().getUser(),
                saved,
                "STATUS_CHANGE",
                "Complaint Reopened: " + saved.getToken(),
                "Your complaint has been reopened for further investigation."
            );
        }

        // Broadcast back to admin queue and staff queue
        ComplaintDetailDto detailDto = toDetailDto(saved);
        ComplaintSummaryDto summaryDto = toSummaryDto(saved);
        if (saved.getAssignedToStaff() != null) {
            notificationService.broadcastAssignment(saved.getAssignedToStaff().getUserId(), summaryDto);
        }
        notificationService.broadcastNewComplaint(summaryDto);
        notificationService.broadcastStatusChange(saved.getId(), saved.getStatus());
        notificationService.broadcastComplaintUpdate(detailDto);
        return detailDto;
    }

    public Complaint findByIdOrToken(String idOrToken) {
        if (idOrToken == null || idOrToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Complaint ID or token cannot be empty");
        }
        if (idOrToken.matches("\\d+")) {
            return complaintRepo.findById(Integer.parseInt(idOrToken))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Complaint not found: " + idOrToken));
        } else {
            return complaintRepo.findByToken(idOrToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Complaint not found: " + idOrToken));
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Validate student owns this complaint (non-anonymous complaints only) */
    private void assertStudentOwns(Complaint complaint, AppUser student) {
        if (complaint.isAnonymous()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Anonymous complaints cannot be managed by students");
        }
        if (complaint.getStudentUser() == null ||
            !complaint.getStudentUser().getUserId().equals(student.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "You can only manage your own complaints");
        }
    }

    /** Extract a meaningful description from extraData map */
    private String extractDescription(Map<String, Object> extraData) {
        if (extraData == null || extraData.isEmpty()) return "Grievance submitted by student.";
        for (String key : List.of("description", "detailed_description", "errorDescription", "symptoms", "condition_summary", "title", "issueType")) {
            Object val = extraData.get(key);
            if (val != null && !val.toString().isBlank()) {
                return val.toString();
            }
        }
        for (Object val : extraData.values()) {
            if (val != null && !val.toString().isBlank()) {
                return val.toString();
            }
        }
        return "Grievance submitted by student.";
    }

    /**
     * Validate that all required fields from category.formSchema are present in extraData.
     * Returns serialized JSON string of extraData for storage.
     */
    @SuppressWarnings("unchecked")
    private String validateAndSerializeExtraData(Map<String, Object> extraData, String formSchema) {
        try {
            if (formSchema != null && !formSchema.isBlank() && !"[]".equals(formSchema.trim())) {
                List<Map<String, Object>> fields = objectMapper.readValue(
                    formSchema, new TypeReference<List<Map<String, Object>>>() {});
                List<String> missingRequired = new ArrayList<>();
                for (Map<String, Object> field : fields) {
                    Boolean required = (Boolean) field.get("required");
                    if (Boolean.TRUE.equals(required)) {
                        String fieldId = (String) field.get("id");
                        if (fieldId == null || fieldId.isBlank()) {
                            fieldId = (String) field.get("key");
                        }
                        if (fieldId != null && (extraData == null || !extraData.containsKey(fieldId)
                                || extraData.get(fieldId) == null
                                || extraData.get(fieldId).toString().isBlank())) {
                            // Also check case-insensitive or common variations
                            boolean matched = false;
                            if (extraData != null) {
                                for (String k : extraData.keySet()) {
                                    if (k.equalsIgnoreCase(fieldId)) {
                                        matched = extraData.get(k) != null && !extraData.get(k).toString().isBlank();
                                        break;
                                    }
                                }
                            }
                            if (!matched) {
                                missingRequired.add(fieldId);
                            }
                        }
                    }
                }
                if (!missingRequired.isEmpty()) {
                    log.warn("Submission had missing form schema fields: {}", missingRequired);
                }
            }
            return extraData != null ? objectMapper.writeValueAsString(extraData) : null;
        } catch (Exception e) {
            log.warn("Could not parse form schema or extraData: {}", e.getMessage());
            try {
                return extraData != null ? objectMapper.writeValueAsString(extraData) : null;
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private LocationDto toLocationDto(Location l) {
        if (l == null) return null;
        return LocationDto.builder()
            .id(l.getId()).building(l.getBuilding())
            .floor(l.getFloor()).room(l.getRoom())
            .campusZone(l.getCampusZone())
            .build();
    }

    // =========================================================================
    // Clustering — Manual Override
    // =========================================================================

    /**
     * Admin manually clusters a complaint into a specific incident.
     * Validates same category. Clears clusterExcluded flag.
     */
    public ComplaintDetailDto clusterManually(Integer complaintId, Integer targetIncidentId, AppUser admin) {
        Complaint c = complaintRepo.findById(complaintId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Complaint not found: " + complaintId));
        Incident target = incidentRepo.findById(targetIncidentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Incident not found: " + targetIncidentId));

        if (c.getCategory() != null && target.getCategory() != null
                && !c.getCategory().getId().equals(target.getCategory().getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Category mismatch: complaint category does not match incident category");
        }
        if ("RESOLVED".equals(target.getStatus()) || "ARCHIVED".equals(target.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cannot cluster into a resolved or archived incident");
        }

        Incident prev = c.getIncident();
        if (prev != null && !prev.getId().equals(targetIncidentId)) {
            // Remove from old incident
            int oldCount = prev.getComplaintCount() - 1;
            if (oldCount <= 0) prev.setStatus("ARCHIVED");
            else prev.setComplaintCount(oldCount);
            incidentRepo.save(prev);
        }

        c.setIncident(target);
        c.setClusterExcluded(false);
        c.setClusterScore(null); // manual override — no auto score
        target.setComplaintCount(target.getComplaintCount() + 1);
        target.setLastComplaintAt(java.time.LocalDateTime.now());
        incidentRepo.save(target);

        // Staff inheritance
        clusterService.inheritStaffIfAbsent(c, target);

        addStatusHistory(c, c.getStatus(), admin.getId(), admin.getName(), "ADMIN",
                "Manually clustered into incident #" + targetIncidentId);

        Complaint saved = complaintRepo.save(c);
        ComplaintDetailDto detail = toDetailDto(saved);
        notificationService.broadcastComplaintUpdate(detail);
        notificationService.broadcastNewComplaint(toSummaryDto(saved));
        return detail;
    }

    /**
     * Admin manually removes a complaint from its current incident.
     * Sets clusterExcluded = true to prevent re-auto-clustering.
     * Complaint remains standalone (incidentId = null).
     */
    public ComplaintDetailDto unclusterComplaint(Integer complaintId, String reason, AppUser admin) {
        Complaint c = complaintRepo.findById(complaintId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Complaint not found: " + complaintId));

        Incident incident = c.getIncident();
        if (incident == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Complaint is not part of any incident");
        }

        int newCount = incident.getComplaintCount() - 1;
        if (newCount <= 0) {
            incident.setStatus("ARCHIVED");
            incident.setComplaintCount(0);
        } else {
            incident.setComplaintCount(newCount);
        }
        incidentRepo.save(incident);

        String auditNote = "Removed from incident #" + incident.getId()
                + (reason != null && !reason.isBlank() ? ". Reason: " + reason : "");

        c.setIncident(null);
        c.setClusterExcluded(true);
        c.setClusterScore(null);

        addStatusHistory(c, c.getStatus(), admin.getId(), admin.getName(), "ADMIN", auditNote);
        Complaint saved = complaintRepo.save(c);
        ComplaintDetailDto detail = toDetailDto(saved);
        notificationService.broadcastComplaintUpdate(detail);
        notificationService.broadcastNewComplaint(toSummaryDto(saved));
        return detail;
    }

    /**
     * Build IncidentSummaryDto with all cluster members for complaint detail view.
     * Returns null if the complaint is not part of any incident.
     */
    private IncidentSummaryDto buildIncidentSummaryDto(Complaint c) {
        if (c.getIncident() == null) return null;
        Incident incident = c.getIncident();
        List<ClusterMemberDto> members = complaintRepo.findByIncidentId(incident.getId()).stream()
                .map(m -> ClusterMemberDto.builder()
                        .id(m.getId())
                        .token(m.getToken())
                        .description(m.getDescription())
                        .status(m.getStatus())
                        .clusterScore(m.getClusterScore())
                        .createdAt(m.getCreatedAt() != null ? m.getCreatedAt().toString() : null)
                        .build())
                .collect(Collectors.toList());
        return IncidentSummaryDto.builder()
                .id(incident.getId())
                .status(incident.getStatus())
                .complaintCount(incident.getComplaintCount())
                .summary(incident.getSummary())
                .members(members)
                .createdAt(incident.getCreatedAt() != null ? incident.getCreatedAt().toString() : null)
                .lastComplaintAt(incident.getLastComplaintAt() != null ? incident.getLastComplaintAt().toString() : null)
                .build();
    }
}

