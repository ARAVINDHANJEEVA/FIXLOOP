package com.fixloop.service;

import com.fixloop.domain.Complaint;
import com.fixloop.domain.Department;
import com.fixloop.domain.Skill;
import com.fixloop.domain.StaffProfile;
import com.fixloop.repository.StaffProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutoAssignServiceTest {

    @Mock
    private StaffProfileRepository staffRepo;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AutoAssignService autoAssignService;

    private Department dept;
    private Skill plumbingSkill;
    private StaffProfile specialistStaff;
    private StaffProfile generalStaff;

    @BeforeEach
    void setUp() {
        dept = Department.builder().id(1).name("Maintenance").code("MAINT").build();
        plumbingSkill = Skill.builder().id(10).name("plumbing").build();

        specialistStaff = StaffProfile.builder()
                .userId(101)
                .department(dept)
                .jobTitle("Plumber Specialist")
                .status("ACTIVE")
                .currentWeightedLoad(new BigDecimal("20.00"))
                .build();

        generalStaff = StaffProfile.builder()
                .userId(102)
                .department(dept)
                .jobTitle("General Technician")
                .status("ACTIVE")
                .currentWeightedLoad(new BigDecimal("10.00"))
                .build();
    }

    @Test
    void testTier1_ExactSkillMatch_ReturnsSpecialist() {
        when(staffRepo.findActiveByDeptAndSkill(eq(1), eq(10), any(PageRequest.class)))
                .thenReturn(List.of(specialistStaff));

        Optional<StaffProfile> result = autoAssignService.findBestStaff(1, Set.of(plumbingSkill));

        assertTrue(result.isPresent());
        assertEquals(101, result.get().getUserId());
        verify(staffRepo, times(1)).findActiveByDeptAndSkill(eq(1), eq(10), any(PageRequest.class));
        verify(staffRepo, never()).findActiveByDept(any(), any());
    }

    @Test
    void testTier2_NoSkillMatch_FallbacksToActiveStaffInDept() {
        when(staffRepo.findActiveByDeptAndSkill(eq(1), eq(10), any(PageRequest.class)))
                .thenReturn(Collections.emptyList());
        when(staffRepo.findActiveByDept(eq(1), any(PageRequest.class)))
                .thenReturn(List.of(generalStaff));

        Optional<StaffProfile> result = autoAssignService.findBestStaff(1, Set.of(plumbingSkill));

        assertTrue(result.isPresent());
        assertEquals(102, result.get().getUserId());
        verify(staffRepo, times(1)).findActiveByDept(eq(1), any(PageRequest.class));
    }

    @Test
    void testTier3_NoActiveStaffInDept_ReturnsEmptyForManualAssignment() {
        when(staffRepo.findActiveByDeptAndSkill(eq(1), eq(10), any(PageRequest.class)))
                .thenReturn(Collections.emptyList());
        when(staffRepo.findActiveByDept(eq(1), any(PageRequest.class)))
                .thenReturn(Collections.emptyList());

        Optional<StaffProfile> result = autoAssignService.findBestStaff(1, Set.of(plumbingSkill));

        assertTrue(result.isEmpty());
    }

    @Test
    void testAssignComplaint_SetsStatusAndIncrementsStaffLoad() {
        Complaint complaint = Complaint.builder()
                .id(501)
                .priorityScore(new BigDecimal("85.50"))
                .build();

        autoAssignService.assignComplaint(complaint, specialistStaff);

        assertEquals("ASSIGNED", complaint.getStatus());
        assertEquals(specialistStaff, complaint.getAssignedToStaff());
        verify(staffRepo, times(1)).adjustLoad(101, new BigDecimal("85.50"));
    }

    @Test
    void testReleaseLoad_DecrementsStaffLoadByPriorityScore() {
        Complaint complaint = Complaint.builder()
                .id(501)
                .assignedToStaff(specialistStaff)
                .priorityScore(new BigDecimal("85.50"))
                .build();

        autoAssignService.releaseLoad(complaint);

        verify(staffRepo, times(1)).adjustLoad(101, new BigDecimal("-85.50"));
    }
}
