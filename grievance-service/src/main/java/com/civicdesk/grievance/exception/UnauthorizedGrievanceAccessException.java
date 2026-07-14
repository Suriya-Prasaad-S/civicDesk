package com.civicdesk.grievance.exception;

/**
 * Thrown when a caller tries to act on a grievance they do not own / are not
 * permitted to access. Mapped to HTTP 403 by the global exception handler.
 */
public class UnauthorizedGrievanceAccessException extends RuntimeException {

    public UnauthorizedGrievanceAccessException(String message) {
        super(message);
    }
}
