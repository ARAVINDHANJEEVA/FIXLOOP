package com.fixloop.repository;

import com.fixloop.domain.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Integer> {
    Optional<Department> findByName(String name);
    boolean existsByCode(String code);
}
