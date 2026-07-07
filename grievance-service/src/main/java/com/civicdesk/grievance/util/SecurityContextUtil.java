package com.civicdesk.grievance.util;

/**
 * Minimal stub for SecurityContext utilities used by services.
 * Replace with the real implementation that reads from Spring Security context.
 */
public final class SecurityContextUtil {

    private SecurityContextUtil() {}

    public static String getCurrentUserId() {
        return null;
    }

    public static String getCurrentRole() {
        return null;
    }
}
