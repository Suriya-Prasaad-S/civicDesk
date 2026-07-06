package com.civicdesk.servicerequest.controller;

import com.civicdesk.servicerequest.dto.request.ServiceRequestCreateRequest;
import com.civicdesk.servicerequest.dto.request.UpdateRequestStatusRequest;
import com.civicdesk.servicerequest.dto.request.ServiceRequestAnalyticsRequest;
import com.civicdesk.servicerequest.dto.response.MessageResponse;
import com.civicdesk.servicerequest.dto.response.RequestListItemResponse;
import com.civicdesk.servicerequest.dto.response.RequestDetailResponse;
import com.civicdesk.servicerequest.dto.response.CitizenRequestItemResponse;
import com.civicdesk.servicerequest.dto.response.ServiceRequestAnalyticsResponse;
import com.civicdesk.servicerequest.dto.response.ServiceRequestResponse;
import com.civicdesk.servicerequest.enums.RequestStatus;
import com.civicdesk.servicerequest.security.JwtUserContext;
import com.civicdesk.servicerequest.service.ServiceRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/civicDesk/serviceRequest")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Service Requests", description = "Submit and manage citizen service requests")
public class ServiceRequestController {

    private final ServiceRequestService requestService;

    @PostMapping("/submitRequest")
    @PreAuthorize("hasAuthority('ROLE_CIT')")
    @Operation(summary = "Submit a new service request")
    public ResponseEntity<MessageResponse> submitRequest(
            @Valid @RequestBody ServiceRequestCreateRequest request) {
        ServiceRequestResponse res = requestService.submitRequest(request, JwtUserContext.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MessageResponse.builder()
                        .message(res.getMessage())
                        .id(res.getRequestId())
                        .build());
    }

    @GetMapping("/getAllRequests")
    @PreAuthorize("hasAnyAuthority('ROLE_ADM','ROLE_DS','ROLE_CO','ROLE_FO')")
    @Operation(summary = "Get all service requests (Staff)")
    public ResponseEntity<List<RequestListItemResponse>> getAll(
            @RequestParam(required = false) RequestStatus status) {
        List<RequestListItemResponse> data = (status != null)
                ? requestService.getByStatusAsListItems(status)
                : requestService.getAllAsListItems();
        return ResponseEntity.ok(data);
    }

    @GetMapping("/getRequest/{requestId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get service request by ID")
        public ResponseEntity<RequestDetailResponse> getById(@PathVariable Long requestId) {
            RequestDetailResponse response = requestService.getByIdAsDetail(
                    requestId, JwtUserContext.getCurrentUserId(), JwtUserContext.getCurrentRole());
            return ResponseEntity.ok(response);
    }

    @GetMapping("/getRequestsByCitizen/{citizenId}")
    @PreAuthorize("hasAnyAuthority('ROLE_CIT','ROLE_ADM','ROLE_DS')")
    @Operation(summary = "Get service requests by citizen ID")
    public ResponseEntity<List<CitizenRequestItemResponse>> getByCitizen(
            @PathVariable Long citizenId) {
        return ResponseEntity.ok(requestService.getByCitizenIdAsCitizenItems(citizenId));
    }

    @PutMapping("/updateRequestStatus/{requestId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADM','ROLE_DS','ROLE_FO')")
    @Operation(summary = "Update request status")
    public ResponseEntity<MessageResponse> updateStatus(
            @PathVariable Long requestId,
            @Valid @RequestBody UpdateRequestStatusRequest request) {
        ServiceRequestResponse res = requestService.updateStatus(
                requestId, request.getStatus(), request.getRemarks(),
                JwtUserContext.getCurrentUserId(), JwtUserContext.getCurrentRole());
        return ResponseEntity.ok(MessageResponse.builder()
                .message(res.getMessage())
                .id(res.getRequestId())
                .data(res)
                .build());
    }
 
    @PostMapping("/getServiceRequestAnalytics")
    @PreAuthorize("hasAnyAuthority('ROLE_ADM','ROLE_DS','ROLE_CO','ROLE_FO')")
    @Operation(summary = "Get service request analytics for dashboards")
    public ResponseEntity<ServiceRequestAnalyticsResponse> getServiceRequestAnalytics(
            @Valid @RequestBody ServiceRequestAnalyticsRequest request) {
        return ResponseEntity.ok(requestService.getServiceRequestAnalytics(request));
    }
}
