package com.civicdesk.servicerequest.service;

import com.civicdesk.servicerequest.dto.request.ServiceCatalogRequest;
import com.civicdesk.servicerequest.dto.response.ServiceCatalogResponse;
import com.civicdesk.servicerequest.dto.response.ServiceDetailResponse;
import com.civicdesk.servicerequest.dto.response.ServiceListItemResponse;
import com.civicdesk.servicerequest.entity.ServiceCatalog;
import com.civicdesk.servicerequest.enums.ServiceCategory;
import com.civicdesk.servicerequest.enums.ServiceStatus;
import com.civicdesk.servicerequest.exception.ResourceNotFoundException;
import com.civicdesk.servicerequest.repository.ServiceCatalogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceCatalogService {

    private final ServiceCatalogRepository catalogRepository;

    @Transactional
    public ServiceCatalogResponse create(ServiceCatalogRequest request) {
        ServiceCatalog catalog = ServiceCatalog.builder()
                .serviceName(request.getServiceName())
                .departmentId(request.getDepartmentId())
                .category(request.getCategory())
                .processingDays(request.getProcessingDays())
                .requiredDocuments(request.getRequiredDocuments() == null ? null : String.join(",", request.getRequiredDocuments()))
                .fee(request.getFee())
                .status(ServiceStatus.ACTIVE)
                .build();
        ServiceCatalog saved = catalogRepository.save(catalog);
        log.info("Service created: serviceId={} name={}", saved.getServiceId(), saved.getServiceName());
        ServiceCatalogResponse response = mapToResponse(saved);
        response.setMessage("Service created successfully. New service has been added to the catalog.");
        return response;
    }

    @Transactional
    public ServiceCatalogResponse update(Long serviceId, ServiceCatalogRequest request) {
        ServiceCatalog catalog = getEntityById(serviceId);
        catalog.setServiceName(request.getServiceName());
        catalog.setDepartmentId(request.getDepartmentId());
        catalog.setCategory(request.getCategory());
        catalog.setProcessingDays(request.getProcessingDays());
        catalog.setRequiredDocuments(request.getRequiredDocuments() == null ? null : String.join(",", request.getRequiredDocuments()));
        catalog.setFee(request.getFee());
        log.info("Service updated: serviceId={}", serviceId);
        ServiceCatalogResponse response = mapToResponse(catalogRepository.save(catalog));
        response.setMessage("Service updated successfully.");
        return response;
    }

    @Transactional
    public ServiceCatalogResponse updateStatus(Long serviceId, ServiceStatus status) {
        ServiceCatalog catalog = getEntityById(serviceId);
        catalog.setStatus(status);
        log.info("Service status updated: serviceId={} status={}", serviceId, status);
        return mapToResponse(catalogRepository.save(catalog));
    }

    public ServiceCatalogResponse getById(Long serviceId) {
        return mapToResponse(getEntityById(serviceId));
    }

    public List<ServiceCatalogResponse> getAll() {
        return catalogRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    public List<ServiceCatalogResponse> getAllActive() {
        return catalogRepository.findByStatus(ServiceStatus.ACTIVE).stream().map(this::mapToResponse).toList();
    }

        public ServiceDetailResponse getByIdAsDetail(Long serviceId) {
            return mapToDetailResponse(getEntityById(serviceId));
        }

        public List<ServiceListItemResponse> getAllAsListItems() {
            return catalogRepository.findAll().stream().map(this::mapToListItemResponse).toList();
        }

        public List<ServiceListItemResponse> getAllActiveAsListItems() {
            return catalogRepository.findByStatus(ServiceStatus.ACTIVE).stream().map(this::mapToListItemResponse).toList();
        }
    public List<ServiceCatalogResponse> getByCategory(ServiceCategory category) {
        return catalogRepository.findByCategoryAndStatus(category, ServiceStatus.ACTIVE)
                .stream().map(this::mapToResponse).toList();
    }

    public List<ServiceCatalogResponse> getByDepartment(String departmentId) {
        return catalogRepository.findByDepartmentId(departmentId)
                .stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public void delete(Long serviceId) {
        ServiceCatalog catalog = getEntityById(serviceId);
        catalogRepository.delete(catalog);
        log.info("Service deleted: serviceId={}", serviceId);
    }

    public ServiceCatalog getEntityById(Long serviceId) {
        return catalogRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Service not found. No service exists with the given serviceId."));
    }

    private ServiceCatalogResponse mapToResponse(ServiceCatalog s) {
        return ServiceCatalogResponse.builder()
                .serviceId(s.getServiceId())
                .serviceName(s.getServiceName())
                .departmentId(s.getDepartmentId())
                .category(s.getCategory())
                .processingDays(s.getProcessingDays())
                .requiredDocuments(s.getRequiredDocuments() == null || s.getRequiredDocuments().isBlank()
                        ? Collections.emptyList()
                        : Arrays.stream(s.getRequiredDocuments().split(","))
                        .map(String::trim)
                        .collect(Collectors.toList()))
                .fee(s.getFee())
                .status(s.getStatus())
                .createdAt(s.getCreatedAt())
                .build();
    }

    private ServiceDetailResponse mapToDetailResponse(ServiceCatalog s) {
        return ServiceDetailResponse.builder()
                .serviceId(s.getServiceId())
                .serviceName(s.getServiceName())
                .category(s.getCategory())
                .departmentId(s.getDepartmentId())
                .estimatedFee(s.getFee())
                .estimatedDays(s.getProcessingDays())
                .status(s.getStatus())
                .requiredDocuments(s.getRequiredDocuments())
                .build();
    }

    private ServiceListItemResponse mapToListItemResponse(ServiceCatalog s) {
        return ServiceListItemResponse.builder()
                .serviceId(s.getServiceId())
                .serviceName(s.getServiceName())
                .category(s.getCategory())
                .status(s.getStatus())
                .departmentId(s.getDepartmentId())
                .build();
    }

}
