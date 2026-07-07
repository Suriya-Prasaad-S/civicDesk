package com.civicdesk.auth.repository;

import com.civicdesk.auth.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, String> {

    boolean existsByName(String name);

    Optional<Department> findByName(String name);
}
