package com.staffs.leavebooking.staffmanagement.application.commands;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * CQRS Command record for creating a new staff member via admin POST /staff
 * (Lecture 6 — CQRS Commands).
 *
 * <p><strong>Bean Validation:</strong> Validates at the controller level before Firebase
 * user creation, preventing orphan Firebase accounts when domain validation would fail.
 *
 * @param firstName              the staff member's first name (required, max 50)
 * @param surname                the staff member's surname (required, max 50)
 * @param email                  the staff member's email (required, must be unique)
 * @param department             the department (required)
 * @param lineManagerId          the UUID of the staff member's line manager (optional)
 * @param hireDate               the date hired (required, must not be in the future)
 * @param currentRole            the job title (required)
 * @param startDateOfCurrentRole when the current role started (required)
 * @param jobLevel               the seniority level (optional)
 * @param employmentType         the contract type: FULL_TIME, PART_TIME, CONTRACT (required)
 * @param defaultLeaveEntitlement annual leave days (0 or negative defaults to 25)
 * @param password               optional password for Firebase (defaults to Password123!)
 * @param role                   optional role for Firebase custom claim (defaults to STAFF)
 */
public record AddStaffMemberCommand(

        @NotBlank(message = "First name is required")
        @Size(max = 50, message = "First name must not exceed 50 characters")
        String firstName,

        @NotBlank(message = "Surname is required")
        @Size(max = 50, message = "Surname must not exceed 50 characters")
        String surname,

        @NotBlank(message = "Email is required")
        String email,

        @NotBlank(message = "Department is required")
        String department,

        String lineManagerId,           // Optional — nullable

        @NotNull(message = "Hire date is required")
        @PastOrPresent(message = "Hire date cannot be in the future")
        LocalDate hireDate,

        @NotBlank(message = "Current role is required")
        String currentRole,

        @NotNull(message = "Start date of current role is required")
        LocalDate startDateOfCurrentRole,

        String jobLevel,                // Optional — nullable

        @NotBlank(message = "Employment type is required")
        String employmentType,

        int defaultLeaveEntitlement,    // 0 or negative defaults to 25 in the service

        String password,                // Optional — defaults to Password123!
        String role                     // Optional — defaults to STAFF
) {
    public static final String DEFAULT_PASSWORD = "Password123!";
    public static final String DEFAULT_ROLE = "STAFF";

    public String effectivePassword() {
        return (password != null && !password.isBlank()) ? password : DEFAULT_PASSWORD;
    }

    public String effectiveRole() {
        return (role != null && !role.isBlank()) ? role : DEFAULT_ROLE;
    }
}
