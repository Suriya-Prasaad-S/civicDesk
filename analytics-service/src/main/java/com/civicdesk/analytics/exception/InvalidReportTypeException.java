package com.civicdesk.analytics.exception;

public class InvalidReportTypeException extends RuntimeException {
    public InvalidReportTypeException(String message) {
        super(message);
    }

    public InvalidReportTypeException(String message, Throwable cause) {
        super(message, cause);
    }
}
