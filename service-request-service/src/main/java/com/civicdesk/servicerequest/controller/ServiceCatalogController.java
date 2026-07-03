package com.civicdesk.servicerequest.controller;

import com.civicdesk.servicerequest.dto.ApiResponse;
import com.civicdesk.servicerequest.dto.ServiceCatalogRequest;
import com.civicdesk.servicerequest.dto.ServiceCatalogResponse;
import com.civicdesk.servicerequest.dto.UpdateServiceStatusRequest;
import com.civicdesk.servicerequest.enums.ServiceCategory;
import com.civicdesk.servicerequest.service.ServiceCatalogService;
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
@Tag(name = "Service Catalog", description = "Government service catalog — public browse, admin management")
public class ServiceCatalogController {

    private final ServiceCatalogService catalogService;

    @GetMapping("/getAllServices")
    @Operation(summary = "Get all active services (PUBLIC)")
    public ResponseEntity<ApiResponse<List<ServiceCatalogResponse>>> getAllActive() {
        return ResponseEntity.ok(ApiResponse.<List<ServiceCatalogResponse>>builder()
                .success(true).message("Active services fetched").data(catalogService.getAllActive()).build());
    }

    @GetMapping("/getService/{serviceId}")
    @Operation(summary = "Get service by ID (PUBLIC)")
    public ResponseEntity<ApiResponse<ServiceCatalogResponse>> getById(@PathVariable Long serviceId) {
        return ResponseEntity.ok(ApiResponse.<ServiceCatalogResponse>builder()
                .success(true).message("Service fetched").data(catalogService.getById(serviceId)).build());
    }

    @PostMapping("/createService")
    @PreAuthorize("hasAuthority('ROLE_ADM')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Create a new service (Admin)")
    public ResponseEntity<ApiResponse<ServiceCatalogResponse>> createService(
            @Valid @RequestBody ServiceCatalogRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<ServiceCatalogResponse>builder()
                        .success(true).message("Service created successfully").data(catalogService.create(request)).build());
    }

    @PutMapping("/updateService/{serviceId}")
    @PreAuthorize("hasAuthority('ROLE_ADM')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Update a service (Admin)")
    public ResponseEntity<ApiResponse<ServiceCatalogResponse>> updateService(
            @PathVariable Long serviceId,
            @Valid @RequestBody ServiceCatalogRequest request) {
        return ResponseEntity.ok(ApiResponse.<ServiceCatalogResponse>builder()
                .success(true).message("Service updated").data(catalogService.update(serviceId, request)).build());
    }
}
