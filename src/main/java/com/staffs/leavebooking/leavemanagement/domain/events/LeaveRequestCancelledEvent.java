package com.staffs.leavebooking.leavemanagement.domain.events;

import com.staffs.leavebooking.common.events.LocalEvent;

import java.time.LocalDate;

/**
 * Local domain event raised when a leave request is cancelled by a staff member or admin
 * (Lecture 7 — Domain Events, "Simpler Subscriber" pattern).
 *
 * <p><strong>DDD Concept (Lecture 7):</strong> This is a local event — it is raised by the
 * {@link com.staffs.leavebooking.leavemanagement.domain.LeaveRequest} aggregate and consumed
 * within the same bounded context (Leave Management). It never crosses bounded-context
 * boundaries.
 *
 * <p><strong>Raised by:</strong>
 * {@link com.staffs.leavebooking.leavemanagement.domain.LeaveRequest#cancel(String, String)}
 * — the command method that transitions a request from PENDING or APPROVED to CANCELLED.
 *
 * <p><strong>Key business logic — the {@code wasPreviouslyApproved} flag:</strong>
 * Cancellation can happen from two different states, and the allowance impact differs:
 * <ul>
 *   <li>{@code wasPreviouslyApproved = true} (was APPROVED → CANCELLED):
 *       The listener calls {@link com.staffs.leavebooking.leavemanagement.domain.LeaveAllowance#creditBackDays(int)}
 *       to return days from the "used" bucket to "available"
 *       ({@code daysUsed -= numberOfDays})</li>
 *   <li>{@code wasPreviouslyApproved = false} (was PENDING → CANCELLED):
 *       The listener calls {@link com.staffs.leavebooking.leavemanagement.domain.LeaveAllowance#releasePendingDays(int)}
 *       to return days from the "pending" bucket to "available"
 *       ({@code daysPending -= numberOfDays})</li>
 * </ul>
 *
 * <p><strong>Event flow:</strong>
 * <pre>
 * 1. LeaveRequest.cancel(cancelledBy, reason) → addDomainEvent(new LeaveRequestCancelledEvent(...))
 * 2. ApplicationService saves LeaveRequest to repository
 * 3. ApplicationService extracts events and passes them to DomainEventManager
 * 4. DomainEventManager persists event to event_store (status: LOCAL) and publishes via Spring
 * 5. @TransactionalEventListener receives event AFTER_COMMIT
 * 6. Listener checks wasPreviouslyApproved:
 *      - true  → LeaveAllowance.creditBackDays(numberOfDays)
 *      - false → LeaveAllowance.releasePendingDays(numberOfDays)
 * </pre>
 *
 * <p><strong>Immutability:</strong> Implemented as a Java {@code record} so all fields are
 * final and the event is immutable. The {@link #withId(Long)} method creates a new copy
 * with the database-assigned ID (the "wither" pattern).
 *
 * @param id                     surrogate ID from the event store (null until persisted)
 * @param occurredOn             the date this event occurred (typically today)
 * @param leaveRequestId         the ID of the leave request that was cancelled
 * @param staffMemberId          the ID of the staff member whose leave was cancelled
 * @param cancelledBy            the ID of the user who cancelled (could be staff or admin)
 * @param numberOfDays           the number of working days to credit back or release
 * @param wasPreviouslyApproved  true if the request was APPROVED before cancellation (credit back used days),
 *                               false if it was PENDING (release pending days)
 * @see LocalEvent for the marker interface for in-process events
 * @see com.staffs.leavebooking.leavemanagement.domain.LeaveRequest#cancel(String, String) for the source
 * @see com.staffs.leavebooking.leavemanagement.domain.LeaveAllowance#creditBackDays(int) for the approved-cancel path
 * @see com.staffs.leavebooking.leavemanagement.domain.LeaveAllowance#releasePendingDays(int) for the pending-cancel path
 */
public record LeaveRequestCancelledEvent(
        /** Surrogate ID assigned by the event store after persistence. Null when first created. */
        Long id,

        /** The date this event occurred — used for auditing and event ordering. */
        LocalDate occurredOn,

        /** The unique ID of the leave request that was cancelled. Used to correlate events to requests. */
        String leaveRequestId,

        /** The ID of the staff member — used to look up the correct LeaveAllowance. */
        String staffMemberId,

        /** The ID of the user who cancelled the request (may be the staff member or an admin). */
        String cancelledBy,

        /** The number of working days to credit back or release on the allowance. */
        int numberOfDays,

        /**
         * Flag indicating whether the request was APPROVED before cancellation.
         * <ul>
         *   <li>{@code true} → days must be credited back from "used" ({@code daysUsed -= numberOfDays})</li>
         *   <li>{@code false} → days must be released from "pending" ({@code daysPending -= numberOfDays})</li>
         * </ul>
         */
        boolean wasPreviouslyApproved
) implements LocalEvent {

    // ─────────────────────────────────────────────────────────────────
    // CONVENIENCE CONSTRUCTOR
    // ─────────────────────────────────────────────────────────────────

    /**
     * Convenience constructor for use by the aggregate — creates the event WITHOUT a database ID.
     *
     * <p>The {@code id} is set to {@code null} because the event store has not yet assigned
     * a surrogate ID. After persistence, {@link #withId(Long)} is called to create a copy
     * with the database-assigned ID.
     *
     * @param occurredOn             the date this event occurred
     * @param leaveRequestId         the ID of the cancelled leave request
     * @param staffMemberId          the ID of the staff member
     * @param cancelledBy            the ID of the user who cancelled
     * @param numberOfDays           the number of working days affected
     * @param wasPreviouslyApproved  whether the request was approved before cancellation
     */
    public LeaveRequestCancelledEvent(LocalDate occurredOn, String leaveRequestId,
                                       String staffMemberId, String cancelledBy,
                                       int numberOfDays, boolean wasPreviouslyApproved) {
        this(null, occurredOn, leaveRequestId, staffMemberId, cancelledBy,
                numberOfDays, wasPreviouslyApproved); // Delegate with null ID
    }

    // ─────────────────────────────────────────────────────────────────
    // WITHER METHOD (immutable ID assignment)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Creates a new copy of this event with the given database-assigned ID.
     *
     * <p><strong>The "wither" pattern:</strong> Since records are immutable, we cannot
     * set the {@code id} field after construction. Instead, the {@code DomainEventManager}
     * calls this method after persisting the event to the event store, producing a new
     * instance with all original fields plus the database ID. The copy is then published
     * via Spring's {@code ApplicationEventPublisher} so that listeners have access to the
     * event's persisted ID.
     *
     * @param newId the surrogate ID from the event_store table
     * @return a new {@code LeaveRequestCancelledEvent} identical to this one but with the given ID
     */
    @Override
    public LeaveRequestCancelledEvent withId(Long newId) {
        // Create a new record instance with the database ID and all original field values
        return new LeaveRequestCancelledEvent(newId, this.occurredOn, this.leaveRequestId,
                this.staffMemberId, this.cancelledBy, this.numberOfDays, this.wasPreviouslyApproved);
    }
}
