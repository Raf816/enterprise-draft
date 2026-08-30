package com.staffs.leavebooking.common.events;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Centrally manages domain event dispatch using the Store-and-Forward pattern
 * (Lecture 7 — Domain Events, Lecture 8 — Outbox Pattern).
 *
 * <p><strong>What it does:</strong> For each domain event raised by an aggregate:
 * <ol>
 *   <li><strong>Persist</strong> — saves the event to the {@code event_store} table
 *       via {@link EventStoreService#append(Event, String)} (same transaction as the aggregate)</li>
 *   <li><strong>Publish</strong> — publishes the event via Spring's in-process
 *       {@link ApplicationEventPublisher}, which triggers:
 *       <ul>
 *         <li>For {@link LocalEvent}: {@code @TransactionalEventListener} in the same context</li>
 *         <li>For {@link RemoteEvent}: {@link RemoteOutboxListener} which publishes to RabbitMQ</li>
 *       </ul>
 *   </li>
 * </ol>
 *
 * <p><strong>Store-and-Forward explained:</strong> The event is stored in the database
 * WITHIN the same transaction as the aggregate's state change. This guarantees that if
 * the aggregate is saved, the event is also saved — no dual-write problem. The event
 * is then forwarded to listeners AFTER the transaction commits.
 *
 * <p><strong>Called by:</strong> Application services (e.g., LeaveRequestApplicationService,
 * StaffApplicationService) after executing a command on an aggregate:
 * <pre>
 * aggregate.someCommand();                          // raises events internally
 * repository.save(mapper.toJpa(aggregate));         // persist state change
 * if (aggregate.domainEventsExist()) {              // check for events
 *     eventManager.manageDomainEvents("LeaveManagement", aggregate.listOfDomainEvents());
 *     aggregate.clearDomainEvents();                // prevent double-dispatch
 * }
 * </pre>
 *
 * @see EventStoreService for event persistence
 * @see RemoteOutboxListener for RabbitMQ publishing
 */
@Service    // Spring-managed singleton — injected into application services
@Slf4j      // Lombok: generates a private static final Logger (SLF4J)
@AllArgsConstructor // Lombok: generates constructor with all final fields (used for dependency injection)
public class DomainEventManager {

    /**
     * Spring's in-process event publisher. Publishes events to any
     * {@code @EventListener} or {@code @TransactionalEventListener} in the application context.
     * This is the bridge between domain events and Spring's event system.
     */
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Service for persisting events to the event_store table.
     * Events are saved BEFORE publishing to ensure they're captured even if
     * the listener fails (Store-and-Forward guarantee).
     */
    private final EventStoreService eventStoreService;

    /**
     * Processes a list of domain events from an aggregate.
     * Runs within the caller's transaction boundary (@Transactional) so that
     * event persistence is atomic with the aggregate's state change.
     *
     * <p>For each event:
     * 1. Log it (for debugging/tracing)
     * 2. Save to event_store (gets a database ID assigned)
     * 3. Attach the database ID to the event via withId()
     * 4. Publish via Spring's event bus (triggers listeners)
     *
     * @param sourceContext the name of the bounded context that raised the events
     *                      (e.g., "LeaveManagement", "StaffManagement") — for audit logging
     * @param events        the list of domain events to process (from aggregate.listOfDomainEvents())
     * @throws NullPointerException if sourceContext or events is null
     */
    @Transactional // Ensures event persistence is part of the calling transaction
    public void manageDomainEvents(String sourceContext, List<Event> events) {
        // Fail fast if required arguments are null
        Objects.requireNonNull(sourceContext, "Source context cannot be null");
        Objects.requireNonNull(events, "Events list cannot be null");

        // Process each event sequentially (order matters — events should be dispatched
        // in the order they were raised by the aggregate)
        for (Event event : events) {
            // Log the event for debugging and operational visibility
            log.info("{} -> {}", sourceContext, event);

            // Step 1: Persist the event to the event_store table.
            // This returns the JPA entity with the auto-generated surrogate ID.
            // Local events get status=LOCAL, remote events get status=PENDING.
            EventStoreJpa savedEvent = eventStoreService.append(event, sourceContext);

            // Step 2: Publish the event via Spring's ApplicationEventPublisher.
            // We use event.withId() to create a copy of the event that includes
            // the database-assigned ID. This is necessary because:
            // - The event record is immutable (can't set id after construction)
            // - Downstream listeners (RemoteOutboxListener) need the ID to update
            //   the event's delivery status (PENDING → PUBLISHED or FAILED)
            eventPublisher.publishEvent(event.withId(savedEvent.getId()));
        }
    }
}
