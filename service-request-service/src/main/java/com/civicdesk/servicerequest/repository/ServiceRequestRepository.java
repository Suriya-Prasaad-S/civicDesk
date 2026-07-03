package com.civicdesk.servicerequest.repository;

import com.civicdesk.servicerequest.entity.ServiceRequest;
import com.civicdesk.servicerequest.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {
    List<ServiceRequest> findByUserId(Long userId);
    List<ServiceRequest> findByCitizenId(Long citizenId);
    List<ServiceRequest> findByAssignedOfficerId(Long officerId);
    List<ServiceRequest> findByStatus(RequestStatus status);

    @Query("SELECT sr FROM ServiceRequest sr WHERE sr.service.departmentId = :deptId")
    List<ServiceRequest> findByDepartmentId(@Param("deptId") Long departmentId);

    @Query("SELECT sr FROM ServiceRequest sr WHERE sr.service.departmentId = :deptId AND sr.status = :status")
    List<ServiceRequest> findByDepartmentIdAndStatus(@Param("deptId") Long departmentId,
                                                      @Param("status") RequestStatus status);

    List<ServiceRequest> findByAssignedOfficerIdAndStatus(Long officerId, RequestStatus status);
}
