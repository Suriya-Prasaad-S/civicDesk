package com.civicdesk.grievance.exception;

/**
 * Thrown when a grievance analytics request is invalid or cannot be processed.
 */
public class InvalidGrievanceAnalyticsRequestException extends RuntimeException {

    public InvalidGrievanceAnalyticsRequestException(String message) {
        super(message);
    }

    public InvalidGrievanceAnalyticsRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
