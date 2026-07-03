package com.civicdesk.grievance.controller;

import com.civicdesk.grievance.dto.*;
import com.civicdesk.grievance.enums.EscalationLevel;
import com.civicdesk.grievance.enums.GrievanceStatus;
import com.civicdesk.grievance.security.JwtUserContext;
import com.civicdesk.grievance.service.GrievanceService;
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
@RequestMapping("/civicDesk/grievance")
@RequiredArgsConstructor
@Tag(name = "Grievance Management", description = "Submit, track, assign, escalate, and resolve citizen grievances")
@SecurityRequirement(name = "BearerAuth")
public class GrievanceController {

    private final GrievanceService grievanceService;

    // ─── CITIZEN ─────────────────────────────────────────────────────────────

    @PostMapping("/createGrievance")
    @PreAuthorize("hasRole('CIT')")
    @Operation(summary = "Submit a grievance", description = "ROLE_CITIZEN only.")
    public ResponseEntity<ApiResponse<Void>> submit(
            @Valid @RequestBody GrievanceRequest request) {
        Long userId = JwtUserContext.getCurrentUserId();
        grievanceService.submit(request, userId);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(ApiResponse.<Void>builder()
                .success(true).message("Created successfully").build());
    }

    @GetMapping("/getMyGrievances")
    @PreAuthorize("hasRole('CIT')")
    @Operation(summary = "Get my grievances", description = "ROLE_CITIZEN only.")
    public ResponseEntity<ApiResponse<List<GrievanceResponse>>> getMyGrievances() {
        Long userId = JwtUserContext.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.<List<GrievanceResponse>>builder()
                .success(true).data(grievanceService.getMyGrievances(userId)).build());
    }

    // ─── SHARED READ ─────────────────────────────────────────────────────────

    @GetMapping("/getGrievanceById/{grievanceId}")
    @PreAuthorize("hasAnyRole('CIT','FO','DS','ENG','CO','ADM')")
    @Operation(summary = "Get grievance by ID")
    public ResponseEntity<ApiResponse<GrievanceResponse>> getById(@PathVariable Long grievanceId) {
        Long userId = JwtUserContext.getCurrentUserId();
        String role = JwtUserContext.getCurrentRole();
        GrievanceResponse response = grievanceService.getById(grievanceId, userId, role);
        return ResponseEntity.ok(ApiResponse.<GrievanceResponse>builder()
                .success(true).data(response).build());
    }

    @GetMapping("/{grievanceId}/timeline")
    @PreAuthorize("hasAnyRole('CIT','FO','DS','ENG','CO','ADM')")
    @Operation(summary = "Get grievance action timeline",
            description = "Full audit trail of all actions taken on this grievance, ordered chronologically.")
    public ResponseEntity<ApiResponse<List<GrievanceActionResponse>>> getTimeline(
            @PathVariable Long grievanceId) {
        return ResponseEntity.ok(ApiResponse.<List<GrievanceActionResponse>>builder()
                .success(true).data(grievanceService.getTimeline(grievanceId)).build());
    }

    @PostMapping("/{grievanceId}/comment")
    @PreAuthorize("hasAnyRole('CIT','FO','DS','ENG','ADM')")
    @Operation(summary = "Add a comment",
            description = "Any authenticated user. Citizens can only comment on their own grievances.")
    public ResponseEntity<ApiResponse<GrievanceActionResponse>> addComment(
            @PathVariable Long grievanceId,
            @RequestParam String remarks) {
        Long userId = JwtUserContext.getCurrentUserId();
        String role = JwtUserContext.getCurrentRole();
        GrievanceActionResponse response = grievanceService.addComment(grievanceId, remarks, userId, role);
        return ResponseEntity.ok(ApiResponse.<GrievanceActionResponse>builder()
                .success(true).message("Comment added.").data(response).build());
    }

    // ─── FIELD OFFICER ────────────────────────────────────────────────────────

    @GetMapping("/getAssignedGrievances")
    @PreAuthorize("hasRole('FO')")
    @Operation(summary = "Get grievances assigned to me (Field Officer)")
    public ResponseEntity<ApiResponse<List<GrievanceResponse>>> getAssignedToMe() {
        Long officerId = JwtUserContext.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.<List<GrievanceResponse>>builder()
                .success(true).data(grievanceService.getAssignedToMe(officerId)).build());
    }

    @GetMapping("/viewAssignedGrievanceById/{grievanceId}")
    @PreAuthorize("hasRole('FO')")
    @Operation(summary = "View assigned grievance by ID (Field Officer)")
    public ResponseEntity<ApiResponse<GrievanceResponse>> viewAssignedById(@PathVariable Long grievanceId) {
        Long userId = JwtUserContext.getCurrentUserId();
        String role = JwtUserContext.getCurrentRole();
        return ResponseEntity.ok(ApiResponse.<GrievanceResponse>builder()
                .success(true).data(grievanceService.getById(grievanceId, userId, role)).build());
    }

    @PostMapping("/createGrievanceActionById/{grievanceId}")
    @PreAuthorize("hasAnyRole('FO','DS')")
    @Operation(summary = "Create grievance action (Field Officer)")
    public ResponseEntity<ApiResponse<Void>> createAction(
            @PathVariable Long grievanceId,
            @RequestBody GrievanceActionRequest request) {
        Long userId = JwtUserContext.getCurrentUserId();
        String role = JwtUserContext.getCurrentRole();
        grievanceService.addComment(grievanceId, request.getDescription(), userId, role);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(ApiResponse.<Void>builder()
                .success(true).message("Created successfully").build());
    }

    @PutMapping("/updateGrievanceActionById/{actionId}")
    @PreAuthorize("hasRole('FO')")
    @Operation(summary = "Update grievance action (Field Officer)")
    public ResponseEntity<ApiResponse<Void>> updateAction(
            @PathVariable Long actionId,
            @RequestBody GrievanceActionRequest request) {
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message("Action updated.").build());
    }

    // ─── DEPT SUPERVISOR ─────────────────────────────────────────────────────

    @GetMapping("/getDepartmentGrievances")
    @PreAuthorize("hasAnyRole('DS','CO','ADM','ENG')")
    @Operation(summary = "Get department grievances")
    public ResponseEntity<ApiResponse<List<GrievanceResponse>>> getDepartmentGrievances(
            @RequestParam(required = false) GrievanceStatus status,
            @RequestParam(required = false) EscalationLevel escalationLevel) {
        List<GrievanceResponse> data;
        if (status != null) data = grievanceService.getByStatus(status);
        else if (escalationLevel != null) data = grievanceService.getByEscalationLevel(escalationLevel);
        else data = grievanceService.getAll();
        return ResponseEntity.ok(ApiResponse.<List<GrievanceResponse>>builder()
                .success(true).data(data).build());
    }

    @GetMapping("/viewDepartmentGrievanceById/{grievanceId}")
    @PreAuthorize("hasAnyRole('DS','CO','ADM')")
    @Operation(summary = "View department grievance by ID")
    public ResponseEntity<ApiResponse<GrievanceResponse>> viewDeptGrievanceById(@PathVariable Long grievanceId) {
        Long userId = JwtUserContext.getCurrentUserId();
        String role = JwtUserContext.getCurrentRole();
        return ResponseEntity.ok(ApiResponse.<GrievanceResponse>builder()
                .success(true).data(grievanceService.getById(grievanceId, userId, role)).build());
    }

    @PostMapping("/assignFieldOfficerById/{grievanceId}")
    @PreAuthorize("hasAnyRole('DS','ADM')")
    @Operation(summary = "Assign field officer to grievance")
    public ResponseEntity<ApiResponse<GrievanceResponse>> assign(
            @PathVariable Long grievanceId,
            @Valid @RequestBody AssignGrievanceRequest request) {
        Long userId = JwtUserContext.getCurrentUserId();
        GrievanceResponse response = grievanceService.assign(grievanceId, request, userId);
        return ResponseEntity.ok(ApiResponse.<GrievanceResponse>builder()
                .success(true).message("Grievance assigned.").data(response).build());
    }

    @PostMapping("/resolveGrievanceById/{grievanceId}")
    @PreAuthorize("hasAnyRole('DS','ADM')")
    @Operation(summary = "Resolve a grievance")
    public ResponseEntity<ApiResponse<GrievanceResponse>> resolve(
            @PathVariable Long grievanceId,
            @Valid @RequestBody UpdateStatusRequest request) {
        Long userId = JwtUserContext.getCurrentUserId();
        String role = JwtUserContext.getCurrentRole();
        GrievanceResponse response = grievanceService.updateStatus(grievanceId, request, userId, role);
        return ResponseEntity.ok(ApiResponse.<GrievanceResponse>builder()
                .success(true).message("Grievance resolved.").data(response).build());
    }

    @PostMapping("/closeGrievanceById/{grievanceId}")
    @PreAuthorize("hasRole('CIT')")
    @Operation(summary = "Close a grievance")
    public ResponseEntity<ApiResponse<GrievanceResponse>> close(
            @PathVariable Long grievanceId,
            @Valid @RequestBody UpdateStatusRequest request) {
        Long userId = JwtUserContext.getCurrentUserId();
        String role = JwtUserContext.getCurrentRole();
        GrievanceResponse response = grievanceService.updateStatus(grievanceId, request, userId, role);
        return ResponseEntity.ok(ApiResponse.<GrievanceResponse>builder()
                .success(true).message("Grievance closed.").data(response).build());
    }

    @PostMapping("/reopenGrievanceById/{grievanceId}")
    @PreAuthorize("hasRole('CIT')")
    @Operation(summary = "Reopen a grievance")
    public ResponseEntity<ApiResponse<GrievanceResponse>> reopen(
            @PathVariable Long grievanceId,
            @Valid @RequestBody UpdateStatusRequest request) {
        Long userId = JwtUserContext.getCurrentUserId();
        String role = JwtUserContext.getCurrentRole();
        GrievanceResponse response = grievanceService.updateStatus(grievanceId, request, userId, role);
        return ResponseEntity.ok(ApiResponse.<GrievanceResponse>builder()
                .success(true).message("Grievance reopened.").data(response).build());
    }

    @PutMapping("/updateGrievanceDetailsById/{grievanceId}")
    @PreAuthorize("hasRole('CIT')")
    @Operation(summary = "Update grievance details (Citizen)")
    public ResponseEntity<ApiResponse<GrievanceResponse>> updateDetails(
            @PathVariable Long grievanceId,
            @Valid @RequestBody GrievanceRequest request) {
        Long userId = JwtUserContext.getCurrentUserId();
        GrievanceResponse response = grievanceService.updateDetails(grievanceId, request, userId);
        return ResponseEntity.ok(ApiResponse.<GrievanceResponse>builder()
                .success(true).message("Grievance updated.").data(response).build());
    }
}
