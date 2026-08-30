package com.staffs.leavebooking.leavemanagement.application.listeners;

import com.staffs.leavebooking.common.events.StaffNotificationEvent;
import com.staffs.leavebooking.leavemanagement.domain.events.LeaveRequestApprovedEvent;
import com.staffs.leavebooking.leavemanagement.domain.events.LeaveRequestRejectedEvent;
import com.staffs.leavebooking.leavemanagement.domain.events.LeaveRequestCancelledEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;

/**
 * Two-stage notification bridge that converts <strong>local</strong> leave-request
 * decision events ({@link LeaveRequestApprovedEvent}, {@link LeaveRequestRejectedEvent},
 * {@link LeaveRequestCancelledEvent}) into <strong>remote</strong>
 * {@link StaffNotificationEvent StaffNotificationEvents}, which are then routed to RabbitMQ
 * so that the affected staff member can be notified about the outcome.
 *
 * <h3>DDD / Architecture Context (Lecture 8 — Two-Stage Local → Remote Pattern)</h3>
 * <p>This class mirrors the two-stage bridge pattern seen in
 * {@link ManagerNotificationPublisher}, but in the opposite direction — notifying
 * <em>staff</em> rather than <em>managers</em>. It listens for local domain events that
 * represent a decision on a leave request (approved, rejected, or cancelled), and raises
 * a remote {@link StaffNotificationEvent} via Spring's {@link ApplicationEventPublisher}.
 * The {@link com.staffs.leavebooking.common.events.RemoteOutboxListener RemoteOutboxListener}
 * intercepts the remote event, persists it to the outbox, and the
 * {@link com.staffs.leavebooking.common.events.RabbitOutboxRouter RabbitOutboxRouter}
 * relays it to RabbitMQ for consumption by the {@link StaffNotificationConsumer}.</p>
 *
 * <h3>How It Fits</h3>
 * <ul>
 *   <li><strong>Stage 1 — Local event sources:</strong>
 *       {@link com.staffs.leavebooking.leavemanagement.domain.LeaveRequest LeaveRequest}
 *       raises approved / rejected / cancelled events via
 *       {@link com.staffs.leavebooking.common.events.DomainEventManager DomainEventManager}.</li>
 *   <li><strong>Stage 2 — This publisher:</strong> listens locally, constructs a
 *       {@link StaffNotificationEvent} with the decision type, and raises it as a remote event.</li>
 *   <li><strong>Outbox pipeline:</strong>
 *       {@link com.staffs.leavebooking.common.events.RemoteOutboxListener RemoteOutboxListener}
 *       → event store →
 *       {@link com.staffs.leavebooking.common.events.RabbitOutboxRouter RabbitOutboxRouter}
 *       → RabbitMQ.</li>
 *   <li><strong>Consumer:</strong>
 *       {@link StaffNotificationConsumer} picks up the message from queue
 *       {@code notifications.staff-request-decided}.</li>
 * </ul>
 *
 * <h3>Brief Requirement</h3>
 * <p>Satisfies: <em>"staff alerts for approved/cancelled requests"</em>.</p>
 *
 * @see StaffNotificationEvent
 * @see StaffNotificationConsumer
 * @see LeaveRequestApprovedEvent
 * @see LeaveRequestRejectedEvent
 * @see LeaveRequestCancelledEvent
 * @see com.staffs.leavebooking.common.events.RemoteOutboxListener
 * @see com.staffs.leavebooking.common.events.RabbitOutboxRouter
 * @see ManagerNotificationPublisher
 */
@Component   // Registers this class as a Spring-managed bean so the event infrastructure can discover it
@Slf4j       // Lombok: generates a private static final SLF4J logger named 'log'
@AllArgsConstructor // Lombok: generates a constructor for the final field, enabling constructor-based DI
public class StaffNotificationPublisher {

