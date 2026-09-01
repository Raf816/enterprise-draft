package com.staffs.leavebooking.leavemanagement.application.listeners;

import com.staffs.leavebooking.leavemanagement.application.handlers.LeaveAllowanceApplicationService;
import com.staffs.leavebooking.leavemanagement.domain.events.LeaveRequestCancelledEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Local event listener that returns leave days to a staff member's
 * {@link com.staffs.leavebooking.leavemanagement.domain.LeaveAllowance LeaveAllowance}
 * when a leave request is cancelled (by staff or manager).
 *
 * <h3>DDD / Architecture Context (Lecture 7 — Simpler Subscriber Pattern)</h3>
 * <p>This class implements the <strong>Simpler Subscriber</strong> pattern from Lecture 7.
 * When a leave request is cancelled, the LeaveRequest aggregate publishes a
 * {@link LeaveRequestCancelledEvent} as a local domain event. This listener reacts
 * asynchronously, returning the days to the staff member's allowance. The key complexity
 * here is that a cancellation can apply to either an <em>approved</em> or a <em>pending</em>
 * request, and the allowance adjustment differs for each case:</p>
 * <ul>
 *   <li><strong>Previously approved:</strong> days are credited back from {@code daysUsed}
 *       (the leave was already counted as consumed).</li>
 *   <li><strong>Still pending:</strong> days are released from {@code daysPending}
 *       (the leave was only reserved, not yet consumed).</li>
 * </ul>
 * <p>The {@link LeaveRequestCancelledEvent#wasPreviouslyApproved()} flag determines which
 * path is taken, keeping the branching logic in the listener rather than the domain aggregate.</p>
 *
 * <h3>How It Fits</h3>
 * <ul>
 *   <li><strong>Event source:</strong>
 *       {@link com.staffs.leavebooking.leavemanagement.domain.LeaveRequest#cancel LeaveRequest.cancel()}
 *       raises {@link LeaveRequestCancelledEvent} via
 *       {@link com.staffs.leavebooking.common.events.DomainEventManager DomainEventManager}.</li>
 *   <li><strong>Event channel:</strong> Spring's {@code ApplicationEventPublisher} (in-process).</li>
 *   <li><strong>Reaction (approved):</strong> Delegates to
 *       {@link LeaveAllowanceApplicationService#creditBackDays(String, int)} — decrements
 *       {@code daysUsed}.</li>
 *   <li><strong>Reaction (pending):</strong> Delegates to
 *       {@link LeaveAllowanceApplicationService#releasePendingDays(String, int)} — decrements
 *       {@code daysPending}.</li>
 * </ul>
 *
 * <h3>Transaction Safety</h3>
 * <p>{@code @TransactionalEventListener(BEFORE_COMMIT)} ensures this listener fires
 * within the same transaction as the leave request cancellation. The day release/credit
 * is atomic with the status change to CANCELLED.</p>
 *
 * @see LeaveRequestCancelledEvent
 * @see LeaveAllowanceApplicationService#creditBackDays(String, int)
 * @see LeaveAllowanceApplicationService#releasePendingDays(String, int)
 * @see com.staffs.leavebooking.leavemanagement.domain.LeaveAllowance#creditBackDays(int)
 * @see com.staffs.leavebooking.leavemanagement.domain.LeaveAllowance#releasePendingDays(int)
 * @see LeaveRequestSubmittedListener
 * @see LeaveRequestApprovedListener
 * @see LeaveRequestRejectedListener
 */
@Component   // Registers this class as a Spring-managed bean so the event infrastructure can discover it
@Slf4j       // Lombok: generates a private static final SLF4J logger named 'log'
@AllArgsConstructor // Lombok: generates a constructor for the final field, enabling constructor-based DI
public class LeaveRequestCancelledListener {

    /** Application service that encapsulates all write operations on the LeaveAllowance aggregate. */
    private final LeaveAllowanceApplicationService leaveAllowanceApplicationService;

    /**
     * Handles a {@link LeaveRequestCancelledEvent} by returning the cancelled days to
     * the staff member's allowance. Branches on whether the request was previously approved
     * or still pending, because the allowance bucket that needs adjusting differs.
     *
     * <p><strong>Flow:</strong></p>
     * <ol>
     *   <li>Log the event details (action type, number of days, staff ID, who cancelled) for traceability.</li>
     *   <li>Check {@link LeaveRequestCancelledEvent#wasPreviouslyApproved()}:
     *       <ul>
     *         <li>{@code true} → delegate to
     *             {@link LeaveAllowanceApplicationService#creditBackDays(String, int)}
     *             ({@code daysUsed -= days}).</li>
     *         <li>{@code false} → delegate to
     *             {@link LeaveAllowanceApplicationService#releasePendingDays(String, int)}
     *             ({@code daysPending -= days}).</li>
     *       </ul>
     *   </li>
     * </ol>
     *
     * @param event the local domain event carrying the staff member's ID, the number of
     *              days to return, who initiated the cancellation, and whether the request
     *              had been approved before cancellation
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT) // Fires WITHIN the source transaction — atomic with cancellation
    public void handle(LeaveRequestCancelledEvent event) {
        // Log the incoming event — includes the branch path for debugging
        log.info("LeaveRequestCancelledEvent received — {} {} days for staff {}, cancelled by {}",
                event.wasPreviouslyApproved() ? "crediting back" : "releasing pending",
                event.numberOfDays(), event.staffMemberId(), event.cancelledBy());

        if (event.wasPreviouslyApproved()) {
            // The request was APPROVED before cancellation — give days back from daysUsed
            // This restores the staff member's used-day count, freeing them for future requests
            leaveAllowanceApplicationService.creditBackDays(
                    event.staffMemberId(),   // Identifies which staff member's allowance to update
                    event.numberOfDays()     // The number of days to subtract from 'daysUsed'
            );
        } else {
            // The request was still PENDING — just release the reservation from daysPending
            // The days were never confirmed as used, so we only need to clear the pending hold
            leaveAllowanceApplicationService.releasePendingDays(
                    event.staffMemberId(),   // Identifies which staff member's allowance to update
                    event.numberOfDays()     // The number of days to release from 'daysPending'
            );
        }
    }
}
