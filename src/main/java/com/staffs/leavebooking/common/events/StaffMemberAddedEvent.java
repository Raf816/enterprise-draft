package com.staffs.leavebooking.common.events;

import java.time.LocalDate;

/**
 * Remote event raised when a new staff member is activated (PENDING_SETUP → ACTIVE)
 * in the Staff Management context (Lecture 8 — Remote Events, Cross-Context Communication).
 *
 * <p><strong>Producer:</strong> {@code StaffMember.updateStatus(ACTIVE)} in Staff Management.
 * The event is NOT raised on creation (createNew/createSkeleton) — it fires when the
 * admin activates the staff member. This ensures the LeaveAllowance is created with
 * the correct department, manager, and leave entitlement (which may be set after creation).
 *
 * <p><strong>Consumer:</strong> {@code StaffMemberAddedListener} in Leave Management
 * (via RabbitMQ queue: {@code leave-management.staff-member-added}).
 * Creates a new {@code LeaveAllowance} record with the specified entitlement.
 *
 * <p><strong>Why this lives in common/events/:</strong> Both the producing context
 * (Staff Management) and the consuming context (Leave Management) need to see this
 * record class. Placing it in the Shared Kernel satisfies both without creating a
 * direct dependency between the two business contexts (Lecture 4 — Shared Kernel).
 *
 * <p><strong>Routing:</strong> Published to exchange {@code staff-management} with
 * routing key {@code staff.member.added} (configured in application.yaml).
 *
 * <p><strong>Lecture 8 equivalence:</strong> Analogous to the case study's
 * {@code NewRestaurantAddedEvent} that created an {@code OrderRestaurant} snapshot
 * in the Ordering context.
 *
 * @param id                 the event store surrogate ID (null before persistence, set via withId)
 * @param occurredOn         the date the event occurred
 * @param staffMemberId      the UUID of the new staff member (= Firebase UID)
 * @param firstName          the staff member's first name
 * @param surname            the staff member's surname
 * @param email              the staff member's email address
 * @param managerId          the UUID of the staff member's line manager
 * @param department         the department the staff member belongs to
 * @param defaultEntitlement the initial annual leave entitlement in days (default: 25)
 */
public record StaffMemberAddedEvent(
        Long id,                    // Event store surrogate ID (null until persisted)
        LocalDate occurredOn,       // When this event was raised
        String staffMemberId,       // The new staff member's UUID (= Firebase UID)
        String firstName,           // Staff member's first name (for LeaveAllowance staffName)
        String surname,             // Staff member's surname (for LeaveAllowance staffName)
        String email,               // Staff member's email (for audit/logging)
        String managerId,           // Line manager's UUID (needed for LeaveAllowance.managerId)
        String department,          // Department name (needed for LeaveAllowance.department)
        int defaultEntitlement      // Annual leave days (e.g., 25 — used as totalEntitlement)
) implements RemoteEvent {

    /**
     * Convenience constructor — used when the event is first raised by the aggregate.
     * The {@code id} is null because the event hasn't been persisted to the event store yet.
     * The id will be assigned by the database and attached via {@link #withId(Long)}.
     */
    public StaffMemberAddedEvent(LocalDate occurredOn, String staffMemberId, String firstName,
                                  String surname, String email, String managerId,
                                  String department, int defaultEntitlement) {
        // Delegate to the canonical constructor with id=null
        this(null, occurredOn, staffMemberId, firstName, surname, email,
                managerId, department, defaultEntitlement);
    }

    /**
     * Wither method — creates a copy of this event with the database-assigned ID.
     * Called by {@link DomainEventManager} after persisting to the event store,
     * so that {@link RemoteOutboxListener} can update the event's delivery status.
     *
     * @param newId the surrogate ID from the event_store table
     * @return a new StaffMemberAddedEvent with all original fields plus the new id
     */
    @Override
    public StaffMemberAddedEvent withId(Long newId) {
        return new StaffMemberAddedEvent(newId, this.occurredOn, this.staffMemberId,
                this.firstName, this.surname, this.email, this.managerId,
                this.department, this.defaultEntitlement);
    }
}
