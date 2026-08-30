package com.staffs.leavebooking.leavemanagement.application.listeners;

import com.staffs.leavebooking.common.events.StaffNotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Remote event consumer that listens on the {@code notifications.staff-request-decided}
 * RabbitMQ queue and processes incoming staff notification messages. In the current
 * prototype, notifications are logged to the console; in production this would dispatch
 * an email or push notification to the affected staff member.
 *
 * <h3>DDD / Architecture Context (Lecture 8 — Remote Subscriber Pattern)</h3>
 * <p>This class is the <strong>consumer side</strong> of the two-stage notification bridge
 * pattern described in Lecture 8. It pairs with
 * {@link StaffNotificationPublisher}, which converts local leave-request decision events
 * ({@link com.staffs.leavebooking.leavemanagement.domain.events.LeaveRequestApprovedEvent LeaveRequestApprovedEvent},
 * {@link com.staffs.leavebooking.leavemanagement.domain.events.LeaveRequestRejectedEvent LeaveRequestRejectedEvent},
 * {@link com.staffs.leavebooking.leavemanagement.domain.events.LeaveRequestCancelledEvent LeaveRequestCancelledEvent})
 * into remote {@link StaffNotificationEvent StaffNotificationEvents} and publishes them to
 * RabbitMQ via the
 * {@link com.staffs.leavebooking.common.events.RabbitOutboxRouter Outbox → RabbitMQ}
 * pipeline. This consumer picks up those notifications from the broker and acts on them.</p>
 *
 * <h3>How It Fits</h3>
 * <ul>
 *   <li><strong>Publisher:</strong>
 *       {@link StaffNotificationPublisher} listens for approved/rejected/cancelled events
 *       and raises {@link StaffNotificationEvent}.</li>
 *   <li><strong>Outbox pipeline:</strong>
 *       {@link com.staffs.leavebooking.common.events.RemoteOutboxListener RemoteOutboxListener}
 *       → event store →
 *       {@link com.staffs.leavebooking.common.events.RabbitOutboxRouter RabbitOutboxRouter}
 *       → RabbitMQ.</li>
 *   <li><strong>Message broker:</strong> RabbitMQ queue
 *       {@code notifications.staff-request-decided}.</li>
 *   <li><strong>Deserialization:</strong>
 *       {@link com.staffs.leavebooking.common.events.CustomMessageConverter CustomMessageConverter}
 *       converts the JSON message body into a {@link StaffNotificationEvent} record.</li>
 *   <li><strong>Reaction:</strong> This consumer logs the notification (prototype behaviour);
 *       in production it would integrate with an email/push notification service.</li>
 * </ul>
 *
 * <h3>Brief Requirement</h3>
 * <p>Satisfies: <em>"staff alerts for approved/cancelled requests"</em>.</p>
 *
 * @see StaffNotificationEvent
 * @see StaffNotificationPublisher
 * @see com.staffs.leavebooking.common.events.CustomMessageConverter
 * @see com.staffs.leavebooking.common.events.RabbitOutboxRouter
 * @see ManagerNotificationConsumer
 */
@Component   // Registers this class as a Spring-managed bean so RabbitMQ infrastructure can discover it
@Slf4j       // Lombok: generates a private static final SLF4J logger named 'log'
@RabbitListener(queues = "notifications.staff-request-decided") // Binds this class to the RabbitMQ queue for staff decision notifications
public class StaffNotificationConsumer {

    /**
     * Receives and processes a {@link StaffNotificationEvent} from RabbitMQ.
     * Logs the notification details including the staff member ID, leave request ID,
     * and the decision type (APPROVED, REJECTED, or CANCELLED).
     *
     * <p><strong>Flow:</strong></p>
     * <ol>
     *   <li>Receive the deserialized {@link StaffNotificationEvent} from RabbitMQ.</li>
     *   <li>Log the notification details for operational visibility.</li>
     *   <li><em>(Production extension point)</em> Send an email or push notification
     *       to the staff member identified by {@code event.staffMemberId()}.</li>
     * </ol>
     *
     * @param event the remote notification event deserialized from the RabbitMQ message,
     *              carrying the staff member ID, leave request ID, and decision type
     *              (APPROVED / REJECTED / CANCELLED)
     */
    @RabbitHandler // Marks this method as the handler for messages arriving on the class-level @RabbitListener queue
    public void receive(StaffNotificationEvent event) {
        // Log the notification — in production this would trigger an email or push notification to the staff member
        log.info("NOTIFICATION → Staff {} alerted: leave request {} has been {}",
                event.staffMemberId(), event.leaveRequestId(), event.decision());
    }
}
