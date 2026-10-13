package com.workforcehub.repository;

import com.workforcehub.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, UUID> {
    Optional<Department> findByDepartmentCode(String departmentCode);
    Optional<Department> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
    boolean existsByDepartmentCode(String departmentCode);
    boolean existsByDepartmentCodeAndIdNot(String departmentCode, UUID id);
    boolean existsByManagerId(UUID managerId);
    java.util.List<Department> findAllByOrderByNameAsc();
}
