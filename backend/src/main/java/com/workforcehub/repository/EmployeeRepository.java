package com.workforcehub.repository;

import com.workforcehub.entity.Employee;
import com.workforcehub.entity.enums.EmploymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID>, JpaSpecificationExecutor<Employee> {
    Optional<Employee> findByEmployeeCode(String employeeCode);
    Optional<Employee> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByEmployeeCode(String employeeCode);
    boolean existsByManagerId(UUID managerId);
    List<Employee> findByDepartmentId(UUID departmentId);
    boolean existsByDepartmentId(UUID departmentId);
    long countByDepartmentId(UUID departmentId);
    long countByEmploymentStatus(EmploymentStatus status);
    Optional<Employee> findByUserAccountId(UUID userAccountId);

    @org.springframework.data.jpa.repository.Query("SELECT e.department.id, COUNT(e) FROM Employee e GROUP BY e.department.id")
    List<Object[]> countEmployeesGroupByDepartmentId();
}

