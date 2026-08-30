package com.staffs.leavebooking.common.events;

import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration-driven router that maps event class names to RabbitMQ destinations
 * (Lecture 8 — Remote Events, Outbox Pattern).
 *
 * <p><strong>How it works:</strong> Reads the {@code rabbitmq.outbox.bindings} section
 * from {@code application.yaml} and populates a map of event FQCN → {exchange, routingKey}.
 * When {@link RemoteOutboxListener} needs to publish an event, it calls
 * {@link #resolve(Event)} to look up where to send it.
 *
 * <p><strong>application.yaml configuration:</strong>
 * <pre>
 * rabbitmq:
 *   outbox:
 *     bindings:
 *       "[com.staffs.leavebooking.common.events.StaffMemberAddedEvent]":
 *         exchange: "staff-management"
 *         routing-key: "staff.member.added"
 *       "[com.staffs.leavebooking.common.events.StaffMemberUpdatedEvent]":
 *         exchange: "staff-management"
 *         routing-key: "staff.member.updated"
 *       "[com.staffs.leavebooking.common.events.ManagerNotificationEvent]":
 *         exchange: "leave-notifications"
 *         routing-key: "notification.manager.pending"
 *       "[com.staffs.leavebooking.common.events.StaffNotificationEvent]":
 *         exchange: "leave-notifications"
 *         routing-key: "notification.staff.decided"
 * </pre>
 *
 * <p><strong>Why configuration-driven?</strong> Adding a new remote event type only
 * requires a YAML config entry — no code changes to the router. This follows the
 * Open-Closed Principle: open for extension (new events), closed for modification.
 *
 * <p><strong>@ConfigurationProperties:</strong> Spring Boot automatically binds
 * YAML properties under the prefix {@code rabbitmq.outbox} to this class's fields.
 * The {@code bindings} map is populated from the nested YAML structure.
 *
 * @see RemoteOutboxListener which calls resolve() before publishing
 * @see RabbitInfrastructureConfig which declares the actual exchanges/queues
 */
@Component // Spring-managed singleton — participates in component scanning
@ConfigurationProperties(prefix = "rabbitmq.outbox") // Binds YAML properties under this prefix
@Getter // Lombok: generates getters (Spring needs getBindings() to populate the map)
public class RabbitOutboxRouter {

    /**
     * Immutable record representing a RabbitMQ destination (exchange + routing key).
     * Used as the value type in the bindings map.
     *
     * @param exchange   the RabbitMQ exchange name (e.g., "staff-management")
     * @param routingKey the routing key for message routing (e.g., "staff.member.added")
     */
    public record Destination(String exchange, String routingKey) {}

    /**
     * Map of event fully-qualified class name → RabbitMQ destination.
     * Populated automatically by Spring Boot from the YAML configuration.
     *
     * <p>Key format: {@code com.staffs.leavebooking.common.events.StaffMemberAddedEvent}
     * <p>Value: {@code Destination("staff-management", "staff.member.added")}
     *
     * <p>Initialised as HashMap so Spring can populate it via the setter (ConfigurationProperties).
     */
    private final Map<String, Destination> bindings = new HashMap<>();

    /**
     * Resolves the RabbitMQ destination for a given event.
     * Looks up the event's fully-qualified class name in the bindings map.
     *
     * <p><strong>Called by:</strong> {@link RemoteOutboxListener#handleRemoteEvent(RemoteEvent)}
     * to determine which exchange and routing key to use for publishing.
     *
     * @param event the domain event to resolve a destination for
     * @return the Destination (exchange + routing key) for this event type
     * @throws IllegalArgumentException if no binding is configured for this event type
     *         (indicates a configuration error — the event type was not added to application.yaml)
     */
    public Destination resolve(Event event) {
        // Get the fully-qualified class name (e.g., "com.staffs.leavebooking.common.events.StaffMemberAddedEvent")
        String className = event.getClass().getName();

        // Look up the destination in the bindings map
        Destination dest = bindings.get(className);

        // If no binding found, throw an exception — this is a configuration error
        // The event type exists in code but wasn't added to the YAML config
        if (dest == null) {
            throw new IllegalArgumentException("No RabbitMQ destination configured for " + className);
        }

        return dest;
    }
}
