package com.staffs.leavebooking.leavemanagement.application.commands;

/**
 * CQRS Command record for an admin to amend a staff member's total leave entitlement
 * (Lecture 5/6 — CQRS Command Pattern).
 *
 * <p><strong>CQRS Command:</strong> In the CQRS pattern, commands represent intentions
 * to change the system's state. This command captures the data needed to amend the
 * total entitlement on a {@link com.staffs.leavebooking.leavemanagement.domain.LeaveAllowance}
 * aggregate. It is constructed by the controller and passed through the facade to the
 * {@link com.staffs.leavebooking.leavemanagement.application.handlers.LeaveAllowanceApplicationService}.
 *
 * <p><strong>Admin-only operation:</strong> Entitlement amendments are restricted to
 * administrators. The facade enforces this via {@code @PreAuthorize("hasRole('ADMIN')")}.
 * Typical use cases include correcting an initial entitlement, adjusting for part-time
 * staff, or granting additional days for long service.
 *
 * <p><strong>Domain validation:</strong> The domain aggregate validates that the new
 * entitlement is not less than the days already used, preventing an allowance from
 * going into a negative balance.
 *
 * @param leaveAllowanceId the UUID of the leave allowance record to amend
 * @param newEntitlement   the new total leave entitlement (in days) for the business year
 * @see com.staffs.leavebooking.leavemanagement.application.handlers.LeaveAllowanceApplicationService#amendEntitlement(AmendEntitlementCommand) for the command handler
 * @see com.staffs.leavebooking.leavemanagement.domain.LeaveAllowance#amendEntitlement for the domain method that processes this command
 * @see com.staffs.leavebooking.leavemanagement.ui.LeaveAllowanceController for the REST endpoint that creates this command
 */
public record AmendEntitlementCommand(
        String leaveAllowanceId,    // UUID of the leave allowance — from URL path variable
        int newEntitlement          // New total entitlement in days — from request body
) {
}
