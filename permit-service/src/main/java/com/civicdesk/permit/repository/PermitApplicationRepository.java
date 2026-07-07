package com.civicdesk.permit.repository;

import com.civicdesk.permit.entity.PermitApplication;
import com.civicdesk.permit.enums.PermitStatus;
import com.civicdesk.permit.enums.PermitType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PermitApplicationRepository extends JpaRepository<PermitApplication, Long> {
    List<PermitApplication> findByUserId(Long userId);
    List<PermitApplication> findByCitizenId(Long citizenId);
    List<PermitApplication> findByStatus(PermitStatus status);
    List<PermitApplication> findByPermitType(PermitType permitType);
    List<PermitApplication> findByDepartmentId(Long departmentId);
    List<PermitApplication> findByStatusAndDepartmentId(PermitStatus status, Long departmentId);
    // Used by compliance to find expiring permits
    List<PermitApplication> findByStatusAndExpiryDateBefore(PermitStatus status, LocalDate date);
}
