package com.fixloop.repository;

import com.fixloop.domain.StaffProfile;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public interface StaffProfileRepository extends JpaRepository<StaffProfile, Integer> {
    
    // Auto-assignment query 1: skill-match + dept + active, sorted by load (post-normalization)
    @Query("SELECT sp FROM StaffProfile sp JOIN sp.skills s WHERE sp.department.id = :deptId AND s.id = :skillId AND sp.status = 'ACTIVE' ORDER BY sp.currentWeightedLoad ASC")
    List<StaffProfile> findActiveByDeptAndSkill(@Param("deptId") Integer deptId, @Param("skillId") Integer skillId, Pageable pageable);
    
    // Fallback: any active staff in dept, sorted by load
    @Query("SELECT sp FROM StaffProfile sp WHERE sp.department.id = :deptId AND sp.status = 'ACTIVE' ORDER BY sp.currentWeightedLoad ASC")
    List<StaffProfile> findActiveByDept(@Param("deptId") Integer deptId, Pageable pageable);
    
    // For analytics: find all with department fetched
    @Query("SELECT sp FROM StaffProfile sp JOIN FETCH sp.user u JOIN FETCH sp.department d")
    List<StaffProfile> findAllWithDetails();
    
    // Workload update: used after assign/resolve
    @Modifying
    @Query("UPDATE StaffProfile sp SET sp.currentWeightedLoad = sp.currentWeightedLoad + :delta WHERE sp.userId = :staffId")
    int adjustLoad(@Param("staffId") Integer staffId, @Param("delta") java.math.BigDecimal delta);
}
