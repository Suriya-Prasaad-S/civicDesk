package com.civicdesk.servicerequest.controller;

import com.civicdesk.servicerequest.dto.request.ServiceCatalogRequest;
import com.civicdesk.servicerequest.dto.response.ServiceCatalogResponse;
import com.civicdesk.servicerequest.dto.response.ServiceListItemResponse;
import com.civicdesk.servicerequest.dto.response.ServiceDetailResponse;
import com.civicdesk.servicerequest.dto.request.UpdateServiceStatusRequest;
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
import com.civicdesk.servicerequest.dto.response.MessageResponse;

@RestController
@RequestMapping("/civicDesk/serviceRequest")
@RequiredArgsConstructor
@Tag(name = "Service Catalog", description = "Government service catalog — public browse, admin management")
public class ServiceCatalogController {

    private final ServiceCatalogService catalogService;

    @GetMapping("/getAllServices")
    @Operation(summary = "Get all active services (PUBLIC)")
    public ResponseEntity<List<ServiceListItemResponse>> getAllActive() {
        return ResponseEntity.ok(catalogService.getAllActiveAsListItems());
    }

    @GetMapping("/getService/{serviceId}")
    @Operation(summary = "Get service by ID (PUBLIC)")
    public ResponseEntity<ServiceDetailResponse> getById(@PathVariable Long serviceId) {
        return ResponseEntity.ok(catalogService.getByIdAsDetail(serviceId));
    }

        @PostMapping("/createService")
        @PreAuthorize("hasAuthority('ROLE_ADM')")
        @SecurityRequirement(name = "BearerAuth")
        @Operation(summary = "Create a new service (Admin)")
        public ResponseEntity<MessageResponse> createService(
            @Valid @RequestBody ServiceCatalogRequest request) {
        ServiceCatalogResponse res = catalogService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(MessageResponse.builder()
                .message(res.getMessage())
                .id(res.getServiceId())
                .data(res)
                .build());
        }

    @PutMapping("/updateService/{serviceId}")
    @PreAuthorize("hasAuthority('ROLE_ADM')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Update a service (Admin)")
    public ResponseEntity<MessageResponse> updateService(
            @PathVariable Long serviceId,
            @Valid @RequestBody ServiceCatalogRequest request) {
        ServiceCatalogResponse res = catalogService.update(serviceId, request);
        return ResponseEntity.ok(MessageResponse.builder()
                .message(res.getMessage())
                .id(res.getServiceId())
                .data(res)
                .build());
    }
}
