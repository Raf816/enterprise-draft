package com.staffs.leavebooking.common.events;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

/**
 * JPA entity mapping to the {@code event_store} database table
 * (Lecture 8 — Outbox Pattern, Event Store).
 *
 * <p><strong>Purpose:</strong> This table acts as both an event store (audit log of
 * everything that happened) and an outbox (staging area for events waiting to be
 * published to RabbitMQ). It is the local persistence side of the Store-and-Forward pattern.
 *
 * <p><strong>Table structure (from schema.sql):</strong>
 * <pre>
 * CREATE TABLE event_store (
 *     id              BIGINT AUTO_INCREMENT PRIMARY KEY,
 *     occurred_on     DATE NOT NULL,
 *     event_body      CLOB NOT NULL,
 *     event_type      VARCHAR(100) NOT NULL,
 *     status          VARCHAR(20) NOT NULL,
 *     retry_count     INT NOT NULL DEFAULT 0,
 *     source_context  VARCHAR(100)
 * );
 * </pre>
 *
 * <p><strong>Status lifecycle for remote events:</strong>
 * <pre>
 * PENDING → PUBLISHED   (happy path: RabbitMQ accepted the message)
 * PENDING → FAILED      (sad path: all 3 retry attempts exhausted)
 * PENDING → UNROUTABLE  (config error: no exchange/routing-key mapping found)
 * </pre>
 *
 * <p><strong>Status for local events:</strong> Always {@code LOCAL} — they are processed
 * in-memory and never sent to RabbitMQ.
 *
 * <p><strong>Why not use the domain event record directly?</strong> JPA entities are mutable
 * (setters, managed lifecycle) while domain events are immutable records. Keeping them
 * separate follows the same pattern as aggregate ↔ JPA entity separation.
 *
 * <p><strong>Lombok annotations:</strong>
 * <ul>
 *   <li>{@code @Getter} — generates getters for all fields</li>
 *   <li>{@code @Setter} — generates setters for all fields (JPA needs setters)</li>
 *   <li>{@code @ToString} — generates toString() for logging</li>
 * </ul>
 *
 * @see EventStoreService for the service that reads/writes this entity
 * @see EventStoreCleanupJob for the scheduled purge of old events
 */
@Entity(name = "event_store")   // JPA entity name — used in JPQL queries
@Table(name = "event_store")    // Maps to the event_store table in the database
@Getter     // Lombok: generates getter methods for all fields
@Setter     // Lombok: generates setter methods for all fields (required by JPA)
@ToString   // Lombok: generates toString() method for logging/debugging
public class EventStoreJpa {

    /**
     * Auto-generated surrogate primary key.
     * This is the ID that gets attached to domain events via {@link Event#withId(Long)}
     * so that downstream listeners can update the event's delivery status.
     */
    @Id // JPA: marks this field as the primary key
    @Column(name = "id") // Maps to the 'id' column in the event_store table
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment in the database
    private Long id;

    /**
     * The date the event occurred (when the aggregate raised it).
     * Used by the cleanup job to determine which events are old enough to purge.
     */
    @Column(name = "occurred_on", nullable = false) // NOT NULL constraint
    private LocalDate occurredOn;

    /**
     * The event payload serialised as a JSON string.
     * Contains all the event's data fields (e.g., staffMemberId, department, etc.).
     * Stored as a CLOB (Character Large Object) to handle large payloads.
     * Serialised by Jackson's ObjectMapper in EventStoreService.
     */
    @Column(name = "event_body", nullable = false, length = 65000) // CLOB-like storage
    private String eventBody;

    /**
     * The simple class name of the event (e.g., "StaffMemberAddedEvent", "LeaveRequestApprovedEvent").
     * Used for querying/filtering events by type in the event store.
     */
    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    /**
     * The delivery status of the event. One of:
     * LOCAL     — local event, processed in-memory (no broker publish needed)
     * PENDING   — remote event, waiting to be published to RabbitMQ
     * PUBLISHED — remote event, successfully sent to RabbitMQ
     * FAILED    — remote event, all retry attempts exhausted
     * UNROUTABLE — remote event, no exchange/routing-key configuration found
     *
     * @see EventStoreService.StatusOfMessageDelivery
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /**
     * Number of times RabbitMQ publishing has been retried for this event.
     * Incremented by {@link RemoteOutboxListener} on each retry failure.
     * Defaults to 0. Max retries is 3 (configured in @Retryable on RemoteOutboxListener).
     */
    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    /**
     * The bounded context that produced this event (e.g., "LeaveManagement", "StaffManagement").
     * Used for audit logging and debugging — helps identify which part of the system
     * generated each event.
     */
    @Column(name = "source_context", length = 100)
    private String sourceContext;
}
