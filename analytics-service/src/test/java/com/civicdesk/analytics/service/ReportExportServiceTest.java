package com.civicdesk.analytics.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.civicdesk.analytics.entity.CivicReport;

@ExtendWith(MockitoExtension.class)
class ReportExportServiceTest {

    @InjectMocks
    private ReportExportService reportExportService;

    @Mock
    private ObjectMapper objectMapper;

    @Test
    void exportReport_ShouldThrow_WhenReportNull() {

        assertThrows(
                IllegalArgumentException.class,
                () -> reportExportService.exportReportToExcel(null));
    }
    @Test
    void exportReport_ShouldThrow_WhenReportTypeNull() {

        CivicReport report = new CivicReport();

        assertThrows(
                IllegalArgumentException.class,
                () -> reportExportService.exportReportToExcel(report));
    }
    @Test
    void exportReport_ShouldThrow_WhenUnsupportedType() {

        CivicReport report = new CivicReport();
        report.setReportType("ABC");

        assertThrows(
                IllegalArgumentException.class,
                () -> reportExportService.exportReportToExcel(report));
    }
}                