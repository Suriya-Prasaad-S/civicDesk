package com.civicdesk.citizen.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.civicdesk.citizen.dto.response.ApiResponse;


import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse> handleCitizenNotFound(ResourceNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    private static ResponseEntity<ApiResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiResponse.error(message));
    }    

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse> handleBadRequest(BadRequestException e) {
        return ResponseEntity.status(400).body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse> handleForbidden(ForbiddenException e) {
        return ResponseEntity.status(403).body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse> handleAccessDenied(AccessDeniedException e) {
        // Raised by @PreAuthorize when a caller's role is not permitted on an endpoint.
        return ResponseEntity.status(403).body(ApiResponse.error("Access denied"));
    }

   @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidation(MethodArgumentNotValidException ex) {
        return firstFieldError(ex.getBindingResult().getFieldErrors());
    }

    private static ResponseEntity<ApiResponse> firstFieldError(List<FieldError> fieldErrors) {
        String message = fieldErrors.stream()
                .min(Comparator.comparingInt(GlobalExceptionHandler::constraintRank))
                .map(FieldError::getDefaultMessage)
                .orElse("Validation failed for the submitted request");
        return error(HttpStatus.BAD_REQUEST, message);
    }    

    private static int constraintRank(FieldError fieldError) {
        String code = fieldError.getCode();
        if (code != null
                && (code.startsWith("NotBlank") || code.startsWith("NotNull") || code.startsWith("NotEmpty"))) {
            return 0;
        }
        return 1;
    }

     /** Catch-all so that no unexpected error ever leaks a raw stack trace to the client. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleUnexpected(Exception ex) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.");
    }
}
