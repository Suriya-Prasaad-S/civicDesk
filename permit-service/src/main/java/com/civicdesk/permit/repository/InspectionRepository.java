package com.civicdesk.permit.repository;

import com.civicdesk.permit.entity.Inspection;
import com.civicdesk.permit.enums.InspectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InspectionRepository extends JpaRepository<Inspection, Long> {
    List<Inspection> findByPermitApplication_PermitId(Long permitId);
    List<Inspection> findByAssignedOfficerId(Long officerId);
    List<Inspection> findByAssignedOfficerIdAndStatus(Long officerId, InspectionStatus status);
    List<Inspection> findByStatus(InspectionStatus status);
}
