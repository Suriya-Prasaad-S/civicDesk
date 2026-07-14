package com.civicdesk.grievance.exception;

/**
 * Thrown when a grievance could not be persisted because of an unexpected
 * data-access or runtime failure.
 */
public class GrievanceCreationException extends RuntimeException {

    public GrievanceCreationException(String message) {
        super(message);
    }

    public GrievanceCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
