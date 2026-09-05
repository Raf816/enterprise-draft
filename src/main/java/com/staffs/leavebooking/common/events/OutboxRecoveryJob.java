package com.staffs.leavebooking.common.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Scheduled recovery poller for stranded outbox events (Lecture 8 — Outbox Pattern).
 *
 * <p><strong>Problem this solves:</strong> The {@link RemoteOutboxListener} publishes
 * events to RabbitMQ asynchronously after the producing transaction commits. If the
 * application process dies between the commit and the RabbitMQ publish, or if all
 * retry attempts are exhausted, the event remains stranded in the event_store with
 * status PENDING or FAILED. Without this recovery job, those events would never
 * be delivered.
 *
 * <p><strong>How it works:</strong>
 * <ol>
 *   <li>Runs every 5 minutes via {@code @Scheduled}</li>
 *   <li>Queries the event_store for all PENDING and FAILED events</li>
 *   <li>For each stranded event, deserialises the event body back to its original
 *       class using the stored {@code event_type} field</li>
 *   <li>Resolves the RabbitMQ destination (exchange + routing key) via {@link RabbitOutboxRouter}</li>
 *   <li>Re-publishes to RabbitMQ</li>
 *   <li>On success: marks the event as PUBLISHED</li>
 *   <li>On failure: increments the retry count and leaves as PENDING/FAILED
 *       for the next poll cycle</li>
 * </ol>
 *
 * <p><strong>Safety:</strong> RabbitMQ consumers must be idempotent — re-publishing
 * an event that was already partially processed could result in duplicate delivery.
 * Our consumers handle this: {@code createAllowanceForNewStaff} has an idempotency
 * guard (composite unique constraint on staff_member_id + business_year_start),
 * and {@code updateStaffDetails} is a simple overwrite that is safe to replay.
 *
 * <p><strong>Maximum retry limit:</strong> Events with a retry count exceeding
 * {@link #MAX_RECOVERY_RETRIES} are skipped and logged as requiring manual investigation.
 * This prevents infinite retry loops for events with permanent failures (e.g.,
 * malformed payloads, missing routing configuration).
 *
 * <p><strong>Complement to {@link EventStoreCleanupJob}:</strong> The cleanup job
 * purges old PUBLISHED/LOCAL events (housekeeping). This recovery job re-publishes
 * stranded PENDING/FAILED events (resilience). Together they complete the outbox
 * lifecycle management.
 *
 * @see EventStoreService#findStrandedEvents() for the query
 * @see RemoteOutboxListener for the primary publish path
 * @see EventStoreCleanupJob for the complementary cleanup job
 */
@Component
@Slf4j
@AllArgsConstructor
public class OutboxRecoveryJob {

    /** Maximum number of recovery retries before an event is skipped */
    private static final int MAX_RECOVERY_RETRIES = 10;

    private final EventStoreService eventStoreService;
    private final RabbitTemplate rabbitTemplate;
    private final RabbitOutboxRouter rabbitOutboxRouter;
    private final ObjectMapper objectMapper;

    /**
     * Scheduled method that polls for stranded PENDING and FAILED events
     * and re-attempts RabbitMQ publication.
     *
     * <p>Runs every 5 minutes. The fixed rate ensures consistent polling
     * regardless of how long each execution takes.
     *
     * <p>Enabled by {@code @EnableScheduling} on the main application class.
     */
    @Scheduled(fixedRate = 300_000) // Every 5 minutes (300,000 ms)
    public void recoverStrandedEvents() {
        List<EventStoreJpa> stranded = eventStoreService.findStrandedEvents();

        if (stranded.isEmpty()) {
            return; // Nothing to recover — skip logging to avoid noise
        }

        log.info("Outbox recovery: found {} stranded event(s). Attempting re-publish.", stranded.size());

        int recovered = 0;
        int skipped = 0;

        for (EventStoreJpa event : stranded) {
            // Skip events that have exceeded the maximum recovery retry limit
            if (event.getRetryCount() >= MAX_RECOVERY_RETRIES) {
                skipped++;
                log.warn("Outbox recovery: skipping event {} (type={}, retries={}) — exceeds max retries. Manual investigation required.",
                        event.getId(), event.getEventType(), event.getRetryCount());
                continue;
            }

            try {
                // Deserialise the stored JSON back to the original event class
                String eventClassName = "com.staffs.leavebooking.common.events." + event.getEventType();
                Class<?> eventClass = Class.forName(eventClassName);
                Object eventObject = objectMapper.readValue(event.getEventBody(), eventClass);

                // Resolve the RabbitMQ destination
                RabbitOutboxRouter.Destination destination = rabbitOutboxRouter.resolve((Event) eventObject);

                // Re-publish to RabbitMQ
                rabbitTemplate.convertAndSend(destination.exchange(), destination.routingKey(), eventObject);

                // Mark as PUBLISHED on success
                eventStoreService.updateStatus(event.getId(),
                        EventStoreService.StatusOfMessageDelivery.PUBLISHED, false);
                recovered++;

                log.info("Outbox recovery: successfully re-published event {} (type={})",
                        event.getId(), event.getEventType());

            } catch (AmqpException e) {
                // Broker still unavailable — increment retry count and leave for next poll
                eventStoreService.updateStatus(event.getId(),
                        EventStoreService.StatusOfMessageDelivery.PENDING, true);
                log.warn("Outbox recovery: failed to re-publish event {} (type={}, retry={}). Will retry next cycle.",
                        event.getId(), event.getEventType(), event.getRetryCount() + 1, e);

            } catch (ClassNotFoundException e) {
                // Event type class no longer exists — permanent failure, mark as FAILED
                // to prevent infinite polling (the event will never be deserialised)
                eventStoreService.updateStatus(event.getId(),
                        EventStoreService.StatusOfMessageDelivery.FAILED, true);
                log.error("Outbox recovery: unknown event type '{}' for event {}. Marked as FAILED.",
                        event.getEventType(), event.getId(), e);
                skipped++;

            } catch (Exception e) {
                // Deserialisation failure or routing error — increment retry and continue
                eventStoreService.updateStatus(event.getId(),
                        EventStoreService.StatusOfMessageDelivery.FAILED, true);
                log.error("Outbox recovery: error processing event {} (type={}). Marked as FAILED.",
                        event.getId(), event.getEventType(), e);
            }
        }

        log.info("Outbox recovery complete: {} recovered, {} skipped, {} remaining",
                recovered, skipped, stranded.size() - recovered - skipped);
    }
}
