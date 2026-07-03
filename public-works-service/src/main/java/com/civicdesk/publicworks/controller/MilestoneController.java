package com.civicdesk.publicworks.controller;

import com.civicdesk.publicworks.dto.ApiResponse;
import com.civicdesk.publicworks.dto.MilestoneRequest;
import com.civicdesk.publicworks.dto.MilestoneResponse;
import com.civicdesk.publicworks.dto.MilestoneUpdateRequest;
import com.civicdesk.publicworks.security.JwtUserContext;
import com.civicdesk.publicworks.service.WorkOrderService;
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
@RequestMapping("/civicDesk/workorders")
@RequiredArgsConstructor
@Tag(name = "Milestone Management", description = "Add and update milestones linked to work orders")
@SecurityRequirement(name = "BearerAuth")
public class MilestoneController {

    private final WorkOrderService workOrderService;

    @PostMapping("/{workOrderId}/milestones/create")
    @PreAuthorize("hasAnyRole('ENG','DS','ADM')")
    @Operation(summary = "Create milestone for a work order")
    public ResponseEntity<ApiResponse<MilestoneResponse>> add(
            @PathVariable Long workOrderId,
            @Valid @RequestBody MilestoneRequest request) {
        MilestoneResponse response = workOrderService.addMilestone(workOrderId, request);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(ApiResponse.<MilestoneResponse>builder()
                .success(true).message("Milestone created successfully").data(response).build());
    }

    @GetMapping("/{workOrderId}/milestones")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get milestones for a work order")
    public ResponseEntity<ApiResponse<List<MilestoneResponse>>> getByWorkOrder(@PathVariable Long workOrderId) {
        return ResponseEntity.ok(ApiResponse.<List<MilestoneResponse>>builder()
                .success(true).data(workOrderService.getMilestonesByWorkOrder(workOrderId)).build());
    }

    @GetMapping("/{workOrderId}/milestones/{milestoneId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get milestone by ID")
    public ResponseEntity<ApiResponse<MilestoneResponse>> getMilestoneById(
            @PathVariable Long workOrderId,
            @PathVariable Long milestoneId) {
        return ResponseEntity.ok(ApiResponse.<MilestoneResponse>builder()
                .success(true).data(workOrderService.getMilestoneById(milestoneId)).build());
    }

    @PutMapping("/{workOrderId}/milestones/{milestoneId}/update")
    @PreAuthorize("hasAnyRole('FO','ENG','DS','ADM')")
    @Operation(summary = "Update milestone details")
    public ResponseEntity<ApiResponse<MilestoneResponse>> update(
            @PathVariable Long workOrderId,
            @PathVariable Long milestoneId,
            @Valid @RequestBody MilestoneUpdateRequest request) {
        Long userId = JwtUserContext.getCurrentUserId();
        String role = JwtUserContext.getCurrentRole();
        MilestoneResponse response = workOrderService.updateMilestone(milestoneId, request, userId, role);
        return ResponseEntity.ok(ApiResponse.<MilestoneResponse>builder()
                .success(true).message("Milestone updated.").data(response).build());
    }

    @PutMapping("/{workOrderId}/milestones/{milestoneId}/complete")
    @PreAuthorize("hasAnyRole('FO','ENG','DS','ADM')")
    @Operation(summary = "Mark milestone as complete")
    public ResponseEntity<ApiResponse<MilestoneResponse>> complete(
            @PathVariable Long workOrderId,
            @PathVariable Long milestoneId,
            @Valid @RequestBody MilestoneUpdateRequest request) {
        Long userId = JwtUserContext.getCurrentUserId();
        String role = JwtUserContext.getCurrentRole();
        MilestoneResponse response = workOrderService.updateMilestone(milestoneId, request, userId, role);
        return ResponseEntity.ok(ApiResponse.<MilestoneResponse>builder()
                .success(true).message("Milestone completed.").data(response).build());
    }

    @PutMapping("/{workOrderId}/milestones/{milestoneId}/status")
    @PreAuthorize("hasAnyRole('FO','ENG','DS','ADM')")
    @Operation(summary = "Update milestone status")
    public ResponseEntity<ApiResponse<MilestoneResponse>> updateStatus(
            @PathVariable Long workOrderId,
            @PathVariable Long milestoneId,
            @Valid @RequestBody MilestoneUpdateRequest request) {
        Long userId = JwtUserContext.getCurrentUserId();
        String role = JwtUserContext.getCurrentRole();
        MilestoneResponse response = workOrderService.updateMilestone(milestoneId, request, userId, role);
        return ResponseEntity.ok(ApiResponse.<MilestoneResponse>builder()
                .success(true).message("Milestone status updated.").data(response).build());
    }

    @DeleteMapping("/{workOrderId}/milestones/{milestoneId}/delete")
    @PreAuthorize("hasAnyRole('ENG','DS','ADM')")
    @Operation(summary = "Soft-delete a milestone")
    public ResponseEntity<ApiResponse<Void>> deleteMilestone(
            @PathVariable Long workOrderId,
            @PathVariable Long milestoneId) {
        workOrderService.deleteMilestone(milestoneId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message("Milestone deleted successfully").build());
    }

    @GetMapping("/delayed-milestones")
    @PreAuthorize("hasAnyRole('DS','CO','ADM')")
    @Operation(summary = "Get delayed milestones")
    public ResponseEntity<ApiResponse<List<MilestoneResponse>>> getDelayed() {
        return ResponseEntity.ok(ApiResponse.<List<MilestoneResponse>>builder()
                .success(true).data(workOrderService.getDelayedMilestones()).build());
    }
}
