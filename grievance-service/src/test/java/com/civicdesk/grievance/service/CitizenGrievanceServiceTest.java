package com.civicdesk.grievance.service;

import com.civicdesk.grievance.client.AuthClient;
import com.civicdesk.grievance.dto.request.GrievanceCreateReq;
import com.civicdesk.grievance.dto.request.GrievanceDetailsUpdateReq;
import com.civicdesk.grievance.dto.request.GrievanceReopenReq;
import com.civicdesk.grievance.dto.response.ApiResponse;
import com.civicdesk.grievance.dto.response.DepartmentResponse;
import com.civicdesk.grievance.entity.Grievance;
import com.civicdesk.grievance.enums.Category;
import com.civicdesk.grievance.enums.GrievanceStatus;
import com.civicdesk.grievance.exception.InvalidGrievanceStateException;
import com.civicdesk.grievance.mapper.GrievanceMapper;
import com.civicdesk.grievance.repository.GrievanceActionRepo;
import com.civicdesk.grievance.repository.GrievanceRepo;
import com.civicdesk.grievance.security.JwtUserContext;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CitizenGrievanceServiceTest {

    @Mock
    private GrievanceRepo grievanceRepo;

    @Mock
    private GrievanceActionRepo grievanceActionRepo;

    @Mock
    private AuthClient authClient;

    @Mock
    private GrievanceMapper mapper;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private AuditHelperService auditHelperService;

    @Mock
    private NotificationHelperService notificationHelperService;

    @InjectMocks
    private CitizenGrievanceService service;

    private Grievance grievance;

    @BeforeEach
    void setUp() {

        grievance = new Grievance();

        grievance.setGrievanceId("30000001");
        grievance.setCitizenId("1001");
        grievance.setDepartmentId("DPT01");
        grievance.setStatus(GrievanceStatus.O);
        grievance.setCategory(Category.RI);
        grievance.setSubmissionDate(LocalDateTime.now());
    }

    @Test
    void createGrievance_Success() {

        GrievanceCreateReq req =
                new GrievanceCreateReq();

        req.setCategory("RI");
        req.setGrievanceTitle("Road");
        req.setDescription("Broken road");

        DepartmentResponse department =
                new DepartmentResponse(
                        "DPT01",
                        "Infrastructure",
                        "5001");

        ApiResponse apiResponse =
                ApiResponse.data(department);

        when(authClient.getDepartmentById("DPT01"))
                .thenReturn(apiResponse);

        when(objectMapper.convertValue(
                any(),
                eq(DepartmentResponse.class)))
                .thenReturn(department);

        when(grievanceRepo.save(any()))
                .thenReturn(grievance);

        try (MockedStatic<JwtUserContext> jwt =
                     mockStatic(JwtUserContext.class)) {

            jwt.when(JwtUserContext::getCurrentUserId)
                    .thenReturn("1001");

            service.createGrievance(req);

            verify(grievanceRepo)
                    .save(any());

            verify(notificationHelperService)
                    .notify(
                            anyLong(),
                            anyString(),
                            anyString(),
                            any(),
                            anyLong(),
                            any());

            verify(auditHelperService)
                    .log("CREATE_GRIEVANCE");
        }
    }

    @Test
    void updateGrievanceDetails_Success() {

        GrievanceDetailsUpdateReq req =
                new GrievanceDetailsUpdateReq();

        req.setGrievanceTitle("Updated");
        req.setDescription("Updated Desc");

        when(grievanceRepo.findById("30000001"))
                .thenReturn(Optional.of(grievance));

        when(grievanceRepo.save(any()))
                .thenReturn(grievance);

        try (MockedStatic<JwtUserContext> jwt =
                     mockStatic(JwtUserContext.class)) {

            jwt.when(JwtUserContext::getCurrentUserId)
                    .thenReturn("1001");

            service.updateGrievanceDetails(
                    "30000001",
                    req);

            verify(grievanceRepo)
                    .save(any());

            verify(auditHelperService)
                    .log("UPDATE_GRIEVANCE");
        }
    }

    @Test
    void updateGrievanceDetails_InvalidStatus() {

        grievance.setStatus(
                GrievanceStatus.R);

        when(grievanceRepo.findById("30000001"))
                .thenReturn(Optional.of(grievance));

        try (MockedStatic<JwtUserContext> jwt =
                     mockStatic(JwtUserContext.class)) {

            jwt.when(JwtUserContext::getCurrentUserId)
                    .thenReturn("1001");

            assertThrows(
                    InvalidGrievanceStateException.class,
                    () -> service.updateGrievanceDetails(
                            "30000001",
                            new GrievanceDetailsUpdateReq()));
        }
    }

    @Test
    void closeGrievance_Success() {

        grievance.setStatus(
                GrievanceStatus.R);

        when(grievanceRepo.findById("30000001"))
                .thenReturn(Optional.of(grievance));

        when(grievanceRepo.save(any()))
                .thenReturn(grievance);

        try (MockedStatic<JwtUserContext> jwt =
                     mockStatic(JwtUserContext.class)) {

            jwt.when(JwtUserContext::getCurrentUserId)
                    .thenReturn("1001");

            service.closeGrievance("30000001");

            verify(grievanceRepo)
                    .save(any());

            verify(auditHelperService)
                    .log("CLOSE_GRIEVANCE");
        }
    }

    @Test
    void reopenGrievance_Success() {

        grievance.setStatus(
                GrievanceStatus.R);

        DepartmentResponse department =
                new DepartmentResponse(
                        "DPT01",
                        "Infra",
                        "5001");

        ApiResponse apiResponse =
                ApiResponse.data(department);

        when(grievanceRepo.findById("30000001"))
                .thenReturn(Optional.of(grievance));

        when(authClient.getDepartmentById("DPT01"))
                .thenReturn(apiResponse);

        when(objectMapper.convertValue(
                any(),
                eq(DepartmentResponse.class)))
                .thenReturn(department);

        when(grievanceRepo.save(any()))
                .thenReturn(grievance);

        GrievanceReopenReq req =
                new GrievanceReopenReq();

        req.setReason("Not fixed");

        try (MockedStatic<JwtUserContext> jwt =
                     mockStatic(JwtUserContext.class)) {

            jwt.when(JwtUserContext::getCurrentUserId)
                    .thenReturn("1001");

            service.reopenGrievance(
                    "30000001",
                    req);

            verify(grievanceRepo)
                    .save(any());

            verify(auditHelperService)
                    .log("REOPEN_GRIEVANCE");
        }
    }
}