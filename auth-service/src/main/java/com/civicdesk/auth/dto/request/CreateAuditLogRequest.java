package com.civicdesk.auth.dto.request;

public class CreateAuditLogRequest {

    private String userId;
    private String action;
    private String module;
    private String ipAddress;

    public CreateAuditLogRequest() {
    }

    public CreateAuditLogRequest(String userId, String action, String module, String ipAddress) {
        this.userId = userId;
        this.action = action;
        this.module = module;
        this.ipAddress = ipAddress;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
}
