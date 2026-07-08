package com.civicdesk.publicworks.service;

import com.civicdesk.publicworks.dto.*;
import com.civicdesk.publicworks.entity.Milestone;
import com.civicdesk.publicworks.entity.WorkOrder;
import com.civicdesk.publicworks.enums.MilestoneStatus;
import com.civicdesk.publicworks.enums.WorkCategory;
import com.civicdesk.publicworks.enums.WorkOrderStatus;
import com.civicdesk.publicworks.enums.WorkPriority;
import com.civicdesk.publicworks.client.AuditLogClient;
import com.civicdesk.publicworks.exception.BadRequestException;
import com.civicdesk.publicworks.exception.ForbiddenException;
import com.civicdesk.publicworks.exception.ResourceNotFoundException;
import com.civicdesk.publicworks.repository.MilestoneRepository;
import com.civicdesk.publicworks.repository.WorkOrderRepository;
import com.civicdesk.publicworks.util.EnumUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.time.temporal.ChronoUnit;
import com.civicdesk.publicworks.client.NotificationClient;
import com.civicdesk.publicworks.dto.NotificationRequest;


@Service
@RequiredArgsConstructor
@Slf4j
public class WorkOrderService {

    private final WorkOrderRepository workOrderRepository;
    private final MilestoneRepository milestoneRepository;
    private final NotificationClient notificationClient;
    private final AuditLogClient auditLogClient;

    // ─── WORK ORDER CRUD ─────────────────────────────────────────────────────

    @Transactional
    public WorkOrderResponse create(
            CreateWorkOrderRequest request,
            Long userId) {
        WorkOrder wo = WorkOrder.builder()
                .projectName(request.getProjectName())
                .category(
                        EnumUtil.parseWorkCategory(
                                request.getCategory()))
                .ward(request.getWard())
                .zone(request.getZone())
                .budgetAllocated(
                        request.getBudgetAllocated())
                .budgetConsumedTotal(BigDecimal.ZERO)
                .startDate(request.getStartDate())
                .expectedEndDate(
                        request.getExpectedEndDate())
                .remarks(request.getRemarks())
                .assignedBy(userId)
                .status(WorkOrderStatus.PLANNED)
                .build();

        WorkOrder saved =
                workOrderRepository.save(wo);

        auditLogClient.log(
                String.valueOf(userId),
                "CREATE_WORK_ORDER",
                "PUBLIC_WORKS");

        sendNotification(
                userId,
                "Work Order Created",
                "Work order '"
                        + saved.getProjectName()
                        + "' was created.",
                saved.getWorkOrderId(),
                "WORK_ORDER");

        return mapToResponse(saved);
    }

    public WorkOrderResponse getById(Long id) {
        return mapToResponse(getEntityById(id));
    }
    public PublicWorkOrderResponse getPublicById(
            Long workOrderId) {

        WorkOrder workOrder =
                getEntityById(workOrderId);

        return mapToPublicResponse(
                workOrder);
    }
    public List<PublicWorkOrderResponse>
    getPublicByWard(String ward) {

        List<WorkOrder> workOrders =
                workOrderRepository
                        .findByWardAndIsDeletedFalse(
                                ward);

        return workOrders
                .stream()
                .map(this::mapToPublicResponse)
                .toList();
    }

