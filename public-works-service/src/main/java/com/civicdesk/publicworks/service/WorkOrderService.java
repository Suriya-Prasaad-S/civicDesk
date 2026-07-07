package com.civicdesk.publicworks.service;

import com.civicdesk.publicworks.dto.*;
import com.civicdesk.publicworks.entity.Milestone;
import com.civicdesk.publicworks.entity.WorkOrder;
import com.civicdesk.publicworks.enums.MilestoneStatus;
import com.civicdesk.publicworks.enums.WorkCategory;
import com.civicdesk.publicworks.enums.WorkOrderStatus;
import com.civicdesk.publicworks.enums.WorkPriority;
import com.civicdesk.publicworks.exception.BadRequestException;
import com.civicdesk.publicworks.exception.ForbiddenException;
import com.civicdesk.publicworks.exception.ResourceNotFoundException;
import com.civicdesk.publicworks.repository.MilestoneRepository;
import com.civicdesk.publicworks.repository.WorkOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final MilestoneRepository milestoneRepository;

    // ─── WORK ORDER CRUD ─────────────────────────────────────────────────────

    @Transactional
    public WorkOrderResponse create(WorkOrderRequest request, Long userId) {
        WorkOrder wo = WorkOrder.builder()
                .projectName(request.getProjectName())
                .category(request.getCategory())
                .departmentId(request.getDepartmentId())
                .ward(request.getWard())
                .zone(request.getZone())
                .location(request.getLocation())
                .priority(request.getPriority() != null ? request.getPriority() : WorkPriority.MEDIUM)
                .status(WorkOrderStatus.PLANNED)
                .budgetAllocated(request.getBudgetAllocated())
                .budgetConsumedTotal(BigDecimal.ZERO)
                .startDate(request.getStartDate())
                .expectedEndDate(request.getExpectedEndDate())
                .remarks(request.getRemarks())
                .assignedBy(userId)
                .build();

        WorkOrder saved = workOrderRepository.save(wo);
        log.info("Work order created: id={} projectName={} budget={}", saved.getWorkOrderId(), saved.getProjectName(), saved.getBudgetAllocated());
        return mapToResponse(saved);
    }

    public WorkOrderResponse getById(Long id) {
        return mapToResponse(getEntityById(id));
    }

    @Transactional
    public WorkOrderResponse update(Long workOrderId, WorkOrderRequest request, Long userId) {
        WorkOrder wo = getEntityById(workOrderId);
        wo.setProjectName(request.getProjectName());
        wo.setCategory(request.getCategory());
        wo.setDepartmentId(request.getDepartmentId());
        wo.setWard(request.getWard());
        wo.setZone(request.getZone());
        wo.setLocation(request.getLocation());
        if (request.getPriority() != null) wo.setPriority(request.getPriority());
        wo.setBudgetAllocated(request.getBudgetAllocated());
        wo.setStartDate(request.getStartDate());
        wo.setExpectedEndDate(request.getExpectedEndDate());
        wo.setRemarks(request.getRemarks());
        WorkOrder saved = workOrderRepository.save(wo);
        log.info("Work order updated: id={}", workOrderId);
        return mapToResponse(saved);
    }

    public List<WorkOrderResponse> getAll(WorkOrderStatus status, WorkCategory category, String ward) {
        List<WorkOrder> results;
        if (status != null) {
            results = workOrderRepository.findByStatus(status);
        } else if (category != null) {
            results = workOrderRepository.findByCategory(category);
        } else if (ward != null && !ward.isBlank()) {
            results = workOrderRepository.findByWard(ward);
        } else {
            results = workOrderRepository.findAll();
        }
        return results.stream().map(this::mapToResponse).toList();
    }

    public List<WorkOrderResponse> getByDepartment(Long departmentId, WorkOrderStatus status) {
        List<WorkOrder> results = status != null
                ? workOrderRepository.findByStatusAndDepartmentId(status, departmentId)
                : workOrderRepository.findByDepartmentId(departmentId);
        return results.stream().map(this::mapToResponse).toList();
    }

    public List<WorkOrderResponse> getAssignedToMe(Long contractorId) {
        return workOrderRepository.findByContractorId(contractorId)
                .stream().map(this::mapToResponse).toList();
    }

    // ─── CONTRACTOR ASSIGNMENT ────────────────────────────────────────────────

    @Transactional
    public WorkOrderResponse assignContractor(Long workOrderId, AssignContractorRequest request, Long assignedBy) {
        WorkOrder wo = getEntityById(workOrderId);

        if (wo.getStatus() == WorkOrderStatus.COMPLETED || wo.getStatus() == WorkOrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot assign contractor to a " + wo.getStatus() + " work order.");
        }

        wo.setContractorId(request.getContractorId());
        wo.setAssignedBy(assignedBy);
        if (request.getRemarks() != null) wo.setRemarks(request.getRemarks());

        WorkOrder saved = workOrderRepository.save(wo);
        log.info("Contractor assigned: workOrderId={} contractorId={}", workOrderId, request.getContractorId());
        return mapToResponse(saved);
    }

    // ─── STATUS TRANSITION ────────────────────────────────────────────────────

    @Transactional
    public WorkOrderResponse updateStatus(Long workOrderId, UpdateWorkOrderStatusRequest request,
                                           Long userId, String role) {
        WorkOrder wo = getEntityById(workOrderId);

        // Field officer / PWE can only update their own assigned work orders
        if (("FO".equals(role) || "ENG".equals(role))
                && !userId.equals(wo.getContractorId())) {
            throw new ForbiddenException("You can only update work orders assigned to you.");
        }

        validateStatusTransition(wo.getStatus(), request.getStatus(), role);

        WorkOrderStatus old = wo.getStatus();
        wo.setStatus(request.getStatus());

        // Activate start date when work begins
        if (request.getStatus() == WorkOrderStatus.IN_PROGRESS && wo.getStartDate() == null) {
            wo.setStartDate(LocalDate.now());
        }
        if (request.getStatus() == WorkOrderStatus.COMPLETED) {
            wo.setActualEndDate(LocalDate.now());
        }
        if (request.getRemarks() != null) wo.setRemarks(request.getRemarks());

        WorkOrder saved = workOrderRepository.save(wo);
        log.info("Work order status updated: id={} {} → {}", workOrderId, old, request.getStatus());
        return mapToResponse(saved);
    }

    @Transactional
    public void cancel(Long workOrderId) {
        WorkOrder wo = getEntityById(workOrderId);
        wo.setStatus(WorkOrderStatus.CANCELLED);
        workOrderRepository.save(wo);
        log.info("Work order cancelled: id={}", workOrderId);
    }

    // ─── BUDGET TRACKING ──────────────────────────────────────────────────────

    @Transactional
    public WorkOrderResponse updateBudgetSpent(Long workOrderId, UpdateBudgetRequest request,
                                                Long userId, String role) {
        WorkOrder wo = getEntityById(workOrderId);

        if (("FO".equals(role) || "ENG".equals(role))
                && !userId.equals(wo.getContractorId())) {
            throw new ForbiddenException("You can only update budget for work orders assigned to you.");
        }

        wo.setBudgetConsumedTotal(request.getBudgetSpent());
        if (request.getRemarks() != null) wo.setRemarks(request.getRemarks());

        if (request.getBudgetSpent().compareTo(wo.getBudgetAllocated()) > 0) {
            log.warn("Budget overrun: workOrderId={} allocated={} spent={}",
                    workOrderId, wo.getBudgetAllocated(), request.getBudgetSpent());
        }

        WorkOrder saved = workOrderRepository.save(wo);
        return mapToResponse(saved);
    }

    public List<WorkOrderResponse> getBudgetOverruns() {
        return workOrderRepository.findBudgetOverruns().stream().map(this::mapToResponse).toList();
    }

    public List<BudgetSummaryResponse> getBudgetSummary() {
        List<Object[]> rows = workOrderRepository.budgetSummaryByStatus();
        List<BudgetSummaryResponse> summary = new ArrayList<>();
        for (Object[] row : rows) {
            BigDecimal allocated = row[2] != null ? (BigDecimal) row[2] : BigDecimal.ZERO;
            BigDecimal spent     = row[3] != null ? (BigDecimal) row[3] : BigDecimal.ZERO;
            summary.add(BudgetSummaryResponse.builder()
                    .status(row[0].toString())
                    .workOrderCount((Long) row[1])
                    .totalAllocated(allocated)
                    .totalSpent(spent)
                    .totalRemaining(allocated.subtract(spent))
                    .build());
        }
        return summary;
    }

    // ─── MILESTONES ───────────────────────────────────────────────────────────

    @Transactional
    public MilestoneResponse addMilestone(Long workOrderId, MilestoneRequest request) {
        WorkOrder wo = getEntityById(workOrderId);

        Milestone milestone = Milestone.builder()
                .workOrder(wo)
                .description(request.getDescription())
                .plannedDate(request.getPlannedDate())
                .status(MilestoneStatus.PENDING)
                .completionPercentage(request.getCompletionPercentage() != null ? request.getCompletionPercentage() : 0)
                .budgetConsumed(request.getBudgetConsumed() != null ? request.getBudgetConsumed() : BigDecimal.ZERO)
                .remarks(request.getRemarks())
                .build();

        Milestone saved = milestoneRepository.save(milestone);
        log.info("Milestone added: id={} workOrderId={}", saved.getMilestoneId(), workOrderId);
        return mapMilestoneToResponse(saved);
    }

    @Transactional
    public MilestoneResponse updateMilestone(Long milestoneId, MilestoneUpdateRequest request,
                                              Long userId, String role) {
        Milestone milestone = getMilestoneEntityById(milestoneId);

        // Field officers / PWE can only update milestones on their assigned work orders
        if (("FO".equals(role) || "ENG".equals(role))
                && !userId.equals(milestone.getWorkOrder().getContractorId())) {
            throw new ForbiddenException("You can only update milestones on work orders assigned to you.");
        }

        milestone.setStatus(request.getStatus());
        if (request.getCompletionPercentage() != null) {
            milestone.setCompletionPercentage(request.getCompletionPercentage());
        }
        if (request.getStatus() == MilestoneStatus.COMPLETED) {
            milestone.setCompletedDate(
                    request.getCompletedDate() != null ? request.getCompletedDate() : LocalDate.now());
            milestone.setCompletionPercentage(100);
        }
        if (request.getBudgetConsumed() != null) milestone.setBudgetConsumed(request.getBudgetConsumed());
        if (request.getRemarks() != null) milestone.setRemarks(request.getRemarks());

        Milestone saved = milestoneRepository.save(milestone);
        log.info("Milestone updated: id={} status={}", milestoneId, request.getStatus());
        return mapMilestoneToResponse(saved);
    }

    public List<MilestoneResponse> getMilestonesByWorkOrder(Long workOrderId) {
        getEntityById(workOrderId); // validate exists
        return milestoneRepository.findByWorkOrder_WorkOrderId(workOrderId)
                .stream().map(this::mapMilestoneToResponse).toList();
    }

    public MilestoneResponse getMilestoneById(Long milestoneId) {
        return mapMilestoneToResponse(getMilestoneEntityById(milestoneId));
    }

    @Transactional
    public void deleteMilestone(Long milestoneId) {
        com.civicdesk.publicworks.entity.Milestone milestone = getMilestoneEntityById(milestoneId);
        milestone.setIsDeleted(true);
        milestoneRepository.save(milestone);
        log.info("Milestone soft-deleted: id={}", milestoneId);
    }

    public List<MilestoneResponse> getDelayedMilestones() {
        return milestoneRepository.findByStatusAndPlannedDateBefore(MilestoneStatus.PENDING, LocalDate.now())
                .stream().map(this::mapMilestoneToResponse).toList();
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────────

    private void validateStatusTransition(WorkOrderStatus current, WorkOrderStatus next, String role) {
        boolean valid = switch (current) {
            case PLANNED      -> next == WorkOrderStatus.IN_PROGRESS || next == WorkOrderStatus.CANCELLED;
            case IN_PROGRESS  -> next == WorkOrderStatus.ON_HOLD || next == WorkOrderStatus.COMPLETED
                              || next == WorkOrderStatus.CANCELLED;
            case ON_HOLD      -> next == WorkOrderStatus.IN_PROGRESS || next == WorkOrderStatus.CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
        if (!valid) {
            throw new BadRequestException("Invalid work order status transition: " + current + " → " + next);
        }
        // Only supervisors/admin can cancel
        if (next == WorkOrderStatus.CANCELLED
                && !("DS".equals(role) || "ADM".equals(role))) {
            throw new ForbiddenException("Only supervisors or admins can cancel work orders.");
        }
    }

    public WorkOrder getEntityById(Long id) {
        return workOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Work order not found with id: " + id));
    }

    private Milestone getMilestoneEntityById(Long id) {
        return milestoneRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found with id: " + id));
    }

    private WorkOrderResponse mapToResponse(WorkOrder w) {
        BigDecimal remaining = w.getBudgetAllocated() != null
                ? w.getBudgetAllocated().subtract(w.getBudgetConsumedTotal() != null ? w.getBudgetConsumedTotal() : BigDecimal.ZERO)
                : BigDecimal.ZERO;
        return WorkOrderResponse.builder()
                .workOrderId(w.getWorkOrderId())
                .projectName(w.getProjectName())
                .category(w.getCategory())
                .departmentId(w.getDepartmentId())
                .ward(w.getWard())
                .zone(w.getZone())
                .location(w.getLocation())
                .assignedContractorId(w.getContractorId())
                .assignedEngineerId(w.getAssignedBy())
                .priority(w.getPriority())
                .status(w.getStatus())
                .budgetAllocated(w.getBudgetAllocated())
                .budgetConsumedTotal(w.getBudgetConsumedTotal())
                .budgetRemaining(remaining)
                .startDate(w.getStartDate())
                .expectedEndDate(w.getExpectedEndDate())
                .actualEndDate(w.getActualEndDate())
                .remarks(w.getRemarks())
                .createdAt(w.getCreatedAt())
                .updatedAt(w.getUpdatedAt())
                .build();
    }

    private MilestoneResponse mapMilestoneToResponse(Milestone m) {
        return MilestoneResponse.builder()
                .milestoneId(m.getMilestoneId())
                .workOrderId(m.getWorkOrder().getWorkOrderId())
                .description(m.getDescription())
                .plannedDate(m.getPlannedDate())
                .completedDate(m.getCompletedDate())
                .status(m.getStatus())
                .completionPercentage(m.getCompletionPercentage())
                .budgetConsumed(m.getBudgetConsumed())
                .remarks(m.getRemarks())
                .createdAt(m.getCreatedAt())
                .updatedAt(m.getUpdatedAt())
                .build();
    }
}
