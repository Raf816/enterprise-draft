package com.staffs.leavebooking.leavemanagement.application.commands;

import java.time.LocalDate;

/**
 * CQRS Command record for submitting a new leave request
 * (Lecture 5/6 — CQRS Command Pattern).
 *
 * <p><strong>CQRS Command:</strong> In the CQRS pattern, commands represent intentions
 * to change the system's state. This command captures all the data needed to create a
 * new {@link com.staffs.leavebooking.leavemanagement.domain.LeaveRequest} aggregate.
 * It is constructed by the controller and passed through the facade to the
 * {@link com.staffs.leavebooking.leavemanagement.application.handlers.LeaveRequestApplicationService}.
 *
 * <p><strong>Security best practice:</strong> The {@code staffMemberId} and {@code managerId}
 * are resolved from the authenticated user's JWT token and the request body respectively —
 * the staffMemberId is NEVER accepted from the HTTP request body. This prevents users
 * from submitting leave requests on behalf of other staff members.
 *
 * <p><strong>Immutability:</strong> As a Java record, this command is immutable after
 * construction. This is important for commands because they should not be modified
 * after being created — they represent a point-in-time intention.
 *
 * @param staffMemberId the UUID of the staff member submitting the request (from JWT, not body)
 * @param managerId     the UUID of the manager who will approve/reject this request
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
        String managerId,       // From request body — the assigned approver
        LocalDate startDate,    // From request body — first day of leave (validated: today or future)
        LocalDate endDate,      // From request body — last day of leave (domain validates >= startDate)
        String leaveType,       // From request body — e.g., "ANNUAL" (parsed to LeaveType enum in service)
        String reason           // From request body — optional free-text reason (nullable)
) {
}
