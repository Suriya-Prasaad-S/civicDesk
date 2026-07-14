package com.civicdesk.citizen.enums;

/**
 * Allowed citizen gender values. Enforced at the API layer and at the DB layer
 * via a CHECK constraint.
 */
public enum Gender {
    Male,
    Female,
    Other
}
