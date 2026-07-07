package com.civicdesk.servicerequest.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RequestStatus {
    SUBMITTED("S"),
    UNDER_REVIEW("U"),
    PENDING_DOCUMENTS("P"),
    APPROVED("A"),
    REJECTED("R"),
    COMPLETED("C");

    private final String code;

    RequestStatus(String code) {
        this.code = code;
    }

    @JsonValue
    public String getCode() {
        return code;
    }

    @JsonCreator
    public static RequestStatus fromValue(String value) {
        for (RequestStatus status : RequestStatus.values()) {
            if (status.code.equalsIgnoreCase(value) || status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status: " + value);
    }

    /** Terminal states accept no further transitions and no new document uploads. */
    public boolean isTerminal() {
        return this == REJECTED || this == COMPLETED;
    }

    /** The statuses this status may legally transition to. */
    public java.util.Set<RequestStatus> allowedNextStates() {
        return switch (this) {
            case SUBMITTED -> java.util.EnumSet.of(UNDER_REVIEW);
            case UNDER_REVIEW -> java.util.EnumSet.of(PENDING_DOCUMENTS, APPROVED, REJECTED);
            case PENDING_DOCUMENTS -> java.util.EnumSet.of(UNDER_REVIEW, REJECTED);
            case APPROVED -> java.util.EnumSet.of(COMPLETED, REJECTED);
            case REJECTED, COMPLETED -> java.util.EnumSet.noneOf(RequestStatus.class);
        };
    }
}
