package com.staffs.leavebooking.common.events;

import java.time.LocalDate;

/**
 * Remote event raised when a staff member's department or line manager is changed
 * in the Staff Management context (Lecture 8 — Remote Events).
 *
 * <p><strong>Producer:</strong> {@code StaffMember.updateDepartment()} in Staff Management.
 * Raised whenever the department or lineManagerId changes, triggering a sync to
 * the Leave Management context.
 *
 * <p><strong>Consumer:</strong> {@code StaffMemberUpdatedListener} in Leave Management
 * (via RabbitMQ queue: {@code leave-management.staff-member-updated}).
 * Updates the denormalised {@code managerId} and {@code department} fields on the
 * staff member's {@code LeaveAllowance} record.
 *
 * <p><strong>Why denormalise?</strong> The LeaveAllowance stores a snapshot of the
 * staff member's department and manager so that leave queries (e.g., "show team
 * allowances for Lucy's team") can be answered without querying the Staff Management
 * context. This event keeps that snapshot in sync when the source data changes.
 *
 * <p><strong>Routing:</strong> Published to exchange {@code staff-management} with
 * routing key {@code staff.member.updated} (configured in application.yaml).
 *
 * @param id            the event store surrogate ID (null before persistence)
 * @param occurredOn    the date the update occurred
 * @param staffMemberId the UUID of the staff member whose details changed
 * @param managerId     the new line manager's UUID (may be same if only dept changed)
 * @param department    the new department name (may be same if only manager changed)
 */
public record StaffMemberUpdatedEvent(
        Long id,                // Event store surrogate ID (null until persisted)
        LocalDate occurredOn,   // When this event was raised
        String staffMemberId,   // The staff member whose details changed
        String managerId,       // The new/current line manager UUID
        String department       // The new/current department name
) implements RemoteEvent {

    /**
     * Convenience constructor — used when the event is first raised (no ORM id yet).
     */
    public StaffMemberUpdatedEvent(LocalDate occurredOn, String staffMemberId,
                                    String managerId, String department) {
        this(null, occurredOn, staffMemberId, managerId, department);
    }

    /**
     * Wither method — creates a copy with the database-assigned ID attached.
     */
    @Override
    public StaffMemberUpdatedEvent withId(Long newId) {
        return new StaffMemberUpdatedEvent(newId, this.occurredOn, this.staffMemberId,
                this.managerId, this.department);
    }
}
