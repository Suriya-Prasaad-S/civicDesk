package com.civicdesk.auth.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse {

    private String message;
    private Object data;

    public ApiResponse(String message, Object data) {
        this.message = message;
        this.data = data;
    }

    public static ApiResponse data(Object data) {
        return new ApiResponse(null, data);
    }

    public static ApiResponse of(String message, Object data) {
        return new ApiResponse(message, data);
    }

    public static ApiResponse error(String message) {
        return new ApiResponse(message, null);
    }

    public String getMessage() {
        return message;
    }

    public Object getData() {
        return data;
    }
}
