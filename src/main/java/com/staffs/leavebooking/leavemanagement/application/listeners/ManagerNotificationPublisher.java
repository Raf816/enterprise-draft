package com.staffs.leavebooking.leavemanagement.application.listeners;

import com.staffs.leavebooking.common.events.DomainEventManager;
import com.staffs.leavebooking.common.events.Event;
import com.staffs.leavebooking.common.events.ManagerNotificationEvent;
import com.staffs.leavebooking.leavemanagement.domain.events.LeaveRequestSubmittedEvent;
import com.staffs.leavebooking.leavemanagement.infrastructure.entities.LeaveRequestJpa;
import com.staffs.leavebooking.leavemanagement.infrastructure.repositories.LeaveRequestRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDate;
import java.util.List;

/**
 * Two-stage notification bridge that converts a <strong>local</strong>
 * {@link LeaveRequestSubmittedEvent} into a <strong>remote</strong>
 * {@link ManagerNotificationEvent}, which is then routed to RabbitMQ so that a
 * manager can be alerted about a pending leave request.
 *
 * <h3>DDD / Architecture Context (Lecture 8 — Two-Stage Local → Remote Pattern)</h3>
 * <p>This class demonstrates the <strong>two-stage event bridge</strong> pattern covered
 * in Lecture 8. It listens for a local domain event (raised in-process by the LeaveRequest
 * aggregate), enriches it with additional data (manager ID, dates) by querying the
 * repository, and then publishes a remote notification event via
 * {@link com.staffs.leavebooking.common.events.DomainEventManager DomainEventManager}. The
 * {@link com.staffs.leavebooking.common.events.RemoteOutboxListener RemoteOutboxListener}
 * intercepts the remote event, persists it to the outbox, and the
 * {@link com.staffs.leavebooking.common.events.RabbitOutboxRouter RabbitOutboxRouter}
 * relays it to RabbitMQ for consumption by the
 * {@link ManagerNotificationConsumer}.</p>
 *
 * <h3>Why Two Stages?</h3>
 * <p>The local domain event ({@link LeaveRequestSubmittedEvent}) is intentionally
 * lean — it only carries the data needed by in-process subscribers. The remote
 * notification event ({@link ManagerNotificationEvent}) is richer, containing fields
 * like manager ID, start/end dates, and reason that are needed by the notification
 * consumer. This publisher acts as the adapter between the two, performing the data
 * enrichment lookup.</p>
 *
 * <h3>How It Fits</h3>
 * <ul>
 *   <li><strong>Stage 1 — Local event source:</strong>
 *       {@link com.staffs.leavebooking.leavemanagement.domain.LeaveRequest#submit LeaveRequest.submit()}
 *       raises {@link LeaveRequestSubmittedEvent}.</li>
 *   <li><strong>Stage 2 — This publisher:</strong> listens locally, enriches the data, and
 *       raises {@link ManagerNotificationEvent} (a
 *       {@link com.staffs.leavebooking.common.events.RemoteEvent RemoteEvent}).</li>
 *   <li><strong>Outbox pipeline:</strong>
 *       {@link com.staffs.leavebooking.common.events.RemoteOutboxListener RemoteOutboxListener}
 *       → event store →
 *       {@link com.staffs.leavebooking.common.events.RabbitOutboxRouter RabbitOutboxRouter}
 *       → RabbitMQ.</li>
 *   <li><strong>Consumer:</strong>
 *       {@link ManagerNotificationConsumer} picks up the message from queue
 *       {@code notifications.manager-pending-request}.</li>
 * </ul>
 *
 * <h3>Brief Requirement</h3>
 * <p>Satisfies: <em>"manager alerts re pending requests"</em>.</p>
 *
 * @see LeaveRequestSubmittedEvent
 * @see ManagerNotificationEvent
 * @see ManagerNotificationConsumer
 * @see com.staffs.leavebooking.common.events.RemoteOutboxListener
 * @see com.staffs.leavebooking.common.events.RabbitOutboxRouter
 * @see StaffNotificationPublisher
 */
@Component   // Registers this class as a Spring-managed bean so the event infrastructure can discover it
@Slf4j       // Lombok: generates a private static final SLF4J logger named 'log'
@AllArgsConstructor // Lombok: generates a constructor for all final fields, enabling constructor-based DI
public class ManagerNotificationPublisher {

    /** Repository used to look up the full LeaveRequest entity for data enrichment (manager ID, dates, reason). */
    private final LeaveRequestRepository leaveRequestRepository;

    /** Domain event manager — persists events to event_store and publishes via Spring (within a transaction). */
    private final DomainEventManager domainEventManager;

    /**
     * Handles a local {@link LeaveRequestSubmittedEvent} by looking up the full leave
     * request details and publishing a remote {@link ManagerNotificationEvent} so the
     * assigned manager is notified about the pending request.
     *
     * <p><strong>Fix (2026-08-31):</strong> Previously published the notification event
     * directly via {@code ApplicationEventPublisher}, bypassing {@link DomainEventManager}.
     * This meant the event was never persisted to the event_store and
     * {@link com.staffs.leavebooking.common.events.RemoteOutboxListener RemoteOutboxListener}
     * (which uses {@code @TransactionalEventListener(AFTER_COMMIT)}) had no active
     * transaction to bind to — so the notification was silently dropped. Now routes
     * through {@link DomainEventManager} within a {@code @Transactional} boundary,
     * ensuring the event is stored, gets a database ID via {@code withId()}, and
     * {@code RemoteOutboxListener} has a transaction to hook into.
     *
     * @param event the local domain event carrying the leave request ID, staff member ID,
     *              and number of days; published after the leave request submission is committed
     */
    @Async  // Executes on a separate thread pool so the HTTP response is not blocked
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // Only fires after the source transaction commits successfully
    @Transactional(propagation = Propagation.REQUIRES_NEW) // Own transaction so DomainEventManager can persist + RemoteOutboxListener can bind
    public void onLeaveRequestSubmitted(LeaveRequestSubmittedEvent event) {
        log.info("Publishing manager notification for leave request {} by staff {}",
                event.leaveRequestId(), event.staffMemberId());

        leaveRequestRepository.findById(event.leaveRequestId()).ifPresent(request -> {
            ManagerNotificationEvent notification = new ManagerNotificationEvent(
                    LocalDate.now(),
                    request.getManagerId(),
                    event.staffMemberId(),
                    "Staff Member",
                    event.leaveRequestId(),
                    request.getStartDate(),
                    request.getEndDate(),
                    event.numberOfDays(),
                    request.getReason()
            );
            // Route through DomainEventManager — persists to event_store and publishes within this transaction
            domainEventManager.manageDomainEvents("ManagerNotificationPublisher", List.of(notification));
        });
    }
}
