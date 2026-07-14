package com.civicdesk.permit.controller;


import com.civicdesk.permit.dto.PermitApplicationRequest;
import com.civicdesk.permit.dto.PermitApplicationResponse;
import com.civicdesk.permit.dto.RenewPermitRequest;
import com.civicdesk.permit.enums.PermitStatus;
import com.civicdesk.permit.enums.PermitType;
import com.civicdesk.permit.security.JwtUserContext;
import com.civicdesk.permit.service.PermitService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermitControllerTest {
    @Mock
    private PermitService permitService;

    @InjectMocks
    private PermitController permitController;

    @Test
    void apply_Success() {

        PermitApplicationRequest request = new PermitApplicationRequest();

        request.setCitizenId(100L);
        request.setPermitType(PermitType.TRADE_LICENSE);
        request.setPropertyAddress("Tambaram");
        request.setValidityPeriod(12);
        request.setFee(BigDecimal.valueOf(5000));

        try (MockedStatic<JwtUserContext> jwt =
                     mockStatic(JwtUserContext.class)) {

            jwt.when(JwtUserContext::getCurrentUserId)
                    .thenReturn(100L);

            ResponseEntity<Map<String, String>> response =
                    permitController.apply(request);

            assertEquals(
                    HttpStatus.CREATED,
                    response.getStatusCode());

            assertEquals(
                    "Permit application created successfully",
                    response.getBody().get("message"));

            verify(permitService)
                    .applyForPermit(request, 100L);
        }
    }

    @Test
    void getAll_Citizen_Success() {

        PermitApplicationResponse permit =
                PermitApplicationResponse.builder()
                        .permitId(1L)
                        .permitType(PermitType.TRADE_LICENSE)
                        .build();

        when(permitService.getMyPermits(100L))
                .thenReturn(List.of(permit));

        try (MockedStatic<JwtUserContext> jwt =
                     mockStatic(JwtUserContext.class)) {

            jwt.when(JwtUserContext::getCurrentUserId)
                    .thenReturn(100L);

            jwt.when(JwtUserContext::getCurrentRole)
                    .thenReturn("CIT");

            ResponseEntity<?> response =
                    permitController.getAll(
                            null,
                            null);

            assertEquals(
                    HttpStatus.OK,
                    response.getStatusCode());
        }
    }

    @Test
    void getById_Success() {

        PermitApplicationResponse permit =
                PermitApplicationResponse.builder()
                        .permitId(1L)
                        .permitType(PermitType.TRADE_LICENSE)
                        .status(PermitStatus.APPLIED)
                        .build();

        when(permitService.getById(
                anyLong(),
                anyLong(),
                anyString()))
                .thenReturn(permit);

        try (MockedStatic<JwtUserContext> jwt =
                     mockStatic(JwtUserContext.class)) {

            jwt.when(JwtUserContext::getCurrentUserId)
                    .thenReturn(100L);

            jwt.when(JwtUserContext::getCurrentRole)
                    .thenReturn("ADM");

            ResponseEntity<?> response =
                    permitController.getById(1L);

            assertEquals(
                    HttpStatus.OK,
                    response.getStatusCode());
        }
    }

    @Test
    void uploadDocument_Success() {

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "permit.pdf",
                        "application/pdf",
                        "dummy".getBytes());

        ResponseEntity<?> response =
                permitController.uploadDocument(
                        1L,
                        List.of("IDProof"),
                        List.of(file));

        assertEquals(
                HttpStatus.CREATED,
                response.getStatusCode());

        verify(permitService)
                .uploadDocuments(
                        eq(1L),
                        anyList(),
                        anyList());
    }

    @Test
    void uploadDocument_InvalidCounts() {

        MockMultipartFile file =
                new MockMultipartFile(
                        "file",
                        "permit.pdf",
                        "application/pdf",
                        "dummy".getBytes());

        ResponseEntity<?> response =
                permitController.uploadDocument(
                        1L,
                        List.of("IDProof", "NOC"),
                        List.of(file));

        assertEquals(
                HttpStatus.BAD_REQUEST,
                response.getStatusCode());
    }

    @Test
    void getDocuments_Success() {

        when(permitService.getDocuments(1L))
                .thenReturn(List.of());

        ResponseEntity<?> response =
                permitController.getDocuments(1L);

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode());
    }

    @Test
    void renew_Success() {

        RenewPermitRequest request =
                new RenewPermitRequest();

        request.setValidityPeriod(24);

        try (MockedStatic<JwtUserContext> jwt =
                     mockStatic(JwtUserContext.class)) {

            jwt.when(JwtUserContext::getCurrentUserId)
                    .thenReturn(100L);

            ResponseEntity<Map<String, String>> response =
                    permitController.renew(
                            1L,
                            request);

            assertEquals(
                    HttpStatus.OK,
                    response.getStatusCode());

            assertEquals(
                    "Permit renewal submitted.",
                    response.getBody().get("message"));

            verify(permitService)
                    .renewPermit(
                            1L,
                            request,
                            100L);
        }
    }

    @Test
    void getQueue_Success() {

        when(permitService.getAll())
                .thenReturn(List.of());

        ResponseEntity<?> response =
                permitController.getQueue(null);

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode());
    }

    @Test
    void makeDecision_Success() {

        ResponseEntity<Map<String, String>> response =
                permitController.makeDecision(
                        1L,
                        Map.of(
                                "decision",
                                "APPROVED"));

        assertEquals(
                HttpStatus.OK,
                response.getStatusCode());

        verify(permitService)
                .updateStatus(
                        eq(1L),
                        eq(PermitStatus.APPROVED),
                        any());
    }
}