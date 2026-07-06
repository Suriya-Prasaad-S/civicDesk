package com.civicdesk.servicerequest.service;

import com.civicdesk.servicerequest.dto.request.ServiceCatalogRequest;
import com.civicdesk.servicerequest.dto.response.ServiceCatalogResponse;
import com.civicdesk.servicerequest.entity.ServiceCatalog;
import com.civicdesk.servicerequest.enums.ServiceCategory;
import com.civicdesk.servicerequest.enums.ServiceStatus;
import com.civicdesk.servicerequest.exception.ResourceNotFoundException;
import com.civicdesk.servicerequest.repository.ServiceCatalogRepository;
import com.civicdesk.servicerequest.client.AuditLogClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ServiceCatalogServiceTest {

    @Mock
    private ServiceCatalogRepository catalogRepository;

    @Mock
    private AuditLogClient auditLogClient;

    @InjectMocks
    private ServiceCatalogService serviceCatalogService;

    private ServiceCatalog catalog;
    private ServiceCatalogRequest request;

    @BeforeEach
    void setUp() {
        catalog = ServiceCatalog.builder()
                .serviceId(1L)
                .serviceName("Test Service")
                .departmentId("DEP1")
                .category(ServiceCategory.REGISTRATION)
                .processingDays(5)
                .requiredDocuments("ID_PROOF,ADDRESS_PROOF")
                .fee(new BigDecimal("10.00"))
                .status(ServiceStatus.ACTIVE)
                .build();

        request = new ServiceCatalogRequest();
        request.setServiceName("New Service");
        request.setDepartmentId("DEP2");
        request.setCategory(ServiceCategory.REGISTRATION);
        request.setProcessingDays(10);
        request.setFee(new BigDecimal("20.00"));
        request.setRequiredDocuments(List.of("ID_PROOF"));
    }

    @Test
    void create_Success() {
        when(catalogRepository.save(any(ServiceCatalog.class))).thenReturn(catalog);

        ServiceCatalogResponse response = serviceCatalogService.create(request);

        assertNotNull(response);
        assertEquals("Test Service", response.getServiceName());
        assertTrue(response.getMessage().contains("Service created successfully"));
        verify(catalogRepository, times(1)).save(any());
    }

    @Test
    void update_Success() {
        when(catalogRepository.findById(1L)).thenReturn(Optional.of(catalog));
        when(catalogRepository.save(any(ServiceCatalog.class))).thenAnswer(i -> i.getArgument(0));

        ServiceCatalogResponse response = serviceCatalogService.update(1L, request);

        assertNotNull(response);
        assertEquals("New Service", response.getServiceName());
        assertEquals("DEP2", response.getDepartmentId());
        assertTrue(response.getMessage().contains("Service updated successfully"));
    }

    @Test
    void updateStatus_Success() {
        when(catalogRepository.findById(1L)).thenReturn(Optional.of(catalog));
        when(catalogRepository.save(any(ServiceCatalog.class))).thenAnswer(i -> i.getArgument(0));

        ServiceCatalogResponse response = serviceCatalogService.updateStatus(1L, ServiceStatus.INACTIVE);

        assertNotNull(response);
        assertEquals(ServiceStatus.INACTIVE, response.getStatus());
    }

    @Test
    void getById_Success() {
        when(catalogRepository.findById(1L)).thenReturn(Optional.of(catalog));

        ServiceCatalogResponse response = serviceCatalogService.getById(1L);

        assertNotNull(response);
        assertEquals(1L, response.getServiceId());
    }

    @Test
    void getById_NotFound_ThrowsResourceNotFoundException() {
        when(catalogRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
            serviceCatalogService.getById(99L)
        );
    }

    @Test
    void getAllActive_Success() {
        when(catalogRepository.findByStatus(ServiceStatus.ACTIVE)).thenReturn(List.of(catalog));

        List<ServiceCatalogResponse> list = serviceCatalogService.getAllActive();

        assertNotNull(list);
        assertEquals(1, list.size());
    }

    @Test
    void delete_Success() {
        when(catalogRepository.findById(1L)).thenReturn(Optional.of(catalog));
        doNothing().when(catalogRepository).delete(catalog);

        assertDoesNotThrow(() -> serviceCatalogService.delete(1L));

        verify(catalogRepository, times(1)).delete(catalog);
    }
}
