package com.civicdesk.publicworks.controller;
import com.civicdesk.publicworks.dto.PublicWorkOrderResponse;
import com.civicdesk.publicworks.dto.*;
import com.civicdesk.publicworks.enums.WorkCategory;
import com.civicdesk.publicworks.enums.WorkOrderStatus;
import com.civicdesk.publicworks.security.JwtUserContext;
import com.civicdesk.publicworks.service.WorkOrderService;
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
@RequestMapping("/civicDesk/workorders")
@RequiredArgsConstructor
@Tag(name = "Public Works", description = "Create, assign, track, and complete public works orders")
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    // ─── PUBLIC (CITIZEN) ─────────────────────────────────────────────────────

    @GetMapping("/public/getByWard")
    @Operation(summary = "Get work orders by ward (Citizen - public view)")
    public ResponseEntity<ApiResponse<List<PublicWorkOrderResponse>>> getByWard(
            @RequestParam String ward) {

        return ResponseEntity.ok(
                ApiResponse.<List<PublicWorkOrderResponse>>builder()
                        .success(true)
                        .message("Public works orders fetched successfully")
                        .data(workOrderService.getPublicByWard(ward))
                        .build());
    }

    @GetMapping("/public/{workOrderId}")
    @Operation(summary = "Get public work order details (Citizen)")
    public ResponseEntity<ApiResponse<PublicWorkOrderResponse>> getPublicById(
            @PathVariable Long workOrderId) {

        return ResponseEntity.ok(
                ApiResponse.<PublicWorkOrderResponse>builder()
                        .success(true)
                        .message("Work order details fetched successfully")
                        .data(workOrderService.getPublicById(workOrderId))
                        .build());
    }

    // ─── ENGINEER (PWE) ───────────────────────────────────────────────────────

    @PostMapping("/createWorkOrder")
    @PreAuthorize("hasAnyRole('ENG','DS','ADM')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Create a work order")
    public ResponseEntity<ApiResponse<WorkOrderResponse>> create(
            @Valid @RequestBody CreateWorkOrderRequest request) {
        Long userId = JwtUserContext.getCurrentUserId();
        WorkOrderResponse response = workOrderService.create(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<WorkOrderResponse>builder()
                .success(true).message("Work order created successfully").data(response).build());
    }

    @GetMapping("/getAllWorkOrders")
    @PreAuthorize("hasAnyRole('ENG','DS','CO','ADM')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Get all work orders")
    public ResponseEntity<ApiResponse<List<WorkOrderSummaryResponse>>> getAll(
            @RequestParam(required = false) WorkOrderStatus status,
            @RequestParam(required = false) WorkCategory category,
            @RequestParam(required = false) String ward) {

        return ResponseEntity.ok(
                ApiResponse.<List<WorkOrderSummaryResponse>>builder()
                        .success(true)
                        .message("Work orders fetched successfully")
                        .data(
                                workOrderService.getAllSummary(
                                        status,
                                        category,
                                        ward))
                        .build());
    }

    @GetMapping("/{workOrderId}")
    @PreAuthorize("hasAnyRole('ENG','DS','CO','ADM')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Get work order by ID")
    public ResponseEntity<ApiResponse<WorkOrderDetailResponse>> getById(
            @PathVariable Long workOrderId) {

        return ResponseEntity.ok(
                ApiResponse.<WorkOrderDetailResponse>builder()
                        .success(true)
                        .message("Work order details fetched successfully")
                        .data(
                                workOrderService.getWorkOrderDetail(
                                        workOrderId))
                        .build());
    }

    @PutMapping("/{workOrderId}/update")
    @PreAuthorize("hasAnyRole('ENG','DS','ADM')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Update work order details (Planned or OnHold only)")
    public ResponseEntity<ApiResponse<WorkOrderResponse>> update(
            @PathVariable Long workOrderId,
            @Valid @RequestBody UpdateWorkOrderRequest request) {
        Long userId = JwtUserContext.getCurrentUserId();
        WorkOrderResponse response = workOrderService.update(workOrderId, request, userId);
        return ResponseEntity.ok(ApiResponse.<WorkOrderResponse>builder()
                .success(true).message("Work order updated successfully").data(response).build());
    }

    @PutMapping("/{workOrderId}/updateStatus")
    @PreAuthorize("hasAnyRole('ENG','DS','ADM')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Update work order status")
    public ResponseEntity<ApiResponse<WorkOrderResponse>> updateStatus(
            @PathVariable Long workOrderId,
            @Valid @RequestBody UpdateWorkOrderStatusRequest request) {
        Long userId = JwtUserContext.getCurrentUserId();
        String role = JwtUserContext.getCurrentRole();
        WorkOrderResponse response = workOrderService.updateStatus(workOrderId, request, userId, role);
        return ResponseEntity.ok(ApiResponse.<WorkOrderResponse>builder()
                .success(true).message("Work order status updated successfully").data(response).build());
    }

    @PutMapping("/{workOrderId}/assignContractor")
    @PreAuthorize("hasAnyRole('ENG','DS','ADM')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Assign contractor to work order")
    public ResponseEntity<ApiResponse<WorkOrderResponse>> assignContractor(
            @PathVariable Long workOrderId,
            @Valid @RequestBody AssignContractorRequest request) {
        Long userId = JwtUserContext.getCurrentUserId();
        WorkOrderResponse response = workOrderService.assignContractor(workOrderId, request, userId);
        return ResponseEntity.ok(ApiResponse.<WorkOrderResponse>builder()
                .success(true).message("Contractor assigned successfully").data(response).build());
    }

    @PutMapping("/{workOrderId}/complete")
    @PreAuthorize("hasAnyRole('ENG','DS','ADM')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Mark work order as complete")
    public ResponseEntity<ApiResponse<WorkOrderResponse>> complete(
            @PathVariable Long workOrderId,
            @Valid @RequestBody CompleteWorkOrderRequest request) {
        Long userId = JwtUserContext.getCurrentUserId();
        String role = JwtUserContext.getCurrentRole();
        WorkOrderResponse response = workOrderService.completeWorkOrder(
                workOrderId,
                request);
        return ResponseEntity.ok(ApiResponse.<WorkOrderResponse>builder()
                .success(true).message("Work order marked as completed successfully").data(response).build());
    }

    @PutMapping("/{workOrderId}/cancel")
    @PreAuthorize("hasAnyRole('ENG','DS','ADM')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Cancel (soft-delete) a work order")
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable Long workOrderId) {
        workOrderService.cancel(workOrderId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true).message("Work order cancelled successfully").build());
    }

    // ─── SUPERVISOR / COMPLIANCE ──────────────────────────────────────────────

    @GetMapping("/budgetSummary")
    @PreAuthorize("hasAnyRole('CO','ADM','DS')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Get budget summary for all active work orders")
    public ResponseEntity<ApiResponse<List<BudgetSummaryResponse>>> getBudgetSummary() {
        return ResponseEntity.ok(ApiResponse.<List<BudgetSummaryResponse>>builder()
                .success(true).message("Budget summary fetched successfully")
                .data(workOrderService.getBudgetSummary()).build());
    }

    @PostMapping("/analytics")
