package com.civicdesk.auth.dto.request;

public class CreateDepartmentRequest {

    private String name;

    public CreateDepartmentRequest() {
    }

    public CreateDepartmentRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
