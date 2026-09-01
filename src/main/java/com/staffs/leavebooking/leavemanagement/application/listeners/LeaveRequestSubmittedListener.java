package com.staffs.leavebooking.leavemanagement.application.listeners;

import com.staffs.leavebooking.leavemanagement.application.handlers.LeaveAllowanceApplicationService;
import com.staffs.leavebooking.leavemanagement.domain.events.LeaveRequestSubmittedEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Local event listener that reserves pending leave days on a staff member's
 * {@link com.staffs.leavebooking.leavemanagement.domain.LeaveAllowance LeaveAllowance}
 * when a new leave request is submitted.
 *
 * <h3>DDD / Architecture Context (Lecture 7 — Simpler Subscriber Pattern)</h3>
 * <p>This class implements the <strong>Simpler Subscriber</strong> pattern from Lecture 7.
 * Rather than coupling the LeaveRequest aggregate directly to the LeaveAllowance aggregate,
 * the LeaveRequest publishes a {@link LeaveRequestSubmittedEvent} as a local domain event,
 * and this listener reacts synchronously (BEFORE_COMMIT) to update the LeaveAllowance
 * within the same transaction. This preserves the single-responsibility of each aggregate
 * while guaranteeing atomic consistency between the leave request and allowance.</p>
 *
 * <h3>How It Fits</h3>
 * <ul>
 *   <li><strong>Event source:</strong>
 *       {@link com.staffs.leavebooking.leavemanagement.domain.LeaveRequest#submit LeaveRequest.submit()}
 *       raises {@link LeaveRequestSubmittedEvent} via
 *       {@link com.staffs.leavebooking.common.events.DomainEventManager DomainEventManager}.</li>
 *   <li><strong>Event channel:</strong> Spring's {@code ApplicationEventPublisher} (in-process).</li>
 *   <li><strong>Reaction:</strong> This listener delegates to
 *       {@link LeaveAllowanceApplicationService#reserveDays(String, int)} to increment
 *       {@code daysPending} on the allowance, enforcing the over-booking invariant.</li>
 * </ul>
 *
 * <h3>Transaction Safety</h3>
 * <p>{@code @TransactionalEventListener(BEFORE_COMMIT)} ensures this listener fires
 * within the same transaction as the leave request submission. If the allowance
 * reservation fails (e.g., insufficient balance), the entire transaction rolls back —
 * the leave request is never persisted as PENDING. This guarantees atomic consistency
 * between the leave request and allowance aggregates.</p>
 *
 * @see LeaveRequestSubmittedEvent
 * @see LeaveAllowanceApplicationService#reserveDays(String, int)
 * @see com.staffs.leavebooking.leavemanagement.domain.LeaveAllowance#reserveDays(int)
 * @see LeaveRequestApprovedListener
 * @see LeaveRequestRejectedListener
 * @see LeaveRequestCancelledListener
 */
@Component   // Registers this class as a Spring-managed bean so the event infrastructure can discover it
@Slf4j       // Lombok: generates a private static final SLF4J logger named 'log'
@AllArgsConstructor // Lombok: generates a constructor for the final field, enabling constructor-based DI
public class LeaveRequestSubmittedListener {

    /** Application service that encapsulates all write operations on the LeaveAllowance aggregate. */
    private final LeaveAllowanceApplicationService leaveAllowanceApplicationService;

    /**
     * Handles a {@link LeaveRequestSubmittedEvent} by reserving the requested number
     * of leave days on the staff member's current-year allowance.
     *
     * <p><strong>Flow:</strong></p>
     * <ol>
     *   <li>Log the event details (request ID, staff ID, number of days) for traceability.</li>
     *   <li>Delegate to {@link LeaveAllowanceApplicationService#reserveDays(String, int)}
     *       which loads the {@link com.staffs.leavebooking.leavemanagement.domain.LeaveAllowance LeaveAllowance}
     *       aggregate, calls {@code reserveDays()} on the domain object (enforcing the
     *       over-booking invariant), maps the result back to JPA, and persists it.</li>
     * </ol>
     *
     * @param event the local domain event carrying the staff member's ID and the number of
     *              leave days to reserve; published within the producing transaction before commit
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT) // Fires WITHIN the source transaction — rollback on failure
    public void handle(LeaveRequestSubmittedEvent event) {
        // Log the incoming event for operational traceability and debugging
        log.info("LeaveRequestSubmittedEvent received — reserving {} days for staff {}",
                event.numberOfDays(), event.staffMemberId());

        // Delegate to the application service to reserve the days on the LeaveAllowance aggregate
        // This will: load the allowance → call domain method → enforce invariants → persist
        leaveAllowanceApplicationService.reserveDays(
                event.staffMemberId(),   // Identifies which staff member's allowance to update
                event.numberOfDays()     // The number of days to move into the 'pending' bucket
        );
    }
}
