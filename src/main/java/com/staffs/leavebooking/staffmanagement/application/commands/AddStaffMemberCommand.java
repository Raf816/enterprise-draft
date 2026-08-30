package com.staffs.leavebooking.staffmanagement.application.commands;

import java.time.LocalDate;

/**
 * CQRS Command record for creating a new staff member via admin POST /staff
 * (Lecture 6 — CQRS Commands).
 *
 * <p><strong>CQRS pattern:</strong> This command carries all the data needed
 * to create a new staff member. It is passed from the controller through the
 * facade to the application service, which uses it to construct the domain aggregate.
 *
 * <p><strong>Firebase coordination:</strong> The controller creates the Firebase user
 * FIRST (getting a UID), then passes this command to the application service which
 * creates the staff record using that Firebase UID as the ID.
 *
 * <p><strong>Defaults:</strong>
 * <ul>
 *   <li>{@code password} — defaults to "Password123!" if not provided (prototype default)</li>
 *   <li>{@code role} — defaults to "STAFF" if not provided</li>
 *   <li>{@code defaultLeaveEntitlement} — if 0 or negative, defaults to 25 in the service layer</li>
 * </ul>
 *
 * @param firstName              the staff member's first name
 * @param surname                the staff member's surname
 * @param email                  the staff member's email (must be unique)
 * @param department             the department (e.g., "Networks", "Digital")
 * @param lineManagerId          the UUID of the staff member's line manager
 * @param hireDate               the date the staff member was hired
 * @param currentRole            the job title (e.g., "Software Engineer")
 * @param startDateOfCurrentRole when the current role started
 * @param jobLevel               the seniority level (e.g., "JUNIOR", "MID", "SENIOR")
 * @param employmentType         the contract type (FULL_TIME, PART_TIME, CONTRACT)
 * @param defaultLeaveEntitlement annual leave days (defaults to 25 if not specified)
 * @param password               optional password for Firebase account (defaults to Password123!)
 * @param role                   optional role for Firebase custom claim (defaults to STAFF)
 */
public record AddStaffMemberCommand(
        String firstName,               // Staff member's first name
        String surname,                 // Staff member's surname
        String email,                   // Must be unique (checked by Firebase and repository)
        String department,              // Department name
        String lineManagerId,           // Line manager's UUID
        LocalDate hireDate,             // Date hired (cannot be in the future)
        String currentRole,             // Job title
        LocalDate startDateOfCurrentRole, // When current role started
        String jobLevel,                // Seniority level (JUNIOR, MID, SENIOR, etc.)
        String employmentType,          // Contract type (FULL_TIME, PART_TIME, CONTRACT)
        int defaultLeaveEntitlement,    // Annual leave days (0 → default 25)
        String password,                // Optional Firebase password (default: Password123!)
        String role                     // Optional Firebase role (default: STAFF)
) {
    /** Default password for Firebase account creation if none specified */
    public static final String DEFAULT_PASSWORD = "Password123!";

    /** Default role for Firebase custom claims if none specified */
    public static final String DEFAULT_ROLE = "STAFF";

    /**
     * Returns the password to use for Firebase account creation.
     * If the command's password is null or blank, returns the default "Password123!".
     *
     * @return the effective password for Firebase user creation
     */
    public String effectivePassword() {
        return (password != null && !password.isBlank()) ? password : DEFAULT_PASSWORD;
    }

    /**
     * Returns the role to use for Firebase custom claims.
     * If the command's role is null or blank, returns "STAFF".
     *
     * @return the effective role for Firebase custom claims
     */
    public String effectiveRole() {
        return (role != null && !role.isBlank()) ? role : DEFAULT_ROLE;
    }
}
