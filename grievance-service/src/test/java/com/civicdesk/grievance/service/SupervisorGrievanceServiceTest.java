package com.civicdesk.grievance.service;

import com.civicdesk.grievance.client.AuthClient;
import com.civicdesk.grievance.dto.request.AssignFieldOfficerReq;
import com.civicdesk.grievance.dto.request.ResolveReq;
import com.civicdesk.grievance.dto.response.ApiResponse;
import com.civicdesk.grievance.dto.response.UserResponse;
import com.civicdesk.grievance.entity.Grievance;
import com.civicdesk.grievance.enums.EscalationLevel;
import com.civicdesk.grievance.enums.GrievanceStatus;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupervisorGrievanceServiceTest {

    @Mock
    private GrievanceRepo grievanceRepo;

    @Mock
    private GrievanceActionRepo grievanceActionRepo;

    @Mock
    private GrievanceMapper mapper;

    @Mock
    private AuthClient authClient;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private AuditHelperService auditHelperService;

    @Mock
    private NotificationHelperService notificationHelperService;

    @InjectMocks
    private SupervisorGrievanceService service;

    @Test
    void getDepartmentGrievances_Success() {

        UserResponse supervisor = new UserResponse();
        supervisor.setDepartmentId("DPT01");

        try (MockedStatic<JwtUserContext> jwt =
                     mockStatic(JwtUserContext.class)) {

            jwt.when(JwtUserContext::getCurrentUserId)
                    .thenReturn("1001");

            when(authClient.getUserById("1001"))
                    .thenReturn(ApiResponse.data(supervisor));

            when(objectMapper.convertValue(
                    any(),
                    eq(UserResponse.class)))
                    .thenReturn(supervisor);

            when(grievanceRepo.findByDepartmentId("DPT01"))
                    .thenReturn(List.of(new Grievance()));

            service.getDepartmentGrievances();

            verify(grievanceRepo)
                    .findByDepartmentId("DPT01");
        }
    }

    @Test
    void assignFieldOfficer_Success() {

        UserResponse supervisor = new UserResponse();
        supervisor.setDepartmentId("DPT01");

        UserResponse officer = new UserResponse();
        officer.setDepartmentId("DPT01");
        officer.setRole("FO");
        officer.setStatus("A");

        Grievance grievance = new Grievance();
        grievance.setGrievanceId("3001");
        grievance.setDepartmentId("DPT01");
        grievance.setStatus(GrievanceStatus.O);
        grievance.setEscalationLevel(EscalationLevel.L2);

        AssignFieldOfficerReq req =
                new AssignFieldOfficerReq();

        // IMPORTANT:
        // Service calls Long.valueOf(fieldOfficerId)
        req.setFieldOfficerId("2001");
        req.setMessage("Take action");

        try (MockedStatic<JwtUserContext> jwt =
                     mockStatic(JwtUserContext.class)) {

            jwt.when(JwtUserContext::getCurrentUserId)
                    .thenReturn("1001");

            when(authClient.getUserById("1001"))
                    .thenReturn(ApiResponse.data(supervisor));

            when(authClient.getUserById("2001"))
                    .thenReturn(ApiResponse.data(officer));

            when(objectMapper.convertValue(
                    any(),
                    eq(UserResponse.class)))
                    .thenReturn(supervisor)
                    .thenReturn(officer);

            when(grievanceRepo.findById("3001"))
                    .thenReturn(Optional.of(grievance));

            when(grievanceRepo.save(any(Grievance.class)))
                    .thenReturn(grievance);

            service.assignFieldOfficer(
                    "3001",
                    req);

            verify(grievanceRepo)
                    .save(any(Grievance.class));

            verify(auditHelperService)
                    .log("ASSIGN_FIELD_OFFICER");
        }
    }

    @Test
    void resolveGrievance_Success() {

        UserResponse supervisor = new UserResponse();
        supervisor.setDepartmentId("DPT01");

        Grievance grievance = new Grievance();
        grievance.setGrievanceId("3001");
        grievance.setCitizenId("5001");
        grievance.setDepartmentId("DPT01");
        grievance.setStatus(GrievanceStatus.IP);
        grievance.setEscalationLevel(EscalationLevel.L2);

        ResolveReq req = new ResolveReq();
        req.setMessage("Resolved");

        try (MockedStatic<JwtUserContext> jwt =
                     mockStatic(JwtUserContext.class)) {

            jwt.when(JwtUserContext::getCurrentUserId)
                    .thenReturn("1001");

            when(authClient.getUserById("1001"))
                    .thenReturn(ApiResponse.data(supervisor));

            when(objectMapper.convertValue(
                    any(),
                    eq(UserResponse.class)))
                    .thenReturn(supervisor);

            when(grievanceRepo.findById("3001"))
                    .thenReturn(Optional.of(grievance));

            // IMPORTANT
            when(grievanceRepo.save(any(Grievance.class)))
                    .thenReturn(grievance);

            service.resolveGrievance(
                    "3001",
                    req);

            verify(grievanceRepo)
                    .save(any(Grievance.class));

            verify(auditHelperService)
                    .log("RESOLVE_GRIEVANCE");
        }
    }
}