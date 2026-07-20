package com.civicdesk.citizen.enums;

/**
 * Notification categories understood by notification-service. Must stay in sync with the
 * {@code NotificationType} enum owned by notification-service.
 */
public enum NotificationType {

    ACCOUNT_CREATED,
    PASSWORD_CHANGED,
    ACCOUNT_SUSPENDED,
    ACCOUNT_REACTIVATED,
    LOGIN_ALERT,
    ROLE_CHANGED,
    SECURITY_ALERT,
    GRIEVANCE_UPDATE,
    PERMIT_UPDATE,
    SERVICE_REQUEST_UPDATE,
    WORK_ORDER_UPDATE,
    SLA_BREACH_ALERT,
    SYSTEM_ALERT,
    GENERAL
}
