package com.civicdesk.servicerequest.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class InactiveServiceException extends RuntimeException {
    public InactiveServiceException(String message) { super(message); }
}
