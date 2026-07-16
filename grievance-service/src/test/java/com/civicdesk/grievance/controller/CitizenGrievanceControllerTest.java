package com.civicdesk.grievance.controller;

import com.civicdesk.grievance.dto.request.GrievanceCreateReq;
import com.civicdesk.grievance.dto.request.GrievanceDetailsUpdateReq;
import com.civicdesk.grievance.dto.request.GrievanceReopenReq;
import com.civicdesk.grievance.service.CitizenGrievanceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CitizenGrievanceControllerTest {

    @Mock
    private CitizenGrievanceService citizenGrievanceService;

    @InjectMocks
    private CitizenGrievanceController controller;

    @Test
    void createGrievance_Success() {

        GrievanceCreateReq req = new GrievanceCreateReq();
        req.setCategory("RI");
        req.setGrievanceTitle("Road issue");
        req.setDescription("Bad road");

        ResponseEntity<?> response =
                controller.createGrievance(req);

        assertEquals(
                HttpStatus.CREATED,
                response.getStatusCode());

        verify(citizenGrievanceService)
                .createGrievance(req);
    }

    @Test
    void updateGrievanceDetails_Success() {

        GrievanceDetailsUpdateReq req =
                new GrievanceDetailsUpdateReq();

        req.setGrievanceTitle("Updated");
        req.setDescription("Updated desc");

        ResponseEntity<?> response =
                controller.updateGrievanceDetails(
                        "30000001",
                        req);

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode());

        verify(citizenGrievanceService)
                .updateGrievanceDetails(
                        "30000001",
                        req);
    }

    @Test
    void getMyGrievances_Success() {

        ResponseEntity<?> response =
                controller.getMyGrievances();

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode());

        verify(citizenGrievanceService)
                .getMyGrievances();
    }

    @Test
    void getGrievanceById_Success() {

        ResponseEntity<?> response =
                controller.getGrievanceById(
                        "30000001");

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode());

        verify(citizenGrievanceService)
                .getGrievanceById("30000001");
    }

    @Test
    void closeGrievance_Success() {

        ResponseEntity<?> response =
                controller.closeGrievance(
                        "30000001");

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode());

        verify(citizenGrievanceService)
                .closeGrievance("30000001");
    }

    @Test
    void reopenGrievance_Success() {

        GrievanceReopenReq req =
                new GrievanceReopenReq();

        req.setReason("Not fixed");

        ResponseEntity<?> response =
                controller.reopenGrievance(
                        "30000001",
                        req);

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode());

        verify(citizenGrievanceService)
                .reopenGrievance(
                        "30000001",
                        req);
    }
}