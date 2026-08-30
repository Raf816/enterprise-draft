package com.staffs.leavebooking.staffmanagement.application.commands;

/**
 * CQRS Command record for updating a staff member's department and line manager
 * (Lecture 6 — CQRS Commands).
 *
 * <p><strong>Triggers:</strong> {@code StaffMemberUpdatedEvent} (remote event)
 * which syncs the department and manager on the staff member's LeaveAllowance
 * in the Leave Management context.
 *
 * <p><strong>Supports partial updates:</strong> If either field is null, the
 * application service retains the current value (null = "no change").
 *
 * @param staffMemberId the UUID of the staff member to update
 * @param department    the new department name (null = keep current)
 * @param lineManagerId the new line manager's UUID (null = keep current)
 */
public record UpdateDepartmentCommand(
        String staffMemberId,   // Which staff member to update
        String department,      // New department (null = no change)
        String lineManagerId    // New line manager UUID (null = no change)
) {
}
