package com.civicdesk.auth.repository.spec;

import com.civicdesk.auth.entity.AuditLog;
import org.springframework.data.jpa.domain.Specification;

public final class AuditLogSpecifications {

    private AuditLogSpecifications() {
    }

    public static Specification<AuditLog> hasUserId(String userId) {
        return (root, query, cb) -> cb.equal(root.get("userId"), userId);
    }

    public static Specification<AuditLog> hasAction(String action) {
        return (root, query, cb) -> cb.equal(root.get("action"), action.trim().toUpperCase());
    }

    public static Specification<AuditLog> hasModule(String module) {
        return (root, query, cb) -> cb.equal(root.get("module"), module.trim().toUpperCase());
    }
}
