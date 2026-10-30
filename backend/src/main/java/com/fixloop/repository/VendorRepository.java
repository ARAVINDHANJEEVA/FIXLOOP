package com.fixloop.repository;

import com.fixloop.domain.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VendorRepository extends JpaRepository<Vendor, Integer> {
    // Find vendors who have a specific skill (for vendor picker filtered by category skills)
    @Query("SELECT v FROM Vendor v JOIN v.skills s WHERE s.id IN :skillIds")
    List<Vendor> findBySkillIds(@Param("skillIds") List<Integer> skillIds);
}
