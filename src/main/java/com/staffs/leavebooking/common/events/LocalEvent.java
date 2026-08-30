package com.staffs.leavebooking.common.events;

/**
 * Marker interface for events that are raised and consumed within a single bounded context
 * (Lecture 7 — Local Domain Events, "Simpler Subscriber" pattern).
 *
 * <p><strong>How local events work:</strong>
 * <ol>
 *   <li>An aggregate command method raises the event: {@code addDomainEvent(new LeaveRequestApprovedEvent(...))}</li>
 *   <li>The application service extracts events and passes them to {@link DomainEventManager}</li>
 *   <li>{@link DomainEventManager} persists the event to the event store (status: LOCAL)
 *       and publishes it via Spring's {@code ApplicationEventPublisher}</li>
 *   <li>A {@code @TransactionalEventListener(AFTER_COMMIT)} in the SAME bounded context
 *       receives the event and performs a side effect (e.g., updating LeaveAllowance days)</li>
 * </ol>
 *
 * <p><strong>Local events in this system (all in Leave Management):</strong>
 * <ul>
 *   <li>{@code LeaveRequestSubmittedEvent} — reserves pending days on the allowance</li>
 *   <li>{@code LeaveRequestApprovedEvent} — confirms days (pending → used)</li>
 *   <li>{@code LeaveRequestRejectedEvent} — releases pending days back to available</li>
 *   <li>{@code LeaveRequestCancelledEvent} — credits days back (used→available or pending→available)</li>
 * </ul>
 *
 * <p><strong>Contrast with {@link RemoteEvent}:</strong> Local events never leave the
 * process boundary. They use Spring's in-memory event bus, not RabbitMQ. They are
 * processed within the same JVM, just on a separate thread (after the transaction commits).
 *
 * @see RemoteEvent for events that cross bounded context boundaries via RabbitMQ
 */
public interface LocalEvent extends Event {
    // Marker interface — no additional methods beyond those inherited from Event.
    // The distinction between LocalEvent and RemoteEvent drives routing:
    // - LocalEvent → processed in-memory by @TransactionalEventListener
    // - RemoteEvent → published to RabbitMQ by RemoteOutboxListener
}
