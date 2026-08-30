package com.staffs.leavebooking.staffmanagement.domain;

/**
 * Enum representing the employment contract types for a staff member
 * (Lecture 2 — DDD Value Objects, Domain Modelling).
 *
 * <p><strong>Used by:</strong> {@link StaffMember#employmentType()} field.
 * Stored as a string in the database (e.g., "FULL_TIME") and converted
 * back to the enum via {@code EmploymentType.valueOf()} when reconstituting
 * the aggregate from persistence.
 *
 * <p><strong>Three contract types:</strong>
 * <ul>
 *   <li>{@code FULL_TIME} — standard full-time employee (e.g., Raf Ahmed)</li>
 *   <li>{@code PART_TIME} — part-time employee (e.g., Phil James — 20 days entitlement)</li>
 *   <li>{@code CONTRACT} — contractor with fixed-term agreement</li>
 * </ul>
 */
public enum EmploymentType {
    FULL_TIME,  // Standard full-time employee
    PART_TIME,  // Part-time employee (may have reduced leave entitlement)
    CONTRACT    // Fixed-term contractor
}
