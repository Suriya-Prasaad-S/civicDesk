package com.civicdesk.publicworks.service;
import com.civicdesk.publicworks.dto.*;
import com.civicdesk.publicworks.entity.Milestone;
import com.civicdesk.publicworks.exception.ResourceNotFoundException;

import java.util.List;
import com.civicdesk.publicworks.client.AuditLogClient;
import com.civicdesk.publicworks.client.NotificationClient;
import com.civicdesk.publicworks.dto.CreateWorkOrderRequest;
import com.civicdesk.publicworks.dto.WorkOrderResponse;
import com.civicdesk.publicworks.entity.WorkOrder;
import com.civicdesk.publicworks.enums.WorkCategory;
import com.civicdesk.publicworks.enums.WorkOrderStatus;
import com.civicdesk.publicworks.repository.MilestoneRepository;
import com.civicdesk.publicworks.repository.WorkOrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkOrderServiceTest {

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private MilestoneRepository milestoneRepository;

    @Mock
    private NotificationClient notificationClient;

    @Mock
    private AuditLogClient auditLogClient;

    @InjectMocks
    private WorkOrderService workOrderService;

    @Test
    void createWorkOrderSuccessfully() {

        CreateWorkOrderRequest request =
                new CreateWorkOrderRequest();

        request.setProjectName("Road Repair");
        request.setCategory("ROAD_REPAIR");
        request.setWard("Ward-1");
        request.setZone("Zone-A");
        request.setBudgetAllocated(
                new BigDecimal("100000"));
        request.setStartDate(LocalDate.now());
        request.setExpectedEndDate(
                LocalDate.now().plusDays(30));

        WorkOrder workOrder =
                WorkOrder.builder()
                        .workOrderId(1L)
                        .projectName("Road Repair")
                        .category(WorkCategory.ROAD_REPAIR)
                        .status(WorkOrderStatus.PLANNED)
                        .build();

        when(workOrderRepository.save(any()))
                .thenReturn(workOrder);

        WorkOrderResponse response =
                workOrderService.create(
                        request,
                        10000001L);

        assertNotNull(response);
        assertEquals(
                "Road Repair",
                response.getProjectName());

        verify(workOrderRepository)
                .save(any());

        verify(notificationClient)
                .sendNotification(any());

        verify(auditLogClient)
                .log(
                        anyString(),
                        eq("CREATE_WORK_ORDER"),
                        eq("PUBLIC_WORKS"));
    }

    @Test
    void getByIdSuccessfully() {

        WorkOrder workOrder =
                WorkOrder.builder()
                        .workOrderId(1L)
                        .projectName("Drain Work")
                        .category(WorkCategory.DRAINAGE)
                        .status(WorkOrderStatus.PLANNED)
                        .build();

        when(workOrderRepository.findById(1L))
                .thenReturn(Optional.of(workOrder));

        WorkOrderResponse response =
                workOrderService.getById(1L);

        assertNotNull(response);

        assertEquals(
                "Drain Work",
                response.getProjectName());
    }

    @Test
    void workOrderNotFound() {

        when(workOrderRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                RuntimeException.class,
                () -> workOrderService.getById(99L));
    }

    @Test
    void getAllByStatus() {

        WorkOrder wo =
                WorkOrder.builder()
                        .workOrderId(1L)
                        .projectName("Road Work")
                        .category(WorkCategory.ROAD_REPAIR)
                        .status(WorkOrderStatus.PLANNED)
                        .build();

        when(workOrderRepository.findByStatus(
                WorkOrderStatus.PLANNED))
                .thenReturn(List.of(wo));

        List<WorkOrderResponse> result =
                workOrderService.getAll(
                        WorkOrderStatus.PLANNED,
                        null,
                        null);

        assertEquals(1, result.size());
    }
    @Test
    void getAllWhenEmpty() {

        when(workOrderRepository.findAll())
                .thenReturn(List.of());

        List<WorkOrderResponse> result =
                workOrderService.getAll(
                        null,
                        null,
                        null);

        assertTrue(result.isEmpty());
    }
    @Test
    void assignContractorSuccessfully() {

        WorkOrder wo =
                WorkOrder.builder()
                        .workOrderId(1L)
                        .projectName("Road Repair")
                        .category(WorkCategory.ROAD_REPAIR)
                        .status(WorkOrderStatus.PLANNED)
                        .ward("Ward-1")
                        .zone("Zone-A")
                        .build();

        AssignContractorRequest request =
                new AssignContractorRequest();

        request.setAssignedContractorId(
                "10000005");

        when(workOrderRepository.findById(1L))
                .thenReturn(Optional.of(wo));

        when(workOrderRepository.save(any()))
                .thenReturn(wo);

        workOrderService.assignContractor(
                1L,
                request,
                10000001L);

        verify(workOrderRepository)
                .save(any());
    }

    @Test
    void assignContractorCompletedWorkOrder() {

        WorkOrder wo =
                WorkOrder.builder()
                        .status(
                                WorkOrderStatus.COMPLETED)
                        .build();

        when(workOrderRepository.findById(1L))
                .thenReturn(Optional.of(wo));

        AssignContractorRequest request =
                new AssignContractorRequest();

        request.setAssignedContractorId("10");

        assertThrows(
                RuntimeException.class,
                () -> workOrderService.assignContractor(
                        1L,
                        request,
                        1L));
    }
    @Test
    void cancelWorkOrder() {

        WorkOrder wo =
                WorkOrder.builder()
                        .status(
                                WorkOrderStatus.PLANNED)
                        .build();

        when(workOrderRepository.findById(1L))
                .thenReturn(Optional.of(wo));

        workOrderService.cancel(1L);

        verify(workOrderRepository)
                .save(any());
    }
    @Test
    void getEntityByIdSuccess() {

        WorkOrder wo =
                WorkOrder.builder()
                        .workOrderId(1L)
                        .build();

        when(workOrderRepository.findById(1L))
                .thenReturn(Optional.of(wo));

        assertNotNull(
                workOrderService.getEntityById(1L));
    }
    @Test
    void getEntityByIdFailure() {

        when(workOrderRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> workOrderService.getEntityById(1L));
    }
    @Test
    void getMilestonesByWorkOrderEmpty() {

        WorkOrder wo =
                WorkOrder.builder()
                        .workOrderId(1L)
                        .build();

        when(workOrderRepository.findById(1L))
                .thenReturn(Optional.of(wo));

        when(milestoneRepository
                .findByWorkOrder_WorkOrderId(1L))
                .thenReturn(List.of());

        assertTrue(
                workOrderService
                        .getMilestonesByWorkOrder(1L)
                        .isEmpty());
    }
    @Test
    void addMilestoneSuccessfully() {

        WorkOrder wo =
                WorkOrder.builder()
                        .workOrderId(1L)
                        .build();

        CreateMilestoneRequest request =
                new CreateMilestoneRequest();

        request.setDescription(
                "Foundation Completed");

        request.setPlannedDate(
                LocalDate.now());

        Milestone milestone =
                Milestone.builder()
                        .milestoneId(1L)
                        .description(
                                "Foundation Completed")
                        .workOrder(wo)
                        .build();

        when(workOrderRepository.findById(1L))
                .thenReturn(Optional.of(wo));

        when(milestoneRepository.save(any()))
                .thenReturn(milestone);

        MilestoneResponse response =
                workOrderService.addMilestone(
                        1L,
                        request);

        assertNotNull(response);
    }

    @Test
    void getDelayedMilestonesEmpty() {

        when(milestoneRepository
                .findByStatusAndPlannedDateBefore(
                        any(),
                        any()))
                .thenReturn(List.of());

        assertTrue(
                workOrderService
                        .getDelayedMilestones()
                        .isEmpty());
    }

    @Test
    void completeWorkOrderSuccessfully() {

        WorkOrder wo =
                WorkOrder.builder()
                        .workOrderId(1L)
                        .projectName("Road Repair")
                        .category(WorkCategory.ROAD_REPAIR)
                        .status(WorkOrderStatus.IN_PROGRESS)
                        .ward("Ward-1")
                        .zone("Zone-A")
                        .build();

        when(workOrderRepository.findById(1L))
                .thenReturn(Optional.of(wo));

        when(workOrderRepository.save(any()))
                .thenReturn(wo);

        CompleteWorkOrderRequest request =
                new CompleteWorkOrderRequest();

        request.setActualEndDate(
                LocalDate.now());

        WorkOrderResponse response =
                workOrderService.completeWorkOrder(
                        1L,
                        request);

        assertNotNull(response);
    }
    @Test
    void getAssignedToMeEmpty() {

        when(workOrderRepository
                .findByContractorId(10L))
                .thenReturn(List.of());

        assertTrue(
                workOrderService
                        .getAssignedToMe(10L)
                        .isEmpty());
    }
    @Test
    void getBudgetOverrunsEmpty() {

        when(workOrderRepository
                .findBudgetOverruns())
                .thenReturn(List.of());

        assertTrue(
                workOrderService
                        .getBudgetOverruns()
                        .isEmpty());
    }
    @Test
    void getPublicByWardEmpty() {

        when(workOrderRepository
                .findByWardAndIsDeletedFalse(
                        "Ward-1"))
                .thenReturn(List.of());

        assertTrue(
                workOrderService
                        .getPublicByWard("Ward-1")
                        .isEmpty());
    }

    @Test
    void getByDepartmentEmpty() {

        when(workOrderRepository
                .findByDepartmentId(1L))
                .thenReturn(List.of());

        assertTrue(
                workOrderService
                        .getByDepartment(
                                1L,
                                null)
                        .isEmpty());
    }

    @Test
    void getAllSummaryEmpty() {

        when(workOrderRepository.findAll())
                .thenReturn(List.of());

        assertTrue(
                workOrderService
                        .getAllSummary(
                                null,
                                null,
                                null)
                        .isEmpty());
    }

    @Test
    void getAnalyticsShouldReturnResponse() {

        when(workOrderRepository.findAll())
                .thenReturn(List.of());

        when(workOrderRepository
                .countDelayedWorkOrders())
                .thenReturn(0L);

        when(workOrderRepository
                .getTotalAllocatedBudget())
                .thenReturn(BigDecimal.ZERO);

        when(workOrderRepository
                .getTotalConsumedBudget())
                .thenReturn(BigDecimal.ZERO);

        when(milestoneRepository
                .countDelayedMilestones())
                .thenReturn(0L);

        WorkOrderAnalyticsResponse response =
                workOrderService.getAnalytics(
                        null,
                        null);

        assertNotNull(response);
    }








}