package com.civicdesk.permit.service;

import com.civicdesk.permit.client.NotificationClient;
import com.civicdesk.permit.dto.PermitApplicationRequest;
import com.civicdesk.permit.dto.PermitApplicationResponse;
import com.civicdesk.permit.dto.RenewPermitRequest;
import com.civicdesk.permit.entity.PermitApplication;
import com.civicdesk.permit.enums.PermitStatus;
import com.civicdesk.permit.enums.PermitType;
import com.civicdesk.permit.exception.BadRequestException;
import com.civicdesk.permit.exception.ForbiddenException;
import com.civicdesk.permit.repository.InspectionRepository;
import com.civicdesk.permit.repository.PermitApplicationRepository;
import com.civicdesk.permit.repository.PermitDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermitServiceTest {

    @Mock
    private PermitApplicationRepository permitRepository;

    @Mock
    private PermitDocumentRepository documentRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private InspectionRepository inspectionRepository;

    @Mock
    private NotificationClient notificationClient;

    @InjectMocks
    private PermitService permitService;

    private PermitApplication permit;

    @BeforeEach
    void setUp() {

        permit = PermitApplication.builder()
                .permitId(1L)
                .citizenId(100L)
                .userId(100L)
                .permitType(PermitType.TRADE_LICENSE)
                .applicationDate(LocalDate.now())
                .propertyAddress("Tambaram")
                .validityPeriod(12)
                .fee(BigDecimal.valueOf(5000))
                .status(PermitStatus.APPLIED)
                .departmentId(1L)
                .build();
    }

    @Test
    void applyForPermit_Success() {

        PermitApplicationRequest request =
                new PermitApplicationRequest();

        request.setCitizenId(100L);
        request.setPermitType(PermitType.TRADE_LICENSE);
        request.setPropertyAddress("Tambaram");
        request.setValidityPeriod(12);
        request.setFee(BigDecimal.valueOf(5000));

        when(permitRepository.save(any(PermitApplication.class)))
                .thenReturn(permit);

        PermitApplicationResponse response =
                permitService.applyForPermit(request, 100L);

        assertNotNull(response);
        assertEquals(PermitType.TRADE_LICENSE,
                response.getPermitType());

        verify(permitRepository, times(1))
                .save(any(PermitApplication.class));

        verify(notificationClient, times(1))
                .sendNotification(any());
    }

    @Test
    void getById_Success() {

        when(permitRepository.findById(1L))
                .thenReturn(Optional.of(permit));

        PermitApplicationResponse response =
                permitService.getById(
                        1L,
                        999L,
                        "ADM");

        assertNotNull(response);
        assertEquals(1L,
                response.getPermitId());
    }

    @Test
    void getById_Citizen_OwnPermit_Success() {

        when(permitRepository.findById(1L))
                .thenReturn(Optional.of(permit));

        PermitApplicationResponse response =
                permitService.getById(
                        1L,
                        100L,
                        "CIT");

        assertNotNull(response);
    }

    @Test
    void getById_Citizen_Forbidden() {

        when(permitRepository.findById(1L))
                .thenReturn(Optional.of(permit));

        assertThrows(
                ForbiddenException.class,
                () -> permitService.getById(
                        1L,
                        999L,
                        "CIT")
        );
    }

    @Test
    void getAll_Success() {

        when(permitRepository.findAll())
                .thenReturn(List.of(permit));

        List<PermitApplicationResponse> result =
                permitService.getAll();

        assertEquals(1, result.size());
    }

    @Test
    void getMyPermits_Success() {

        when(permitRepository.findByUserId(100L))
                .thenReturn(List.of(permit));

        List<PermitApplicationResponse> result =
                permitService.getMyPermits(100L);

        assertEquals(1, result.size());
    }

    @Test
    void getByStatus_Success() {

        when(permitRepository.findByStatus(
                PermitStatus.APPLIED))
                .thenReturn(List.of(permit));

        List<PermitApplicationResponse> result =
                permitService.getByStatus(
                        PermitStatus.APPLIED);

        assertEquals(1, result.size());
    }

    @Test
    void getByType_Success() {

        when(permitRepository.findByPermitType(
                PermitType.TRADE_LICENSE))
                .thenReturn(List.of(permit));

        List<PermitApplicationResponse> result =
                permitService.getByType(
                        PermitType.TRADE_LICENSE);

        assertEquals(1, result.size());
    }

    @Test
    void renewPermit_Success() {

        permit.setStatus(PermitStatus.APPROVED);

        when(permitRepository.findById(1L))
                .thenReturn(Optional.of(permit));

        when(permitRepository.save(any()))
                .thenReturn(permit);

        RenewPermitRequest request =
                new RenewPermitRequest();

        request.setValidityPeriod(24);

        PermitApplicationResponse response =
                permitService.renewPermit(
                        1L,
                        request,
                        100L);

        assertNotNull(response);

        verify(permitRepository,
                times(1))
                .save(any());

        verify(notificationClient,
                times(1))
                .sendNotification(any());
    }

    @Test
    void renewPermit_Forbidden() {

        permit.setStatus(PermitStatus.APPROVED);

        when(permitRepository.findById(1L))
                .thenReturn(Optional.of(permit));

        RenewPermitRequest request =
                new RenewPermitRequest();

        request.setValidityPeriod(24);

        assertThrows(
                ForbiddenException.class,
                () -> permitService.renewPermit(
                        1L,
                        request,
                        999L)
        );
    }

    @Test
    void renewPermit_InvalidStatus() {

        permit.setStatus(PermitStatus.APPLIED);

        when(permitRepository.findById(1L))
                .thenReturn(Optional.of(permit));

        RenewPermitRequest request =
                new RenewPermitRequest();

        request.setValidityPeriod(24);

        assertThrows(
                BadRequestException.class,
                () -> permitService.renewPermit(
                        1L,
                        request,
                        100L)
        );
    }

    @Test
    void updateStatus_Approved_Success() {

        permit.setStatus(PermitStatus.UNDER_REVIEW);

        when(permitRepository.findById(1L))
                .thenReturn(Optional.of(permit));

        when(permitRepository.save(any()))
                .thenAnswer(i -> i.getArgument(0));

        PermitApplicationResponse response =
                permitService.updateStatus(
                        1L,
                        PermitStatus.APPROVED,
                        "Approved");

        assertNotNull(response);

        assertEquals(
                PermitStatus.APPROVED,
                permit.getStatus());

        verify(notificationClient,
                times(1))
                .sendNotification(any());
    }

    @Test
    void updateStatus_InvalidTransition() {

        permit.setStatus(PermitStatus.APPLIED);

        when(permitRepository.findById(1L))
                .thenReturn(Optional.of(permit));

        assertThrows(
                BadRequestException.class,
                () -> permitService.updateStatus(
                        1L,
                        PermitStatus.APPROVED,
                        "Invalid")
        );
    }

    @Test
    void applyInspectionOutcome_Pass() {

        permit.setStatus(PermitStatus.INSPECTION_SCHEDULED);

        when(permitRepository.findById(1L))
                .thenReturn(Optional.of(permit));

        permitService.applyInspectionOutcome(
                1L,
                com.civicdesk.permit.enums.InspectionOutcome.PASS);

        verify(permitRepository)
                .save(any());

        assertEquals(
                PermitStatus.APPROVED,
                permit.getStatus());
    }

    @Test
    void applyInspectionOutcome_Fail() {

        permit.setStatus(PermitStatus.INSPECTION_SCHEDULED);

        when(permitRepository.findById(1L))
                .thenReturn(Optional.of(permit));

        permitService.applyInspectionOutcome(
                1L,
                com.civicdesk.permit.enums.InspectionOutcome.FAIL);

        verify(permitRepository)
                .save(any());

        assertEquals(
                PermitStatus.REJECTED,
                permit.getStatus());
    }
}