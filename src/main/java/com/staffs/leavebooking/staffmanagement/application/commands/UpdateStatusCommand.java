package com.staffs.leavebooking.staffmanagement.application.commands;

/**
 * CQRS Command record for updating a staff member's employment status
 * (Lecture 6 — CQRS Commands).
 *
 * <p><strong>Key transitions:</strong>
 * <ul>
 *   <li>PENDING_SETUP → ACTIVE: triggers {@code StaffMemberAddedEvent} which creates
 *       the LeaveAllowance in the Leave Management context</li>
 *   <li>ACTIVE → TERMINATED: soft-delete (terminal state, cannot be undone)</li>
 * </ul>
 *
 * <p>The aggregate enforces the state machine invariant: TERMINATED cannot
 * transition to any other state (throws {@code IllegalStateException}).
 *
 * @param staffMemberId    the UUID of the staff member to update
 * @param employmentStatus the new status (PENDING_SETUP, ACTIVE, ON_LEAVE, TERMINATED)
 */
public record UpdateStatusCommand(
        String staffMemberId,      // Which staff member to update
        String employmentStatus    // New status value (validated by EmploymentStatus.valueOf())
) {
}
