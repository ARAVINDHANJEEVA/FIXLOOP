package com.fixloop.repository;

import com.fixloop.domain.Incident;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface IncidentRepository extends JpaRepository<Incident, Integer> {

    /**
     * Smart clustering candidates: OPEN incidents in the same category and building,
     * with a last_complaint_at within the cluster window and below max size.
     * Results ordered newest-first so we pick the most active incident when scores tie.
     */
    @Query("""
            SELECT i FROM Incident i
            WHERE i.category.id = :catId
              AND i.location.building = :building
              AND i.status = 'OPEN'
              AND i.lastComplaintAt >= :since
              AND i.complaintCount < :maxSize
            ORDER BY i.lastComplaintAt DESC
            """)
    List<Incident> findCandidates(
            @Param("catId") Integer catId,
            @Param("building") String building,
            @Param("since") LocalDateTime since,
            @Param("maxSize") int maxSize
    );

    /**
     * Legacy: find open incident matching cluster key within time window.
     * Kept for backward compatibility but no longer used by the main scoring path.
     */
    @Query("SELECT i FROM Incident i WHERE i.clusterKey = :clusterKey AND i.status = 'OPEN' AND i.lastComplaintAt >= :since ORDER BY i.lastComplaintAt DESC")
    List<Incident> findOpenByClusterKey(
            @Param("clusterKey") String clusterKey,
            @Param("since") LocalDateTime since,
            Pageable pageable
    );

    /** For admin incident queue and manual-cluster target picker. */
    List<Incident> findByCategoryIdAndStatus(Integer categoryId, String status);

    /** All open incidents — for admin manual-cluster dropdown across all categories. */
    List<Incident> findByStatus(String status);
}
