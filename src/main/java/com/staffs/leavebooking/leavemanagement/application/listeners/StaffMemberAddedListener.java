package com.staffs.leavebooking.leavemanagement.application.listeners;

import com.staffs.leavebooking.common.events.StaffMemberAddedEvent;
import com.staffs.leavebooking.leavemanagement.application.handlers.LeaveAllowanceApplicationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Remote event consumer that listens on the {@code leave-management.staff-member-added}
 * RabbitMQ queue and creates a new
 * {@link com.staffs.leavebooking.leavemanagement.domain.LeaveAllowance LeaveAllowance}
 * for each newly-hired staff member.
 *
 * <h3>DDD / Architecture Context (Lecture 8 — Remote Subscriber Pattern)</h3>
 * <p>This class implements the <strong>Remote Subscriber</strong> pattern from Lecture 8.
 * When the Staff Management bounded context creates a new staff member, it publishes a
 * {@link StaffMemberAddedEvent} as a remote domain event via the
 * {@link com.staffs.leavebooking.common.events.RabbitOutboxRouter Outbox → RabbitMQ}
 * pipeline. This consumer, running in the Leave Management bounded context, picks up that
 * event from the message broker and initialises the staff member's leave allowance
 * locally — achieving eventual consistency across bounded contexts without direct
 * cross-context calls or shared databases.</p>
 *
 * <h3>How It Fits</h3>
 * <ul>
 *   <li><strong>Event source (remote):</strong> Staff Management publishes
 *       {@link StaffMemberAddedEvent} via the Transactional Outbox pattern
 *       ({@link com.staffs.leavebooking.common.events.RemoteOutboxListener RemoteOutboxListener}
 *       routes it to RabbitMQ).</li>
 *   <li><strong>Message broker:</strong> RabbitMQ queue
 *       {@code leave-management.staff-member-added}.</li>
 *   <li><strong>Deserialization:</strong>
 *       {@link com.staffs.leavebooking.common.events.CustomMessageConverter CustomMessageConverter}
 *       converts the JSON message body into a {@link StaffMemberAddedEvent} record.</li>
 *   <li><strong>Reaction:</strong> This listener delegates to
 *       {@link LeaveAllowanceApplicationService#createAllowanceForNewStaff} to create
 *       the LeaveAllowance aggregate with default entitlement.</li>
 * </ul>
 *
 * <h3>Idempotency</h3>
 * <p>The application service includes an idempotency guard — if an allowance already
 * exists for the staff member in the current year, the creation is skipped. This protects
 * against duplicate messages caused by at-least-once delivery from RabbitMQ.</p>
 *
 * <h3>Error Handling</h3>
 * <p>The handler wraps the processing logic in a try-catch to prevent poison-pill messages
 * from causing infinite redelivery loops. Failures are logged as errors and the message is
 * acknowledged (removed from the queue) to avoid blocking subsequent messages.</p>
 *
 * @see StaffMemberAddedEvent
 * @see LeaveAllowanceApplicationService#createAllowanceForNewStaff(String, String, String, String, String, int)
 * @see com.staffs.leavebooking.leavemanagement.domain.LeaveAllowance#createNew
 * @see com.staffs.leavebooking.common.events.CustomMessageConverter
 * @see com.staffs.leavebooking.common.events.RabbitOutboxRouter
 * @see StaffMemberUpdatedListener
 */
@Component       // Registers this class as a Spring-managed bean so RabbitMQ infrastructure can discover it
@AllArgsConstructor // Lombok: generates a constructor for the final field, enabling constructor-based DI
@Slf4j           // Lombok: generates a private static final SLF4J logger named 'log'
@RabbitListener(queues = "leave-management.staff-member-added") // Binds this class to the RabbitMQ queue for staff creation events
public class StaffMemberAddedListener {

    /** Application service that encapsulates all write operations on the LeaveAllowance aggregate. */
    private final LeaveAllowanceApplicationService leaveAllowanceApplicationService;

    /**
     * Receives and processes a {@link StaffMemberAddedEvent} from RabbitMQ, creating
     * a new {@link com.staffs.leavebooking.leavemanagement.domain.LeaveAllowance LeaveAllowance}
     * record for the newly-added staff member.
     *
     * <p><strong>Flow:</strong></p>
     * <ol>
     *   <li>Log the incoming event for operational traceability.</li>
     *   <li>Delegate to {@link LeaveAllowanceApplicationService#createAllowanceForNewStaff}
     *       which checks for an existing allowance (idempotency), generates a new
     *       {@link com.staffs.leavebooking.common.domain.Identity Identity}, creates the
     *       domain aggregate, maps it to JPA, and persists it.</li>
     *   <li>If any exception occurs, log the error and consume the message to prevent
     *       infinite redelivery.</li>
     * </ol>
     *
     * @param event the remote domain event deserialized from the RabbitMQ message,
     *              carrying the staff member's ID, manager ID, name, department,
     *              and default leave entitlement
     */
    @RabbitHandler // Marks this method as the handler for messages arriving on the class-level @RabbitListener queue
    public void receive(StaffMemberAddedEvent event) {
        try {
            // Log the incoming event for operational traceability across bounded contexts
            log.info("StaffMemberAddedEvent received from RabbitMQ — creating allowance for staff {}",
                    event.staffMemberId());

            // Delegate to the application service to create the allowance for the new staff member
            // The service handles idempotency (skips if allowance already exists for the current year)
            leaveAllowanceApplicationService.createAllowanceForNewStaff(
                    event.staffMemberId(),       // Unique identifier for the staff member (from Staff Management)
                    event.managerId(),           // The staff member's line manager (denormalised snapshot)
                    event.firstName(),           // Staff member's first name (denormalised snapshot)
                    event.surname(),             // Staff member's surname (denormalised snapshot)
                    event.department(),          // Staff member's department (denormalised snapshot)
                    event.defaultEntitlement()   // Default annual leave days to assign to the new allowance
            );
        } catch (Exception e) {
            // Catch-all to prevent poison-pill messages from blocking the queue
            // The message is acknowledged (consumed) even on failure to avoid infinite redelivery loops
            log.error("Failed to process StaffMemberAddedEvent for staff {}: {}",
                    event.staffMemberId(), e.getMessage());
        }
    }
}
