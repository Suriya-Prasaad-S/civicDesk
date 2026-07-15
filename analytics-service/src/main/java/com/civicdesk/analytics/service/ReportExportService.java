package com.civicdesk.analytics.service;

import com.civicdesk.analytics.entity.CivicReport;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportExportService {

    private final ObjectMapper objectMapper;

        public byte[] exportReportToExcel(CivicReport report)
                throws Exception {

                if (report == null) {
                        throw new IllegalArgumentException(
                                "Report cannot be null");
                }

                String reportType =
                        report.getReportType();

                if (reportType == null ||
                        reportType.isBlank()) {

                        throw new IllegalArgumentException(
                                "Report type cannot be null");
                }

                return switch (reportType
                        .trim()
                        .toUpperCase()) {

                        case "GRIEVANCE" ->
                                exportGrievanceReport(report);

                        case "PERMIT" ->
                                exportPermitReport(report);

                        case "SERVICE_REQUEST" ->
                                throw new IllegalArgumentException(
                                    "Unsupported report type: "
                                        + reportType);
                                // exportServiceRequestReport(report);

                        case "WORK_ORDER" ->
                                exportWorkOrderReport(report);

                        default ->
                                throw new IllegalArgumentException(
                                        "Unsupported report type: "
                                                + reportType);
                };
        }

    private Map<String, Object> getMetrics(
            CivicReport report) {

        return report.getMetrics() != null
                ? report.getMetrics()
                : Map.of();
    }        

        private byte[] exportGrievanceReport(
                CivicReport report) throws Exception {
                try (XSSFWorkbook workbook = new XSSFWorkbook();
                     ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

                    Sheet sheet = workbook.createSheet("Grievance Report");

                    CellStyle titleStyle = createTitleStyle(workbook);
                    CellStyle sectionStyle = createSectionStyle(workbook);
                    CellStyle tableHeaderStyle = createTableHeaderStyle(workbook);

                    int rowIndex = 0;

                    // Title
                    rowIndex = createTitle(sheet, rowIndex, "CIVICDESK GRIEVANCE REPORT", titleStyle);

                    // Report Details
                    rowIndex = createSectionHeader(sheet, rowIndex, "REPORT DETAILS", sectionStyle);

                    rowIndex = createKeyValueRow(sheet, rowIndex, "Report ID", report.getReportId());
                    rowIndex = createKeyValueRow(sheet, rowIndex, "Report Type", report.getReportType());
                    rowIndex = createKeyValueRow(sheet, rowIndex, "Status", report.getStatus());
                    rowIndex = createKeyValueRow(
                            sheet,
                            rowIndex,
                            "Generated Date",
                            report.getGeneratedDate() != null
                                    ? report.getGeneratedDate().toString()
                                    : "");

                    rowIndex++;

                    Map<String, Object> metrics =
                        getMetrics(report);

                    // Summary
                    rowIndex = createSectionHeader(sheet, rowIndex, "SUMMARY", sectionStyle);

                    rowIndex = createKeyValueRow(
                            sheet,
                            rowIndex,
                            "Total Grievances",
                            metrics.getOrDefault(
                                    "totalGrievances",
                                    0));

                    rowIndex++;

                    // Status Breakdown
                    rowIndex = writeLabelCountSection(
                            sheet,
                            rowIndex,
                            "STATUS BREAKDOWN",
                            metrics.get("statusBreakdown"),
                            sectionStyle,
                            tableHeaderStyle);

                    // Category Breakdown
                    rowIndex = writeLabelCountSection(
                            sheet,
                            rowIndex,
                            "CATEGORY BREAKDOWN",
                            metrics.get("categoryBreakdown"),
                            sectionStyle,
                            tableHeaderStyle);

                    // Assignment Breakdown
                    rowIndex = writeLabelCountSection(
                            sheet,
                            rowIndex,
                            "ASSIGNMENT BREAKDOWN",
                            metrics.get("assignmentBreakdown"),
                            sectionStyle,
                            tableHeaderStyle);

                    // Escalation Breakdown
                    rowIndex = writeLabelCountSection(
                            sheet,
                            rowIndex,
                            "ESCALATION BREAKDOWN",
                            metrics.get("escalationBreakdown"),
                            sectionStyle,
                            tableHeaderStyle);

                    // Trend Analysis
                    rowIndex = writeTrendSection(
                            sheet,
                            rowIndex,
                            "TREND ANALYSIS",
                            metrics.get("trend"),
                            sectionStyle,
                            tableHeaderStyle);

                    sheet.autoSizeColumn(0);
                    sheet.autoSizeColumn(1);

                    workbook.write(outputStream);

                    return outputStream.toByteArray();
                }                
        }

    private byte[] exportPermitReport(
            CivicReport report) throws Exception {

        try (XSSFWorkbook workbook = new XSSFWorkbook();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet =
                    workbook.createSheet("Permit Report");

            CellStyle titleStyle =
                    createTitleStyle(workbook);

            CellStyle sectionStyle =
                    createSectionStyle(workbook);

            CellStyle tableHeaderStyle =
                    createTableHeaderStyle(workbook);

            int rowIndex = 0;

            // Title
            rowIndex = createTitle(
                    sheet,
                    rowIndex,
                    "CIVICDESK PERMIT REPORT",
                    titleStyle);

            // Report Details
            rowIndex = createSectionHeader(
                    sheet,
                    rowIndex,
                    "REPORT DETAILS",
                    sectionStyle);

            rowIndex = createKeyValueRow(
                    sheet,
                    rowIndex,
                    "Report ID",
                    report.getReportId());

            rowIndex = createKeyValueRow(
                    sheet,
                    rowIndex,
                    "Report Type",
                    report.getReportType());

            rowIndex = createKeyValueRow(
                    sheet,
                    rowIndex,
                    "Status",
                    report.getStatus());

            rowIndex = createKeyValueRow(
                    sheet,
                    rowIndex,
                    "Generated Date",
                    report.getGeneratedDate());

            rowIndex++;

            Map<String, Object> metrics =
                    getMetrics(report);

            // Summary
            rowIndex = createSectionHeader(
                    sheet,
                    rowIndex,
                    "SUMMARY",
                    sectionStyle);

            rowIndex = createKeyValueRow(
                    sheet,
                    rowIndex,
                    "Total Permits",
                    metrics.getOrDefault(
                            "totalPermits",
                            0));

            rowIndex = createKeyValueRow(
                    sheet,
                    rowIndex,
                    "Average Decision Days",
                    metrics.getOrDefault(
                            "averageDecisionDays",
                            0));

            rowIndex++;

            // Status Breakdown
            rowIndex = writeLabelCountSection(
                    sheet,
                    rowIndex,
                    "STATUS BREAKDOWN",
                    metrics.get("statusBreakdown"),
                    sectionStyle,
                    tableHeaderStyle);

            // Permit Type Breakdown
            rowIndex = writeLabelCountSection(
                    sheet,
                    rowIndex,
                    "PERMIT TYPE BREAKDOWN",
                    metrics.get("permitTypeBreakdown"),
                    sectionStyle,
                    tableHeaderStyle);

            // Application Trend
            rowIndex = writeTrendSection(
                    sheet,
                    rowIndex,
                    "APPLICATION TREND",
                    metrics.get("applicationTrend"),
                    sectionStyle,
                    tableHeaderStyle);

            // Decision Trend
            rowIndex = writeTrendSection(
                    sheet,
                    rowIndex,
                    "DECISION TREND",
                    metrics.get("decisionTrend"),
                    sectionStyle,
                    tableHeaderStyle);

            Map<String, Object> inspection =
                    getNestedMap(
                            metrics,
                            "inspection");

            if (inspection != null) {

                rowIndex = writeLabelCountSection(
                        sheet,
                        rowIndex,
                        "INSPECTION STATUS BREAKDOWN",
                        inspection.get("statusBreakdown"),
                        sectionStyle,
                        tableHeaderStyle);

                rowIndex = writeLabelCountSection(
                        sheet,
                        rowIndex,
                        "INSPECTION OUTCOME BREAKDOWN",
                        inspection.get("outcomeBreakdown"),
                        sectionStyle,
                        tableHeaderStyle);
            }

            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);

            workbook.write(outputStream);

            return outputStream.toByteArray();
        }
    }        

    private byte[] exportWorkOrderReport(
            CivicReport report) throws Exception {

        try (XSSFWorkbook workbook = new XSSFWorkbook();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet =
                    workbook.createSheet("Work Order Report");

            CellStyle titleStyle =
                    createTitleStyle(workbook);

            CellStyle sectionStyle =
                    createSectionStyle(workbook);

            CellStyle tableHeaderStyle =
                    createTableHeaderStyle(workbook);

            int rowIndex = 0;

            // Title
            rowIndex = createTitle(
                    sheet,
                    rowIndex,
                    "CIVICDESK WORK ORDER REPORT",
                    titleStyle);

            // Report Details
            rowIndex = createSectionHeader(
                    sheet,
                    rowIndex,
                    "REPORT DETAILS",
                    sectionStyle);

            rowIndex = createKeyValueRow(
                    sheet,
                    rowIndex,
                    "Report ID",
                    report.getReportId());

            rowIndex = createKeyValueRow(
                    sheet,
                    rowIndex,
                    "Report Type",
                    report.getReportType());

            rowIndex = createKeyValueRow(
                    sheet,
                    rowIndex,
                    "Status",
                    report.getStatus());

            rowIndex = createKeyValueRow(
                    sheet,
                    rowIndex,
                    "Generated Date",
                    report.getGeneratedDate());

            rowIndex++;

            Map<String, Object> metrics =
                    getMetrics(report);

            // Summary
            rowIndex = createSectionHeader(
                    sheet,
                    rowIndex,
                    "SUMMARY",
                    sectionStyle);

            rowIndex = createKeyValueRow(
                    sheet,
                    rowIndex,
                    "Total Work Orders",
                    metrics.getOrDefault(
                            "totalWorkOrders",
                            0));

            rowIndex = createKeyValueRow(
                    sheet,
                    rowIndex,
                    "Completed Work Orders",
                    metrics.getOrDefault(
                            "completedWorkOrders",
                            0));

            rowIndex = createKeyValueRow(
                    sheet,
                    rowIndex,
                    "Delayed Work Orders",
                    metrics.getOrDefault(
                            "delayedWorkOrders",
                            0));

            rowIndex = createKeyValueRow(
                    sheet,
                    rowIndex,
                    "Average Completion Days",
                    metrics.getOrDefault(
                            "averageCompletionDays",
                            0));

            rowIndex++;

            // Status Breakdown
            rowIndex = writeLabelCountSection(
                    sheet,
                    rowIndex,
                    "STATUS BREAKDOWN",
                    metrics.get("statusBreakdown"),
                    sectionStyle,
                    tableHeaderStyle);

            // Category Breakdown
            rowIndex = writeLabelCountSection(
                    sheet,
                    rowIndex,
                    "CATEGORY BREAKDOWN",
                    metrics.get("categoryBreakdown"),
                    sectionStyle,
                    tableHeaderStyle);

            // Trend
            rowIndex = writeTrendSection(
                    sheet,
                    rowIndex,
                    "WORK ORDER TREND",
                    metrics.get("trend"),
                    sectionStyle,
                    tableHeaderStyle);

            // Budget Analytics
            Map<String, Object> budget =
                    getNestedMap(
                            metrics,
                            "budget");

            if (budget != null) {

                rowIndex = createSectionHeader(
                        sheet,
                        rowIndex,
                        "BUDGET ANALYTICS",
                        sectionStyle);

                rowIndex = createKeyValueRow(
                        sheet,
                        rowIndex,
                        "Allocated Budget",
                        budget.get("allocated"));

                rowIndex = createKeyValueRow(
                        sheet,
                        rowIndex,
                        "Consumed Budget",
                        budget.get("consumed"));

                rowIndex = createKeyValueRow(
                        sheet,
                        rowIndex,
                        "Utilization Percentage",
                        budget.get("utilizationPercentage"));

                rowIndex++;
            }

            // Milestone Analytics
            Map<String, Object> milestones =
                    getNestedMap(
                            metrics,
                            "milestones");

            if (milestones != null) {

                rowIndex = createSectionHeader(
                        sheet,
                        rowIndex,
                        "MILESTONE ANALYTICS",
                        sectionStyle);

                rowIndex = createKeyValueRow(
                        sheet,
                        rowIndex,
                        "Delayed Milestones",
                        milestones.get("delayedMilestones"));

                rowIndex++;

                rowIndex = writeLabelCountSection(
                        sheet,
                        rowIndex,
                        "MILESTONE STATUS BREAKDOWN",
                        milestones.get("statusBreakdown"),
                        sectionStyle,
                        tableHeaderStyle);
            }

            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);

            workbook.write(outputStream);

            return outputStream.toByteArray();
        }
    }    

    private Map<String, Object> getNestedMap(
            Map<String, Object> metrics,
            String key) {

        return objectMapper.convertValue(
                metrics.get(key),
                new TypeReference<Map<String, Object>>() {});
    }        
    
    private int createTitle(
                Sheet sheet,
                int rowIndex,
                String title,
                CellStyle style) {

        Row row = sheet.createRow(rowIndex);

        Cell cell = row.createCell(0);
        cell.setCellValue(title);
        cell.setCellStyle(style);

        sheet.addMergedRegion(
                new CellRangeAddress(rowIndex, rowIndex, 0, 3));

        return rowIndex + 2;
        }

    private int createSectionHeader(
            Sheet sheet,
            int rowIndex,
            String title,
            CellStyle style) {

        Row row = sheet.createRow(rowIndex);

        Cell cell = row.createCell(0);
        cell.setCellValue(title);
        cell.setCellStyle(style);

        return rowIndex + 1;
    }

        private int createKeyValueRow(
                Sheet sheet,
                int rowIndex,
                String key,
                Object value) {

                Row row = sheet.createRow(rowIndex);

                row.createCell(0).setCellValue(key);

                row.createCell(1).setCellValue(
                        value != null
                                ? String.valueOf(value)
                                : "");

                return rowIndex + 1;
        }

    private int writeLabelCountSection(
            Sheet sheet,
            int rowIndex,
            String title,
            Object data,
            CellStyle sectionStyle,
            CellStyle tableHeaderStyle) {

        rowIndex = createSectionHeader(
                sheet,
                rowIndex,
                title,
                sectionStyle);

        Row header = sheet.createRow(rowIndex++);

        Cell h1 = header.createCell(0);
        h1.setCellValue("Label");
        h1.setCellStyle(tableHeaderStyle);

        Cell h2 = header.createCell(1);
        h2.setCellValue("Count");
        h2.setCellStyle(tableHeaderStyle);        

        List<Map<String, Object>> rows =
                objectMapper.convertValue(
                        data,
                        new TypeReference<List<Map<String, Object>>>() {
                        });

        if (rows == null || rows.isEmpty()) {

                Row row = sheet.createRow(rowIndex++);

                row.createCell(0)
                        .setCellValue("No Data Available");

                return rowIndex + 1;
        }                       

        for (Map<String, Object> rowData : rows) {

            Row row = sheet.createRow(rowIndex++);

            row.createCell(0)
                    .setCellValue(String.valueOf(rowData.get("label")));

            row.createCell(1)
                    .setCellValue(
                            Integer.parseInt(
                                    String.valueOf(
                                            rowData.get("count"))));
        }

        return rowIndex + 1;
    }

    private int writeTrendSection(
            Sheet sheet,
            int rowIndex,
            String title,
            Object data,
            CellStyle sectionStyle,
            CellStyle tableHeaderStyle) {

        rowIndex = createSectionHeader(
                sheet,
                rowIndex,
                title,
                sectionStyle);

        Row header = sheet.createRow(rowIndex++);

        Cell h1 = header.createCell(0);
        h1.setCellValue("Date");
        h1.setCellStyle(tableHeaderStyle);

        Cell h2 = header.createCell(1);
        h2.setCellValue("Count");
        h2.setCellStyle(tableHeaderStyle);

        List<Map<String, Object>> trendRows =
                objectMapper.convertValue(
                        data,
                        new TypeReference<List<Map<String, Object>>>() {
                        });

        if (trendRows == null || trendRows.isEmpty()) {

                Row row = sheet.createRow(rowIndex++);

                row.createCell(0)
                        .setCellValue("No Data Available");

                return rowIndex + 1;
        }                                

        for (Map<String, Object> item : trendRows) {

            Row row = sheet.createRow(rowIndex++);

            row.createCell(0)
                    .setCellValue(String.valueOf(item.get("date")));

            row.createCell(1)
                    .setCellValue(
                            Integer.parseInt(
                                    String.valueOf(item.get("count"))));
        }

        return rowIndex;
    }

    private CellStyle createTitleStyle(Workbook workbook) {

        CellStyle style = workbook.createCellStyle();

        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        font.setColor(IndexedColors.WHITE.getIndex());

        style.setFont(font);

        style.setFillForegroundColor(
                IndexedColors.DARK_BLUE.getIndex());

        style.setFillPattern(
                FillPatternType.SOLID_FOREGROUND);

        return style;
    }

    private CellStyle createSectionStyle(Workbook workbook) {

        CellStyle style = workbook.createCellStyle();

        Font font = workbook.createFont();

        font.setBold(true);
        font.setColor(
                IndexedColors.WHITE.getIndex());

        style.setFont(font);

        style.setFillForegroundColor(
                IndexedColors.BLUE.getIndex());

        style.setFillPattern(
                FillPatternType.SOLID_FOREGROUND);

        return style;
    }

    private CellStyle createTableHeaderStyle(
            Workbook workbook) {

        CellStyle style = workbook.createCellStyle();

        Font font = workbook.createFont();
        font.setBold(true);

        style.setFont(font);

        style.setFillForegroundColor(
                IndexedColors.GREY_25_PERCENT.getIndex());

        style.setFillPattern(
                FillPatternType.SOLID_FOREGROUND);

        return style;
    }
}