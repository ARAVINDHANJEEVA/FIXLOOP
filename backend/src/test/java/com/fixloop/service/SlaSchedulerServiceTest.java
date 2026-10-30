package com.fixloop.service;

import com.fixloop.domain.AppUser;
import com.fixloop.domain.Complaint;
import com.fixloop.domain.EscalationLog;
import com.fixloop.domain.StaffProfile;
import com.fixloop.domain.StatusHistory;
import com.fixloop.repository.ComplaintRepository;
import com.fixloop.repository.EscalationLogRepository;
import com.fixloop.repository.StatusHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlaSchedulerServiceTest {

    @Mock
    private ComplaintRepository complaintRepo;

    @Mock
    private EscalationLogRepository escalationLogRepo;

    @Mock
    private StatusHistoryRepository statusHistoryRepo;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private SlaSchedulerService slaSchedulerService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(slaSchedulerService, "atRiskBufferHours", 2);
    }

    @Test
    void testRunSlaCheck_AtRiskCandidate_FlaggedAndAlerted() {
        LocalDateTime deadline = LocalDateTime.now().plusHours(1);
        Complaint atRiskComplaint = Complaint.builder()
                .id(101)
                .token("FXL-2026-000101")
                .status("ASSIGNED")
                .isAtRisk(false)
                .isBreached(false)
                .slaDeadline(deadline)
                .build();

        when(complaintRepo.findAtRiskCandidates(any(LocalDateTime.class))).thenReturn(List.of(atRiskComplaint));
        when(complaintRepo.findBreachedCandidates(any(LocalDateTime.class))).thenReturn(Collections.emptyList());

        slaSchedulerService.runSlaCheck();

        assertTrue(atRiskComplaint.isAtRisk(), "Complaint must be flagged at-risk");
        verify(complaintRepo, times(1)).save(atRiskComplaint);
        verify(notificationService, times(1)).broadcastSlaAlert(argThat(map -> "AT_RISK".equals(map.get("type"))));
    }

    @Test
    void testRunSlaCheck_BreachedCandidate_FlaggedEscalatedAndLogged() {
        LocalDateTime pastDeadline = LocalDateTime.now().minusHours(2);
        StaffProfile staff = StaffProfile.builder().userId(55).build();
        Complaint breachedComplaint = Complaint.builder()
                .id(102)
                .token("FXL-2026-000102")
                .status("IN_PROGRESS")
                .escalationLevel((short) 0)
                .assignedToStaff(staff)
                .isAtRisk(false)
                .isBreached(false)
                .slaDeadline(pastDeadline)
                .build();

        when(complaintRepo.findAtRiskCandidates(any(LocalDateTime.class))).thenReturn(Collections.emptyList());
        when(complaintRepo.findBreachedCandidates(any(LocalDateTime.class))).thenReturn(List.of(breachedComplaint));

        slaSchedulerService.runSlaCheck();

        assertTrue(breachedComplaint.isBreached(), "Complaint must be flagged breached");
        assertEquals((short) 1, breachedComplaint.getEscalationLevel(), "Escalation level must be incremented to 1");

        // Verify EscalationLog created with 'AUTO'
        ArgumentCaptor<EscalationLog> logCaptor = ArgumentCaptor.forClass(EscalationLog.class);
        verify(escalationLogRepo, times(1)).save(logCaptor.capture());
        EscalationLog savedLog = logCaptor.getValue();
        assertEquals("AUTO", savedLog.getTriggeredBy());
        assertEquals((short) 0, savedLog.getPreviousLevel());
        assertEquals((short) 1, savedLog.getNewLevel());

        // Verify StatusHistory created
        ArgumentCaptor<StatusHistory> histCaptor = ArgumentCaptor.forClass(StatusHistory.class);
        verify(statusHistoryRepo, times(1)).save(histCaptor.capture());
        assertEquals("SYSTEM", histCaptor.getValue().getChangedByRole());

        verify(notificationService, times(1)).broadcastSlaAlert(argThat(map -> "BREACHED".equals(map.get("type"))));
    }

    @Test
    void testRunSlaCheck_Idempotency_SecondRunDoesNotDoubleEscalate() {
        // First run returns one candidate
        Complaint breachedComplaint = Complaint.builder()
                .id(103)
                .token("FXL-2026-000103")
                .status("ASSIGNED")
                .escalationLevel((short) 0)
                .isBreached(false)
                .slaDeadline(LocalDateTime.now().minusHours(1))
                .build();

        when(complaintRepo.findAtRiskCandidates(any(LocalDateTime.class))).thenReturn(Collections.emptyList());
        when(complaintRepo.findBreachedCandidates(any(LocalDateTime.class)))
                .thenReturn(List.of(breachedComplaint)) // 1st invocation
                .thenReturn(Collections.emptyList());   // 2nd invocation (already flagged is_breached=true in DB)

        // Run 1
        slaSchedulerService.runSlaCheck();
        assertEquals((short) 1, breachedComplaint.getEscalationLevel());

        // Run 2
        slaSchedulerService.runSlaCheck();

        // Level must remain 1, save must have only been called once for breached
        assertEquals((short) 1, breachedComplaint.getEscalationLevel());
        verify(escalationLogRepo, times(1)).save(any(EscalationLog.class));
    }

    @Test
    void testForceEscalate_ManualByAdmin() {
        AppUser admin = AppUser.builder().id(1).name("Super Admin").build();
        Complaint complaint = Complaint.builder()
                .id(104)
                .token("FXL-2026-000104")
                .status("ASSIGNED")
                .escalationLevel((short) 1)
                .build();

        when(complaintRepo.findById(104)).thenReturn(Optional.of(complaint));

        slaSchedulerService.forceEscalate(104, admin);

        assertEquals((short) 2, complaint.getEscalationLevel(), "Escalation level must be incremented to 2");

        ArgumentCaptor<EscalationLog> logCaptor = ArgumentCaptor.forClass(EscalationLog.class);
        verify(escalationLogRepo, times(1)).save(logCaptor.capture());
        assertEquals("MANUAL", logCaptor.getValue().getTriggeredBy());

        ArgumentCaptor<StatusHistory> histCaptor = ArgumentCaptor.forClass(StatusHistory.class);
        verify(statusHistoryRepo, times(1)).save(histCaptor.capture());
        assertEquals("ADMIN", histCaptor.getValue().getChangedByRole());
        assertEquals(1, histCaptor.getValue().getChangedById());
    }
}
