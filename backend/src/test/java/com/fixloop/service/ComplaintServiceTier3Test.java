package com.fixloop.service;

import com.fixloop.domain.*;
import com.fixloop.dto.complaint.DispatchRequest;
import com.fixloop.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComplaintServiceTier3Test {

    @Mock
    private ComplaintRepository complaintRepo;

    @Mock
    private AutoAssignService autoAssignService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private StatusHistoryRepository statusHistoryRepo;

    @Mock
    private StaffProfileRepository staffRepo;

    @Mock
    private VendorRepository vendorRepo;

    @Mock
    private ComplaintVendorRepository complaintVendorRepo;

    @Mock
    private EscalationLogRepository escalationLogRepo;

    @Mock
    private AttachmentRepository attachmentRepo;

    @Mock
    private ReviewRepository reviewRepo;

    @Mock
    private IncidentRepository incidentRepo;

    @InjectMocks
    private ComplaintService complaintService;

    private Complaint testComplaint;
    private AppUser adminUser;
    private Category category;

    @BeforeEach
    void setUp() {
        category = Category.builder()
                .id(1)
                .name("Hostel")
                .sensitive(false)
                .requiredSkills(Collections.emptySet())
                .build();

        testComplaint = Complaint.builder()
                .id(999)
                .token("FXL-2026-000999")
                .category(category)
                .status("SUBMITTED")
                .priorityScore(new BigDecimal("75.00"))
                .createdAt(LocalDateTime.now())
                .build();

        adminUser = AppUser.builder()
                .id(1)
                .name("Admin Krishnan")
                .email("admin@fixloop.edu")
                .role("ADMIN")
                .restrictedAccess(true)
                .build();
    }

    @Test
    void testDispatch_Tier3Fallback_NoMatchingStaff_FlagsForManualAssignmentAndNotifiesAdmin() {
        // Given: autoAssignService finds NO matching staff (Tier 3 fallback)
        when(complaintRepo.findById(999)).thenReturn(Optional.of(testComplaint));
        when(autoAssignService.findBestStaff(eq(2), any())).thenReturn(Optional.empty());
        when(complaintRepo.save(any(Complaint.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DispatchRequest request = DispatchRequest.builder()
                .dispatchType("INTERNAL_STAFF")
                .departmentId(2)
                .build();

        // When
        complaintService.dispatch(999, request, adminUser);

        // Then:
        // 1. Complaint status must be set to UNDER_ADMIN_REVIEW
        assertEquals("UNDER_ADMIN_REVIEW", testComplaint.getStatus(),
                "Complaint must be placed in UNDER_ADMIN_REVIEW when auto-assign finds no staff");

        // 2. Complaint must remain unassigned to staff
        assertNull(testComplaint.getAssignedToStaff(),
                "Complaint assigned staff must remain null pending manual assignment");

        // 3. Status history must explicitly record the manual triage requirement
        ArgumentCaptor<StatusHistory> historyCaptor = ArgumentCaptor.forClass(StatusHistory.class);
        verify(statusHistoryRepo, atLeastOnce()).save(historyCaptor.capture());
        StatusHistory lastHistory = historyCaptor.getValue();
        assertEquals("UNDER_ADMIN_REVIEW", lastHistory.getStatus());
        assertTrue(lastHistory.getNote().contains("manual assignment required"),
                "Audit note must indicate manual assignment is required");

        // 4. Admin alert must be broadcasted to /topic/admin/alerts
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> alertCaptor = ArgumentCaptor.forClass(Map.class);
        verify(notificationService, times(1)).broadcastAdminAlert(alertCaptor.capture());
        Map<String, Object> alert = alertCaptor.getValue();
        assertEquals("MANUAL_ASSIGNMENT_REQUIRED", alert.get("type"));
        assertEquals(999, alert.get("complaintId"));
        assertEquals("FXL-2026-000999", alert.get("token"));

        // 5. Staff assignment notification must NOT be fired
        verify(notificationService, never()).broadcastAssignment(any(), any());
    }
}
