package com.fixloop.repository;

import com.fixloop.domain.PriorityWeightConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PriorityWeightConfigRepository extends JpaRepository<PriorityWeightConfig, Integer> {
    // Always use the most recently effective config
    Optional<PriorityWeightConfig> findTopByOrderByEffectiveFromDesc();
}
