package com.civicdesk.permit.service;

import com.civicdesk.permit.client.NotificationClient;
import com.civicdesk.permit.dto.ConductInspectionRequest;
import com.civicdesk.permit.dto.InspectionResponse;
import com.civicdesk.permit.dto.ScheduleInspectionRequest;
import com.civicdesk.permit.entity.Inspection;
import com.civicdesk.permit.entity.PermitApplication;
import com.civicdesk.permit.enums.InspectionOutcome;
import com.civicdesk.permit.enums.InspectionStatus;
import com.civicdesk.permit.enums.PermitStatus;
import com.civicdesk.permit.exception.BadRequestException;
import com.civicdesk.permit.exception.ForbiddenException;
import com.civicdesk.permit.exception.ResourceNotFoundException;
import com.civicdesk.permit.repository.InspectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InspectionServiceTest {

    @Mock
    private InspectionRepository inspectionRepository;

    @Mock
    private PermitService permitService;

    @Mock
    private NotificationClient notificationClient;

    @InjectMocks
    private InspectionService inspectionService;

    private PermitApplication permit;
    private Inspection inspection;

    @BeforeEach
    void setUp() {

        permit = PermitApplication.builder()
                .permitId(1L)
                .userId(100L)
                .citizenId(100L)
                .status(PermitStatus.APPLIED)
                .validityPeriod(12)
                .build();

        inspection = Inspection.builder()
                .inspectionId(1L)
                .permitApplication(permit)
                .assignedOfficerId(500L)
                .scheduledDate(LocalDate.now().plusDays(2))
                .status(InspectionStatus.SCHEDULED)
                .build();
    }

    @Test
    void scheduleInspection_Success() {

        ScheduleInspectionRequest request =
                new ScheduleInspectionRequest();

        request.setOfficerId(500L);
        request.setScheduledDate(
                LocalDate.now().plusDays(5));

        when(permitService.getEntityById(1L))
                .thenReturn(permit);

        when(inspectionRepository.save(any(Inspection.class)))
                .thenReturn(inspection);

        InspectionResponse response =
                inspectionService.scheduleInspection(
                        1L,
                        request);

        assertNotNull(response);

        verify(inspectionRepository,
                times(1))
                .save(any());

        verify(permitService,
                times(1))
                .updateStatus(
                        eq(1L),
                        eq(PermitStatus.INSPECTION_SCHEDULED),
                        anyString());

        verify(notificationClient,
                times(1))
                .sendNotification(any());
    }

    @Test
    void scheduleInspection_InvalidPermitStatus() {

        permit.setStatus(
                PermitStatus.APPROVED);

        ScheduleInspectionRequest request =
                new ScheduleInspectionRequest();

        request.setOfficerId(500L);
        request.setScheduledDate(
                LocalDate.now().plusDays(5));

        when(permitService.getEntityById(1L))
                .thenReturn(permit);

        assertThrows(
                BadRequestException.class,
                () -> inspectionService.scheduleInspection(
                        1L,
                        request)
        );

        verify(inspectionRepository,
                never())
                .save(any());
    }

    @Test
    void cancelInspection_Success() {

        when(inspectionRepository.findById(1L))
                .thenReturn(Optional.of(inspection));

        when(inspectionRepository.save(any()))
                .thenAnswer(i -> i.getArgument(0));

        InspectionResponse response =
                inspectionService.cancelInspection(1L);

        assertNotNull(response);

        assertEquals(
                InspectionStatus.CANCELLED,
                inspection.getStatus());

        verify(permitService)
                .updateStatus(
                        eq(1L),
                        eq(PermitStatus.UNDER_REVIEW),
                        anyString());
    }

    @Test
    void cancelInspection_InvalidStatus() {

        inspection.setStatus(
                InspectionStatus.COMPLETED);

        when(inspectionRepository.findById(1L))
                .thenReturn(Optional.of(inspection));

        assertThrows(
                BadRequestException.class,
                () -> inspectionService.cancelInspection(
                        1L)
        );
    }

    @Test
    void conductInspection_Success() {

        ConductInspectionRequest request =
                new ConductInspectionRequest();

        request.setOutcome(
                InspectionOutcome.PASS);

        request.setConductedDate(
                LocalDate.now());

        request.setRemarks(
                "Site verified");

        request.setGeoCoordinates(
                "13.0827,80.2707");

        when(inspectionRepository.findById(1L))
                .thenReturn(Optional.of(inspection));

        when(inspectionRepository.save(any()))
                .thenReturn(inspection);

        InspectionResponse response =
                inspectionService.conductInspection(
                        1L,
                        request,
                        500L);

        assertNotNull(response);

        verify(inspectionRepository,
                times(1))
                .save(any());

        verify(permitService,
                times(1))
                .applyInspectionOutcome(
                        1L,
                        InspectionOutcome.PASS);

        verify(notificationClient,
                times(1))
                .sendNotification(any());
    }

    @Test
    void conductInspection_ForbiddenOfficer() {

        ConductInspectionRequest request =
                new ConductInspectionRequest();

        request.setOutcome(
                InspectionOutcome.PASS);

        request.setConductedDate(
                LocalDate.now());

        when(inspectionRepository.findById(1L))
                .thenReturn(Optional.of(inspection));

        assertThrows(
                ForbiddenException.class,
                () -> inspectionService.conductInspection(
                        1L,
                        request,
                        999L)
        );
    }

    @Test
    void conductInspection_InvalidStatus() {

        inspection.setStatus(
                InspectionStatus.COMPLETED);

        ConductInspectionRequest request =
                new ConductInspectionRequest();

        request.setOutcome(
                InspectionOutcome.PASS);

        request.setConductedDate(
                LocalDate.now());

        when(inspectionRepository.findById(1L))
                .thenReturn(Optional.of(inspection));

        assertThrows(
                BadRequestException.class,
                () -> inspectionService.conductInspection(
                        1L,
                        request,
                        500L)
        );
    }

    @Test
    void getByPermitId_Success() {

        when(permitService.getEntityById(1L))
                .thenReturn(permit);

        when(inspectionRepository
                .findByPermitApplication_PermitId(1L))
                .thenReturn(List.of(inspection));

        List<InspectionResponse> result =
                inspectionService.getByPermitId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getAssignedToMe_Success() {

        when(inspectionRepository
                .findByAssignedOfficerId(500L))
                .thenReturn(List.of(inspection));

        List<InspectionResponse> result =
                inspectionService.getAssignedToMe(
                        500L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getScheduledAssignedToMe_Success() {

        when(inspectionRepository
                .findByAssignedOfficerIdAndStatus(
                        500L,
                        InspectionStatus.SCHEDULED))
                .thenReturn(List.of(inspection));

        List<InspectionResponse> result =
                inspectionService.getScheduledAssignedToMe(
                        500L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void getById_Success() {

        when(inspectionRepository.findById(1L))
                .thenReturn(Optional.of(inspection));

        InspectionResponse response =
                inspectionService.getById(1L);

        assertNotNull(response);

        assertEquals(
                1L,
                response.getInspectionId());
    }

    @Test
    void getById_NotFound() {

        when(inspectionRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> inspectionService.getById(99L)
        );
    }

    @Test
    void getAll_Success() {

        when(inspectionRepository.findAll())
                .thenReturn(List.of(inspection));

        List<InspectionResponse> result =
                inspectionService.getAll();

        assertEquals(1, result.size());
    }

    @Test
    void getByStatus_Success() {

        when(inspectionRepository.findByStatus(
                InspectionStatus.SCHEDULED))
                .thenReturn(List.of(inspection));

        List<InspectionResponse> result =
                inspectionService.getByStatus(
                        InspectionStatus.SCHEDULED);

        assertEquals(1, result.size());
    }
}