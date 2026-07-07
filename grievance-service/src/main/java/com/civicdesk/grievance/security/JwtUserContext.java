package com.civicdesk.grievance.security;

public class JwtUserContext {

    private static final ThreadLocal<String>   USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> EMAIL   = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE    = new ThreadLocal<>();

    public static void set(String userId, String email, String role) {
        USER_ID.set(userId);
        EMAIL.set(email);
        ROLE.set(role);
    }

    public static String   getCurrentUserId() { return USER_ID.get(); }
    public static String getCurrentEmail()  { return EMAIL.get(); }
    public static String getCurrentRole()   { return ROLE.get(); }

    public static void clear() {
        USER_ID.remove();
        EMAIL.remove();
        ROLE.remove();
    }
}
