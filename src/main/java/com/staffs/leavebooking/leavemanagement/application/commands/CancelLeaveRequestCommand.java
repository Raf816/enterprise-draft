package com.staffs.leavebooking.leavemanagement.application.commands;

/**
 * CQRS Command record for cancelling an existing leave request
 * (Lecture 5/6 — CQRS Command Pattern).
 *
 * <p><strong>CQRS Command:</strong> In the CQRS pattern, commands represent intentions
 * to change the system's state. This command captures all the data needed to cancel a
 * {@link com.staffs.leavebooking.leavemanagement.domain.LeaveRequest} aggregate.
 * It is constructed by the controller and passed through the facade to the
 * {@link com.staffs.leavebooking.leavemanagement.application.handlers.LeaveRequestApplicationService}.
 *
 * <p><strong>Cancellation rules:</strong> A leave request can be cancelled from either
 * PENDING or APPROVED status. The domain aggregate enforces the valid state transitions:
 * <ul>
 *   <li>PENDING → CANCELLED: releases reserved (pending) days back to the allowance</li>
 *   <li>APPROVED → CANCELLED: credits back used days to the allowance</li>
 *   <li>REJECTED → CANCELLED: not allowed (already in a terminal state)</li>
 * </ul>
 *
 * <p><strong>Audit trail:</strong> The {@code cancelledBy} field records who cancelled
 * the request (staff member themselves or an admin), and the optional {@code reason}
 * field captures why. Both are persisted on the leave request for audit purposes.
 *
 * @param leaveRequestId the UUID of the leave request to cancel
 * @param cancelledBy    the UUID of the user performing the cancellation (from JWT)
 * @param reason         optional reason for the cancellation (may be null)
 * @see com.staffs.leavebooking.leavemanagement.application.handlers.LeaveRequestApplicationService#cancelRequest(CancelLeaveRequestCommand) for the command handler
 * @see com.staffs.leavebooking.leavemanagement.domain.LeaveRequest#cancel for the domain method that processes this command
 */
public record CancelLeaveRequestCommand(
        String leaveRequestId,  // UUID of the leave request to cancel — from URL path variable
        String cancelledBy,     // UUID of the canceller — from JWT (staff member or admin)
        String reason           // Optional cancellation reason — from request body (nullable)
) {
}
