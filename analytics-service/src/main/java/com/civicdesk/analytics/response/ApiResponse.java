package com.civicdesk.analytics.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse {
    private String message;
    private Object data;
    private String error;
    private boolean success;

    public static ApiResponse of(String message, Object data) {
        return ApiResponse.builder()
                .message(message)
                .data(data)
                .success(true)
                .build();
    }

    public static ApiResponse data(Object data) {
        return ApiResponse.builder()
                .data(data)
                .success(true)
                .build();
    }

    public static ApiResponse error(String error) {
        return ApiResponse.builder()
                .error(error)
                .success(false)
                .build();
    }

    public static ApiResponse message(String message) {
        return ApiResponse.builder()
                .message(message)
                .success(true)
                .build();
    }
}
