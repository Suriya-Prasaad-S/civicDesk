package com.civicdesk.analytics.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.civicdesk.analytics.exception.InvalidReportTypeException;
import com.civicdesk.analytics.repository.CivicReportRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.civicdesk.analytics.client.GrievanceFeignClient;
import com.civicdesk.analytics.client.PermitFeignClient;
import com.civicdesk.analytics.client.ServiceRequestFeignClient;
import com.civicdesk.analytics.client.WorkOrderFeignClient;

import com.civicdesk.analytics.dto.request.GenerateReportRequest;




@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock
    private CivicReportRepository repository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ReportExportService reportExportService;

    @Mock
    private GrievanceFeignClient grievanceFeignClient;

    @Mock
    private PermitFeignClient permitFeignClient;

    @Mock
    private ServiceRequestFeignClient serviceRequestFeignClient;

    @Mock
    private WorkOrderFeignClient workOrderFeignClient;

    @Mock
    private AuditHelperService auditHelperService;

    @InjectMocks
    private ReportServiceImpl service;

    @Test
    void generateReport_ShouldThrow_WhenRequestIsNull() {

        assertThrows(
                InvalidReportTypeException.class,
                () -> service.generateReport(null, "user1"));
    }    

    @Test
    void generateReport_ShouldThrow_WhenTypeMissing() {

        GenerateReportRequest request =
                new GenerateReportRequest();

        assertThrows(
                InvalidReportTypeException.class,
                () -> service.generateReport(request, "user1"));
    }

    @Test
    void generateReport_ShouldThrow_WhenTypeInvalid() {

        GenerateReportRequest request =
                GenerateReportRequest.builder()
                        .type("ABC")
                        .build();

        assertThrows(
                InvalidReportTypeException.class,
                () -> service.generateReport(request, "user1"));
    }    
}