package com.fixloop.service;

import com.fixloop.domain.Complaint;
import com.fixloop.domain.Incident;
import com.fixloop.domain.Location;
import com.fixloop.domain.StaffProfile;
import com.fixloop.domain.StatusHistory;
import com.fixloop.repository.ComplaintRepository;
import com.fixloop.repository.IncidentRepository;
import com.fixloop.repository.StaffProfileRepository;
import com.fixloop.repository.StatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Smart incident clustering engine.
 *
 * Algorithm per complaint submission:
 *  1. Hard gates: skip if sensitive, skip if cluster_excluded.
 *  2. Advisory lock on (category, building) — serialises concurrent submissions.
 *  3. Fetch OPEN incident candidates (same category, same building, within window, under max size).
 *  4. Score each candidate via 3-factor weighted formula:
 *       score = 0.45 x locationScore(all members)
 *             + 0.35 x tfCosineScore(all member descriptions)
 *             + 0.20 x timeDecay(lastComplaintAt)
 *     Hard semantic floor: if cosine < 0.35 -> score = 0.0 (proximity/time cannot compensate).
 *  5. Pick best-scoring incident above threshold (0.60).
 *  6. Merge into best, or create new incident.
 *  7. If merged, inherit staff assignment from existing cluster members (with explicit load update).
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class IncidentClusterService {

    private final IncidentRepository incidentRepo;
    private final ComplaintRepository complaintRepo;
    private final StaffProfileRepository staffRepo;
    private final StatusHistoryRepository statusHistoryRepo;
    private final SimilarityService similarityService;
    private final JdbcTemplate jdbcTemplate;

    // ── Configurable via application.properties ──────────────────────
    @Value("${fixloop.cluster.window-hours:72}")
    private int clusterWindowHours;

    @Value("${fixloop.cluster.threshold:0.60}")
    private double clusterThreshold;

    @Value("${fixloop.cluster.max-size:50}")
    private int maxClusterSize;

    @Value("${fixloop.cluster.semantic-floor:0.35}")
    private double semanticFloor;

    @Value("${fixloop.cluster.time-decay-lambda:0.02}")
    private double timeDecayLambda;

    private static final double W_LOC  = 0.45;
    private static final double W_SEM  = 0.35;
    private static final double W_TIME = 0.20;

    /**
     * Main entry point called during complaint creation.
     * Returns the Incident the complaint was merged into (or newly created),
     * or null if this complaint type is exempt from clustering.
     */
    public Incident clusterComplaint(Complaint complaint) {
        // GATE 1: sensitive categories are never clustered
        if (complaint.getCategory().isSensitive()) {
            log.debug("Skipping cluster for sensitive category: {}", complaint.getCategory().getName());
            return null;
        }

        // GATE 2: admin previously excluded this specific complaint
        if (complaint.isClusterExcluded()) {
            log.debug("Skipping cluster for manually excluded complaint #{}", complaint.getId());
            return null;
        }

        String building = complaint.getLocation().getBuilding();

        // ADVISORY LOCK: serialise read-decide-write per (category, building)
        // Releases automatically at transaction commit/rollback.
        // Unrelated (category, building) pairs process in parallel.
        String lockKey = complaint.getCategory().getId() + ":" + building;
        try {
            jdbcTemplate.query(
                    "SELECT pg_advisory_xact_lock(hashtext(?))",
                    (ResultSetExtractor<Void>) rs -> null,
                    lockKey
            );
        } catch (Exception e) {
            log.warn("Could not acquire advisory lock for {}: {}", lockKey, e.getMessage());
        }

        LocalDateTime since = LocalDateTime.now().minusHours(clusterWindowHours);
        List<Incident> candidates = incidentRepo.findCandidates(
                complaint.getCategory().getId(), building, since, maxClusterSize);

        log.debug("Complaint #{}: {} cluster candidates found for cat={} building={}",
                complaint.getId(), candidates.size(), complaint.getCategory().getId(), building);

        Incident best = null;
        double bestScore = clusterThreshold; // must beat threshold to qualify

        for (Incident candidate : candidates) {
            double score = computeScore(complaint, candidate);
            log.debug("  candidate incident #{}: score={}", candidate.getId(), score);
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }

        if (best != null) {
            log.info("Complaint #{} clustered into incident #{} (score={})",
                    complaint.getId(), best.getId(), bestScore);
            best.setComplaintCount(best.getComplaintCount() + 1);
            best.setLastComplaintAt(LocalDateTime.now());
            complaint.setClusterScore(BigDecimal.valueOf(Math.round(bestScore * 1000.0) / 1000.0));
            Incident saved = incidentRepo.save(best);

            // Inherit staff assignment from cluster (if complaint not already assigned)
            inheritStaffIfAbsent(complaint, saved);

            return saved;
        }

        log.info("No qualifying incident for complaint #{} — creating new incident", complaint.getId());
        return createNewIncident(complaint);
    }

    // ─────────────────────────────────────────────────────────────────
    // Scoring
    // ─────────────────────────────────────────────────────────────────

    private double computeScore(Complaint c, Incident candidate) {
        // Semantic score: TF cosine against all member descriptions
        List<String> memberDescriptions = complaintRepo.findDescriptionsByIncidentId(candidate.getId());
        double semanticScore = similarityService.similarity(c.getDescription(), memberDescriptions);

        // Hard semantic floor — no amount of proximity or recency can compensate for zero word overlap
        if (semanticScore < semanticFloor) {
            return 0.0;
        }

        // Location score: score against ALL member locations, take the best match
        List<Location> memberLocations = complaintRepo.findLocationsByIncidentId(candidate.getId());
        double locationScore = memberLocations.stream()
                .mapToDouble(m -> scoreOnePair(c.getLocation(), m))
                .max()
                .orElse(0.0);

        // Time decay: e^(-lambda x hours since last complaint in incident)
        double hoursSince = Duration.between(candidate.getLastComplaintAt(), LocalDateTime.now()).toHours();
        double timeDecay = Math.exp(-timeDecayLambda * hoursSince);

        return W_LOC * locationScore + W_SEM * semanticScore + W_TIME * timeDecay;
    }

    /**
     * Room-mention aware location scoring:
     * - Both locations have a specific room -> must be same room (strict match).
     * - One or neither has a room -> relax to floor/building granularity.
     */
    private double scoreOnePair(Location a, Location b) {
        boolean aHasRoom = hasRoom(a);
        boolean bHasRoom = hasRoom(b);

        if (aHasRoom && bHasRoom) {
            // Both specified a room: strict equality required
            return a.getRoom().equalsIgnoreCase(b.getRoom()) ? 1.0 : 0.0;
        }

        // At least one did not specify a room: relax to building/floor
        if (a.getBuilding() != null && a.getBuilding().equalsIgnoreCase(b.getBuilding())) {
            boolean sameFloor = a.getFloor() != null && b.getFloor() != null
                    && a.getFloor().equalsIgnoreCase(b.getFloor());
            return sameFloor ? 0.80 : 0.60;
        }

        return 0.0; // different buildings: blocked by pre-filter, but guard here too
    }

    private boolean hasRoom(Location loc) {
        if (loc == null || loc.getRoom() == null) return false;
        String room = loc.getRoom().trim();
        return !room.isEmpty()
                && !room.equalsIgnoreCase("N/A")
                && !room.equalsIgnoreCase("unknown")
                && !room.equalsIgnoreCase("none")
                && !room.equals("-");
    }

    // ─────────────────────────────────────────────────────────────────
    // Staff inheritance
    // ─────────────────────────────────────────────────────────────────

    /**
     * If this complaint has no staff assigned yet, inherit the staff member
     * from any existing sibling in the incident that is already assigned.
     * Updates current_weighted_load and writes a status_history row.
     */
    void inheritStaffIfAbsent(Complaint complaint, Incident incident) {
        if (complaint.getAssignedToStaff() != null) return; // already assigned

        complaintRepo.findByIncidentId(incident.getId()).stream()
                .filter(sibling -> sibling.getAssignedToStaff() != null
                        && !sibling.getId().equals(complaint.getId()))
                .findFirst()
                .ifPresent(sibling -> {
                    StaffProfile staff = sibling.getAssignedToStaff();
                    complaint.setAssignedToStaff(staff);
                    complaint.setStatus("ASSIGNED");

                    // Explicit load update — critical for correct least-load queries
                    if (complaint.getPriorityScore() != null) {
                        staffRepo.adjustLoad(staff.getUserId(), complaint.getPriorityScore());
                    }

                    // Audit trail in status_history
                    StatusHistory sh = StatusHistory.builder()
                            .complaint(complaint)
                            .status("ASSIGNED")
                            .changedById(0)
                            .changedByName("SYSTEM")
                            .changedByRole("SYSTEM")
                            .note("Auto-inherited assignment from incident #" + incident.getId()
                                    + " (staff: " + (staff.getUser() != null ? staff.getUser().getName() : staff.getUserId()) + ")")
                            .build();
                    statusHistoryRepo.save(sh);

                    log.info("Complaint #{} inherited staff assignment from incident #{} -> staff #{}",
                            complaint.getId(), incident.getId(), staff.getUserId());
                });
    }

    // ─────────────────────────────────────────────────────────────────
    // Incident creation
    // ─────────────────────────────────────────────────────────────────

    private Incident createNewIncident(Complaint complaint) {
        String clusterKey = complaint.getCategory().getId() + ":"
                + complaint.getLocation().getBuilding();
        Incident incident = Incident.builder()
                .category(complaint.getCategory())
                .location(complaint.getLocation()) // display-only: first complaint's location
                .clusterKey(clusterKey)
                .status("OPEN")
                .complaintCount(1)
                .summary(complaint.getDescription()) // first complaint's description = incident summary
                .lastComplaintAt(LocalDateTime.now())
                .build();
        complaint.setClusterScore(BigDecimal.ONE); // 1.0 = founder of this incident
        return incidentRepo.save(incident);
    }
}