    /** Spring's in-process event publisher — used to raise remote StaffNotificationEvents. */
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Handles a local {@link LeaveRequestApprovedEvent} by publishing a remote
     * {@link StaffNotificationEvent} with decision type {@code "APPROVED"}.
     *
     * <p><strong>Flow:</strong></p>
     * <ol>
     *   <li>Log the approval notification intent for traceability.</li>
     *   <li>Construct a {@link StaffNotificationEvent} with today's date, the staff member's ID,
     *       leave request ID, decision type {@code "APPROVED"}, the approving manager's ID,
     *       and the number of days.</li>
     *   <li>Publish the remote event via {@link ApplicationEventPublisher} — this is
     *       intercepted by
     *       {@link com.staffs.leavebooking.common.events.RemoteOutboxListener RemoteOutboxListener}
     *       and routed to RabbitMQ.</li>
     * </ol>
     *
     * @param event the local domain event carrying the staff member's ID, the approving
     *              manager's ID, the leave request ID, and the number of days
     */
    @Async  // Executes on a separate thread pool so the HTTP response is not blocked
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // Only fires after the source transaction commits successfully
    public void onLeaveRequestApproved(LeaveRequestApprovedEvent event) {
        // Log the intent to publish a staff notification for the approved request
        log.info("Publishing staff notification: request {} APPROVED", event.leaveRequestId());
        // Construct and publish the remote notification event with the "APPROVED" decision
        // RemoteOutboxListener will intercept this and route it to RabbitMQ
        eventPublisher.publishEvent(new StaffNotificationEvent(
                LocalDate.now(),             // The date the notification was generated
                event.staffMemberId(),       // The staff member to notify about the decision
                event.leaveRequestId(),      // The leave request this decision relates to
                "APPROVED",                  // The decision type — tells the staff member the outcome
                event.managerId(),           // The manager who made the decision
                event.numberOfDays()));      // The number of leave days that were approved
    }

    /**
     * Handles a local {@link LeaveRequestRejectedEvent} by publishing a remote
     * {@link StaffNotificationEvent} with decision type {@code "REJECTED"}.
     *
     * <p><strong>Flow:</strong></p>
     * <ol>
     *   <li>Log the rejection notification intent for traceability.</li>
     *   <li>Construct a {@link StaffNotificationEvent} with today's date, the staff member's ID,
     *       leave request ID, decision type {@code "REJECTED"}, the rejecting manager's ID,
     *       and the number of days.</li>
     *   <li>Publish the remote event via {@link ApplicationEventPublisher} — this is
     *       intercepted by
     *       {@link com.staffs.leavebooking.common.events.RemoteOutboxListener RemoteOutboxListener}
     *       and routed to RabbitMQ.</li>
     * </ol>
     *
     * @param event the local domain event carrying the staff member's ID, the rejecting
     *              manager's ID, the leave request ID, and the number of days
     */
    @Async  // Executes on a separate thread pool so the HTTP response is not blocked
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // Only fires after the source transaction commits successfully
    public void onLeaveRequestRejected(LeaveRequestRejectedEvent event) {
        // Log the intent to publish a staff notification for the rejected request
        log.info("Publishing staff notification: request {} REJECTED", event.leaveRequestId());
        // Construct and publish the remote notification event with the "REJECTED" decision
        // RemoteOutboxListener will intercept this and route it to RabbitMQ
        eventPublisher.publishEvent(new StaffNotificationEvent(
                LocalDate.now(),             // The date the notification was generated
                event.staffMemberId(),       // The staff member to notify about the decision
                event.leaveRequestId(),      // The leave request this decision relates to
                "REJECTED",                  // The decision type — tells the staff member the outcome
                event.managerId(),           // The manager who made the decision
                event.numberOfDays()));      // The number of leave days that were rejected
    }

    /**
     * Handles a local {@link LeaveRequestCancelledEvent} by publishing a remote
     * {@link StaffNotificationEvent} with decision type {@code "CANCELLED"}.
     *
     * <p><strong>Flow:</strong></p>
     * <ol>
     *   <li>Log the cancellation notification intent for traceability.</li>
     *   <li>Construct a {@link StaffNotificationEvent} with today's date, the staff member's ID,
     *       leave request ID, decision type {@code "CANCELLED"}, who initiated the cancellation,
     *       and the number of days.</li>
     *   <li>Publish the remote event via {@link ApplicationEventPublisher} — this is
     *       intercepted by
     *       {@link com.staffs.leavebooking.common.events.RemoteOutboxListener RemoteOutboxListener}
     *       and routed to RabbitMQ.</li>
     * </ol>
     *
     * @param event the local domain event carrying the staff member's ID, who cancelled it,
     *              the leave request ID, and the number of days
     */
    @Async  // Executes on a separate thread pool so the HTTP response is not blocked
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // Only fires after the source transaction commits successfully
    public void onLeaveRequestCancelled(LeaveRequestCancelledEvent event) {
        // Log the intent to publish a staff notification for the cancelled request
        log.info("Publishing staff notification: request {} CANCELLED", event.leaveRequestId());
        // Construct and publish the remote notification event with the "CANCELLED" decision
        // RemoteOutboxListener will intercept this and route it to RabbitMQ
        eventPublisher.publishEvent(new StaffNotificationEvent(
                LocalDate.now(),             // The date the notification was generated
                event.staffMemberId(),       // The staff member to notify about the cancellation
                event.leaveRequestId(),      // The leave request this cancellation relates to
                "CANCELLED",                 // The decision type — tells the staff member the request was cancelled
                event.cancelledBy(),         // The person who initiated the cancellation (staff or manager)
                event.numberOfDays()));      // The number of leave days that were cancelled
    }
}
