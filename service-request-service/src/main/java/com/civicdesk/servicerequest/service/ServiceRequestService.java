package com.civicdesk.servicerequest.service;

import com.civicdesk.servicerequest.dto.request.ServiceRequestAnalyticsRequest;
import com.civicdesk.servicerequest.dto.response.ServiceRequestAnalyticsResponse;
import com.civicdesk.servicerequest.dto.request.ServiceRequestCreateRequest;
import com.civicdesk.servicerequest.dto.response.ServiceRequestResponse;
import com.civicdesk.servicerequest.dto.response.RequestListItemResponse;
import com.civicdesk.servicerequest.dto.response.CitizenRequestItemResponse;
import com.civicdesk.servicerequest.dto.response.RequestDetailResponse;
import com.civicdesk.servicerequest.dto.response.DocumentItemResponse;
import com.civicdesk.servicerequest.entity.ServiceCatalog;
import com.civicdesk.servicerequest.entity.ServiceRequest;
import com.civicdesk.servicerequest.enums.RequestStatus;
import com.civicdesk.servicerequest.enums.ServiceStatus;
import com.civicdesk.servicerequest.exception.BadRequestException;
import com.civicdesk.servicerequest.exception.ForbiddenException;
import com.civicdesk.servicerequest.exception.InactiveServiceException;
import com.civicdesk.servicerequest.exception.ResourceNotFoundException;
import com.civicdesk.servicerequest.client.NotificationClient;
import com.civicdesk.servicerequest.dto.request.NotificationRequest;
import com.civicdesk.servicerequest.repository.ServiceCatalogRepository;
import com.civicdesk.servicerequest.repository.ServiceRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceRequestService {

    private final ServiceRequestRepository requestRepository;
    private final ServiceCatalogService catalogService;
    private final ServiceCatalogRepository catalogRepository;
    private final NotificationClient notificationClient;

    // ─── CITIZEN ─────────────────────────────────────────────────────────────

    @Transactional
    public ServiceRequestResponse submitRequest(ServiceRequestCreateRequest request, Long userId) {
        ServiceCatalog service = catalogService.getEntityById(request.getServiceId());

        if (service.getStatus() != ServiceStatus.ACTIVE) {
            throw new InactiveServiceException("Request cannot be submitted. The selected service is currently inactive and not accepting new requests.");
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
        
        try {
            NotificationRequest notificationPayload = NotificationRequest.builder()
                    .userId(userId)
                    .title("Request Submitted")
                    .message("Service request submitted successfully. Your request has been received and assigned to an officer. Expected completion date is " + service.getProcessingDays() + " working days from today.")
                    .notificationType("SERVICE_REQUEST_UPDATE")
                    .referenceId(saved.getRequestId())
                    .referenceType("SERVICE_REQUEST")
                    .build();
            notificationClient.sendNotification(notificationPayload);
        } catch (Exception ex) {
            log.error("Failed to send submission notification: {}", ex.getMessage());
        }

        ServiceRequestResponse response = mapToResponse(saved);
        response.setMessage("Service request submitted successfully. Your request has been received and assigned to an officer. Expected completion date is " + service.getProcessingDays() + " working days from today.");
        return response;
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

        public RequestDetailResponse getByIdAsDetail(Long requestId, Long userId, String role) {
            ServiceRequest sr = getEntityById(requestId);
            if ("CIT".equals(role) && !sr.getUserId().equals(userId)) {
                throw new ForbiddenException("Access denied. You can only view your own requests.");
            }
            return mapToDetailResponse(sr);
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

    // ─── LIST ITEM RESPONSES ──────────────────────────────────────────────────

    public List<RequestListItemResponse> getAllAsListItems() {
        return requestRepository.findAll().stream().map(this::mapToListItemResponse).toList();
    }

    public List<RequestListItemResponse> getByStatusAsListItems(RequestStatus status) {
        return requestRepository.findByStatus(status).stream().map(this::mapToListItemResponse).toList();
    }

    public List<CitizenRequestItemResponse> getByCitizenIdAsCitizenItems(Long citizenId) {
        return requestRepository.findByUserId(citizenId).stream().map(this::mapToCitizenItemResponse).toList();
    }

    @Transactional(readOnly = true)
    public ServiceRequestAnalyticsResponse getServiceRequestAnalytics(ServiceRequestAnalyticsRequest request) {
        Long deptId = request.deptId();
        LocalDate fromDate = request.fromDate();
        LocalDate toDate = request.toDate();

        long totalRequests = requestRepository.countRequests(deptId, fromDate, toDate);
        long overdueRequests = requestRepository.countOverdueRequests(deptId, fromDate, toDate, LocalDate.now());

        Map<RequestStatus, Long> statusMap = Arrays.stream(RequestStatus.values())
                .collect(Collectors.toMap(
                        Function.identity(),
                        status -> 0L,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new));
        requestRepository.getStatusBreakdown(deptId, fromDate, toDate)
                .forEach(row -> statusMap.put((RequestStatus) row[0], ((Number) row[1]).longValue()));
        List<ServiceRequestAnalyticsResponse.LabelCount> statusBreakdown = statusMap.entrySet().stream()
                .map(entry -> new ServiceRequestAnalyticsResponse.LabelCount(entry.getKey().name(), entry.getValue()))
                .toList();

        Map<String, Long> serviceMap = catalogRepository.findByStatus(ServiceStatus.ACTIVE).stream()
                .collect(Collectors.toMap(
                        ServiceCatalog::getServiceName,
                        service -> 0L,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new));
        requestRepository.getServiceBreakdown(deptId, fromDate, toDate)
                .forEach(row -> serviceMap.put((String) row[0], ((Number) row[1]).longValue()));
        List<ServiceRequestAnalyticsResponse.LabelCount> serviceBreakdown = serviceMap.entrySet().stream()
                .map(entry -> new ServiceRequestAnalyticsResponse.LabelCount(entry.getKey(), entry.getValue()))
                .toList();

        List<ServiceRequestAnalyticsResponse.DateCount> trend = requestRepository.getTrend(deptId, fromDate, toDate)
                .stream()
                .map(row -> new ServiceRequestAnalyticsResponse.DateCount((LocalDate) row[0], ((Number) row[1]).longValue()))
                .toList();

        return new ServiceRequestAnalyticsResponse(
                totalRequests,
                statusBreakdown,
                serviceBreakdown,
                trend,
                overdueRequests);
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
        
        try {
            NotificationRequest notificationPayload = NotificationRequest.builder()
                    .userId(updated.getUserId())
                    .title("Request Status Updated")
                    .message("Request status updated successfully. Status has been moved to " + formatStatus(newStatus) + ".")
                    .notificationType("SERVICE_REQUEST_UPDATE")
                    .referenceId(updated.getRequestId())
                    .referenceType("SERVICE_REQUEST")
                    .build();
            notificationClient.sendNotification(notificationPayload);
        } catch (Exception ex) {
            log.error("Failed to send status update notification: {}", ex.getMessage());
        }

        ServiceRequestResponse response = mapToResponse(updated);
        response.setMessage("Request status updated successfully. Status has been moved to " + formatStatus(newStatus) + ".");
        return response;
    }

    private String formatStatus(RequestStatus status) {
        if (status == null) return "";
        String name = status.name().replace("_", " ").toLowerCase();
        String[] words = name.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

    private void validateStatusTransition(RequestStatus current, RequestStatus next, String role) {
        if (!current.allowedNextStates().contains(next)) {
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
                .orElseThrow(() -> new ResourceNotFoundException("Request not found. No request exists with the given requestId."));
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

    private RequestListItemResponse mapToListItemResponse(ServiceRequest sr) {
        return RequestListItemResponse.builder()
                .requestId(sr.getRequestId())
                .citizenId(sr.getCitizenId())
                .serviceId(sr.getService().getServiceId())
                .serviceName(sr.getService().getServiceName())
                .submissionDate(sr.getSubmissionDate())
                .status(sr.getStatus())
                .assignedOfficerId(sr.getAssignedOfficerId())
                .build();
    }

    private CitizenRequestItemResponse mapToCitizenItemResponse(ServiceRequest sr) {
        return CitizenRequestItemResponse.builder()
                .requestId(sr.getRequestId())
                .serviceId(sr.getService().getServiceId())
                .serviceName(sr.getService().getServiceName())
                .submissionDate(sr.getSubmissionDate())
                .status(sr.getStatus())
                .assignedOfficerId(sr.getAssignedOfficerId())
                .build();
    }
    
        private RequestDetailResponse mapToDetailResponse(ServiceRequest sr) {
            return RequestDetailResponse.builder()
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