    @Transactional
    public WorkOrderResponse update(
            Long workOrderId,
            UpdateWorkOrderRequest request,
            Long userId) {
        WorkOrder wo = getEntityById(workOrderId);

        if(request.getProjectName()!=null){
            wo.setProjectName(request.getProjectName());
        }

        if(request.getBudgetAllocated()!=null){
            wo.setBudgetAllocated(
                    request.getBudgetAllocated());
        }

        if(request.getExpectedEndDate()!=null){
            wo.setExpectedEndDate(
                    request.getExpectedEndDate());
        }

        if(request.getRemarks()!=null){
            wo.setRemarks(request.getRemarks());
        }

        WorkOrder saved =
                workOrderRepository.save(wo);

        auditLogClient.log(
                String.valueOf(userId),
                "UPDATE_WORK_ORDER",
                "PUBLIC_WORKS");

        sendNotification(
                userId,
                "Work Order Updated",
                "Work order '"
                        + saved.getProjectName()
                        + "' was updated.",
                saved.getWorkOrderId(),
                "WORK_ORDER");

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

        wo.setContractorId(
                Long.parseLong(
                        request.getAssignedContractorId()));
        wo.setAssignedBy(assignedBy);


        WorkOrder saved = workOrderRepository.save(wo);
        auditLogClient.log(
                String.valueOf(assignedBy),
                "ASSIGN_CONTRACTOR",
                "PUBLIC_WORKS");

        log.info("Contractor assigned: workOrderId={} contractorId={}",
                workOrderId,
                request.getAssignedContractorId());
        sendNotification(
                Long.valueOf(
                        request.getAssignedContractorId()),
                "Work Order Assigned",
                "You have been assigned work order '"
                        + saved.getProjectName()
                        + "'.",
                saved.getWorkOrderId(),
                "WORK_ORDER");
        return mapToResponse(saved);
    }

    // ─── STATUS TRANSITION ────────────────────────────────────────────────────

    @Transactional
    public WorkOrderResponse updateStatus(Long workOrderId, UpdateWorkOrderStatusRequest request,
                                           Long userId, String role) {
        WorkOrder wo = getEntityById(workOrderId);

        // Field officer / PWE can only update their own assigned work orders
        if ("FO".equals(role)
                && !userId.equals(wo.getContractorId())) {

            throw new ForbiddenException(
                    "You can only update work orders assigned to you.");
        }

        WorkOrderStatus newStatus =
                WorkOrderStatus.valueOf(
                        request.getStatus().toUpperCase());

        validateStatusTransition(
                wo.getStatus(),
                newStatus,
                role);

        WorkOrderStatus old = wo.getStatus();
        wo.setStatus(newStatus);

        // Activate start date when work begins
        if (newStatus == WorkOrderStatus.IN_PROGRESS) {
            wo.setStartDate(LocalDate.now());
        }
        if (newStatus == WorkOrderStatus.COMPLETED){
            wo.setActualEndDate(LocalDate.now());
        }
        if (request.getRemarks() != null) wo.setRemarks(request.getRemarks());

        WorkOrder saved = workOrderRepository.save(wo);
        log.info("Work order status updated: id={} {} → {}",
                workOrderId,
                old,
                newStatus);

        auditLogClient.log(
                String.valueOf(userId),
                "UPDATE_WORK_ORDER_STATUS",
                "PUBLIC_WORKS");

        if (saved.getContractorId() != null) {

            sendNotification(
                    saved.getContractorId(),
                    "Work Order Status Updated",
                    "Status changed to "
                            + newStatus,
                    saved.getWorkOrderId(),
                    "WORK_ORDER");
        }
        return mapToResponse(saved);
    }

    @Transactional
    public void cancel(Long workOrderId){

        WorkOrder wo =
                getEntityById(workOrderId);

        wo.setStatus(
                WorkOrderStatus.CANCELLED);

        wo.setIsDeleted(true);

        workOrderRepository.save(wo);

        log.info("Work order cancelled: id={}", workOrderId);

        auditLogClient.log(
                String.valueOf(wo.getAssignedBy()),
                "CANCEL_WORK_ORDER",
                "PUBLIC_WORKS");
    }

    // ─── BUDGET TRACKING ──────────────────────────────────────────────────────

    @Transactional
    public WorkOrderResponse updateBudgetSpent(Long workOrderId, UpdateBudgetRequest request,
                                                Long userId, String role) {
        WorkOrder wo = getEntityById(workOrderId);

        if ("FO".equals(role)
                && !userId.equals(wo.getContractorId())) {

            throw new ForbiddenException(
                    "You can only update budget for work orders assigned to you.");
        }

        wo.setBudgetConsumedTotal(request.getBudgetSpent());
        if (request.getRemarks() != null) wo.setRemarks(request.getRemarks());

        if (request.getBudgetSpent().compareTo(wo.getBudgetAllocated()) > 0) {
            log.warn("Budget overrun: workOrderId={} allocated={} spent={}",
                    workOrderId, wo.getBudgetAllocated(), request.getBudgetSpent());
        }

        WorkOrder saved = workOrderRepository.save(wo);

        auditLogClient.log(
                String.valueOf(userId),
                "UPDATE_WORK_ORDER_BUDGET",
                "PUBLIC_WORKS");
        if (saved.getAssignedBy() != null) {

            sendNotification(
                    saved.getAssignedBy(),
                    "Budget Updated",
                    "Budget updated for work order '"
                            + saved.getProjectName()
                            + "'.",
                    saved.getWorkOrderId(),
                    "WORK_ORDER");
        }

        return mapToResponse(saved);
    }

