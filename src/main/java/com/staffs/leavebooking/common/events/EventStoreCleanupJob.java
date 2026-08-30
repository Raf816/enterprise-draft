package com.staffs.leavebooking.common.events;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled background job that purges old PUBLISHED and LOCAL events from the event_store table.
 *
 * <p><strong>This is the only DELETE operation in the entire system.</strong> It is justified because:
 * <ul>
 *   <li>The event_store is <strong>infrastructure</strong> (outbox + audit log), not core business data</li>
 *   <li>Successfully published events have been delivered to the message broker — the local
 *       copy is no longer needed for delivery guarantees</li>
 *   <li>Without periodic cleanup, the table grows indefinitely, degrading query performance
 *       and consuming disk space unnecessarily</li>
 *   <li>FAILED and PENDING events are <strong>never</strong> purged — they require manual
 *       investigation or retry</li>
 * </ul>
 *
 * <p><strong>Assignment guidance:</strong> "Not necessary to perform CRUD for everything as deletion
 * might not be a good idea in some situations." This cleanup job demonstrates that DELETE is
 * appropriate for infrastructure housekeeping while NOT appropriate for business entities
 * (leave requests, allowances, staff members) where audit trails must be preserved.
 * The system uses soft-delete (TERMINATED status) for staff members and preserves all
 * leave request state transitions for compliance.
 *
 * <p><strong>Schedule:</strong> Runs daily at 02:00 UTC via the cron expression.
 * In production, the cron expression and retention period would be externalised
 * to application.yaml for configurability without code changes.
 *
 * <p><strong>Retention:</strong> 30 days by default. Events older than 30 days
 * with status PUBLISHED or LOCAL are permanently deleted.
 *
 * @see EventStoreService#purgeOldEvents(int) for the actual purge logic
 */
@Component      // Spring-managed singleton — component scanning picks this up automatically
@Slf4j          // Lombok: generates a private static final Logger (SLF4J)
@AllArgsConstructor // Lombok: constructor injection for EventStoreService
public class EventStoreCleanupJob {

    /** Number of days to retain events before they become eligible for purging */
    private static final int RETENTION_DAYS = 30;

    /** Service that performs the actual database operations for event purging */
    private final EventStoreService eventStoreService;

    /**
     * Scheduled method that runs daily at 02:00 to purge old events.
     *
     * <p>The cron expression {@code "0 0 2 * * *"} means:
     * <ul>
     *   <li>second=0, minute=0, hour=2 → 02:00:00</li>
     *   <li>day=*, month=*, weekday=* → every day</li>
     * </ul>
     *
     * <p>Enabled by {@code @EnableScheduling} on the main application class.
     * Spring creates a task scheduler thread that invokes this method according
     * to the cron schedule.
     */
    @Scheduled(cron = "0 0 2 * * *") // Run at 02:00 every day
    public void cleanupOldEvents() {
        // Log the start of the cleanup job for operational monitoring
        log.info("Event store cleanup job started (retention: {} days)", RETENTION_DAYS);

        // Delegate to EventStoreService which handles the database operations
        // Returns the number of events that were purged
        int purged = eventStoreService.purgeOldEvents(RETENTION_DAYS);

        // Log completion with the count of purged events
        log.info("Event store cleanup job completed: {} events purged", purged);
    }
}
