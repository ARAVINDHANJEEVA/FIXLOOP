package com.fixloop.repository;

import com.fixloop.domain.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skill, Integer> {
    Optional<Skill> findByName(String name);
    List<Skill> findByNameIn(List<String> names);
}
