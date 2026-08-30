package com.staffs.leavebooking.leavemanagement.application.listeners;

import com.staffs.leavebooking.common.events.StaffMemberUpdatedEvent;
import com.staffs.leavebooking.leavemanagement.application.handlers.LeaveAllowanceApplicationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Remote event consumer that listens on the {@code leave-management.staff-member-updated}
 * RabbitMQ queue and synchronises denormalised staff details (manager, department)
 * on the staff member's
 * {@link com.staffs.leavebooking.leavemanagement.domain.LeaveAllowance LeaveAllowance}.
 *
 * <h3>DDD / Architecture Context (Lecture 8 — Remote Subscriber Pattern)</h3>
 * <p>This class implements the <strong>Remote Subscriber</strong> pattern from Lecture 8.
 * In a DDD architecture with separate bounded contexts, the Leave Management context keeps
 * a <em>denormalised snapshot</em> of certain staff details (e.g. manager ID, department)
 * on the LeaveAllowance aggregate so that it can make decisions without cross-context joins
 * or synchronous API calls. When the Staff Management context updates a staff member's
 * details, it publishes a {@link StaffMemberUpdatedEvent} via the
 * {@link com.staffs.leavebooking.common.events.RabbitOutboxRouter Outbox → RabbitMQ}
 * pipeline. This consumer picks up the event and brings the local snapshot up to date,
 * achieving eventual consistency.</p>
 *
 * <h3>How It Fits</h3>
 * <ul>
 *   <li><strong>Event source (remote):</strong> Staff Management publishes
 *       {@link StaffMemberUpdatedEvent} via the Transactional Outbox pattern
 *       ({@link com.staffs.leavebooking.common.events.RemoteOutboxListener RemoteOutboxListener}
 *       routes it to RabbitMQ).</li>
 *   <li><strong>Message broker:</strong> RabbitMQ queue
 *       {@code leave-management.staff-member-updated}.</li>
 *   <li><strong>Deserialization:</strong>
 *       {@link com.staffs.leavebooking.common.events.CustomMessageConverter CustomMessageConverter}
 *       converts the JSON message body into a {@link StaffMemberUpdatedEvent} record.</li>
 *   <li><strong>Reaction:</strong> This listener delegates to
 *       {@link LeaveAllowanceApplicationService#updateStaffDetails(String, String, String)} to
 *       update the denormalised manager ID and department on the LeaveAllowance.</li>
 * </ul>
 *
 * <h3>Error Handling</h3>
 * <p>The handler wraps the processing logic in a try-catch to prevent poison-pill messages
 * from causing infinite redelivery loops. Failures are logged as errors and the message is
 * acknowledged (removed from the queue) to avoid blocking subsequent messages.</p>
 *
 * @see StaffMemberUpdatedEvent
 * @see LeaveAllowanceApplicationService#updateStaffDetails(String, String, String)
 * @see com.staffs.leavebooking.leavemanagement.domain.LeaveAllowance#updateStaffDetails(String, String)
 * @see com.staffs.leavebooking.common.events.CustomMessageConverter
 * @see com.staffs.leavebooking.common.events.RabbitOutboxRouter
 * @see StaffMemberAddedListener
 */
@Component       // Registers this class as a Spring-managed bean so RabbitMQ infrastructure can discover it
@AllArgsConstructor // Lombok: generates a constructor for the final field, enabling constructor-based DI
@Slf4j           // Lombok: generates a private static final SLF4J logger named 'log'
@RabbitListener(queues = "leave-management.staff-member-updated") // Binds this class to the RabbitMQ queue for staff update events
public class StaffMemberUpdatedListener {

    /** Application service that encapsulates all write operations on the LeaveAllowance aggregate. */
    private final LeaveAllowanceApplicationService leaveAllowanceApplicationService;

    /**
     * Receives and processes a {@link StaffMemberUpdatedEvent} from RabbitMQ, updating
     * the denormalised staff details on the staff member's
     * {@link com.staffs.leavebooking.leavemanagement.domain.LeaveAllowance LeaveAllowance}.
     *
     * <p><strong>Flow:</strong></p>
     * <ol>
     *   <li>Log the incoming event for operational traceability.</li>
     *   <li>Delegate to {@link LeaveAllowanceApplicationService#updateStaffDetails(String, String, String)}
     *       which loads the current-year allowance, calls the domain method to update
     *       the denormalised manager ID and department, maps the result back to JPA,
     *       and persists it.</li>
     *   <li>If any exception occurs, log the error and consume the message to prevent
     *       infinite redelivery.</li>
     * </ol>
     *
     * @param event the remote domain event deserialized from the RabbitMQ message,
     *              carrying the staff member's ID, updated manager ID, and updated
     *              department name
     */
    @RabbitHandler // Marks this method as the handler for messages arriving on the class-level @RabbitListener queue
    public void receive(StaffMemberUpdatedEvent event) {
        try {
            // Log the incoming event for operational traceability across bounded contexts
            log.info("StaffMemberUpdatedEvent received from RabbitMQ — updating details for staff {}",
                    event.staffMemberId());

            // Delegate to the application service to sync the denormalised snapshot on the allowance
            // This keeps the Leave Management context's local data consistent without cross-context queries
            leaveAllowanceApplicationService.updateStaffDetails(
                    event.staffMemberId(),   // Identifies which staff member's allowance to update
                    event.managerId(),       // The new/updated manager ID (denormalised snapshot)
                    event.department()       // The new/updated department (denormalised snapshot)
            );
        } catch (Exception e) {
            // Catch-all to prevent poison-pill messages from blocking the queue
            // The message is acknowledged (consumed) even on failure to avoid infinite redelivery loops
            log.error("Failed to process StaffMemberUpdatedEvent for staff {}: {}",
                    event.staffMemberId(), e.getMessage());
        }
    }
}
