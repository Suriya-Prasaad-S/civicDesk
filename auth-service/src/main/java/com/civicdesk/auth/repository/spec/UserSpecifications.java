package com.civicdesk.auth.repository.spec;

import com.civicdesk.auth.entity.User;
import org.springframework.data.jpa.domain.Specification;

public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<User> hasRole(String role) {
        return (root, query, cb) -> cb.equal(root.get("role"), role);
    }

    public static Specification<User> hasStatus(String status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<User> inDepartment(String departmentId) {
        return (root, query, cb) -> cb.equal(root.get("departmentId"), departmentId);
    }
}
