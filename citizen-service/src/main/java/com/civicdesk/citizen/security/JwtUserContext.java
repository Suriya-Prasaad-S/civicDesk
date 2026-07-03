package com.civicdesk.citizen.security;

/**
 * Thread-local holder for the authenticated user's ID extracted from JWT.
 * Set by JwtAuthFilter on each request; cleared after the request completes.
 */
public class JwtUserContext {

    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> EMAIL_HOLDER = new ThreadLocal<>();

    public static void setCurrentUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    public static Long getCurrentUserId() {
        return USER_ID_HOLDER.get();
    }

    public static void setCurrentEmail(String email) {
        EMAIL_HOLDER.set(email);
    }

    public static String getCurrentEmail() {
        return EMAIL_HOLDER.get();
    }

    public static void clear() {
        USER_ID_HOLDER.remove();
        EMAIL_HOLDER.remove();
    }
}
