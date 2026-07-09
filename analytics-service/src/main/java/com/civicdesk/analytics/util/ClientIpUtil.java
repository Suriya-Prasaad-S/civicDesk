package com.civicdesk.analytics.util;

import jakarta.servlet.http.HttpServletRequest;

public class ClientIpUtil {
    public static String resolve(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isBlank()) {
            int commaIndex = ip.indexOf(',');
            if (commaIndex > -1) {
                ip = ip.substring(0, commaIndex).trim();
            }
            return ip;
        }

        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isBlank()) {
            return ip;
        }

        ip = request.getRemoteAddr();
        if (ip != null && !ip.isBlank()) {
            return ip;
        }

        return "UNKNOWN";
    }
}
