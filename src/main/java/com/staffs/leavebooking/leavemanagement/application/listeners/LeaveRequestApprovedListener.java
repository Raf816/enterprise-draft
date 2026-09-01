package com.staffs.leavebooking.leavemanagement.application.listeners;

import com.staffs.leavebooking.leavemanagement.application.handlers.LeaveAllowanceApplicationService;
import com.staffs.leavebooking.leavemanagement.domain.events.LeaveRequestApprovedEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Local event listener that confirms leave days on a staff member's
 * {@link com.staffs.leavebooking.leavemanagement.domain.LeaveAllowance LeaveAllowance}
 * when a manager approves a leave request.
 *
 * <h3>DDD / Architecture Context (Lecture 7 — Simpler Subscriber Pattern)</h3>
 * <p>This class implements the <strong>Simpler Subscriber</strong> pattern from Lecture 7.
 * When a manager approves a leave request, the LeaveRequest aggregate publishes a
 * {@link LeaveRequestApprovedEvent} as a local domain event. This listener reacts
 * synchronously (BEFORE_COMMIT), moving the requested days from the {@code daysPending} bucket into the
 * {@code daysUsed} bucket on the staff member's LeaveAllowance. This decouples the
 * approval workflow from the allowance-tracking aggregate, keeping each aggregate
 * focused on its own invariants within the Leave Management bounded context.</p>
 *
 * <h3>How It Fits</h3>
 * <ul>
 *   <li><strong>Event source:</strong>
 *       {@link com.staffs.leavebooking.leavemanagement.domain.LeaveRequest#approve LeaveRequest.approve()}
 *       raises {@link LeaveRequestApprovedEvent} via
 *       {@link com.staffs.leavebooking.common.events.DomainEventManager DomainEventManager}.</li>
 *   <li><strong>Event channel:</strong> Spring's {@code ApplicationEventPublisher} (in-process).</li>
 *   <li><strong>Reaction:</strong> This listener delegates to
 *       {@link LeaveAllowanceApplicationService#confirmDays(String, int)} to transition days
 *       from {@code daysPending} to {@code daysUsed} on the allowance.</li>
 * </ul>
 *
 * <h3>Transaction Safety</h3>
 * <p>{@code @TransactionalEventListener(BEFORE_COMMIT)} ensures this listener fires
 * within the same transaction as the leave request approval. The day confirmation
 * (daysPending → daysUsed) is atomic with the status change to APPROVED.</p>
 *
 * @see LeaveRequestApprovedEvent
 * @see LeaveAllowanceApplicationService#confirmDays(String, int)
 * @see com.staffs.leavebooking.leavemanagement.domain.LeaveAllowance#confirmDays(int)
 * @see LeaveRequestSubmittedListener
 * @see LeaveRequestRejectedListener
 * @see LeaveRequestCancelledListener
 */
@Component   // Registers this class as a Spring-managed bean so the event infrastructure can discover it
@Slf4j       // Lombok: generates a private static final SLF4J logger named 'log'
@AllArgsConstructor // Lombok: generates a constructor for the final field, enabling constructor-based DI
public class LeaveRequestApprovedListener {

    /** Application service that encapsulates all write operations on the LeaveAllowance aggregate. */
    private final LeaveAllowanceApplicationService leaveAllowanceApplicationService;

    /**
     * Handles a {@link LeaveRequestApprovedEvent} by confirming the approved number of
     * leave days, transitioning them from {@code daysPending} to {@code daysUsed}.
     *
     * <p><strong>Flow:</strong></p>
     * <ol>
     *   <li>Log the event details (number of days, staff ID, approving manager ID) for traceability.</li>
     *   <li>Delegate to {@link LeaveAllowanceApplicationService#confirmDays(String, int)}
     *       which loads the {@link com.staffs.leavebooking.leavemanagement.domain.LeaveAllowance LeaveAllowance}
     *       aggregate, calls {@code confirmDays()} on the domain object (moving days from
     *       pending to used), maps the result back to JPA, and persists it.</li>
     * </ol>
     *
     * @param event the local domain event carrying the staff member's ID, the approving
     *              manager's ID, and the number of days to confirm; published after the
     *              leave request approval is committed
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT) // Fires WITHIN the source transaction — atomic with approval
    public void handle(LeaveRequestApprovedEvent event) {
        // Log the incoming event for operational traceability and debugging
        log.info("LeaveRequestApprovedEvent received — confirming {} days for staff {}, approved by {}",
                event.numberOfDays(), event.staffMemberId(), event.managerId());

        // Delegate to the application service to confirm the days on the LeaveAllowance aggregate
        // This will: load the allowance → call domain method → daysPending -= days, daysUsed += days → persist
        leaveAllowanceApplicationService.confirmDays(
                event.staffMemberId(),   // Identifies which staff member's allowance to update
                event.numberOfDays()     // The number of days to move from 'pending' to 'used'
        );
    }
}
