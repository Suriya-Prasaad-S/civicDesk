package com.civicdesk.grievance.enums;

/**
 * Fixed list of grievance categories, stored and exposed as short codes.
 * Each category routes to exactly one department (resolved to a {@code departmentId}
 * via the IAM {@code departments} table at create time).
 *
 * <pre>
 *   RI = Road infrastructure   → Infrastructure
 *   WS = Water supply          → Public Health
 *   SN = Sanitation            → Public Health
 *   SD = Service delay         → Compliance & Audit
 *   CR = Corruption            → Compliance & Audit
 *   OT = Other                 → Administration
 * </pre>
 */
public enum Category {

    RI("DPT01"),
    WS("DPT02"),
    SN("DPT02"),
    SD("DPT03"),
    CR("DPT03"),
    OT("DPT05");

    private final String departmentId;

    Category(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getDepartmentId() {
        return departmentId;
    }
}
