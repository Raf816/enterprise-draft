package com.staffs.leavebooking.common.events;

/**
 * Base interface for all domain events in the system (Lecture 7 — Domain Events).
 *
 * <p><strong>DDD Concept:</strong> Domain events represent something meaningful that
 * happened in the business domain. They are raised by aggregates when their state
 * changes and consumed by listeners that trigger side effects (e.g., updating
 * another aggregate or sending a notification).
 *
 * <p><strong>Immutability:</strong> Events are implemented as Java records, which are
 * immutable by default. Once created, an event's data never changes — this is critical
 * because events represent historical facts ("this thing happened").
 *
 * <p><strong>The id() and withId() pattern:</strong> Events need a surrogate ID from the
 * event_store table so that downstream listeners (like {@link RemoteOutboxListener}) can
 * update the event's delivery status. Since records are immutable, we can't set the ID
 * after construction. The {@code withId()} method creates a new copy of the event with
 * the database-assigned ID attached — this is the "wither" pattern for immutable objects.
 *
 * <p><strong>Event lifecycle:</strong>
 * <pre>
 * 1. Aggregate creates event:     new LeaveRequestApprovedEvent(null, ...)  // id is null
 * 2. EventStoreService.append():  saves to DB, gets auto-generated id (e.g., 42)
 * 3. event.withId(42):            creates a copy with id=42
 * 4. Spring publishes the copy:   listeners receive the event WITH the database id
 * 5. RemoteOutboxListener:        uses event.id() to update status to PUBLISHED
 * </pre>
 *
 * <p><strong>Two subtypes:</strong>
 * <ul>
 *   <li>{@link LocalEvent} — stays within a single bounded context (Spring in-memory)</li>
 *   <li>{@link RemoteEvent} — crosses bounded context boundaries (via RabbitMQ)</li>
 * </ul>
 *
 * @see LocalEvent for in-process events
 * @see RemoteEvent for cross-context events via RabbitMQ
 */
public interface Event {

    /**
     * Returns the surrogate id assigned by the event store after persistence.
     * This is {@code null} when the event is first created by the aggregate,
     * and populated after {@link EventStoreService#append(Event, String)} saves it.
     *
     * @return the database-assigned event store ID, or null before persistence
     */
    Long id();

    /**
     * Creates a new copy of this event with the given database ID attached.
     * This is the "wither" pattern — since records are immutable, we can't
     * modify the id field, so we create a new instance with all original
     * fields plus the new id.
     *
     * <p>Called by {@link DomainEventManager} after persisting the event
     * to the event store, so that downstream listeners have access to the
     * event's database ID (needed for status updates).
     *
     * @param id the surrogate ID from the event_store table
     * @return a new Event instance identical to this one but with the given id
     */
    Event withId(Long id);
}
