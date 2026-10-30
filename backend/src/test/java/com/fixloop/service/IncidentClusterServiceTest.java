package com.fixloop.service;

import com.fixloop.domain.*;
import com.fixloop.repository.ComplaintRepository;
import com.fixloop.repository.IncidentRepository;
import com.fixloop.repository.StaffProfileRepository;
import com.fixloop.repository.StatusHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for IncidentClusterService.
 *
 * Verifies all hard gates, scoring logic, and edge cases
 * WITHOUT hitting the database (all repos mocked).
 */
@ExtendWith(MockitoExtension.class)
class IncidentClusterServiceTest {

    @Mock private IncidentRepository incidentRepo;
    @Mock private ComplaintRepository complaintRepo;
    @Mock private StaffProfileRepository staffRepo;
    @Mock private StatusHistoryRepository statusHistoryRepo;
    @Mock private JdbcTemplate jdbcTemplate;

    @Spy
    private TfCosineSimilarityService similarityService = new TfCosineSimilarityService();

    @InjectMocks
    private IncidentClusterService clusterService;

    // ─── Test fixtures ───────────────────────────────────────────────────────

    private Category category;
    private Location locationA;   // Block A, Floor 2, Room 201
    private Location locationA2;  // Block A, Floor 2, Room 202
    private Location locationB;   // Block B (different building)
    private Complaint complaint;

    @BeforeEach
    void setUp() {
        // Configure service properties
        ReflectionTestUtils.setField(clusterService, "clusterWindowHours", 72);
        ReflectionTestUtils.setField(clusterService, "clusterThreshold", 0.60);
        ReflectionTestUtils.setField(clusterService, "maxClusterSize", 50);
        ReflectionTestUtils.setField(clusterService, "semanticFloor", 0.35);
        ReflectionTestUtils.setField(clusterService, "timeDecayLambda", 0.02);


        // Build test fixtures
        category = new Category();
        category.setId(3);
        category.setName("Hostel");
        category.setSensitive(false);

        locationA = new Location();
        locationA.setId(10);
        locationA.setBuilding("Block A");
        locationA.setFloor("Floor 2");
        locationA.setRoom("201");

        locationA2 = new Location();
        locationA2.setId(11);
        locationA2.setBuilding("Block A");
        locationA2.setFloor("Floor 2");
        locationA2.setRoom("202");

        locationB = new Location();
        locationB.setId(20);
        locationB.setBuilding("Block B");
        locationB.setFloor("Floor 1");
        locationB.setRoom("101");

        complaint = Complaint.builder()
                .id(100)
                .token("SNT-2026-000100")
                .category(category)
                .location(locationA)
                .description("Water leaking from broken pipe near washroom on second floor")
                .severity((short) 3)
                .studentsAffected(5)
                .status("SUBMITTED")
                .priorityScore(BigDecimal.valueOf(0.75))
                .build();
    }

    // ─── GATE 1: Sensitive category ─────────────────────────────────────────

    @Test
    @DisplayName("GATE 1: Sensitive category → returns null, no DB query")
    void sensitiveCategorySkipsCluster() {
        category.setSensitive(true);
        Incident result = clusterService.clusterComplaint(complaint);
        assertThat(result).isNull();
        verifyNoInteractions(incidentRepo);
    }

    // ─── GATE 2: Cluster excluded flag ──────────────────────────────────────

    @Test
    @DisplayName("GATE 2: cluster_excluded=true → returns null, no DB query")
    void clusterExcludedSkipsCluster() {
        complaint.setClusterExcluded(true);
        Incident result = clusterService.clusterComplaint(complaint);
        assertThat(result).isNull();
        verifyNoInteractions(incidentRepo);
    }

    // ─── No candidates → new incident ───────────────────────────────────────

