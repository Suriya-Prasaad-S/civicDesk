package com.civicdesk.auth.dto.request;

public class UpdateDepartmentRequest {

    private String name;

    public UpdateDepartmentRequest() {
    }

    public UpdateDepartmentRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
