package com.staffs.leavebooking.leavemanagement.domain;

/**
 * Enum representing the type of leave being requested
 * (Lecture 2 — Value Objects, Lecture 4 — Domain Modelling).
 *
 * <p><strong>DDD Concept:</strong> Leave types are part of the ubiquitous language.
 * Using an enum rather than a raw string ensures type safety and prevents typos
 * or invalid leave types from entering the domain.
 *
 * <p><strong>Current scope:</strong> Only {@link #ANNUAL} leave is supported in this system.
 * The enum is designed to be extensible — additional leave types (e.g., SICK, PARENTAL,
 * COMPASSIONATE) can be added as new enum constants without changing the existing logic,
 * provided the application service and UI are updated to handle them.
 *
 * <p><strong>Usage:</strong> Stored as a field on {@link LeaveRequest} to categorise
 * what kind of leave the staff member is requesting.
 *
 * @see LeaveRequest for the aggregate that uses this enum
 */
public enum LeaveType {

    /** Annual leave — the standard holiday entitlement. Currently the only supported type. */
    ANNUAL("Annual Leave");

    // ─────────────────────────────────────────────────────────────────
    // FIELD
    // ─────────────────────────────────────────────────────────────────

    /** Human-readable description of this leave type, used for display and logging. */
    private final String description;

    // ─────────────────────────────────────────────────────────────────
    // CONSTRUCTOR
    // ─────────────────────────────────────────────────────────────────

    /**
     * Private enum constructor — sets the human-readable description for this leave type.
     *
     * @param description a short description of this leave type
     */
    LeaveType(String description) {
        this.description = description;
    }

    // ─────────────────────────────────────────────────────────────────
    // ACCESSOR
    // ─────────────────────────────────────────────────────────────────

    /**
     * Returns the human-readable description of this leave type.
     *
     * @return the leave type description (e.g., "Annual Leave")
     */
    public String description() {
        return description;
    }
}
