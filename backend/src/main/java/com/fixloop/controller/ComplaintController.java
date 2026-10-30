package com.fixloop.controller;

import com.fixloop.domain.AppUser;
import com.fixloop.dto.complaint.*;
import com.fixloop.dto.complaint.ClusterActionRequest;
import com.fixloop.repository.AppUserRepository;
import com.fixloop.service.ComplaintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;
    private final AppUserRepository userRepo;

    private AppUser getCurrentUser(Authentication auth) {
        return userRepo.findByEmail(auth.getName()).orElseThrow();
    }

    // ──────────────────────────────────────────────────────────────
    // Student: Submit a new complaint  [THE MISSING ENDPOINT]
    // ──────────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('STUDENT', 'ROLE_STUDENT')")
    public ResponseEntity<ComplaintCreatedDto> createComplaint(
            @Valid @RequestBody ComplaintCreateRequest request,
            Authentication auth) {
        AppUser user = getCurrentUser(auth);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(complaintService.createComplaint(request, user));
    }

    // ──────────────────────────────────────────────────────────────
    // Admin: paginated complaint queue
    // ──────────────────────────────────────────────────────────────

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ComplaintSummaryDto>> getAdminQueue(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) Integer departmentId,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(defaultValue = "false") Boolean restricted,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {
        AppUser currentUser = getCurrentUser(auth);
        if (Boolean.TRUE.equals(restricted) && !currentUser.isRestrictedAccess()) {
            throw new org.springframework.security.access.AccessDeniedException(
                "Access Denied: restricted_access privilege required to view sensitive complaints"
            );
        }
        return ResponseEntity.ok(
            complaintService.getAdminQueue(status, categoryId, departmentId, dateFrom, dateTo, restricted, page, size)
        );
    }

    // ──────────────────────────────────────────────────────────────
    // Staff: personal assigned queue
    // ──────────────────────────────────────────────────────────────

    @GetMapping("/mine")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<Page<ComplaintSummaryDto>> getStaffQueue(
            Authentication auth,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        AppUser currentUser = getCurrentUser(auth);
        return ResponseEntity.ok(complaintService.getStaffQueue(currentUser.getId(), status, page, size));
    }

    // ──────────────────────────────────────────────────────────────
    // Get complaint detail — extended to STUDENT with ownership check
    // ──────────────────────────────────────────────────────────────

    @GetMapping("/{idOrToken}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','STUDENT')")
    public ResponseEntity<ComplaintDetailDto> getById(@PathVariable String idOrToken, Authentication auth) {
        AppUser currentUser = getCurrentUser(auth);
        ComplaintDetailDto detail;
        if (idOrToken.matches("\\d+")) {
            detail = complaintService.getById(Integer.parseInt(idOrToken), currentUser);
        } else {
            detail = complaintService.getByToken(idOrToken, currentUser);
        }
        // Student ownership check
        if ("STUDENT".equals(currentUser.getRole())) {
            if (detail.isAnonymous()) {
                throw new org.springframework.security.access.AccessDeniedException(
                    "Anonymous complaints cannot be viewed via student API");
            }
            if (detail.getStudent() == null ||
                !detail.getStudent().getId().equals(currentUser.getId())) {
                throw new org.springframework.security.access.AccessDeniedException(
                    "You can only view your own complaints");
            }
        }
        return ResponseEntity.ok(detail);
    }

    // ──────────────────────────────────────────────────────────────
    // Admin: dispatch, assign, reassign
    // ──────────────────────────────────────────────────────────────

    @PatchMapping("/{id}/dispatch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ComplaintDetailDto> dispatch(
            @PathVariable Integer id,
            @Valid @RequestBody DispatchRequest request,
            Authentication auth) {
        return ResponseEntity.ok(complaintService.dispatch(id, request, getCurrentUser(auth)));
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ComplaintDetailDto> assign(
            @PathVariable Integer id,
            @RequestBody(required = false) Map<String, Integer> body,
            Authentication auth) {
        DispatchRequest req = DispatchRequest.builder()
            .dispatchType("INTERNAL_STAFF")
            .departmentId(body != null ? body.get("departmentId") : null)
            .build();
        return ResponseEntity.ok(complaintService.dispatch(id, req, getCurrentUser(auth)));
    }

    @PostMapping("/{id}/reassign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ComplaintDetailDto> reassign(
            @PathVariable Integer id,
            @Valid @RequestBody ReassignRequest request,
            Authentication auth) {
        return ResponseEntity.ok(complaintService.reassign(id, request, getCurrentUser(auth)));
    }

    // ──────────────────────────────────────────────────────────────
    // Staff/Admin: status update, request-info, vendor
    // ──────────────────────────────────────────────────────────────

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<ComplaintDetailDto> updateStatus(
            @PathVariable Integer id,
            @Valid @RequestBody StatusUpdateRequest request,
            Authentication auth) {
        return ResponseEntity.ok(complaintService.updateStatus(id, request, getCurrentUser(auth)));
    }

    @PostMapping("/{id}/request-info")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ResponseEntity<ComplaintDetailDto> requestInfo(
            @PathVariable("id") String id,
            @Valid @RequestBody RequestInfoRequest request,
            Authentication auth) {
        return ResponseEntity.ok(complaintService.requestInfo(id, request, getCurrentUser(auth)));
    }

    @PostMapping("/{id}/vendor")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ResponseEntity<ComplaintDetailDto> updateVendor(
            @PathVariable Integer id,
            @Valid @RequestBody VendorUpdateRequest request,
            Authentication auth) {
        return ResponseEntity.ok(complaintService.updateVendor(id, request, getCurrentUser(auth)));
    }

    // ──────────────────────────────────────────────────────────────
    // Student: reply to info request, submit review, reopen
    // ──────────────────────────────────────────────────────────────

    @PostMapping("/{id}/reply")
    @PreAuthorize("hasAnyRole('STUDENT', 'ROLE_STUDENT')")
    public ResponseEntity<ComplaintDetailDto> reply(
            @PathVariable("id") String id,
            @Valid @RequestBody ReplyRequest request,
            Authentication auth) {
        return ResponseEntity.ok(
            complaintService.replyToInfoRequest(id, request.getReplyText(), getCurrentUser(auth)));
    }

    @PostMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('STUDENT', 'ROLE_STUDENT')")
    public ResponseEntity<ComplaintDetailDto> submitReview(
            @PathVariable("id") String id,
            @Valid @RequestBody ReviewRequest request,
            Authentication auth) {
        return ResponseEntity.ok(
            complaintService.submitReview(id, request.getRating(), request.getComment(), getCurrentUser(auth)));
    }

    @PostMapping("/{id}/reopen")
    @PreAuthorize("hasAnyRole('STUDENT', 'ROLE_STUDENT')")
    public ResponseEntity<ComplaintDetailDto> reopen(
            @PathVariable("id") String id,
            @RequestBody(required = false) ReopenRequest request,
            Authentication auth) {
        String reason = request != null ? request.getReason() : null;
        return ResponseEntity.ok(complaintService.reopenComplaint(id, reason, getCurrentUser(auth)));
    }

    // ──────────────────────────────────────────────────────────────
    // History — extended to STUDENT with ownership check
    // ──────────────────────────────────────────────────────────────

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','STUDENT')")
    public ResponseEntity<List<StatusHistoryEntryDto>> getHistory(
            @PathVariable Integer id, Authentication auth) {
        AppUser currentUser = getCurrentUser(auth);
        ComplaintDetailDto detail = complaintService.getById(id, currentUser);
        if ("STUDENT".equals(currentUser.getRole())) {
            if (detail.getStudent() == null ||
                !detail.getStudent().getId().equals(currentUser.getId())) {
                throw new org.springframework.security.access.AccessDeniedException(
                    "You can only view history of your own complaints");
            }
        }
        return ResponseEntity.ok(detail.getStatusHistory());
    }

    // ──────────────────────────────────────────────────────────────
    // Admin: manual cluster / uncluster override
    // ──────────────────────────────────────────────────────────────

    /**
     * Admin manually clusters a complaint into a specific existing incident.
     * POST /api/complaints/{id}/cluster
     * Body: { targetIncidentId: 42, reason: "same pipe issue" }
     */
    @PostMapping("/{id}/cluster")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ComplaintDetailDto> clusterManually(
            @PathVariable Integer id,
            @RequestBody ClusterActionRequest request,
            Authentication auth) {
        if (request.getTargetIncidentId() == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "targetIncidentId is required");
        }
        return ResponseEntity.ok(complaintService.clusterManually(id, request.getTargetIncidentId(), getCurrentUser(auth)));
    }

    /**
     * Admin removes a complaint from its current incident (sets cluster_excluded = true).
     * POST /api/complaints/{id}/uncluster
     * Body: { reason: "different floor" }
     */
    @PostMapping("/{id}/uncluster")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ComplaintDetailDto> uncluster(
            @PathVariable Integer id,
            @RequestBody(required = false) ClusterActionRequest request,
            Authentication auth) {
        String reason = (request != null) ? request.getReason() : null;
        return ResponseEntity.ok(complaintService.unclusterComplaint(id, reason, getCurrentUser(auth)));
    }
}

