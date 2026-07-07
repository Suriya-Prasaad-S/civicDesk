package com.civicdesk.servicerequest.controller;

import com.civicdesk.servicerequest.dto.*;
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
import java.util.Map;

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
    public ResponseEntity<Map<String, String>> submitRequest(
            @Valid @RequestBody ServiceRequestCreateRequest request) {
        ServiceRequestResponse res = requestService.submitRequest(request, JwtUserContext.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", res.getMessage()));
    }

    @GetMapping("/getAllRequests")
    @PreAuthorize("hasAnyAuthority('ROLE_ADM','ROLE_DS','ROLE_CO','ROLE_FO')")
    @Operation(summary = "Get all service requests (Staff)")
    public ResponseEntity<List<ServiceRequestResponse>> getAll(
            @RequestParam(required = false) RequestStatus status) {
        List<ServiceRequestResponse> data = (status != null)
                ? requestService.getByStatus(status)
                : requestService.getAll();
        return ResponseEntity.ok(data);
    }

    @GetMapping("/getRequest/{requestId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get service request by ID")
    public ResponseEntity<ServiceRequestResponse> getById(@PathVariable Long requestId) {
        ServiceRequestResponse response = requestService.getById(
                requestId, JwtUserContext.getCurrentUserId(), JwtUserContext.getCurrentRole());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/getRequestsByCitizen/{citizenId}")
    @PreAuthorize("hasAnyAuthority('ROLE_CIT','ROLE_ADM','ROLE_DS')")
    @Operation(summary = "Get service requests by citizen ID")
    public ResponseEntity<List<ServiceRequestResponse>> getByCitizen(
            @PathVariable Long citizenId) {
        return ResponseEntity.ok(requestService.getMyRequests(citizenId));
    }

    @PutMapping("/updateRequestStatus/{requestId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADM','ROLE_DS','ROLE_FO')")
    @Operation(summary = "Update request status")
    public ResponseEntity<Map<String, String>> updateStatus(
            @PathVariable Long requestId,
            @Valid @RequestBody UpdateRequestStatusRequest request) {
        ServiceRequestResponse res = requestService.updateStatus(
                requestId, request.getStatus(), request.getRemarks(),
                JwtUserContext.getCurrentUserId(), JwtUserContext.getCurrentRole());
        return ResponseEntity.ok(Map.of("message", res.getMessage()));
    }
 
    @PostMapping("/getServiceRequestAnalytics")
    @PreAuthorize("hasAnyAuthority('ROLE_ADM','ROLE_DS','ROLE_CO','ROLE_FO')")
    @Operation(summary = "Get service request analytics for dashboards")
    public ResponseEntity<ServiceRequestAnalyticsResponse> getServiceRequestAnalytics(
            @Valid @RequestBody ServiceRequestAnalyticsRequest request) {
        return ResponseEntity.ok(requestService.getServiceRequestAnalytics(request));
    }
}
