package com.civicdesk.servicerequest.service;

import com.civicdesk.servicerequest.dto.ServiceRequestCreateRequest;
import com.civicdesk.servicerequest.dto.ServiceRequestResponse;
import com.civicdesk.servicerequest.entity.ServiceCatalog;
import com.civicdesk.servicerequest.entity.ServiceRequest;
import com.civicdesk.servicerequest.enums.RequestStatus;
import com.civicdesk.servicerequest.enums.ServiceStatus;
import com.civicdesk.servicerequest.exception.BadRequestException;
import com.civicdesk.servicerequest.exception.ForbiddenException;
import com.civicdesk.servicerequest.exception.ResourceNotFoundException;
import com.civicdesk.servicerequest.repository.ServiceRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceRequestService {

    private final ServiceRequestRepository requestRepository;
    private final ServiceCatalogService catalogService;

    // ─── CITIZEN ─────────────────────────────────────────────────────────────

    @Transactional
    public ServiceRequestResponse submitRequest(ServiceRequestCreateRequest request, Long userId) {
        ServiceCatalog service = catalogService.getEntityById(request.getServiceId());

        if (service.getStatus() != ServiceStatus.ACTIVE) {
            throw new BadRequestException("Service is not currently available: " + service.getServiceName());
        }

        LocalDate submissionDate = LocalDate.now();
        LocalDate expectedCompletion = submissionDate.plusDays(service.getProcessingDays());

        ServiceRequest serviceRequest = ServiceRequest.builder()
                .citizenId(request.getCitizenId())
                .userId(userId)
                .service(service)
                .submissionDate(submissionDate)
                .fee(service.getFee())
                .expectedCompletionDate(expectedCompletion)
                .status(RequestStatus.SUBMITTED)
                .build();

        ServiceRequest saved = requestRepository.save(serviceRequest);
        log.info("Service request submitted: requestId={} citizenId={} serviceId={}",
                saved.getRequestId(), request.getCitizenId(), request.getServiceId());
        return mapToResponse(saved);
    }

    public List<ServiceRequestResponse> getMyRequests(Long userId) {
        return requestRepository.findByUserId(userId).stream().map(this::mapToResponse).toList();
    }

    public ServiceRequestResponse getById(Long requestId, Long userId, String role) {
        ServiceRequest sr = getEntityById(requestId);
        if ("CIT".equals(role) && !sr.getUserId().equals(userId)) {
            throw new ForbiddenException("Access denied. You can only view your own requests.");
        }
        return mapToResponse(sr);
    }

    // ─── STAFF ───────────────────────────────────────────────────────────────

    public List<ServiceRequestResponse> getAll() {
        return requestRepository.findAll().stream().map(this::mapToResponse).toList();
    }

    public List<ServiceRequestResponse> getByDepartment(Long departmentId) {
        return requestRepository.findByDepartmentId(departmentId).stream().map(this::mapToResponse).toList();
    }

    public List<ServiceRequestResponse> getByDepartmentAndStatus(Long departmentId, RequestStatus status) {
        return requestRepository.findByDepartmentIdAndStatus(departmentId, status)
                .stream().map(this::mapToResponse).toList();
    }

    public List<ServiceRequestResponse> getAssignedToMe(Long officerId) {
        return requestRepository.findByAssignedOfficerId(officerId).stream().map(this::mapToResponse).toList();
    }

    public List<ServiceRequestResponse> getByStatus(RequestStatus status) {
        return requestRepository.findByStatus(status).stream().map(this::mapToResponse).toList();
    }

    @Transactional
    public ServiceRequestResponse assignOfficer(Long requestId, Long officerId) {
        ServiceRequest sr = getEntityById(requestId);
        if (sr.getStatus() == RequestStatus.COMPLETED || sr.getStatus() == RequestStatus.REJECTED) {
            throw new BadRequestException("Cannot assign officer to a closed request.");
        }
        sr.setAssignedOfficerId(officerId);
        if (sr.getStatus() == RequestStatus.SUBMITTED) {
            sr.setStatus(RequestStatus.UNDER_REVIEW);
        }
        ServiceRequest updated = requestRepository.save(sr);
        log.info("Officer assigned: requestId={} officerId={}", requestId, officerId);
        return mapToResponse(updated);
    }

    @Transactional
    public ServiceRequestResponse updateStatus(Long requestId, RequestStatus newStatus, String remarks, Long actorUserId, String actorRole) {
        ServiceRequest sr = getEntityById(requestId);

        // Field officers can only update requests assigned to them
        if ("FO".equals(actorRole) &&
                !actorUserId.equals(sr.getAssignedOfficerId())) {
            throw new ForbiddenException("You can only update requests assigned to you.");
        }

        validateStatusTransition(sr.getStatus(), newStatus, actorRole);

        sr.setStatus(newStatus);
        if (remarks != null && !remarks.isBlank()) {
            sr.setRemarks(remarks);
        }

        ServiceRequest updated = requestRepository.save(sr);
        log.info("Request status updated: requestId={} status={} by userId={}", requestId, newStatus, actorUserId);
        return mapToResponse(updated);
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

    private void validateStatusTransition(RequestStatus current, RequestStatus next, String role) {
        boolean valid = switch (current) {
            case SUBMITTED -> next == RequestStatus.UNDER_REVIEW || next == RequestStatus.REJECTED;
            case UNDER_REVIEW -> next == RequestStatus.PENDING_DOCUMENTS
                    || next == RequestStatus.APPROVED
                    || next == RequestStatus.REJECTED;
            case PENDING_DOCUMENTS -> next == RequestStatus.UNDER_REVIEW || next == RequestStatus.REJECTED;
            case APPROVED -> next == RequestStatus.COMPLETED;
            case COMPLETED, REJECTED -> false;
        };

        if (!valid) {
            throw new BadRequestException(
                    String.format("Invalid status transition: %s → %s", current, next));
        }

        // Only supervisors/admins can approve
        if ((next == RequestStatus.APPROVED || next == RequestStatus.REJECTED)
                && "FO".equals(role)) {
            throw new ForbiddenException("Only supervisors or admins can approve or reject requests.");
        }
    }

    private ServiceRequest getEntityById(Long requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Request not found with id: " + requestId));
    }

    private ServiceRequestResponse mapToResponse(ServiceRequest sr) {
        return ServiceRequestResponse.builder()
                .requestId(sr.getRequestId())
                .citizenId(sr.getCitizenId())
                .userId(sr.getUserId())
                .serviceId(sr.getService().getServiceId())
                .serviceName(sr.getService().getServiceName())
                .serviceCategory(sr.getService().getCategory().name())
                .departmentId(sr.getService().getDepartmentId())
                .submissionDate(sr.getSubmissionDate())
                .assignedOfficerId(sr.getAssignedOfficerId())
                .fee(sr.getFee())
                .expectedCompletionDate(sr.getExpectedCompletionDate())
                .status(sr.getStatus())
                .remarks(sr.getRemarks())
                .createdAt(sr.getCreatedAt())
                .build();
    }
}
