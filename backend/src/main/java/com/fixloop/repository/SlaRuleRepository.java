package com.fixloop.repository;

import com.fixloop.domain.SlaRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SlaRuleRepository extends JpaRepository<SlaRule, Integer> {
    List<SlaRule> findByCategoryId(Integer categoryId);
    Optional<SlaRule> findByCategoryIdAndSeverityTier(Integer categoryId, String severityTier);
}
