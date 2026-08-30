package com.staffs.leavebooking.common.domain;

import com.staffs.leavebooking.common.events.Event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base class for all Aggregate Roots (Lecture 3 — Aggregates, Lecture 7 — Domain Events).
 *
 * <p><strong>DDD Concept (Lecture 3):</strong> An Aggregate Root is the entry point
 * to an aggregate — a cluster of related objects treated as a single unit for data changes.
 * All external access to the aggregate goes through the root. The root enforces
 * invariants (business rules) that must always be true for the aggregate.
 *
 * <p><strong>Aggregates in this system:</strong>
 * <ul>
 *   <li>{@code LeaveRequest} — the root of the leave request aggregate (manages state machine transitions)</li>
 *   <li>{@code LeaveAllowance} — the root of the leave balance aggregate (manages day calculations)</li>
 *   <li>{@code StaffMember} — the root of the staff member aggregate (manages employment lifecycle)</li>
 * </ul>
 *
 * <p><strong>Domain Events (Lecture 7):</strong> When an aggregate's state changes,
 * it raises domain events. These events are stored in the {@code domainEvents} list
 * and are extracted by the application service after the command completes.
 * The application service then passes them to {@code DomainEventManager} for
 * persistence and dispatch.
 *
 * <p><strong>Uni-directional event flow:</strong>
 * <pre>
 * 1. Controller → Facade → ApplicationService calls aggregate command method
 * 2. Aggregate method changes state + calls addDomainEvent(new SomeEvent(...))
 * 3. ApplicationService saves aggregate to repository
 * 4. ApplicationService extracts events: aggregate.listOfDomainEvents()
 * 5. ApplicationService passes events to DomainEventManager for dispatch
 * 6. ApplicationService calls aggregate.clearDomainEvents()
 * </pre>
 *
 * <p><strong>Why events live on the aggregate:</strong> This keeps event creation
 * co-located with the business logic that triggers them. The aggregate knows
 * exactly what happened, so it creates the most accurate event possible.
 *
 * @param <T> the specific aggregate type (flows through to Identity&lt;T&gt;)
 * @see Entity for identity-based equality
 * @see Event for the event interface
 */
public abstract class AggregateRoot<T> extends Entity<T> {

    /**
     * Internal list of domain events raised during command processing.
     * Events accumulate here as the aggregate processes commands,
     * then the application service extracts and dispatches them.
     *
     * <p>Using ArrayList because:
     * - Order matters (events should be dispatched in the order they were raised)
     * - Random access is not needed, but sequential iteration is
     * - The list is typically small (1-2 events per command)
     */
    private final List<Event> domainEvents = new ArrayList<>();

    /**
     * Constructor — requires an identity (inherited from Entity).
     * Protected because only concrete aggregate classes should be instantiated.
     *
     * @param id the unique identity for this aggregate root
     */
    protected AggregateRoot(Identity<T> id) {
        super(id); // Delegate to Entity constructor which validates non-null
    }

    /**
     * Adds a domain event to the internal list.
     * Called by aggregate command methods when a state change occurs.
     * Protected because only the aggregate itself should raise its own events.
     *
     * <p><strong>Example usage in LeaveRequest:</strong>
     * <pre>
     * public void approve(String decidedBy, String reason) {
     *     this.status = APPROVED;
     *     addDomainEvent(new LeaveRequestApprovedEvent(...));
     * }
     * </pre>
     *
     * @param event the domain event to add
     */
    protected void addDomainEvent(Event event) {
        domainEvents.add(event);
    }

    /**
     * Removes a specific event from the internal list.
     * Rarely used — provided for completeness in case an event needs to be
     * retracted before dispatch (e.g., if a later validation fails).
     *
     * @param event the domain event to remove
     */
    protected void removeDomainEvent(Event event) {
        domainEvents.remove(event);
    }

    /**
     * Returns an unmodifiable view of the domain events list.
     * Called by the application service AFTER the command completes
     * to extract events for dispatch via DomainEventManager.
     *
     * <p>Unmodifiable so callers cannot accidentally add/remove events —
     * only the aggregate itself controls its event list.
     *
     * @return immutable list of pending domain events
     */
    public List<Event> listOfDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    /**
     * Clears all domain events from the internal list.
     * Called by the application service AFTER events have been
     * extracted and dispatched. This prevents events from being
     * dispatched twice if the aggregate is reused.
     */
    public void clearDomainEvents() {
        domainEvents.clear();
    }

    /**
     * Checks whether the aggregate has any pending domain events.
     * Used by the application service to decide whether to invoke
     * the DomainEventManager (skip if no events to dispatch).
     *
     * @return true if there are pending domain events
     */
    public boolean domainEventsExist() {
        return !domainEvents.isEmpty();
    }
}
