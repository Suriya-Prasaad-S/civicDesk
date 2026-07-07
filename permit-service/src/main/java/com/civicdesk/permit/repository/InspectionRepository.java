package com.civicdesk.permit.repository;

import com.civicdesk.permit.entity.Inspection;
import com.civicdesk.permit.enums.InspectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import org.springframework.data.jpa.repository.Query;

@Repository
public interface InspectionRepository extends JpaRepository<Inspection, Long> {
    List<Inspection> findByPermitApplication_PermitId(Long permitId);
    List<Inspection> findByAssignedOfficerId(Long officerId);
    List<Inspection> findByAssignedOfficerIdAndStatus(Long officerId, InspectionStatus status);
    List<Inspection> findByStatus(InspectionStatus status);

    @Query("""
SELECT i.status as label,
       COUNT(i) as count
FROM Inspection i
GROUP BY i.status
""")
    <T> List<T> getStatusBreakdown(Class<T> type);

    @Query("""
SELECT i.outcome as label,
       COUNT(i) as count
FROM Inspection i
WHERE i.outcome IS NOT NULL
GROUP BY i.outcome
""")
    <T> List<T> getOutcomeBreakdown(Class<T> type);


}
