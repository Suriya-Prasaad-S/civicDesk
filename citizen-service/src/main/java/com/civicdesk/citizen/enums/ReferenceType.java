package com.civicdesk.citizen.enums;

/**
 * The kind of entity a notification refers to. Must stay in sync with the {@code ReferenceType}
 * enum owned by notification-service.
 */
public enum ReferenceType {

    USER,
    SECURITY,
    GRIEVANCE,
    PERMIT,
    SERVICE_REQUEST,
    WORK_ORDER,
    NONE
}
