package com.civicdesk.grievance.controller;

import com.civicdesk.grievance.dto.request.GrievanceActionCreateReq;
import com.civicdesk.grievance.dto.request.GrievanceActionUpdateReq;
import com.civicdesk.grievance.service.FieldOfficerGrievanceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FieldOfficerGrievanceControllerTest {

    @Mock
    private FieldOfficerGrievanceService fieldOfficerGrievanceService;

    @InjectMocks
    private FieldOfficerGrievanceController controller;

    @Test
    void getAssignedGrievances_Success() {

        ResponseEntity<?> response =
                controller.getAssignedGrievances();

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode());

        verify(fieldOfficerGrievanceService)
                .getAssignedGrievances();
    }

    @Test
    void viewAssignedGrievance_Success() {

        ResponseEntity<?> response =
                controller.viewAssignedGrievance("30000001");

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode());

        verify(fieldOfficerGrievanceService)
                .viewAssignedGrievance("30000001");
    }

    @Test
    void createGrievanceAction_Success() {

        GrievanceActionCreateReq req =
                new GrievanceActionCreateReq();

        req.setGrievanceActionTitle("Fix work");
        req.setActionDescription("Started");

        ResponseEntity<?> response =
                controller.createGrievanceAction(
                        "30000001",
                        req);

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode());

        verify(fieldOfficerGrievanceService)
                .createGrievanceAction(
                        "30000001",
                        req);
    }

    @Test
    void updateGrievanceAction_Success() {

        GrievanceActionUpdateReq req =
                new GrievanceActionUpdateReq();

        req.setStatus("IP");

        ResponseEntity<?> response =
                controller.updateGrievanceAction(
                        "40000001",
                        req);

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode());

        verify(fieldOfficerGrievanceService)
                .updateGrievanceAction(
                        "40000001",
                        req);
    }
}