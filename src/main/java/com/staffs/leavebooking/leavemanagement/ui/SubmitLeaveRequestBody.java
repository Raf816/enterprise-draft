package com.staffs.leavebooking.leavemanagement.ui;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * HTTP request body record for POST /leave-requests (submit a new leave request)
 * (Lecture 4 — User Interface Layer, Input Validation).
 *
 * <p><strong>Security best practice:</strong> The {@code staffMemberId} is NOT included
 * in this body. It is derived from the authenticated user's JWT token in the controller.
 * This prevents users from submitting leave requests on behalf of other staff members
 * by injecting a different staffMemberId in the request body.
 *
 * <p><strong>Bean Validation (Jakarta Validation):</strong> This record uses Jakarta
 * Bean Validation annotations to provide early feedback at the controller layer before
 * the request reaches the domain layer. If validation fails, Spring returns a 400 Bad
 * Request with field-level error messages before the facade is even called.
 *
 * <p><strong>Validation strategy:</strong> This is the first line of defence (UI-level).
 * The domain aggregate ({@link com.staffs.leavebooking.leavemanagement.domain.LeaveRequest})
 * performs its own validation via DomainAssertions as the second line of defence,
 * ensuring invariants hold even if the request bypasses the controller.
 *
 * @param managerId the UUID of the manager who will approve/reject this request (required)
 * @param startDate the first day of leave — must be today or in the future (required)
 * @param endDate   the last day of leave (required; domain validates endDate >= startDate)
 * @param leaveType the type of leave (e.g., "ANNUAL") (required)
 * @param reason    optional reason for the leave request (may be null)
 * @see com.staffs.leavebooking.leavemanagement.ui.LeaveRequestController#submitLeaveRequest for the endpoint that uses this body
 * @see com.staffs.leavebooking.leavemanagement.application.commands.SubmitLeaveRequestCommand for the CQRS command built from this body
 */
public record SubmitLeaveRequestBody(
        /** Manager UUID — required; the manager who will review this request. */
        @NotBlank(message = "Manager ID is required")       // Bean Validation: rejects null, empty, and blank strings
        String managerId,

        /** First day of leave — required; must be today or in the future (no retroactive requests). */
        @NotNull(message = "Start date is required")         // Bean Validation: rejects null values
        @FutureOrPresent(message = "Start date must be today or in the future") // Bean Validation: no past dates
        LocalDate startDate,

        /** Last day of leave — required; domain validates that endDate >= startDate. */
        @NotNull(message = "End date is required")           // Bean Validation: rejects null values
        LocalDate endDate,

        /** Leave type (e.g., "ANNUAL") — required; validated against the LeaveType enum in the domain. */
        @NotBlank(message = "Leave type is required")        // Bean Validation: rejects null, empty, and blank strings
        String leaveType,

        /**
         * Optional reason for the leave request.
         * No validation annotation — null and blank are both acceptable.
         */
        String reason
) {}
