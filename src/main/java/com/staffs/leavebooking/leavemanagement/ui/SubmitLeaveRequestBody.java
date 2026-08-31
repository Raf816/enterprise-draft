package com.staffs.leavebooking.leavemanagement.ui;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * HTTP request body record for POST /leave-requests (submit a new leave request).
 *
 * <p><strong>Fields:</strong>
 * <ul>
 *   <li>{@code managerId} — optional. If not provided, auto-resolved from the staff member's
 *       assigned lineManagerId in their staff record. If provided, validated to be a real staff member.</li>
 *   <li>{@code startDate} — required, must be today or future.</li>
 *   <li>{@code endDate} — required, must be today or future. Domain also validates endDate >= startDate.</li>
 *   <li>{@code leaveType} — required, validated against LeaveType enum in domain.</li>
 *   <li>{@code reason} — optional, max 500 characters.</li>
 * </ul>
 *
 * <p><strong>Security:</strong> staffMemberId is NOT in this body — it comes from the JWT token.
 */
public record SubmitLeaveRequestBody(

        /** Manager UUID — optional. If null/blank, resolved from staff record's lineManagerId. */
        String managerId,

        /** First day of leave — required, must be today or future. */
        @NotNull(message = "Start date is required")
        @FutureOrPresent(message = "Start date must be today or in the future")
        LocalDate startDate,

        /** Last day of leave — required, must be today or future. Domain validates endDate >= startDate. */
        @NotNull(message = "End date is required")
        @FutureOrPresent(message = "End date must be today or in the future")
        LocalDate endDate,

        /** Leave type (e.g., "ANNUAL") — required. */
        @NotBlank(message = "Leave type is required")
        String leaveType,

        /** Optional reason — max 500 characters. */
        @Size(max = 500, message = "Reason must not exceed 500 characters")
        String reason
) {}