    public List<WorkOrderResponse> getBudgetOverruns() {
        return workOrderRepository.findBudgetOverruns().stream().map(this::mapToResponse).toList();
    }

    public List<BudgetSummaryResponse> getBudgetSummary() {

        List<Object[]> rows =
                workOrderRepository.budgetSummaryByStatus();

        List<BudgetSummaryResponse> summary =
                new ArrayList<>();

        for (Object[] row : rows) {

            BigDecimal allocated =
                    row[2] != null
                            ? (BigDecimal) row[2]
                            : BigDecimal.ZERO;

            BigDecimal spent =
                    row[3] != null
                            ? (BigDecimal) row[3]
                            : BigDecimal.ZERO;

            summary.add(
                    BudgetSummaryResponse.builder()
                            .status(row[0].toString())
                            .workOrderCount(
                                    ((Number) row[1]).longValue())
                            .totalAllocated(allocated)
                            .totalSpent(spent)
                            .totalRemaining(
                                    allocated.subtract(spent))
                            .build()
            );
        }

        return summary;
    }

    // ─── MILESTONES ───────────────────────────────────────────────────────────

    @Transactional
    public MilestoneResponse addMilestone(Long workOrderId,
                                          CreateMilestoneRequest request) {
        WorkOrder wo = getEntityById(workOrderId);

        Milestone milestone = Milestone.builder()
                .workOrder(wo)
                .description(request.getDescription())
                .plannedDate(request.getPlannedDate())
                .remarks(request.getRemarks())
                .status(MilestoneStatus.PENDING)
                .completionPercentage(0)
                .budgetConsumed(BigDecimal.ZERO)
                .build();

        Milestone saved = milestoneRepository.save(milestone);
        log.info("Milestone added: id={} workOrderId={}", saved.getMilestoneId(), workOrderId);

        auditLogClient.log(
                String.valueOf(
                        wo.getAssignedBy()),
                "CREATE_MILESTONE",
                "PUBLIC_WORKS");
        if (wo.getAssignedBy() != null) {

            sendNotification(
                    wo.getAssignedBy(),
                    "Milestone Added",
                    saved.getDescription(),
                    saved.getMilestoneId(),
                    "WORK_ORDER");
        }



        return mapMilestoneToResponse(saved);
    }

    @Transactional
    public MilestoneResponse updateMilestone(
            Long milestoneId,
            UpdateMilestoneRequest request) {

        Milestone milestone =
                getMilestoneEntityById(milestoneId);

        if (request.getDescription() != null) {
            milestone.setDescription(request.getDescription());
        }

        if (request.getPlannedDate() != null) {
            milestone.setPlannedDate(request.getPlannedDate());
        }

        if (request.getRemarks() != null) {
            milestone.setRemarks(request.getRemarks());
        }

        Milestone saved =
                milestoneRepository.save(
                        milestone);

        auditLogClient.log(
                String.valueOf(
                        saved.getWorkOrder()
                                .getAssignedBy()),
                "UPDATE_MILESTONE",
                "PUBLIC_WORKS");

        if (saved.getWorkOrder()
                .getAssignedBy() != null) {

            sendNotification(
                    saved.getWorkOrder()
                            .getAssignedBy(),
                    "Milestone Updated",
                    saved.getDescription(),
                    saved.getMilestoneId(),
                    "WORK_ORDER");
        }

        return mapMilestoneToResponse(saved);
    }

