package com.civicdesk.auth.dto.request;

public class UpdateUserRequest {

    private String name;
    private String phone;
    private String role;
    private String departmentId;

    public UpdateUserRequest() {
    }

    public UpdateUserRequest(String name, String phone, String role, String departmentId) {
        this.name = name;
        this.phone = phone;
        this.role = role;
        this.departmentId = departmentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }
}
