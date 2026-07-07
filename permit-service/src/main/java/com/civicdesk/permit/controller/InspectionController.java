package com.civicdesk.permit.controller;

import com.civicdesk.permit.dto.ApiResponse;
import com.civicdesk.permit.dto.ConductInspectionRequest;
import com.civicdesk.permit.dto.InspectionResponse;
import com.civicdesk.permit.dto.ScheduleInspectionRequest;
import com.civicdesk.permit.enums.InspectionStatus;
import org.springframework.web.multipart.MultipartFile;
import com.civicdesk.permit.security.JwtUserContext;
import com.civicdesk.permit.service.InspectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/civicDesk/permits")
@RequiredArgsConstructor
@Tag(name = "Permit Inspections", description = "Schedule, conduct, and manage permit site inspections")
@SecurityRequirement(name = "BearerAuth")
public class InspectionController {

    private final InspectionService inspectionService;

    // POST /{permitId}/inspections  — schedule inspection
    @PostMapping("/{permitId}/inspections")
    @PreAuthorize("hasAnyRole('DS','ADM')")
    @Operation(summary = "Schedule a site inspection")
    public ResponseEntity<ApiResponse<InspectionResponse>> schedule(
            @PathVariable Long permitId,
            @Valid @RequestBody ScheduleInspectionRequest request) {
        InspectionResponse response = inspectionService.scheduleInspection(permitId, request);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(ApiResponse.<InspectionResponse>builder()
                .success(true).message("Inspection scheduled and officer notified successfully").data(response).build());
    }

    // GET /{permitId}/inspections  — supervisor views inspection results for a permit (Step 22)
    @GetMapping("/{permitId}/inspections")
    @PreAuthorize("hasAnyRole('DS','CO','ADM','CIT')")
    @Operation(summary = "Get inspections for a permit")
    public ResponseEntity<ApiResponse<List<InspectionResponse>>> getByPermit(@PathVariable Long permitId) {
        return ResponseEntity.ok(ApiResponse.<List<InspectionResponse>>builder()
                .success(true).message("Inspection results fetched successfully")
                .data(inspectionService.getByPermitId(permitId)).build());
    }

    // GET /inspections/myAssignments  — field officer views own assignments (Step 19)
    @GetMapping("/inspections/myAssignments")
    @PreAuthorize("hasRole('FO')")
    @Operation(summary = "Get my assigned inspections (Field Officer)")
    public ResponseEntity<ApiResponse<List<InspectionResponse>>> getMyInspections() {
        Long officerId = JwtUserContext.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.<List<InspectionResponse>>builder()
                .success(true).message("Assigned inspections fetched successfully")
                .data(inspectionService.getAssignedToMe(officerId)).build());
    }

    // GET /inspections/{inspectionId}  — view single inspection (Step 20)
    @GetMapping("/inspections/{inspectionId}")
    @PreAuthorize("hasAnyRole('FO','DS','CO','ADM')")
    @Operation(summary = "Get inspection by ID")
    public ResponseEntity<ApiResponse<InspectionResponse>> getById(@PathVariable Long inspectionId) {
        return ResponseEntity.ok(ApiResponse.<InspectionResponse>builder()
                .success(true).message("Inspection details fetched successfully")
                .data(inspectionService.getById(inspectionId)).build());
    }

    // PUT /inspections/{inspectionId}/submit  — form-data: outcome, remarks, gpsCoordinates, photo (Step 21)
    @PutMapping("/inspections/{inspectionId}/submit")
    @PreAuthorize("hasRole('FO')")
    @Operation(summary = "Submit inspection outcome (Field Officer) — multipart/form-data")
    public ResponseEntity<ApiResponse<InspectionResponse>> conduct(
            @PathVariable Long inspectionId,
            @RequestParam("outcome") String outcome,
            @RequestParam("remarks") String remarks,
            @RequestParam(value = "gpsCoordinates", required = false) String gpsCoordinates,
            @RequestParam(value = "photo", required = false) MultipartFile photo) {
        Long officerId = JwtUserContext.getCurrentUserId();
        ConductInspectionRequest request = new ConductInspectionRequest();
        request.setOutcome(com.civicdesk.permit.enums.InspectionOutcome.valueOf(outcome.toUpperCase()));
        request.setRemarks(remarks);
        request.setGeoCoordinates(gpsCoordinates);
        request.setConductedDate(java.time.LocalDate.now());
        InspectionResponse response = inspectionService.conductInspection(inspectionId, request, officerId);
        return ResponseEntity.ok(ApiResponse.<InspectionResponse>builder()
                .success(true).message("Inspection outcome submitted successfully").data(response).build());
    }
}