@PreAuthorize("hasAnyRole('CO','ADM','DS')")
@SecurityRequirement(name = "BearerAuth")
@Operation(summary = "Work Order Analytics")
public ResponseEntity<ApiResponse<WorkOrderAnalyticsResponse>> getAnalytics(
        @RequestBody WorkOrderAnalyticsRequest request) {

    return ResponseEntity.ok(
            ApiResponse.<WorkOrderAnalyticsResponse>builder()
                    .success(true)
                    .message("Analytics fetched successfully")
                    .data(workOrderService.getAnalytics(
                            request.getFromDate(),
                            request.getToDate()))
                    .build()
    );
}


    @PutMapping("/{workOrderId}/budget")
    @PreAuthorize("hasAnyRole('FO','ENG','DS','ADM')")
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Update work order budget consumption")
    public ResponseEntity<ApiResponse<WorkOrderResponse>>
    updateBudget(
            @PathVariable Long workOrderId,
            @Valid
            @RequestBody
            UpdateBudgetRequest request){

        Long userId =
                JwtUserContext.getCurrentUserId();

        String role =
                JwtUserContext.getCurrentRole();

        WorkOrderResponse response =
                workOrderService.updateBudgetSpent(
                        workOrderId,
                        request,
                        userId,
                        role);

        return ResponseEntity.ok(
                ApiResponse.<WorkOrderResponse>builder()
                        .success(true)
                        .message("Budget updated successfully")
                        .data(response)
                        .build());
    }



}
