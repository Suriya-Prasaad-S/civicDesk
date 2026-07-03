package com.civicdesk.auth.exception;

public class PasswordNotSetException extends RuntimeException {
    public PasswordNotSetException(String message) {
        super(message);
    }
}
