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

    public byte[] exportReportToExcel(CivicReport report) throws Exception {

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Grievance Report");

            CellStyle titleStyle = createTitleStyle(workbook);
            CellStyle sectionStyle = createSectionStyle(workbook);
            CellStyle tableHeaderStyle = createTableHeaderStyle(workbook);

            int rowIndex = 0;

            // Title
            rowIndex = createTitle(sheet, rowIndex, titleStyle);

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

            Map<String, Object> metrics = report.getMetrics();

            // Summary
            rowIndex = createSectionHeader(sheet, rowIndex, "SUMMARY", sectionStyle);

            Object total = metrics.get("totalGrievances");

            rowIndex = createKeyValueRow(
                    sheet,
                    rowIndex,
                    "Total Grievances",
                    total != null ? total.toString() : "0");

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
                    metrics.get("trend"),
                    sectionStyle,
                    tableHeaderStyle);

            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);

            workbook.write(outputStream);

            return outputStream.toByteArray();
        }
    }

    private int createTitle(
            Sheet sheet,
            int rowIndex,
            CellStyle style) {

        Row row = sheet.createRow(rowIndex);

        Cell cell = row.createCell(0);
        cell.setCellValue("CIVICDESK GRIEVANCE REPORT");
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
            String value) {

        Row row = sheet.createRow(rowIndex);

        row.createCell(0).setCellValue(key);
        row.createCell(1).setCellValue(value);

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
            Object data,
            CellStyle sectionStyle,
            CellStyle tableHeaderStyle) {

        rowIndex = createSectionHeader(
                sheet,
                rowIndex,
                "TREND ANALYSIS",
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