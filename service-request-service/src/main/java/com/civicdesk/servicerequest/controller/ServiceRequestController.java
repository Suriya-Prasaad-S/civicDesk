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
    public ResponseEntity<ApiResponse<ServiceRequestResponse>> submitRequest(
            @Valid @RequestBody ServiceRequestCreateRequest request) {
        ServiceRequestResponse response = requestService.submitRequest(request, JwtUserContext.getCurrentUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<ServiceRequestResponse>builder()
                        .success(true).message("Service request submitted successfully").data(response).build());
    }

    @GetMapping("/getAllRequests")
    @PreAuthorize("hasAnyAuthority('ROLE_ADM','ROLE_DS','ROLE_CO','ROLE_FO')")
    @Operation(summary = "Get all service requests (Staff)")
    public ResponseEntity<ApiResponse<List<ServiceRequestResponse>>> getAll(
            @RequestParam(required = false) RequestStatus status) {
        List<ServiceRequestResponse> data = (status != null)
                ? requestService.getByStatus(status)
                : requestService.getAll();
        return ResponseEntity.ok(ApiResponse.<List<ServiceRequestResponse>>builder()
                .success(true).message("Requests fetched").data(data).build());
    }

    @GetMapping("/getRequest/{requestId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get service request by ID")
    public ResponseEntity<ApiResponse<ServiceRequestResponse>> getById(@PathVariable Long requestId) {
        ServiceRequestResponse response = requestService.getById(
                requestId, JwtUserContext.getCurrentUserId(), JwtUserContext.getCurrentRole());
        return ResponseEntity.ok(ApiResponse.<ServiceRequestResponse>builder()
                .success(true).message("Request fetched").data(response).build());
    }

    @GetMapping("/getRequestsByCitizen/{citizenId}")
    @PreAuthorize("hasAnyAuthority('ROLE_CIT','ROLE_ADM','ROLE_DS')")
    @Operation(summary = "Get service requests by citizen ID")
    public ResponseEntity<ApiResponse<List<ServiceRequestResponse>>> getByCitizen(
            @PathVariable Long citizenId) {
        return ResponseEntity.ok(ApiResponse.<List<ServiceRequestResponse>>builder()
                .success(true).data(requestService.getMyRequests(citizenId)).build());
    }

    @PutMapping("/updateRequestStatus/{requestId}")
    @PreAuthorize("hasAnyAuthority('ROLE_ADM','ROLE_DS','ROLE_FO')")
    @Operation(summary = "Update request status")
    public ResponseEntity<ApiResponse<ServiceRequestResponse>> updateStatus(
            @PathVariable Long requestId,
            @Valid @RequestBody UpdateRequestStatusRequest request) {
        ServiceRequestResponse updated = requestService.updateStatus(
                requestId, request.getStatus(), request.getRemarks(),
                JwtUserContext.getCurrentUserId(), JwtUserContext.getCurrentRole());
        return ResponseEntity.ok(ApiResponse.<ServiceRequestResponse>builder()
                .success(true).message("Request status updated to " + request.getStatus()).data(updated).build());
    }
}