    @Transactional
    public MilestoneResponse completeMilestone(
            Long milestoneId,
            CompleteMilestoneRequest request) {

        Milestone milestone =
                getMilestoneEntityById(milestoneId);

        milestone.setStatus(
                MilestoneStatus.COMPLETED);

        milestone.setCompletedDate(
                request.getCompletedDate() != null
                        ? request.getCompletedDate()
                        : LocalDate.now());

        if (request.getBudgetConsumed() != null) {
            milestone.setBudgetConsumed(
                    request.getBudgetConsumed());
        }

        if (request.getRemarks() != null) {
            milestone.setRemarks(
                    request.getRemarks());
        }

        milestone.setCompletionPercentage(100);

        Milestone saved =
                milestoneRepository.save(
                        milestone);

        auditLogClient.log(
                String.valueOf(
                        saved.getWorkOrder()
                                .getAssignedBy()),
                "COMPLETE_MILESTONE",
                "PUBLIC_WORKS");

        if (saved.getWorkOrder()
                .getAssignedBy() != null) {

            sendNotification(
                    saved.getWorkOrder()
                            .getAssignedBy(),
                    "Milestone Completed",
                    saved.getDescription(),
                    saved.getMilestoneId(),
                    "WORK_ORDER");
        }

        return mapMilestoneToResponse(saved);

    }
    @Transactional
    public MilestoneResponse updateMilestoneStatus(
            Long milestoneId,
            UpdateMilestoneStatusRequest request) {

        Milestone milestone =
                getMilestoneEntityById(milestoneId);

        milestone.setStatus(
                MilestoneStatus.valueOf(
                        request.getStatus().toUpperCase()));

        if (request.getRemarks() != null) {
            milestone.setRemarks(request.getRemarks());
        }

        Milestone saved =
                milestoneRepository.save(
                        milestone);

        auditLogClient.log(
                String.valueOf(
                        saved.getWorkOrder()
                                .getAssignedBy()),
                "UPDATE_MILESTONE_STATUS",
                "PUBLIC_WORKS");

        if (saved.getWorkOrder()
                .getAssignedBy() != null) {

            sendNotification(
                    saved.getWorkOrder()
                            .getAssignedBy(),
                    "Milestone Status Updated",
                    "Status changed to "
                            + saved.getStatus(),
                    saved.getMilestoneId(),
                    "WORK_ORDER");
        }

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

    public List<DelayedMilestoneResponse> getDelayedMilestones() {

        return milestoneRepository
                .findByStatusAndPlannedDateBefore(
                        MilestoneStatus.PENDING,
                        LocalDate.now())
                .stream()
                .map(this::mapToDelayedResponse)
                .toList();
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

    private WorkOrderResponse mapToResponse(
            WorkOrder w) {

        WorkOrderResponse r =
                new WorkOrderResponse();

        r.setWorkOrderId(w.getWorkOrderId());
        r.setProjectName(w.getProjectName());
        r.setCategory(w.getCategory().name());
        r.setWard(w.getWard());
        r.setZone(w.getZone());

        r.setBudgetAllocated(
                w.getBudgetAllocated());

        r.setBudgetConsumedTotal(
                w.getBudgetConsumedTotal());

        r.setStartDate(w.getStartDate());
        r.setExpectedEndDate(
                w.getExpectedEndDate());

        r.setActualEndDate(
                w.getActualEndDate());

        r.setAssignedContractorId(
                w.getContractorId());

        r.setAssignedEngineerId(
                w.getAssignedBy());

        r.setStatus(
                w.getStatus().name());

        r.setRemarks(
                w.getRemarks());

        return r;
    }

    private WorkOrderDetailResponse mapToDetailResponse(
            WorkOrder workOrder) {

        WorkOrderDetailResponse response =
                new WorkOrderDetailResponse();

        response.setWorkOrderId(
                String.valueOf(workOrder.getWorkOrderId()));

        response.setProjectName(
                workOrder.getProjectName());

        response.setCategory(
                workOrder.getCategory().name());

        response.setWard(
                workOrder.getWard());

        response.setZone(
                workOrder.getZone());

        response.setBudgetAllocated(
                workOrder.getBudgetAllocated());

        response.setBudgetConsumedTotal(
                workOrder.getBudgetConsumedTotal());

        response.setStartDate(
                workOrder.getStartDate());

        response.setExpectedEndDate(
                workOrder.getExpectedEndDate());

        response.setActualEndDate(
                workOrder.getActualEndDate());

        response.setAssignedContractorId(
                workOrder.getContractorId() == null
                        ? null
                        : String.valueOf(workOrder.getContractorId()));

        response.setAssignedEngineerId(
                workOrder.getAssignedBy() == null
                        ? null
                        : String.valueOf(workOrder.getAssignedBy()));

        response.setStatus(
                workOrder.getStatus().name());

        response.setRemarks(
                workOrder.getRemarks());

        return response;
    }
    public WorkOrderDetailResponse getWorkOrderDetail(
            Long workOrderId) {

        return mapToDetailResponse(
                getEntityById(workOrderId));
    }

    private MilestoneResponse mapMilestoneToResponse(
            Milestone m) {

        MilestoneResponse r =
                new MilestoneResponse();

        r.setMilestoneId(
                String.valueOf(m.getMilestoneId()));

        r.setWorkOrderId(
                String.valueOf(
                        m.getWorkOrder()
                                .getWorkOrderId()));

        r.setDescription(
                m.getDescription());

        r.setPlannedDate(
                m.getPlannedDate());

        r.setCompletedDate(
                m.getCompletedDate());

        r.setBudgetConsumed(
                m.getBudgetConsumed());

        r.setStatus(
                m.getStatus().name());

        r.setRemarks(
                m.getRemarks());

        return r;
    }

    private DelayedMilestoneResponse mapToDelayedResponse(
            Milestone milestone) {

        DelayedMilestoneResponse response =
                new DelayedMilestoneResponse();

        response.setMilestoneId(
                String.valueOf(
                        milestone.getMilestoneId()));

        response.setWorkOrderId(
                String.valueOf(
                        milestone.getWorkOrder()
                                .getWorkOrderId()));

        response.setProjectName(
                milestone.getWorkOrder()
                        .getProjectName());

        response.setDescription(
                milestone.getDescription());

        response.setPlannedDate(
                milestone.getPlannedDate());

        response.setStatus(
                milestone.getStatus().name());

        response.setRemarks(
                milestone.getRemarks());

        response.setDaysOverdue(
                ChronoUnit.DAYS.between(
                        milestone.getPlannedDate(),
                        LocalDate.now()));

        return response;
    }

    private List<AnalyticsLabelCountDto> buildStatusBreakdown(
            LocalDate fromDate,
            LocalDate toDate) {

        List<AnalyticsLabelCountDto> result =
                new ArrayList<>();

        for (WorkOrderStatus status : WorkOrderStatus.values()) {

            long count =
                    workOrderRepository.findByStatus(status)
                            .size();

            result.add(
                    new AnalyticsLabelCountDto(
                            status.name(),
                            count));
        }

        return result;
    }
    private List<AnalyticsLabelCountDto> buildMilestoneStatusBreakdown() {

        List<AnalyticsLabelCountDto> result =
                new ArrayList<>();

        for (MilestoneStatus status : MilestoneStatus.values()) {

            long count =
                    milestoneRepository.findByStatus(status)
                            .size();

            result.add(
                    new AnalyticsLabelCountDto(
                            status.name(),
                            count));
        }

        return result;
    }
    private Double calculateAverageCompletionDays(
            LocalDate fromDate,
            LocalDate toDate) {

        List<WorkOrder> completedOrders =
                workOrderRepository.findByStatus(
                        WorkOrderStatus.COMPLETED);

        double totalDays = 0;
        long count = 0;

        for (WorkOrder workOrder : completedOrders) {

            if (workOrder.getStartDate() == null
                    || workOrder.getActualEndDate() == null) {
                continue;
            }

            if (fromDate != null
                    && workOrder.getActualEndDate().isBefore(fromDate)) {
                continue;
            }

            if (toDate != null
                    && workOrder.getActualEndDate().isAfter(toDate)) {
                continue;
            }

            long days =
                    ChronoUnit.DAYS.between(
                            workOrder.getStartDate(),
                            workOrder.getActualEndDate());

            totalDays += days;
            count++;
        }

        return count > 0
                ? totalDays / count
                : 0.0;
    }
    private long countWorkOrders(
            LocalDate fromDate,
            LocalDate toDate) {

        return workOrderRepository.findAll()
                .stream()
                .filter(workOrder -> {

                    LocalDate createdDate =
                            workOrder.getCreatedAt()
                                    .toLocalDate();

                    if (fromDate != null &&
                            createdDate.isBefore(fromDate)) {
                        return false;
                    }

                    if (toDate != null &&
                            createdDate.isAfter(toDate)) {
                        return false;
                    }

                    return true;
                })
                .count();
    }

    public WorkOrderAnalyticsResponse getAnalytics(
        LocalDate fromDate,
        LocalDate toDate) {

    WorkOrderAnalyticsResponse response =
            new WorkOrderAnalyticsResponse();

        response.setTotalWorkOrders(
                countWorkOrders(
                        fromDate,
                        toDate));

    response.setDelayedWorkOrders(
            workOrderRepository.countDelayedWorkOrders());

    response.setAverageCompletionDays(
            calculateAverageCompletionDays(
                    fromDate,
                    toDate));

    // Budget analytics

    WorkOrderAnalyticsResponse.BudgetAnalytics budget =
            new WorkOrderAnalyticsResponse.BudgetAnalytics();

    budget.setAllocated(
            workOrderRepository.getTotalAllocatedBudget());

    budget.setConsumed(
            workOrderRepository.getTotalConsumedBudget());

    if (budget.getAllocated().compareTo(BigDecimal.ZERO) > 0) {

        budget.setUtilizationPercentage(
                budget.getConsumed()
                        .multiply(BigDecimal.valueOf(100))
                        .divide(
                                budget.getAllocated(),
                                2,
                                java.math.RoundingMode.HALF_UP)
                        .doubleValue());

    } else {
        budget.setUtilizationPercentage(0.0);
    }

    response.setBudget(budget);

    // Milestone analytics

    WorkOrderAnalyticsResponse.MilestoneAnalytics milestoneAnalytics =
            new WorkOrderAnalyticsResponse.MilestoneAnalytics();

    milestoneAnalytics.setDelayedMilestones(
            milestoneRepository.countDelayedMilestones());

    milestoneAnalytics.setStatusBreakdown(
            buildMilestoneStatusBreakdown());

    response.setMilestones(milestoneAnalytics);

    response.setStatusBreakdown(
            buildStatusBreakdown(fromDate, toDate));

        response.setCompletedWorkOrders(
                countCompletedWorkOrders(
                        fromDate,
                        toDate));

        response.setCategoryBreakdown(
                buildCategoryBreakdown());

        response.setWardBreakdown(
                buildWardBreakdown());

    return response;
}

    @Transactional
    public WorkOrderResponse completeWorkOrder(
            Long workOrderId,
            CompleteWorkOrderRequest request){

        WorkOrder wo =
                getEntityById(workOrderId);

        wo.setStatus(
                WorkOrderStatus.COMPLETED);

        wo.setActualEndDate(
                request.getActualEndDate());

        wo.setRemarks(
                request.getRemarks());

        WorkOrder saved =
                workOrderRepository.save(wo);

        auditLogClient.log(
                String.valueOf(
                        saved.getAssignedBy()),
                "COMPLETE_WORK_ORDER",
                "PUBLIC_WORKS");

        if (saved.getAssignedBy() != null) {

            sendNotification(
                    saved.getAssignedBy(),
                    "Work Order Completed",
                    "Work order '"
                            + saved.getProjectName()
                            + "' completed.",
                    saved.getWorkOrderId(),
                    "WORK_ORDER");
        }

        if (saved.getContractorId() != null) {

            sendNotification(
                    saved.getContractorId(),
                    "Work Order Completed",
                    "Assigned work order completed.",
                    saved.getWorkOrderId(),
                    "WORK_ORDER");
        }


        return mapToResponse(saved);
    }

    private List<AnalyticsLabelCountDto>
    buildCategoryBreakdown(){

        return workOrderRepository
                .categoryBreakdown()
                .stream()
                .map(x->
                        new AnalyticsLabelCountDto(
                                x[0].toString(),
                                ((Number)x[1]).longValue()))
                .toList();
    }

    private List<AnalyticsLabelCountDto>
    buildWardBreakdown(){

        return workOrderRepository
                .wardBreakdown()
                .stream()
                .map(x->
                        new AnalyticsLabelCountDto(
                                x[0].toString(),
                                ((Number)x[1]).longValue()))
                .toList();
    }

    private PublicWorkOrderResponse
    mapToPublicResponse(
            WorkOrder workOrder){

        PublicWorkOrderResponse r =
                new PublicWorkOrderResponse();

        r.setWorkOrderId(
                String.valueOf(
                        workOrder.getWorkOrderId()));

        r.setProjectName(
                workOrder.getProjectName());

        r.setCategory(
                workOrder.getCategory().name());

        r.setWard(
                workOrder.getWard());

        r.setStartDate(
                workOrder.getStartDate());

        r.setExpectedEndDate(
                workOrder.getExpectedEndDate());

        r.setActualEndDate(
                workOrder.getActualEndDate());

        r.setStatus(
                workOrder.getStatus().name());

        int total=
                workOrder.getMilestones()==null
                        ?0
                        :workOrder.getMilestones().size();

        int completed=
                (int) workOrder.getMilestones()
                        .stream()
                        .filter(m->
                                m.getStatus()
                                        ==
                                        MilestoneStatus.COMPLETED)
                        .count();

        r.setTotalMilestones(total);
        r.setCompletedMilestones(completed);

        return r;
    }
    private WorkOrderSummaryResponse mapToSummaryResponse(
            WorkOrder workOrder) {

        WorkOrderSummaryResponse response =
                new WorkOrderSummaryResponse();

        response.setWorkOrderId(
                String.valueOf(workOrder.getWorkOrderId()));

        response.setProjectName(
                workOrder.getProjectName());

        response.setCategory(
                workOrder.getCategory().name());

        response.setWard(
                workOrder.getWard());

        response.setStatus(
                workOrder.getStatus().name());

        response.setStartDate(
                workOrder.getStartDate());

        response.setExpectedEndDate(
                workOrder.getExpectedEndDate());

        response.setBudgetAllocated(
                workOrder.getBudgetAllocated());

        response.setBudgetConsumedTotal(
                workOrder.getBudgetConsumedTotal());

        return response;
    }
    public List<WorkOrderSummaryResponse>
    getAllSummary(
            WorkOrderStatus status,
            WorkCategory category,
            String ward) {

        List<WorkOrder> results;

        if (status != null) {
            results =
                    workOrderRepository
                            .findByStatusAndIsDeletedFalse(status);
        }
        else if (category != null) {
            results =
                    workOrderRepository
                            .findByCategory(category);
        }
        else if (ward != null && !ward.isBlank()) {
            results =
                    workOrderRepository
                            .findByWardAndIsDeletedFalse(ward);
        }
        else {
            results =
                    workOrderRepository.findAll();
        }

        return results.stream()
                .map(this::mapToSummaryResponse)
                .toList();
    }


    private void sendNotification(
            Long userId,
            String title,
            String message,
            Long referenceId,
            String referenceType) {

        try {

            NotificationRequest request =
                    new NotificationRequest();

            request.setUserId(userId);

            request.setTitle(title);

            request.setMessage(message);

            request.setNotificationType(
                    "WORK_ORDER_UPDATE");

            request.setReferenceId(
                    referenceId);

            request.setReferenceType(
                    referenceType);

            notificationClient.sendNotification(
                    request);

        } catch (Exception ex) {

            log.error(
                    "Notification dispatch failed",
                    ex);
        }
    }

    private long countCompletedWorkOrders(
            LocalDate fromDate,
            LocalDate toDate) {

        return workOrderRepository.findByStatus(
                        WorkOrderStatus.COMPLETED)
                .stream()
                .filter(workOrder -> {

                    if (workOrder.getActualEndDate() == null) {
                        return false;
                    }

                    if (fromDate != null &&
                            workOrder.getActualEndDate()
                                    .isBefore(fromDate)) {
                        return false;
                    }

                    if (toDate != null &&
                            workOrder.getActualEndDate()
                                    .isAfter(toDate)) {
                        return false;
                    }

                    return true;
                })
                .count();
    }
}
