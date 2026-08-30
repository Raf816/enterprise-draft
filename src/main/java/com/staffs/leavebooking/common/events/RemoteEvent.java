package com.staffs.leavebooking.common.events;

/**
 * Marker interface for events that cross bounded context boundaries via RabbitMQ
 * (Lecture 8 — Remote Events, Outbox Pattern, Remote Subscriber).
 *
 * <p><strong>How remote events work:</strong>
 * <ol>
 *   <li>An aggregate raises the event: {@code addDomainEvent(new StaffMemberAddedEvent(...))}</li>
 *   <li>The application service passes it to {@link DomainEventManager}</li>
 *   <li>{@link DomainEventManager} persists it to the event store (status: PENDING)
 *       and publishes it via Spring's {@code ApplicationEventPublisher}</li>
 *   <li>{@link RemoteOutboxListener} (annotated with {@code @Async} and
 *       {@code @TransactionalEventListener(AFTER_COMMIT)}) receives the event
 *       on a separate thread after the producing transaction commits</li>
 *   <li>{@link RemoteOutboxListener} uses {@link RabbitOutboxRouter} to resolve the
 *       exchange and routing key, then publishes via {@code RabbitTemplate.convertAndSend()}</li>
 *   <li>On success, the event store status is updated to PUBLISHED</li>
 *   <li>A {@code @RabbitListener} in another bounded context consumes the message</li>
 * </ol>
 *
 * <p><strong>Remote events in this system:</strong>
 * <ul>
 *   <li>{@link StaffMemberAddedEvent} — Staff Mgmt → Leave Mgmt (creates LeaveAllowance)</li>
 *   <li>{@link StaffMemberUpdatedEvent} — Staff Mgmt → Leave Mgmt (syncs dept/manager)</li>
 *   <li>{@link ManagerNotificationEvent} — Leave Mgmt → notification queue (manager alert)</li>
 *   <li>{@link StaffNotificationEvent} — Leave Mgmt → notification queue (staff alert)</li>
 * </ul>
 *
 * <p><strong>Why remote events live in common/events/:</strong> Both the producing
 * context and the consuming context need to see the event record class. Placing them
 * in the Shared Kernel ({@code common/}) satisfies both without creating a direct
 * dependency between the two business contexts (Lecture 4 — Shared Kernel).
 *
 * @see LocalEvent for events within a single bounded context
 * @see RemoteOutboxListener for the publishing mechanism
 * @see RabbitOutboxRouter for exchange/routing-key resolution
 */
public interface RemoteEvent extends Event {
    // Marker interface — no additional methods beyond those inherited from Event.
    // The RemoteOutboxListener checks instanceof RemoteEvent to decide
    // whether to publish an event to RabbitMQ.
    // The EventStoreService uses instanceof RemoteEvent to set the initial
    // status to PENDING (instead of LOCAL for local events).
}
