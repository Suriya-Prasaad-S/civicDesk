package com.civicdesk.servicerequest.service;

import com.civicdesk.servicerequest.client.NotificationClient;
import com.civicdesk.servicerequest.dto.ServiceRequestCreateRequest;
import com.civicdesk.servicerequest.dto.ServiceRequestResponse;
import com.civicdesk.servicerequest.entity.ServiceCatalog;
import com.civicdesk.servicerequest.entity.ServiceRequest;
import com.civicdesk.servicerequest.enums.RequestStatus;
import com.civicdesk.servicerequest.enums.ServiceStatus;
import com.civicdesk.servicerequest.exception.BadRequestException;
import com.civicdesk.servicerequest.exception.InactiveServiceException;
import com.civicdesk.servicerequest.repository.ServiceCatalogRepository;
import com.civicdesk.servicerequest.repository.ServiceRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ServiceRequestServiceTest {

    @Mock
    private ServiceRequestRepository requestRepository;

    @Mock
    private ServiceCatalogService catalogService;

    @Mock
    private ServiceCatalogRepository catalogRepository;

    @Mock
    private NotificationClient notificationClient;

    @InjectMocks
    private ServiceRequestService serviceRequestService;

    private ServiceCatalog activeService;
    private ServiceCatalog inactiveService;

    @BeforeEach
    void setUp() {
        activeService = ServiceCatalog.builder()
                .serviceId(1L)
                .serviceName("Birth Certificate")
                .status(ServiceStatus.ACTIVE)
                .category(com.civicdesk.servicerequest.enums.ServiceCategory.CERTIFICATE)
                .processingDays(7)
                .fee(new BigDecimal("50.00"))
                .build();

        inactiveService = ServiceCatalog.builder()
                .serviceId(2L)
                .serviceName("Death Certificate")
                .status(ServiceStatus.INACTIVE)
                .category(com.civicdesk.servicerequest.enums.ServiceCategory.CERTIFICATE)
                .processingDays(5)
                .fee(new BigDecimal("30.00"))
                .build();
    }

    @Test
    void submitRequest_Success() {
        when(catalogService.getEntityById(1L)).thenReturn(activeService);
        
        ServiceRequest savedRequest = ServiceRequest.builder()
                .requestId(100L)
                .citizenId(10L)
                .userId(1L)
                .service(activeService)
                .submissionDate(LocalDate.now())
                .expectedCompletionDate(LocalDate.now().plusDays(7))
                .fee(activeService.getFee())
                .status(RequestStatus.SUBMITTED)
                .build();

        when(requestRepository.save(any(ServiceRequest.class))).thenReturn(savedRequest);

        ServiceRequestCreateRequest createRequest = new ServiceRequestCreateRequest();
        createRequest.setServiceId(1L);
        createRequest.setCitizenId(10L);

        ServiceRequestResponse response = serviceRequestService.submitRequest(createRequest, 1L);

        assertNotNull(response);
        assertEquals(RequestStatus.SUBMITTED, response.getStatus());
        assertTrue(response.getMessage().contains("Expected completion date is 7 working days"));
        verify(notificationClient, times(1)).sendNotification(any());
    }

    @Test
    void submitRequest_InactiveService_ThrowsInactiveServiceException() {
        when(catalogService.getEntityById(2L)).thenReturn(inactiveService);

        ServiceRequestCreateRequest createRequest = new ServiceRequestCreateRequest();
        createRequest.setServiceId(2L);
        createRequest.setCitizenId(10L);

        assertThrows(InactiveServiceException.class, () -> 
            serviceRequestService.submitRequest(createRequest, 1L)
        );

        verify(requestRepository, never()).save(any());
        verify(notificationClient, never()).sendNotification(any());
    }

    @Test
    void updateStatus_Success() {
        ServiceRequest existingRequest = ServiceRequest.builder()
                .requestId(100L)
                .citizenId(10L)
                .userId(1L)
                .service(activeService)
                .status(RequestStatus.SUBMITTED)
                .build();

        when(requestRepository.findById(100L)).thenReturn(Optional.of(existingRequest));
        when(requestRepository.save(any(ServiceRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ServiceRequestResponse response = serviceRequestService.updateStatus(
                100L, RequestStatus.UNDER_REVIEW, "Processing details", 2L, "ADM"
        );

        assertNotNull(response);
        assertEquals(RequestStatus.UNDER_REVIEW, response.getStatus());
        assertTrue(response.getMessage().contains("Status has been moved to Under Review."));
        verify(notificationClient, times(1)).sendNotification(any());
    }

    @Test
    void updateStatus_InvalidTransition_ThrowsBadRequestException() {
        ServiceRequest existingRequest = ServiceRequest.builder()
                .requestId(100L)
                .citizenId(10L)
                .userId(1L)
                .service(activeService)
                .status(RequestStatus.SUBMITTED)
                .build();

        when(requestRepository.findById(100L)).thenReturn(Optional.of(existingRequest));

        assertThrows(BadRequestException.class, () -> 
            serviceRequestService.updateStatus(100L, RequestStatus.COMPLETED, "Done", 2L, "ADM")
        );

        verify(requestRepository, never()).save(any());
        verify(notificationClient, never()).sendNotification(any());
    }

    @Test
    void assignOfficer_Success() {
        ServiceRequest existingRequest = ServiceRequest.builder()
                .requestId(100L)
                .citizenId(10L)
                .userId(1L)
                .service(activeService)
                .status(RequestStatus.SUBMITTED)
                .build();

        when(requestRepository.findById(100L)).thenReturn(Optional.of(existingRequest));
        when(requestRepository.save(any(ServiceRequest.class))).thenAnswer(i -> i.getArgument(0));

        ServiceRequestResponse response = serviceRequestService.assignOfficer(100L, 5L);

        assertNotNull(response);
        assertEquals(5L, existingRequest.getAssignedOfficerId());
        assertEquals(RequestStatus.UNDER_REVIEW, response.getStatus()); // Automatically moves SUBMITTED to UNDER_REVIEW
        verify(requestRepository, times(1)).save(any());
    }

    @Test
    void assignOfficer_ClosedRequest_ThrowsBadRequestException() {
        ServiceRequest existingRequest = ServiceRequest.builder()
                .requestId(100L)
                .citizenId(10L)
                .userId(1L)
                .service(activeService)
                .status(RequestStatus.COMPLETED)
                .build();

        when(requestRepository.findById(100L)).thenReturn(Optional.of(existingRequest));

        assertThrows(BadRequestException.class, () ->
            serviceRequestService.assignOfficer(100L, 5L)
        );

        verify(requestRepository, never()).save(any());
    }

    @Test
    void getAssignedToMe_Success() {
        ServiceRequest requestAssigned = ServiceRequest.builder()
                .requestId(100L)
                .assignedOfficerId(5L)
                .service(activeService)
                .status(RequestStatus.UNDER_REVIEW)
                .build();

        when(requestRepository.findByAssignedOfficerId(5L)).thenReturn(List.of(requestAssigned));

        List<ServiceRequestResponse> list = serviceRequestService.getAssignedToMe(5L);

        assertNotNull(list);
        assertEquals(1, list.size());
        verify(requestRepository, times(1)).findByAssignedOfficerId(5L);
    }
}
