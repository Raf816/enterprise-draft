package com.staffs.leavebooking.leavemanagement.application.commands;

import java.time.LocalDate;

/**
 * CQRS Command record for submitting a new leave request
 * (Lecture 6 — CQRS Commands).
 *
 * <p><strong>Built by:</strong> {@code LeaveRequestController.submitLeaveRequest()},
 * which extracts the staffMemberId from the JWT and resolves the managerId from the
 * staff member's assigned lineManagerId.
 *
 * <p><strong>Security best practice:</strong> The {@code staffMemberId} is resolved from
 * the authenticated user's JWT token. The {@code managerId} is resolved from the staff
 * member's assigned lineManagerId in their staff record — NEVER accepted from the HTTP
 * request body. This prevents users from routing requests to arbitrary people.
 *
 * <p><strong>Immutability:</strong> As a Java record, this command is immutable after
 * construction. This is important for commands because they should not be modified
 * after being created — they represent a point-in-time intention.
 *
 * @param staffMemberId the UUID of the staff member submitting the request (from JWT)
 * @param managerId     the UUID of the assigned line manager (resolved from staff record)
 * @param startDate     the first day of the leave period
 * @param endDate       the last day of the leave period
 * @param leaveType     the type of leave as a string (e.g., "ANNUAL") — parsed to enum in the service
 * @param reason        optional reason for the leave request (may be null)
 * @see com.staffs.leavebooking.leavemanagement.application.handlers.LeaveRequestApplicationService#submitNewRequest(SubmitLeaveRequestCommand) for the command handler
 * @see com.staffs.leavebooking.leavemanagement.ui.SubmitLeaveRequestBody for the HTTP request body this command is built from
 * @see com.staffs.leavebooking.leavemanagement.domain.LeaveRequest#submitNew for the domain factory method that processes this command
 */
public record SubmitLeaveRequestCommand(
        String staffMemberId,   // From JWT — the authenticated user's Firebase UID
        String managerId,       // Resolved from staff record's lineManagerId — not from request body
        LocalDate startDate,    // From request body — first day of leave (validated: today or future)
        LocalDate endDate,      // From request body — last day of leave (domain validates >= startDate)
        String leaveType,       // From request body — e.g., "ANNUAL" (parsed to LeaveType enum in service)
        String reason           // From request body — optional free-text reason (nullable)
) {
}
