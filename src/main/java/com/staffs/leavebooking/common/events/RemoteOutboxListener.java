package com.staffs.leavebooking.common.events;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Async listener that publishes remote events to RabbitMQ after the producing
 * transaction commits. Implements the Outbox pattern from Lecture 8.
 *
 * <p><strong>How it works:</strong>
 * <ol>
 *   <li>{@link DomainEventManager} publishes events via Spring's {@code ApplicationEventPublisher}</li>
 *   <li>This listener catches {@link RemoteEvent} instances (not LocalEvents)</li>
 *   <li>It fires AFTER the producing transaction commits ({@code AFTER_COMMIT}),
 *       ensuring the aggregate's state is safely persisted before broker publishing</li>
 *   <li>It runs on a separate thread ({@code @Async}), so the HTTP response is not
 *       delayed by broker communication</li>
 *   <li>It resolves the destination (exchange + routing key) via {@link RabbitOutboxRouter}</li>
 *   <li>It publishes the event to RabbitMQ via {@link RabbitTemplate}</li>
 *   <li>On success, updates the event store status to PUBLISHED</li>
 *   <li>On failure, Spring Retry retries up to 3 times with exponential backoff</li>
 *   <li>If all retries fail, the {@code @Recover} method marks the event as FAILED</li>
 * </ol>
 *
 * <p><strong>Why AFTER_COMMIT?</strong> If we published to RabbitMQ during the transaction
 * and the transaction then rolled back, consumers would process an event for a state
 * change that never actually happened. AFTER_COMMIT guarantees the aggregate's state
 * is committed before we attempt to notify anyone about it.
 *
 * <p><strong>Why @Async?</strong> RabbitMQ communication is a network operation that
 * could be slow or fail. Running it asynchronously means the API response is returned
 * to the client immediately after the transaction commits — the client doesn't wait
 * for RabbitMQ to acknowledge the message.
 *
 * <p><strong>Retry behaviour (@Retryable):</strong>
 * <ul>
 *   <li>Retries on {@link AmqpException} (broker connection issues, channel errors)</li>
 *   <li>Maximum 3 attempts (1 initial + 2 retries)</li>
 *   <li>Exponential backoff: 500ms → 1000ms → 2000ms</li>
 *   <li>If all attempts fail, the {@code @Recover} method is called</li>
 * </ul>
 *
 * <p><strong>Lecture 8 equivalence:</strong> This is directly analogous to the case study's
 * {@code RemoteOutboxListener} that published {@code NewRestaurantAddedEvent} to CloudAMQP.
 *
 * @see RabbitOutboxRouter for exchange/routing-key resolution
 * @see EventStoreService for status tracking
 * @see RabbitInfrastructureConfig for exchange/queue/binding declarations
 */
@Component      // Spring-managed singleton — Spring detects the event listener annotation
@Slf4j          // Lombok: generates a private static final Logger (SLF4J)
@AllArgsConstructor // Lombok: constructor injection for all dependencies
public class RemoteOutboxListener {

    /** Service for updating event delivery status in the event_store table */
    private final EventStoreService eventStoreService;

    /** Spring AMQP template for publishing messages to RabbitMQ exchanges */
    private final RabbitTemplate rabbitTemplate;

    /** Configuration-driven router that maps event types to exchange + routing key */
    private final RabbitOutboxRouter rabbitOutboxRouter;

    /**
     * Handles a remote event by publishing it to the appropriate RabbitMQ exchange.
     *
     * <p><strong>Annotation breakdown:</strong>
     * <ul>
     *   <li>{@code @Async} — runs on a separate thread (from Spring's task executor pool)</li>
     *   <li>{@code @TransactionalEventListener(AFTER_COMMIT)} — only fires after the
     *       producing transaction has successfully committed</li>
     *   <li>{@code @Retryable} — if RabbitMQ is temporarily unavailable, retries
     *       up to 3 times with exponential backoff (500ms, 1s, 2s)</li>
     * </ul>
     *
     * @param event the remote event to publish (already has a database ID via withId())
     */
    @Async // Run on a separate thread — don't block the HTTP response
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // Fire only after successful commit
    @Retryable(
            retryFor = AmqpException.class,     // Only retry on AMQP/broker errors
            maxAttempts = 3,                     // 1 initial attempt + 2 retries
            backoff = @Backoff(delay = 500, multiplier = 2.0) // 500ms → 1000ms → 2000ms
    )
    public void handleRemoteEvent(RemoteEvent event) {
        RabbitOutboxRouter.Destination destination;

        // Step 1: Resolve the destination (exchange + routing key) for this event type
        // The RabbitOutboxRouter reads from application.yaml to find the mapping
        try {
            destination = rabbitOutboxRouter.resolve(event);
        } catch (IllegalArgumentException e) {
            // No routing configuration found for this event type — this is a config error
            log.error("Unroutable event [{}]. Check RabbitOutboxRouter configuration.",
                    event.getClass().getSimpleName(), e);
            // Mark the event as UNROUTABLE in the event store (won't be retried)
            eventStoreService.updateStatus(event.id(),
                    EventStoreService.StatusOfMessageDelivery.UNROUTABLE, false);
            return; // Don't attempt to publish — there's nowhere to send it
        }

        // Step 2: Publish the event to RabbitMQ
        // convertAndSend() serialises the event to JSON (via CustomMessageConverter)
        // and sends it to the specified exchange with the given routing key
        rabbitTemplate.convertAndSend(
                destination.exchange(),     // e.g., "staff-management"
                destination.routingKey(),   // e.g., "staff.member.added"
                event                       // the event object (serialised to JSON)
        );

        // Step 3: Mark as PUBLISHED in the event store (success path)
        eventStoreService.updateStatus(event.id(),
                EventStoreService.StatusOfMessageDelivery.PUBLISHED, false);
    }

    /**
     * Recovery method called when all retry attempts are exhausted.
     * Spring Retry calls this automatically after the 3rd failed attempt.
     *
     * <p>The event is marked as FAILED in the event store. FAILED events are never
     * auto-purged by the cleanup job — they require manual investigation to understand
     * why publishing failed (broker down? network issue? permissions?).
     *
     * @param e     the AMQP exception from the last failed attempt
     * @param event the event that could not be published
     */
    @Recover // Spring Retry: fallback method when all retries are exhausted
    public void recover(AmqpException e, RemoteEvent event) {
        log.error("Failed to publish event {} to RabbitMQ after retries. Marking as FAILED.",
                event.id(), e);
        // Mark as FAILED and increment the retry count
        eventStoreService.updateStatus(event.id(),
                EventStoreService.StatusOfMessageDelivery.FAILED, true);
    }
}
