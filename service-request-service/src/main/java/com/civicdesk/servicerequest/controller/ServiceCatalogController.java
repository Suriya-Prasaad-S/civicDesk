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
import java.util.Map;

@RestController
@RequestMapping("/civicDesk/serviceRequest")
@RequiredArgsConstructor
@Tag(name = "Service Catalog", description = "Government service catalog — public browse, admin management")
public class ServiceCatalogController {

    private final ServiceCatalogService catalogService;

    @GetMapping("/getAllServices")
    @Operation(summary = "Get all active services (PUBLIC)")
    public ResponseEntity<List<ServiceCatalogResponse>> getAllActive() {
        return ResponseEntity.ok(catalogService.getAllActive());
    }

    @GetMapping("/getService/{serviceId}")
    @Operation(summary = "Get service by ID (PUBLIC)")
    public ResponseEntity<ServiceCatalogResponse> getById(@PathVariable Long serviceId) {
        return ResponseEntity.ok(catalogService.getById(serviceId));
    }

    @PostMapping("/createService")
    @PreAuthorize("hasAuthority('ROLE_ADM')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Create a new service (Admin)")
    public ResponseEntity<Map<String, String>> createService(
            @Valid @RequestBody ServiceCatalogRequest request) {
        ServiceCatalogResponse res = catalogService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", res.getMessage()));
    }

    @PutMapping("/updateService/{serviceId}")
    @PreAuthorize("hasAuthority('ROLE_ADM')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Update a service (Admin)")
    public ResponseEntity<Map<String, String>> updateService(
            @PathVariable Long serviceId,
            @Valid @RequestBody ServiceCatalogRequest request) {
        ServiceCatalogResponse res = catalogService.update(serviceId, request);
        return ResponseEntity.ok(Map.of("message", res.getMessage()));
    }
}
