package com.staffs.leavebooking.leavemanagement.application.listeners;

import com.staffs.leavebooking.common.events.ManagerNotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Remote event consumer that listens on the {@code notifications.manager-pending-request}
 * RabbitMQ queue and processes incoming manager notification messages. In the current
 * prototype, notifications are logged to the console; in production this would dispatch
 * an email or push notification to the assigned manager.
 *
 * <h3>DDD / Architecture Context (Lecture 8 — Remote Subscriber Pattern)</h3>
 * <p>This class is the <strong>consumer side</strong> of the two-stage notification bridge
 * pattern described in Lecture 8. It pairs with
 * {@link ManagerNotificationPublisher}, which converts a local
 * {@link com.staffs.leavebooking.leavemanagement.domain.events.LeaveRequestSubmittedEvent LeaveRequestSubmittedEvent}
 * into a remote {@link ManagerNotificationEvent} and publishes it to RabbitMQ via the
 * {@link com.staffs.leavebooking.common.events.RabbitOutboxRouter Outbox → RabbitMQ}
 * pipeline. This consumer picks up the notification from the broker and acts on it.</p>
 *
 * <h3>How It Fits</h3>
 * <ul>
 *   <li><strong>Publisher:</strong>
 *       {@link ManagerNotificationPublisher} enriches the local event and raises
 *       {@link ManagerNotificationEvent}.</li>
 *   <li><strong>Outbox pipeline:</strong>
 *       {@link com.staffs.leavebooking.common.events.RemoteOutboxListener RemoteOutboxListener}
 *       → event store →
 *       {@link com.staffs.leavebooking.common.events.RabbitOutboxRouter RabbitOutboxRouter}
 *       → RabbitMQ.</li>
 *   <li><strong>Message broker:</strong> RabbitMQ queue
 *       {@code notifications.manager-pending-request}.</li>
 *   <li><strong>Deserialization:</strong>
 *       {@link com.staffs.leavebooking.common.events.CustomMessageConverter CustomMessageConverter}
 *       converts the JSON message body into a {@link ManagerNotificationEvent} record.</li>
 *   <li><strong>Reaction:</strong> This consumer logs the notification (prototype behaviour);
 *       in production it would integrate with an email/push notification service.</li>
 * </ul>
 *
 * <h3>Brief Requirement</h3>
 * <p>Satisfies: <em>"manager alerts re pending requests"</em>.</p>
 *
 * @see ManagerNotificationEvent
 * @see ManagerNotificationPublisher
 * @see com.staffs.leavebooking.common.events.CustomMessageConverter
 * @see com.staffs.leavebooking.common.events.RabbitOutboxRouter
 * @see StaffNotificationConsumer
 */
@Component   // Registers this class as a Spring-managed bean so RabbitMQ infrastructure can discover it
@Slf4j       // Lombok: generates a private static final SLF4J logger named 'log'
@RabbitListener(queues = "notifications.manager-pending-request") // Binds this class to the RabbitMQ queue for manager notifications
public class ManagerNotificationConsumer {

    /**
     * Receives and processes a {@link ManagerNotificationEvent} from RabbitMQ.
     * Logs the notification details including the manager ID, leave request ID,
     * staff member who submitted it, number of days, and the date range.
     *
     * <p><strong>Flow:</strong></p>
     * <ol>
     *   <li>Receive the deserialized {@link ManagerNotificationEvent} from RabbitMQ.</li>
     *   <li>Log the notification details for operational visibility.</li>
     *   <li><em>(Production extension point)</em> Send an email or push notification
     *       to the manager identified by {@code event.managerId()}.</li>
     * </ol>
     *
     * @param event the remote notification event deserialized from the RabbitMQ message,
     *              carrying the manager ID, staff member ID, leave request ID, date range,
     *              number of days, and reason
     */
    @RabbitHandler // Marks this method as the handler for messages arriving on the class-level @RabbitListener queue
    public void receive(ManagerNotificationEvent event) {
        // Log the notification — in production this would trigger an email or push notification to the manager
        log.info("NOTIFICATION → Manager {} alerted: new pending leave request {} from staff {} ({} days, {}-{})",
                event.managerId(), event.leaveRequestId(), event.staffMemberId(),
                event.numberOfDays(), event.startDate(), event.endDate());
    }
}
