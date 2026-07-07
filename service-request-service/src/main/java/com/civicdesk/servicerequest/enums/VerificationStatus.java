package com.civicdesk.servicerequest.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum VerificationStatus {
    PENDING("P"),
    VERIFIED("V"),
    REJECTED("R");

    private final String code;

    VerificationStatus(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static VerificationStatus fromValue(String value) {
        for (VerificationStatus status : VerificationStatus.values()) {
            if (status.code.equalsIgnoreCase(value) || status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status: " + value);
    }
}
