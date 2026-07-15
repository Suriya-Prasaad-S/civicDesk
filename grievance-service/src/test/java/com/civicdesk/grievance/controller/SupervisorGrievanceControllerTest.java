package com.civicdesk.grievance.controller;

import com.civicdesk.grievance.dto.request.AssignFieldOfficerReq;
import com.civicdesk.grievance.dto.request.GrievanceAnalyticsRequest;
import com.civicdesk.grievance.dto.request.ResolveReq;
import com.civicdesk.grievance.service.CitizenGrievanceService;
import com.civicdesk.grievance.service.SupervisorGrievanceService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SupervisorGrievanceControllerTest {

    @Mock
    private SupervisorGrievanceService supervisorService;

    @Mock
    private CitizenGrievanceService citizenService;

    @InjectMocks
    private SupervisorGrievanceController controller;

    @Test
    void getDepartmentGrievances_Success() {

        ResponseEntity<?> response =
                controller.getDepartmentGrievances();

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode());

        verify(supervisorService)
                .getDepartmentGrievances();
    }

    @Test
    void assignFieldOfficer_Success() {

        AssignFieldOfficerReq req =
                new AssignFieldOfficerReq();

        req.setFieldOfficerId("2001");
        req.setMessage("Take action");

        ResponseEntity<?> response =
                controller.assignFieldOfficer(
                        "30000001",
                        req);

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode());

        verify(supervisorService)
                .assignFieldOfficer(
                        "30000001",
                        req);
    }

    @Test
    void resolveGrievance_Success() {

        ResolveReq req = new ResolveReq();
        req.setMessage("Resolved");

        ResponseEntity<?> response =
                controller.resolveGrievance(
                        "30000001",
                        req);

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode());

        verify(supervisorService)
                .resolveGrievance(
                        "30000001",
                        req);
    }

    @Test
    void viewDepartmentGrievance_Success() {

        ResponseEntity<?> response =
                controller.viewDepartmentGrievance(
                        "30000001");

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode());

        verify(supervisorService)
                .viewDepartmentGrievance(
                        "30000001");
    }

    @Test
    void getAnalytics_Success() {

        GrievanceAnalyticsRequest req =
                new GrievanceAnalyticsRequest(
                        LocalDateTime.now().minusDays(10),
                        LocalDateTime.now(),
                        "DPT01");

        ResponseEntity<?> response =
                controller.getGrievanceAnalytics(req);

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode());

        verify(citizenService)
                .getGrievanceAnalytics(req);
    }
}