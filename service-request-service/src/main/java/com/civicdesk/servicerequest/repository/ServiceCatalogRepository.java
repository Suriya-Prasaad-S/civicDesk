package com.civicdesk.servicerequest.repository;

import com.civicdesk.servicerequest.entity.ServiceCatalog;
import com.civicdesk.servicerequest.enums.ServiceCategory;
import com.civicdesk.servicerequest.enums.ServiceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceCatalogRepository extends JpaRepository<ServiceCatalog, Long> {
    List<ServiceCatalog> findByStatus(ServiceStatus status);
    List<ServiceCatalog> findByCategory(ServiceCategory category);
    List<ServiceCatalog> findByDepartmentId(String departmentId);
    List<ServiceCatalog> findByCategoryAndStatus(ServiceCategory category, ServiceStatus status);
}
