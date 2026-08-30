package com.staffs.leavebooking.leavemanagement.domain.events;

import com.staffs.leavebooking.common.events.LocalEvent;

import java.time.LocalDate;

/**
 * Local domain event raised when a new leave request is submitted
 * (Lecture 7 — Domain Events, "Simpler Subscriber" pattern).
 *
 * <p><strong>DDD Concept (Lecture 7):</strong> This is a local event — it is raised by the
 * {@link com.staffs.leavebooking.leavemanagement.domain.LeaveRequest} aggregate and consumed
 * within the same bounded context (Leave Management). It never crosses bounded-context
 * boundaries (contrast with remote events that travel via RabbitMQ).
 *
 * <p><strong>Raised by:</strong>
 * {@link com.staffs.leavebooking.leavemanagement.domain.LeaveRequest#submitNew} — the write-path
 * factory method that creates a new leave request in PENDING status.
 *
 * <p><strong>Consumed by:</strong> A {@code @TransactionalEventListener(AFTER_COMMIT)} that calls
 * {@link com.staffs.leavebooking.leavemanagement.domain.LeaveAllowance#reserveDays(int)}
 * to hold the requested days as "pending" on the staff member's allowance.
 *
 * <p><strong>Allowance impact:</strong> {@code daysPending += numberOfDays}
 *
 * <p><strong>Event flow:</strong>
 * <pre>
 * 1. LeaveRequest.submitNew() → addDomainEvent(new LeaveRequestSubmittedEvent(...))
 * 2. ApplicationService saves LeaveRequest to repository
 * 3. ApplicationService extracts events and passes them to DomainEventManager
 * 4. DomainEventManager persists event to event_store (status: LOCAL) and publishes via Spring
 * 5. @TransactionalEventListener receives event AFTER_COMMIT
 * 6. Listener calls LeaveAllowance.reserveDays(event.numberOfDays())
 * </pre>
 *
 * <p><strong>Immutability:</strong> Implemented as a Java {@code record} so all fields are
 * final and the event is immutable — events represent historical facts that cannot change.
 * The {@link #withId(Long)} method creates a new copy with the database-assigned ID
 * (the "wither" pattern for immutable objects).
 *
 * @param id              surrogate ID from the event store (null until persisted)
 * @param occurredOn      the date this event occurred (typically today)
 * @param leaveRequestId  the ID of the leave request that was submitted
 * @param staffMemberId   the ID of the staff member who submitted the request
 * @param numberOfDays    the number of working days to reserve on the allowance
 * @see LocalEvent for the marker interface for in-process events
 * @see com.staffs.leavebooking.leavemanagement.domain.LeaveRequest#submitNew for the source of this event
 * @see com.staffs.leavebooking.leavemanagement.domain.LeaveAllowance#reserveDays(int) for the handler action
 */
public record LeaveRequestSubmittedEvent(
        /** Surrogate ID assigned by the event store after persistence. Null when first created. */
        Long id,

        /** The date this event occurred — used for auditing and event ordering. */
        LocalDate occurredOn,

        /** The unique ID of the leave request that was submitted. Used to correlate events to requests. */
        String leaveRequestId,

        /** The ID of the staff member — used to look up the correct LeaveAllowance for day reservation. */
        String staffMemberId,

        /** The number of working days to reserve — passed to LeaveAllowance.reserveDays(). */
        int numberOfDays
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
     * @param occurredOn     the date this event occurred
     * @param leaveRequestId the ID of the submitted leave request
     * @param staffMemberId  the ID of the requesting staff member
     * @param numberOfDays   the number of working days to reserve
     */
    public LeaveRequestSubmittedEvent(LocalDate occurredOn, String leaveRequestId,
                                      String staffMemberId, int numberOfDays) {
        this(null, occurredOn, leaveRequestId, staffMemberId, numberOfDays); // Delegate with null ID
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
     * event's persisted ID (needed for status updates like marking as PUBLISHED).
     *
     * @param newId the surrogate ID from the event_store table
     * @return a new {@code LeaveRequestSubmittedEvent} identical to this one but with the given ID
     */
    @Override
    public LeaveRequestSubmittedEvent withId(Long newId) {
        // Create a new record instance with the database ID and all original field values
        return new LeaveRequestSubmittedEvent(newId, this.occurredOn, this.leaveRequestId,
                this.staffMemberId, this.numberOfDays);
    }
}
