package com.staffs.leavebooking.common.events;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Service responsible for persisting and managing domain events in the event store
 * (Lecture 8 — Outbox Pattern, Event Store).
 *
 * <p><strong>Three responsibilities:</strong>
 * <ol>
 *   <li><strong>Append</strong> — persist new events to the event_store table
 *       (called by {@link DomainEventManager} within the aggregate's transaction)</li>
 *   <li><strong>Update status</strong> — track delivery status for remote events
 *       (called by {@link RemoteOutboxListener} after publishing to RabbitMQ)</li>
 *   <li><strong>Purge</strong> — clean up old PUBLISHED/LOCAL events
 *       (called by {@link EventStoreCleanupJob} on a daily schedule)</li>
 * </ol>
 *
 * <p><strong>Event serialisation:</strong> Event payloads are serialised to JSON using
 * Jackson's {@link ObjectMapper}. This means the event_body column contains a complete
 * JSON representation of the event, making it human-readable and debuggable.
 *
 * @see DomainEventManager for the orchestrator that calls append()
 * @see RemoteOutboxListener for the publisher that calls updateStatus()
 * @see EventStoreCleanupJob for the scheduled job that calls purgeOldEvents()
 */
@Service        // Spring-managed singleton — injected into DomainEventManager and RemoteOutboxListener
@Slf4j          // Lombok: generates a private static final Logger (SLF4J)
@AllArgsConstructor // Lombok: constructor injection for all final fields
public class EventStoreService {

    /**
     * Enum representing the possible delivery statuses of an event in the event store.
     * This tracks the lifecycle of each event through the outbox pattern.
     */
    public enum StatusOfMessageDelivery {
        LOCAL,       // Local event — processed in-memory via Spring, never sent to RabbitMQ
        PENDING,     // Remote event — saved to event_store, waiting for RabbitMQ publish
        PUBLISHED,   // Remote event — successfully published to RabbitMQ broker
        FAILED,      // Remote event — all retry attempts exhausted, needs investigation
        UNROUTABLE   // Remote event — no exchange/routing-key mapping found in configuration
    }

    /** Repository for CRUD operations on the event_store table */
    private final EventStoreRepository eventsStore;

    /** Jackson ObjectMapper for serialising event objects to JSON strings */
    private final ObjectMapper objectMapper;

    /**
     * Persists a new event to the event_store table.
     * This runs within the SAME transaction as the aggregate's state change
     * (called by DomainEventManager, which is @Transactional).
     *
     * <p><strong>Transaction guarantee:</strong> If the aggregate save fails and
     * the transaction rolls back, this event save also rolls back — ensuring
     * consistency between state changes and events (no orphaned events).
     *
     * <p><strong>Status assignment:</strong>
     * <ul>
     *   <li>RemoteEvent instances → PENDING (waiting for RabbitMQ publish)</li>
     *   <li>LocalEvent instances → LOCAL (processed in-memory, no broker needed)</li>
     * </ul>
     *
     * @param event         the domain event to persist (any Event implementation)
     * @param sourceContext the bounded context name that raised this event
     * @return the saved JPA entity with the auto-generated database ID
     * @throws IllegalArgumentException if the event payload cannot be serialised to JSON
     */
    @Transactional // Participates in the caller's transaction (DomainEventManager)
    public EventStoreJpa append(Event event, String sourceContext) {
        try {
            // Create a new JPA entity to persist
            EventStoreJpa newEventJpa = new EventStoreJpa();

            // Set the ID to null — JPA's @GeneratedValue will auto-assign it on save
            newEventJpa.setId(null);

            // Store the simple class name (e.g., "StaffMemberAddedEvent") for easy querying
            newEventJpa.setEventType(event.getClass().getSimpleName());

            // Record when the event was persisted
            newEventJpa.setOccurredOn(LocalDate.now());

            // Serialise the entire event object to a JSON string using Jackson
            // This preserves all event fields in human-readable format in the database
            newEventJpa.setEventBody(objectMapper.writeValueAsString(event));

            // Record which bounded context produced this event (for audit/debugging)
            newEventJpa.setSourceContext(sourceContext);

            // Initial retry count is 0 (no retries yet)
            newEventJpa.setRetryCount(0);

            // Determine initial status based on event type:
            // RemoteEvent → PENDING (needs to be published to RabbitMQ)
            // LocalEvent → LOCAL (already processed in-memory, no broker needed)
            if (event instanceof RemoteEvent) {
                newEventJpa.setStatus(StatusOfMessageDelivery.PENDING.name());
            } else {
                newEventJpa.setStatus(StatusOfMessageDelivery.LOCAL.name());
            }

            // Save to database and return the entity (now has auto-generated ID)
            return eventsStore.save(newEventJpa);
        } catch (JacksonException je) {
            // Jackson failed to serialise the event — this is a programming error
            // (event record has a field type that Jackson can't handle)
            throw new IllegalArgumentException("Failed to serialise event payload", je);
        }
    }

    /**
     * Updates the delivery status of an existing event in the event store.
     * Called by {@link RemoteOutboxListener} after attempting to publish to RabbitMQ.
     *
     * <p><strong>Status transitions:</strong>
     * <ul>
     *   <li>PENDING → PUBLISHED (success: RabbitMQ accepted the message)</li>
     *   <li>PENDING → FAILED (all 3 retries exhausted — @Recover method)</li>
     *   <li>PENDING → UNROUTABLE (no routing config found for this event type)</li>
     * </ul>
     *
     * @param eventId            the database ID of the event to update
     * @param status             the new delivery status
     * @param incrementRetryCount whether to increment the retry counter (true on failure)
     */
    @Transactional // Own transaction — runs after the producing transaction has committed
    public void updateStatus(Long eventId, StatusOfMessageDelivery status, boolean incrementRetryCount) {
        // Look up the event by its database ID
        eventsStore.findById(eventId).ifPresent(event -> {
            // Update the status string (e.g., "PENDING" → "PUBLISHED")
            event.setStatus(status.name());

            // Optionally increment the retry counter (done on each failed retry attempt)
            if (incrementRetryCount) {
                event.setRetryCount(event.getRetryCount() + 1);
            }

            // Save the updated entity back to the database
            eventsStore.save(event);

            // Log the status change for operational visibility
            log.info("Event {} marked as {}", eventId, event.getStatus());
        });
    }

    /**
     * Purges old PUBLISHED and LOCAL events from the event store.
     * Called by {@link EventStoreCleanupJob} on a daily schedule.
     *
     * <p><strong>This is the only DELETE operation in the entire system.</strong>
     * It is justified because the event_store is infrastructure (outbox + audit),
     * not core business data. See {@link EventStoreCleanupJob} for the full justification.
     *
     * <p><strong>What gets purged:</strong> PUBLISHED and LOCAL events older than
     * {@code retentionDays}. FAILED and PENDING events are NEVER purged — they
     * need manual investigation.
     *
     * @param retentionDays number of days to keep events before purging (default: 30)
     * @return the number of events purged
     */
    @Transactional
    public int purgeOldEvents(int retentionDays) {
        // Calculate the cutoff date: events older than this will be purged
        LocalDate cutoffDate = LocalDate.now().minusDays(retentionDays);

        // Find all PUBLISHED events older than the cutoff
        var publishedEvents = eventsStore.findByStatusAndOccurredOnBefore(
                StatusOfMessageDelivery.PUBLISHED.name(), cutoffDate);

        // Find all LOCAL events older than the cutoff
        var localEvents = eventsStore.findByStatusAndOccurredOnBefore(
                StatusOfMessageDelivery.LOCAL.name(), cutoffDate);

        // Count total events to be purged (for logging)
        int purgedCount = publishedEvents.size() + localEvents.size();

        // Delete the old events from the database
        eventsStore.deleteAll(publishedEvents);
        eventsStore.deleteAll(localEvents);

        // Log the cleanup result (only if events were actually purged)
        if (purgedCount > 0) {
            log.info("Event store cleanup: purged {} events older than {} days (cutoff: {})",
                    purgedCount, retentionDays, cutoffDate);
        }

        return purgedCount;
    }

    /**
     * Finds all events stranded in PENDING or FAILED status.
     * Called by {@link OutboxRecoveryJob} on a scheduled interval to re-attempt
     * publishing events that were never delivered due to process failure, broker
     * unavailability, or exhausted retries.
     *
     * <p><strong>Why this is needed:</strong> The {@link RemoteOutboxListener} publishes
     * events asynchronously after commit. If the application process dies between the
     * commit and the RabbitMQ publish (or if all 3 retry attempts fail), the event
     * remains PENDING or FAILED in the event store with no automatic recovery.
     * This method provides the query for the recovery poller to find those events.
     *
     * @return list of events in PENDING or FAILED status (empty if none stranded)
     */
    public java.util.List<EventStoreJpa> findStrandedEvents() {
        var pending = eventsStore.findByStatus(StatusOfMessageDelivery.PENDING.name());
        var failed = eventsStore.findByStatus(StatusOfMessageDelivery.FAILED.name());

        var stranded = new java.util.ArrayList<>(pending);
        stranded.addAll(failed);
        return stranded;
    }
}
