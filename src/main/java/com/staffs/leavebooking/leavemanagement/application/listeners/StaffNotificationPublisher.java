package com.staffs.leavebooking.leavemanagement.application.listeners;

import com.staffs.leavebooking.common.events.DomainEventManager;
import com.staffs.leavebooking.common.events.Event;
import com.staffs.leavebooking.common.events.StaffNotificationEvent;
import com.staffs.leavebooking.leavemanagement.domain.events.LeaveRequestApprovedEvent;
import com.staffs.leavebooking.leavemanagement.domain.events.LeaveRequestRejectedEvent;
import com.staffs.leavebooking.leavemanagement.domain.events.LeaveRequestCancelledEvent;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.util.List;

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

    /** Domain event manager — persists events to event_store and publishes via Spring (within a transaction). */
    private final DomainEventManager domainEventManager;

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
    @Transactional // Opens a new transaction so RemoteOutboxListener's @TransactionalEventListener can bind to it
    public void onLeaveRequestApproved(LeaveRequestApprovedEvent event) {
        log.info("Publishing staff notification: request {} APPROVED", event.leaveRequestId());
        domainEventManager.manageDomainEvents("StaffNotificationPublisher", List.of(new StaffNotificationEvent(
                LocalDate.now(), event.staffMemberId(), event.leaveRequestId(),
                "APPROVED", event.managerId(), event.numberOfDays())));
    }

    /**
     * Handles a local {@link LeaveRequestRejectedEvent} by publishing a remote
     * {@link StaffNotificationEvent} with decision type {@code "REJECTED"}.
     *
     * @param event the local domain event carrying the staff member's ID, the rejecting
     *              manager's ID, the leave request ID, and the number of days
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void onLeaveRequestRejected(LeaveRequestRejectedEvent event) {
        log.info("Publishing staff notification: request {} REJECTED", event.leaveRequestId());
        domainEventManager.manageDomainEvents("StaffNotificationPublisher", List.of(new StaffNotificationEvent(
                LocalDate.now(), event.staffMemberId(), event.leaveRequestId(),
                "REJECTED", event.managerId(), event.numberOfDays())));
    }

    /**
     * Handles a local {@link LeaveRequestCancelledEvent} by publishing a remote
     * {@link StaffNotificationEvent} with decision type {@code "CANCELLED"}.
     *
     * @param event the local domain event carrying the staff member's ID, who cancelled it,
     *              the leave request ID, and the number of days
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void onLeaveRequestCancelled(LeaveRequestCancelledEvent event) {
        log.info("Publishing staff notification: request {} CANCELLED", event.leaveRequestId());
        domainEventManager.manageDomainEvents("StaffNotificationPublisher", List.of(new StaffNotificationEvent(
                LocalDate.now(), event.staffMemberId(), event.leaveRequestId(),
                "CANCELLED", event.cancelledBy(), event.numberOfDays())));
    }
}
