package com.staffs.leavebooking.leavemanagement.application.listeners;

import com.staffs.leavebooking.leavemanagement.application.handlers.LeaveAllowanceApplicationService;
import com.staffs.leavebooking.leavemanagement.domain.events.LeaveRequestRejectedEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Local event listener that releases pending leave days back to a staff member's
 * {@link com.staffs.leavebooking.leavemanagement.domain.LeaveAllowance LeaveAllowance}
 * when a manager rejects a leave request.
 *
 * <h3>DDD / Architecture Context (Lecture 7 — Simpler Subscriber Pattern)</h3>
 * <p>This class implements the <strong>Simpler Subscriber</strong> pattern from Lecture 7.
 * When a manager rejects a leave request, the LeaveRequest aggregate publishes a
 * {@link LeaveRequestRejectedEvent} as a local domain event. This listener reacts
 * synchronously (BEFORE_COMMIT), releasing the previously-reserved days from the {@code daysPending}
 * bucket so they become available for future leave requests. This decouples the
 * rejection workflow from the allowance-tracking aggregate, keeping each aggregate
 * focused on its own invariants within the Leave Management bounded context.</p>
 *
 * <h3>How It Fits</h3>
 * <ul>
 *   <li><strong>Event source:</strong>
 *       {@link com.staffs.leavebooking.leavemanagement.domain.LeaveRequest#reject LeaveRequest.reject()}
 *       raises {@link LeaveRequestRejectedEvent} via
 *       {@link com.staffs.leavebooking.common.events.DomainEventManager DomainEventManager}.</li>
 *   <li><strong>Event channel:</strong> Spring's {@code ApplicationEventPublisher} (in-process).</li>
 *   <li><strong>Reaction:</strong> This listener delegates to
 *       {@link LeaveAllowanceApplicationService#releasePendingDays(String, int)} to decrement
 *       {@code daysPending} on the allowance, making those days available again.</li>
 * </ul>
 *
 * <h3>Transaction Safety</h3>
 * <p>{@code @TransactionalEventListener(BEFORE_COMMIT)} ensures this listener fires
 * within the same transaction as the leave request rejection. The pending day release
 * is atomic with the status change to REJECTED.</p>
 *
 * @see LeaveRequestRejectedEvent
 * @see LeaveAllowanceApplicationService#releasePendingDays(String, int)
 * @see com.staffs.leavebooking.leavemanagement.domain.LeaveAllowance#releasePendingDays(int)
 * @see LeaveRequestSubmittedListener
 * @see LeaveRequestApprovedListener
 * @see LeaveRequestCancelledListener
 */
@Component   // Registers this class as a Spring-managed bean so the event infrastructure can discover it
@Slf4j       // Lombok: generates a private static final SLF4J logger named 'log'
@AllArgsConstructor // Lombok: generates a constructor for the final field, enabling constructor-based DI
public class LeaveRequestRejectedListener {

    /** Application service that encapsulates all write operations on the LeaveAllowance aggregate. */
    private final LeaveAllowanceApplicationService leaveAllowanceApplicationService;

    /**
     * Handles a {@link LeaveRequestRejectedEvent} by releasing the pending days
     * that were reserved when the leave request was originally submitted.
     *
     * <p><strong>Flow:</strong></p>
     * <ol>
     *   <li>Log the event details (number of days, staff ID, rejecting manager ID) for traceability.</li>
     *   <li>Delegate to {@link LeaveAllowanceApplicationService#releasePendingDays(String, int)}
     *       which loads the {@link com.staffs.leavebooking.leavemanagement.domain.LeaveAllowance LeaveAllowance}
     *       aggregate, calls {@code releasePendingDays()} on the domain object (decrementing
     *       daysPending), maps the result back to JPA, and persists it.</li>
     * </ol>
     *
     * @param event the local domain event carrying the staff member's ID, the rejecting
     *              manager's ID, and the number of days to release; published after the
     *              leave request rejection is committed
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT) // Fires WITHIN the source transaction — atomic with rejection
    public void handle(LeaveRequestRejectedEvent event) {
        // Log the incoming event for operational traceability and debugging
        log.info("LeaveRequestRejectedEvent received — releasing {} pending days for staff {}, rejected by {}",
                event.numberOfDays(), event.staffMemberId(), event.managerId());

        // Delegate to the application service to release the pending days on the LeaveAllowance aggregate
        // This will: load the allowance → call domain method → daysPending -= days → persist
        leaveAllowanceApplicationService.releasePendingDays(
                event.staffMemberId(),   // Identifies which staff member's allowance to update
                event.numberOfDays()     // The number of days to release from the 'pending' bucket
        );
    }
}
