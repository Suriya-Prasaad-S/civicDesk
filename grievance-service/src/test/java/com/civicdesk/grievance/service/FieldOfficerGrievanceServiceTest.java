package com.civicdesk.grievance.service;

import com.civicdesk.grievance.client.AuthClient;
import com.civicdesk.grievance.dto.request.GrievanceActionCreateReq;
import com.civicdesk.grievance.dto.request.GrievanceActionUpdateReq;
import com.civicdesk.grievance.dto.response.ApiResponse;
import com.civicdesk.grievance.dto.response.DepartmentResponse;
import com.civicdesk.grievance.entity.Grievance;
import com.civicdesk.grievance.entity.GrievanceAction;
import com.civicdesk.grievance.enums.ActionStatus;
import com.civicdesk.grievance.enums.ActionType;
import com.civicdesk.grievance.enums.EscalationLevel;
import com.civicdesk.grievance.enums.GrievanceStatus;
import com.civicdesk.grievance.exception.InvalidGrievanceStateException;
import com.civicdesk.grievance.mapper.GrievanceMapper;
import com.civicdesk.grievance.repository.GrievanceActionRepo;
import com.civicdesk.grievance.repository.GrievanceRepo;
import com.civicdesk.grievance.security.JwtUserContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FieldOfficerGrievanceServiceTest {

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
    private FieldOfficerGrievanceService service;

    @Test
    void getAssignedGrievances_Success() {

        try (MockedStatic<JwtUserContext> jwt =
                     mockStatic(JwtUserContext.class)) {

            jwt.when(JwtUserContext::getCurrentUserId)
                    .thenReturn("FO1");

            when(grievanceRepo.findByAssignedToId("FO1"))
                    .thenReturn(List.of(new Grievance()));

            service.getAssignedGrievances();

            verify(grievanceRepo)
                    .findByAssignedToId("FO1");
        }
    }

    @Test
    void createGrievanceAction_Success() {

        Grievance grievance = new Grievance();
        grievance.setGrievanceId("3001");
        grievance.setAssignedToId("FO1");
        grievance.setStatus(GrievanceStatus.IP);

        when(grievanceRepo.findById("3001"))
                .thenReturn(Optional.of(grievance));

        when(grievanceActionRepo.findByGrievanceIdOrderByActionDateAsc("3001"))
                .thenReturn(List.of());

        when(grievanceActionRepo.save(any()))
                .thenReturn(new GrievanceAction());

        GrievanceActionCreateReq req =
                new GrievanceActionCreateReq();

        req.setGrievanceActionTitle("work");
        req.setActionDescription("started");

        try (MockedStatic<JwtUserContext> jwt =
                     mockStatic(JwtUserContext.class)) {

            jwt.when(JwtUserContext::getCurrentUserId)
                    .thenReturn("FO1");

            service.createGrievanceAction(
                    "3001",
                    req);

            verify(grievanceActionRepo)
                    .save(any());

            verify(auditHelperService)
                    .log("CREATE_GRIEVANCE_ACTION");
        }
    }

    @Test
    void createGrievanceAction_OpenWorkExists() {

        Grievance grievance = new Grievance();
        grievance.setAssignedToId("FO1");
        grievance.setStatus(GrievanceStatus.IP);

        GrievanceAction action =
                new GrievanceAction();

        action.setActionType(ActionType.WK);
        action.setStatus(ActionStatus.IP);

        when(grievanceRepo.findById("3001"))
                .thenReturn(Optional.of(grievance));

        when(grievanceActionRepo.findByGrievanceIdOrderByActionDateAsc("3001"))
                .thenReturn(List.of(action));

        try (MockedStatic<JwtUserContext> jwt =
                     mockStatic(JwtUserContext.class)) {

            jwt.when(JwtUserContext::getCurrentUserId)
                    .thenReturn("FO1");

            assertThrows(
                    InvalidGrievanceStateException.class,
                    () -> service.createGrievanceAction(
                            "3001",
                            new GrievanceActionCreateReq()));
        }
    }

    @Test
    void updateGrievanceAction_CompleteFlow() {

        Grievance grievance = new Grievance();

        grievance.setGrievanceId("3001");
        grievance.setAssignedToId("FO1");
        grievance.setDepartmentId("DPT01");
        grievance.setEscalationLevel(EscalationLevel.L1);
        grievance.setStatus(GrievanceStatus.IP);

        GrievanceAction action =
                new GrievanceAction();

        action.setActionId("4001");
        action.setGrievanceId("3001");
        action.setTakenById("FO1");
        action.setActionType(ActionType.WK);
        action.setStatus(ActionStatus.IP);

        when(grievanceActionRepo.findById("4001"))
                .thenReturn(Optional.of(action));

        when(grievanceRepo.findById("3001"))
                .thenReturn(Optional.of(grievance));

        when(grievanceActionRepo.findByGrievanceIdOrderByActionDateAsc("3001"))
                .thenReturn(List.of(action));

        ApiResponse response =
                ApiResponse.data(
                        new DepartmentResponse(
                                "DPT01",
                                "Infra",
                                "9999"));

        when(authClient.getDepartmentById("DPT01"))
                .thenReturn(response);

        when(objectMapper.convertValue(
                any(),
                eq(DepartmentResponse.class)))
                .thenReturn(
                        new DepartmentResponse(
                                "DPT01",
                                "Infra",
                                "9999"));

        GrievanceActionUpdateReq req =
                new GrievanceActionUpdateReq();

        req.setStatus("CM");

        try (MockedStatic<JwtUserContext> jwt =
                     mockStatic(JwtUserContext.class)) {

            jwt.when(JwtUserContext::getCurrentUserId)
                    .thenReturn("FO1");

            service.updateGrievanceAction(
                    "4001",
                    req);

            verify(auditHelperService)
                    .log("COMPLETE_GRIEVANCE_ACTION");

            verify(notificationHelperService)
                    .notify(
                            anyLong(),
                            anyString(),
                            anyString(),
                            any(),
                            anyLong(),
                            any());
        }
    }
}