    @Test
    @DisplayName("No candidates → creates new incident")
    void noCandidatesCreatesNewIncident() {
        when(incidentRepo.findCandidates(eq(3), eq("Block A"), any(), eq(50)))
                .thenReturn(List.of());

        Incident savedIncident = Incident.builder()
                .id(1).category(category).location(locationA)
                .status("OPEN").complaintCount(1)
                .summary(complaint.getDescription())
                .lastComplaintAt(LocalDateTime.now())
                .build();
        when(incidentRepo.save(any())).thenReturn(savedIncident);

        Incident result = clusterService.clusterComplaint(complaint);

        assertThat(result).isNotNull();
        assertThat(result.getComplaintCount()).isEqualTo(1);
        verify(incidentRepo).save(argThat(i -> "OPEN".equals(i.getStatus()) && i.getComplaintCount() == 1));
    }

    // ─── High semantic + same room → clusters ────────────────────────────────

    @Test
    @DisplayName("High semantic score + same room → merges into existing incident")
    void highSemanticSameRoomClusters() {
        Incident existingIncident = buildOpenIncident(1, category, locationA);

        when(incidentRepo.findCandidates(eq(3), eq("Block A"), any(), eq(50)))
                .thenReturn(List.of(existingIncident));
        when(complaintRepo.findDescriptionsByIncidentId(1))
                .thenReturn(List.of("Water leaking from pipe near washroom, floor is wet"));
        when(complaintRepo.findLocationsByIncidentId(1))
                .thenReturn(List.of(locationA));
        when(incidentRepo.save(any())).thenReturn(existingIncident);
        when(complaintRepo.findByIncidentId(1)).thenReturn(List.of());

        Incident result = clusterService.clusterComplaint(complaint);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1);
        assertThat(result.getComplaintCount()).isEqualTo(2); // incremented
    }

    // ─── Low semantic (<0.35 floor) → does NOT cluster ───────────────────────

    @Test
    @DisplayName("Low semantic score (below floor 0.35) → new incident created")
    void lowSemanticBelowFloorDoesNotCluster() {
        Incident existingIncident = buildOpenIncident(1, category, locationA);

        when(incidentRepo.findCandidates(eq(3), eq("Block A"), any(), eq(50)))
                .thenReturn(List.of(existingIncident));
        when(complaintRepo.findDescriptionsByIncidentId(1))
                .thenReturn(List.of("WiFi not working in study room, no internet access"));
        lenient().when(complaintRepo.findLocationsByIncidentId(1))
                .thenReturn(List.of(locationA));

        Incident newIncident = buildOpenIncident(2, category, locationA);
        when(incidentRepo.save(any())).thenReturn(newIncident);

        Incident result = clusterService.clusterComplaint(complaint);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(2); // new incident, not the existing WiFi one
    }

    // ─── Room-aware location scoring ─────────────────────────────────────────

    @Test
    @DisplayName("Both complaints specify different rooms → location score = 0.0 → no cluster")
    void bothSpecifyRoomsDifferentRoomsDoesNotCluster() {
        // Room 201 vs Room 202 — both specified, different → location = 0.0
        // Even with high semantic: 0.45×0.0 + 0.35×0.9 + 0.20×1.0 = 0 + 0.315 + 0.20 = 0.515 < 0.60
        Incident existingIncident = buildOpenIncident(1, category, locationA2); // Room 202

        when(incidentRepo.findCandidates(eq(3), eq("Block A"), any(), eq(50)))
                .thenReturn(List.of(existingIncident));
        when(complaintRepo.findDescriptionsByIncidentId(1))
                .thenReturn(List.of("Water leaking from pipe near washroom on second floor, room 202 wet"));
        when(complaintRepo.findLocationsByIncidentId(1))
                .thenReturn(List.of(locationA2)); // Room 202 ≠ Room 201

        Incident newIncident = buildOpenIncident(2, category, locationA);
        when(incidentRepo.save(any())).thenReturn(newIncident);

        Incident result = clusterService.clusterComplaint(complaint);
        assertThat(result.getId()).isEqualTo(2); // new incident
    }

    @Test
    @DisplayName("New complaint has no room, existing has room → relax to floor (0.80) → can cluster")
    void newComplaintNoRoomRelaxesToFloor() {
        Location noRoomLocation = new Location();
        noRoomLocation.setId(12);
        noRoomLocation.setBuilding("Block A");
        noRoomLocation.setFloor("Floor 2");
        noRoomLocation.setRoom(null); // no room specified

        complaint.setLocation(noRoomLocation);

        Incident existingIncident = buildOpenIncident(1, category, locationA);

        when(incidentRepo.findCandidates(eq(3), eq("Block A"), any(), eq(50)))
                .thenReturn(List.of(existingIncident));
        when(complaintRepo.findDescriptionsByIncidentId(1))
                .thenReturn(List.of("Water leaking from pipe near washroom on second floor"));
        when(complaintRepo.findLocationsByIncidentId(1))
                .thenReturn(List.of(locationA));
        when(incidentRepo.save(any())).thenReturn(existingIncident);
        when(complaintRepo.findByIncidentId(1)).thenReturn(List.of());

        // no-room vs Room201 → one side has no room → relax to floor → location=0.80
        // 0.45×0.80 + 0.35×(high semantic) + 0.20×1.0 should exceed 0.60
        Incident result = clusterService.clusterComplaint(complaint);
        assertThat(result.getId()).isEqualTo(1); // should cluster
    }

    // ─── Staff inheritance ────────────────────────────────────────────────────

    @Test
    @DisplayName("Staff inheritance: complaint inherits staff, load updated, history logged")
    void staffInheritanceUpdatesLoad() {
        AppUser staffUser = new AppUser();
        staffUser.setId(5);
        staffUser.setName("John Doe");

        StaffProfile staff = new StaffProfile();
        staff.setUserId(5);
        staff.setUser(staffUser);
        staff.setCurrentWeightedLoad(BigDecimal.valueOf(2.0));

        Complaint sibling = Complaint.builder()
                .id(99).assignedToStaff(staff).status("ASSIGNED").build();

        Incident incident = buildOpenIncident(1, category, locationA);
        when(complaintRepo.findByIncidentId(1)).thenReturn(List.of(sibling));

        Complaint newComplaint = Complaint.builder()
                .id(200).token("SNT-2026-000200").category(category).location(locationA)
                .description("same water issue").status("SUBMITTED")
                .priorityScore(BigDecimal.valueOf(0.5)).build();

        clusterService.inheritStaffIfAbsent(newComplaint, incident);

        assertThat(newComplaint.getAssignedToStaff()).isEqualTo(staff);
        assertThat(newComplaint.getStatus()).isEqualTo("ASSIGNED");
        verify(staffRepo).adjustLoad(eq(5), eq(BigDecimal.valueOf(0.5)));
        verify(statusHistoryRepo).save(argThat(sh ->
                "SYSTEM".equals(sh.getChangedByRole()) &&
                sh.getNote().contains("Auto-inherited assignment from incident #1")));
    }

    @Test
    @DisplayName("Staff inheritance: already-assigned complaint is NOT overridden")
    void staffInheritanceDoesNotOverrideExistingAssignment() {
        StaffProfile existingStaff = new StaffProfile();
        existingStaff.setUserId(10);
        complaint.setAssignedToStaff(existingStaff);

        Incident incident = buildOpenIncident(1, category, locationA);

        clusterService.inheritStaffIfAbsent(complaint, incident);

        verifyNoInteractions(complaintRepo, staffRepo, statusHistoryRepo);
        assertThat(complaint.getAssignedToStaff()).isEqualTo(existingStaff);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Incident buildOpenIncident(int id, Category cat, Location loc) {
        Incident i = new Incident();
        i.setId(id);
        i.setCategory(cat);
        i.setLocation(loc);
        i.setStatus("OPEN");
        i.setComplaintCount(1);
        i.setLastComplaintAt(LocalDateTime.now().minusMinutes(30));
        i.setSummary("Water leaking from pipe near washroom");
        return i;
    }
}
