package com.staffs.leavebooking.common.events;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Data repository for the {@code event_store} table (Lecture 3 — Repository pattern).
 *
 * <p><strong>Pattern:</strong> Extends Spring Data's {@link CrudRepository} which provides
 * standard CRUD operations (save, findById, findAll, delete, etc.) without any
 * implementation code. Spring generates the implementation at runtime based on the
 * interface definition and the JPA entity type.
 *
 * <p><strong>Type parameters:</strong>
 * <ul>
 *   <li>{@link EventStoreJpa} — the JPA entity this repository manages</li>
 *   <li>{@code Long} — the type of the entity's primary key ({@code @Id} field)</li>
 * </ul>
 *
 * <p><strong>Custom query method:</strong> {@link #findByStatusAndOccurredOnBefore}
 * uses Spring Data's method-name query derivation — Spring parses the method name
 * and generates the SQL automatically:
 * {@code SELECT * FROM event_store WHERE status = ? AND occurred_on < ?}
 *
 * @see EventStoreJpa for the entity this repository manages
 * @see EventStoreService for the service layer that uses this repository
 */
@Repository // Spring stereotype — marks this as a persistence component for component scanning
public interface EventStoreRepository extends CrudRepository<EventStoreJpa, Long> {

    /**
     * Finds all events with a given status that occurred before the specified cutoff date.
     * Spring Data generates the SQL from the method name automatically:
     * {@code WHERE status = :status AND occurred_on < :cutoffDate}
     *
     * <p><strong>Used by:</strong> {@link EventStoreService#purgeOldEvents(int)} for the
     * cleanup job — finds PUBLISHED and LOCAL events older than the retention period.
     *
     * @param status     the event delivery status to filter by (e.g., "PUBLISHED", "LOCAL")
     * @param cutoffDate events with occurredOn before this date will be returned
     * @return list of matching events (empty if none found)
     */
    List<EventStoreJpa> findByStatusAndOccurredOnBefore(String status, LocalDate cutoffDate);
}
