package com.staffs.leavebooking.staffmanagement.domain;

/**
 * Enum representing the lifecycle states of a staff member
 * (Lecture 3 — Aggregate Invariants, State Machines).
 *
 * <p><strong>State machine transitions:</strong>
 * <pre>
 * PENDING_SETUP → ACTIVE       (admin completes profile and activates — triggers StaffMemberAddedEvent)
 * ACTIVE        → ON_LEAVE     (staff goes on leave)
 * ON_LEAVE      → ACTIVE       (staff returns from leave)
 * ACTIVE        → TERMINATED   (terminal — soft-delete, cannot be undone)
 * ON_LEAVE      → TERMINATED   (terminal — can terminate while on leave)
 * PENDING_SETUP → TERMINATED   (terminal — admin can terminate before activation)
 * </pre>
 *
 * <p><strong>PENDING_SETUP:</strong> Initial state for all new staff members.
 * When a user self-registers via POST /auth/register, a skeleton staff record
 * is created with status PENDING_SETUP. The admin fills in department, manager,
 * role, etc., then activates the user by setting status to ACTIVE.
 * This activation triggers the {@code StaffMemberAddedEvent} which creates
 * the staff member's {@code LeaveAllowance} in the Leave Management context.
 *
 * <p><strong>TERMINATED is terminal:</strong> The aggregate enforces that TERMINATED
 * cannot transition to any other state. This is a soft-delete pattern — the record
 * stays in the database for audit purposes, but the staff member is effectively removed
 * from the active workforce. This is preferred over hard DELETE for compliance.
 *
 * @see StaffMember#updateStatus(EmploymentStatus) for the invariant enforcement
 */
public enum EmploymentStatus {
    PENDING_SETUP,  // New staff member — profile incomplete, awaiting admin activation
    ACTIVE,         // Active employee — can submit leave requests
    ON_LEAVE,       // Currently on leave
    TERMINATED      // Soft-deleted — terminal state, cannot be reactivated
}
