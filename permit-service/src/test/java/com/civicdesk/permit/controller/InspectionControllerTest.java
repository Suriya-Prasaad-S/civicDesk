package com.civicdesk.permit.controller;

import com.civicdesk.permit.dto.ConductInspectionRequest;
import com.civicdesk.permit.dto.InspectionResponse;
import com.civicdesk.permit.dto.ScheduleInspectionRequest;
import com.civicdesk.permit.enums.InspectionOutcome;
import com.civicdesk.permit.enums.InspectionStatus;
import com.civicdesk.permit.security.JwtUserContext;
import com.civicdesk.permit.service.InspectionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InspectionControllerTest {

    @Mock
    private InspectionService inspectionService;

    @InjectMocks
    private InspectionController inspectionController;

    @Test
    void schedule_Success() {

        ScheduleInspectionRequest request =
                new ScheduleInspectionRequest();

        request.setOfficerId(500L);
        request.setScheduledDate(
                LocalDate.now().plusDays(2));

        ResponseEntity<Map<String,String>> response =
                inspectionController.schedule(
                        1L,
                        request);

        assertEquals(
                HttpStatus.CREATED,
                response.getStatusCode());

        verify(inspectionService)
                .scheduleInspection(
                        1L,
                        request);
    }

    @Test
    void getByPermit_Success() {

        when(inspectionService.getByPermitId(1L))
                .thenReturn(List.of());

        ResponseEntity<?> response =
                inspectionController.getByPermit(1L);

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode());
    }

    @Test
    void getMyInspections_Success() {

        try (MockedStatic<JwtUserContext> jwt =
                     mockStatic(JwtUserContext.class)) {

            jwt.when(JwtUserContext::getCurrentUserId)
                    .thenReturn(500L);

            when(inspectionService.getAssignedToMe(500L))
                    .thenReturn(List.of());

            ResponseEntity<?> response =
                    inspectionController.getMyInspections();

            assertEquals(
                    HttpStatus.OK,
                    response.getStatusCode());
        }
    }

    @Test
    void getById_Success() {

        InspectionResponse inspection =
                InspectionResponse.builder()
                        .inspectionId(1L)
                        .status(InspectionStatus.SCHEDULED)
                        .build();

        when(inspectionService.getById(1L))
                .thenReturn(inspection);

        ResponseEntity<?> response =
                inspectionController.getById(1L);

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode());
    }

    @Test
    void conduct_Success() {

        InspectionResponse inspection =
                InspectionResponse.builder()
                        .inspectionId(1L)
                        .status(InspectionStatus.COMPLETED)
                        .build();

        when(inspectionService.conductInspection(
                anyLong(),
                any(ConductInspectionRequest.class),
                anyLong()))
                .thenReturn(inspection);

        try (MockedStatic<JwtUserContext> jwt =
                     mockStatic(JwtUserContext.class)) {

            jwt.when(JwtUserContext::getCurrentUserId)
                    .thenReturn(500L);

            ResponseEntity<?> response =
                    inspectionController.conduct(
                            1L,
                            "PASS",
                            "Inspection completed",
                            "13.0827,80.2707",
                            null);

            assertEquals(
                    HttpStatus.OK,
                    response.getStatusCode());

            verify(inspectionService)
                    .conductInspection(
                            eq(1L),
                            any(ConductInspectionRequest.class),
                            eq(500L));
        }
    }
}