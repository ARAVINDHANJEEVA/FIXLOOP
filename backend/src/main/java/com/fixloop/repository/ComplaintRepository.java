package com.fixloop.repository;

import com.fixloop.domain.Complaint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ComplaintRepository extends JpaRepository<Complaint, Integer>, JpaSpecificationExecutor<Complaint> {
    
    Optional<Complaint> findByToken(String token);
    
    // Staff "My Queue" - scoped server-side (security critical)
    @Query("SELECT c FROM Complaint c WHERE c.assignedToStaff.userId = :staffId ORDER BY c.priorityScore DESC")
    Page<Complaint> findByAssignedStaff(@Param("staffId") Integer staffId, Pageable pageable);
    
    // Same with status filter
    @Query("SELECT c FROM Complaint c WHERE c.assignedToStaff.userId = :staffId AND c.status = :status ORDER BY c.priorityScore DESC")
    Page<Complaint> findByAssignedStaffAndStatus(@Param("staffId") Integer staffId, @Param("status") String status, Pageable pageable);

    // SLA scanner: fetch at-risk candidates (partial index query)
    @Query("SELECT c FROM Complaint c WHERE c.status NOT IN ('RESOLVED', 'CLOSED') AND c.isAtRisk = false AND c.slaDeadline <= :atRiskThreshold")
    List<Complaint> findAtRiskCandidates(@Param("atRiskThreshold") java.time.LocalDateTime atRiskThreshold);
    
    // SLA scanner: fetch breached
    @Query("SELECT c FROM Complaint c WHERE c.status NOT IN ('RESOLVED', 'CLOSED') AND c.isBreached = false AND c.slaDeadline <= :now")
    List<Complaint> findBreachedCandidates(@Param("now") java.time.LocalDateTime now);
    
    // Already flagged AT_RISK (for SLA monitor page)
    @Query("SELECT c FROM Complaint c WHERE c.isAtRisk = true AND c.status NOT IN ('RESOLVED', 'CLOSED') ORDER BY c.slaDeadline ASC")
    List<Complaint> findAtRisk();
    
    // Already flagged BREACHED (for SLA monitor page)
    @Query("SELECT c FROM Complaint c WHERE c.isBreached = true AND c.status NOT IN ('RESOLVED', 'CLOSED') ORDER BY c.slaDeadline ASC")
    List<Complaint> findBreached();
    
    // Sibling complaints in same incident
    @Query("SELECT c.token FROM Complaint c WHERE c.incident.id = :incidentId AND c.id <> :excludeId")
    List<String> findSiblingTokens(@Param("incidentId") Integer incidentId, @Param("excludeId") Integer excludeId);
    
    // Analytics: hotspot query
    @Query("SELECT l.building, cat.name, COUNT(c) FROM Complaint c JOIN c.location l JOIN c.category cat WHERE c.createdAt >= :since GROUP BY l.building, cat.name ORDER BY COUNT(c) DESC")
    List<Object[]> findHotspots(@Param("since") java.time.LocalDateTime since);
    
    // Analytics: category volume
    @Query("SELECT cat.name, COUNT(c) FROM Complaint c JOIN c.category cat WHERE c.createdAt >= :since GROUP BY cat.name")
    List<Object[]> findCategoryVolume(@Param("since") java.time.LocalDateTime since);
    
    // Analytics: SLA trend (weekly buckets) - native query for date_trunc
    @Query(value = "SELECT TO_CHAR(DATE_TRUNC('week', created_at), 'YYYY-MM-DD') as period, COUNT(CASE WHEN is_breached THEN 1 END) as breached, COUNT(CASE WHEN status IN ('RESOLVED','CLOSED') THEN 1 END) as resolved, COUNT(CASE WHEN is_at_risk THEN 1 END) as at_risk FROM complaint WHERE created_at >= :since GROUP BY DATE_TRUNC('week', created_at) ORDER BY DATE_TRUNC('week', created_at)", nativeQuery = true)
    List<Object[]> findSlaTrends(@Param("since") java.time.LocalDateTime since);
    
    // Recurrence count: count complaints in same category+location in last 90 days
    @Query("SELECT COUNT(c) FROM Complaint c WHERE c.category.id = :categoryId AND c.location.id = :locationId AND c.createdAt >= :since90days AND c.id <> :excludeId")
    long countRecurrence(@Param("categoryId") Integer categoryId, @Param("locationId") Integer locationId, @Param("since90days") java.time.LocalDateTime since, @Param("excludeId") Integer excludeId);
    
    // Staff performance analytics
    @Query(value = "SELECT assigned_to_staff_id, " +
            "AVG(EXTRACT(EPOCH FROM (resolved_at - created_at))) / 3600.0 as avgHours, " +
            "COUNT(CASE WHEN is_breached = false THEN 1 END) * 100.0 / NULLIF(COUNT(*), 0) as compliance, " +
            "COUNT(CASE WHEN status NOT IN ('RESOLVED','CLOSED') THEN 1 END) as openCount " +
            "FROM complaint " +
            "WHERE assigned_to_staff_id IS NOT NULL AND created_at >= :since " +
            "GROUP BY assigned_to_staff_id", nativeQuery = true)
    List<Object[]> findStaffPerformance(@Param("since") java.time.LocalDateTime since);

    // Count for resolved this month per staff  
    @Query("SELECT c.assignedToStaff.userId, COUNT(c) FROM Complaint c WHERE c.assignedToStaff IS NOT NULL AND c.status IN ('RESOLVED','CLOSED') AND c.resolvedAt >= :since GROUP BY c.assignedToStaff.userId")
    List<Object[]> countResolvedSince(@Param("since") java.time.LocalDateTime since);

    // Student "My Complaints" — server-scoped to the authenticated student's userId
    @Query("SELECT c FROM Complaint c WHERE c.studentUser.userId = :userId ORDER BY c.createdAt DESC")
    Page<Complaint> findByStudentUserId(@Param("userId") Integer userId, Pageable pageable);

    @Query("SELECT c FROM Complaint c WHERE c.studentUser.userId = :userId AND c.status = :status ORDER BY c.createdAt DESC")
    Page<Complaint> findByStudentUserIdAndStatus(@Param("userId") Integer userId, @Param("status") String status, Pageable pageable);

    // ── Cluster scoring queries ────────────────────────────────────────

    /** TF cosine corpus: descriptions of all member complaints in an incident. */
    @Query("SELECT c.description FROM Complaint c WHERE c.incident.id = :incidentId")
    List<String> findDescriptionsByIncidentId(@Param("incidentId") Integer incidentId);

    /** Location scoring: all member locations in an incident (score against all, take best). */
    @Query("SELECT c.location FROM Complaint c WHERE c.incident.id = :incidentId")
    List<com.fixloop.domain.Location> findLocationsByIncidentId(@Param("incidentId") Integer incidentId);

    /** All complaints in a cluster (for ClusterMemberDto building + staff inheritance). */
    List<Complaint> findByIncidentId(Integer incidentId);

    /** Sibling complaint IDs (integer, for comma display in UI). */
    @Query("SELECT c.id FROM Complaint c WHERE c.incident.id = :incidentId AND c.id <> :excludeId")
    List<Integer> findSiblingIds(
            @Param("incidentId") Integer incidentId,
            @Param("excludeId") Integer excludeId
    );
}

