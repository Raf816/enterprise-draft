package com.staffs.leavebooking.common.events;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Declares the RabbitMQ infrastructure (exchanges, queues, and bindings) as Spring beans
 * (Lecture 8 — Remote Events, Message Broker Configuration).
 *
 * <p><strong>Auto-provisioning:</strong> Spring AMQP automatically creates these resources
 * on the RabbitMQ broker at startup if they don't already exist. This means a fresh
 * RabbitMQ instance requires ZERO manual setup — the application provisions its own
 * messaging infrastructure.
 *
 * <p><strong>Architecture:</strong> Two topic exchanges route events between bounded contexts:
 * <ul>
 *   <li><strong>{@code staff-management}</strong> — carries staff lifecycle events (added, updated)
 *       from the Staff Management context to the Leave Management context.
 *       Consumed by StaffMemberAddedListener and StaffMemberUpdatedListener.</li>
 *   <li><strong>{@code leave-notifications}</strong> — carries notification events triggered by
 *       leave request state changes (pending request, request decided) to notification consumers.
 *       Consumed by ManagerNotificationConsumer and StaffNotificationConsumer.</li>
 * </ul>
 *
 * <p><strong>Why Topic exchanges (not Direct)?</strong>
 * <ul>
 *   <li>Topic exchanges support routing-key wildcards (e.g., {@code staff.member.*}
 *       matches both {@code staff.member.added} and {@code staff.member.updated})</li>
 *   <li>This makes the system extensible — adding a new event type (e.g.,
 *       {@code staff.member.terminated}) only needs a new queue + binding, not a new exchange</li>
 *   <li>Demonstrates understanding of multiple exchange types beyond the simplest (Direct) case</li>
 * </ul>
 *
 * <p><strong>Queue naming convention:</strong> {@code <consumer-context>.<event-name>}
 * This makes it clear which bounded context owns and consumes each queue:
 * <ul>
 *   <li>{@code leave-management.staff-member-added} — consumed by Leave Management</li>
 *   <li>{@code notifications.manager-pending-request} — consumed by notification module</li>
 * </ul>
 *
 * <p><strong>Complete infrastructure map:</strong>
 * <pre>
 * Exchange: staff-management (Topic)
 *   ├── staff.member.added → leave-management.staff-member-added
 *   └── staff.member.updated → leave-management.staff-member-updated
 *
 * Exchange: leave-notifications (Topic)
 *   ├── notification.manager.pending → notifications.manager-pending-request
 *   └── notification.staff.decided → notifications.staff-request-decided
 * </pre>
 *
 * @see RabbitOutboxRouter for routing events to the correct exchange/routing-key
 * @see RemoteOutboxListener for the publishing mechanism
 * @see CustomMessageConverter for JSON message serialisation
 */
@Configuration // Spring configuration class — all @Bean methods register infrastructure with Spring AMQP
public class RabbitInfrastructureConfig {

    // ─────────────────────────────────────────────────────────────────
    // EXCHANGES — the "post offices" that route messages to queues
    // ─────────────────────────────────────────────────────────────────

    /**
     * Topic exchange for Staff Management events.
     * Messages are routed by matching the routing key against binding patterns.
     *
     * <p><strong>Producers:</strong> Staff Management context (via StaffMember aggregate)
     * <p><strong>Routing keys:</strong> staff.member.added, staff.member.updated
     * <p><strong>Consumers:</strong> Leave Management (StaffMemberAddedListener, StaffMemberUpdatedListener)
     *
     * @return a durable TopicExchange named "staff-management"
     */
    @Bean
    public TopicExchange staffManagementExchange() {
        // TopicExchange is durable by default (survives broker restarts)
        return new TopicExchange("staff-management");
    }

    /**
     * Topic exchange for leave notification events.
     * Routes notification messages to the appropriate consumer queues.
     *
     * <p><strong>Producers:</strong> Leave Management context (via LeaveRequest aggregate listeners)
     * <p><strong>Routing keys:</strong> notification.manager.pending, notification.staff.decided
     * <p><strong>Consumers:</strong> ManagerNotificationConsumer, StaffNotificationConsumer
     *
     * @return a durable TopicExchange named "leave-notifications"
     */
    @Bean
    public TopicExchange leaveNotificationsExchange() {
        return new TopicExchange("leave-notifications");
    }

    // ─────────────────────────────────────────────────────────────────
    // QUEUES — the "mailboxes" where messages wait to be consumed
    // ─────────────────────────────────────────────────────────────────

    /**
     * Queue for StaffMemberAddedEvent messages.
     * Consumed by Leave Management when a new staff member is activated
     * (PENDING_SETUP → ACTIVE), triggering LeaveAllowance creation.
     *
     * @return a durable queue named "leave-management.staff-member-added"
     */
    @Bean
    public Queue staffMemberAddedQueue() {
        // QueueBuilder.durable() creates a queue that survives broker restarts
        return QueueBuilder.durable("leave-management.staff-member-added").build();
    }

    /**
     * Queue for StaffMemberUpdatedEvent messages.
     * Consumed by Leave Management when a staff member's department or manager changes,
     * updating the denormalised staff details on the corresponding LeaveAllowance.
     *
     * @return a durable queue named "leave-management.staff-member-updated"
     */
    @Bean
    public Queue staffMemberUpdatedQueue() {
        return QueueBuilder.durable("leave-management.staff-member-updated").build();
    }

    /**
     * Queue for ManagerNotificationEvent messages.
     * Consumed by the notification subsystem to alert managers about pending leave requests.
     *
     * @return a durable queue named "notifications.manager-pending-request"
     */
    @Bean
    public Queue managerNotificationQueue() {
        return QueueBuilder.durable("notifications.manager-pending-request").build();
    }

    /**
     * Queue for StaffNotificationEvent messages.
     * Consumed by the notification subsystem to alert staff about leave request decisions.
     *
     * @return a durable queue named "notifications.staff-request-decided"
     */
    @Bean
    public Queue staffNotificationQueue() {
        return QueueBuilder.durable("notifications.staff-request-decided").build();
    }

    // ─────────────────────────────────────────────────────────────────
    // BINDINGS — the "rules" connecting exchanges to queues via routing keys
    // ─────────────────────────────────────────────────────────────────

    /**
     * Binds the staff-member-added queue to the staff-management exchange
     * with routing key "staff.member.added".
     * When a message with this routing key is published to the exchange,
     * it is delivered to this queue.
     */
    @Bean
    public Binding bindStaffMemberAdded(Queue staffMemberAddedQueue,
                                         TopicExchange staffManagementExchange) {
        return BindingBuilder
                .bind(staffMemberAddedQueue)           // destination queue
                .to(staffManagementExchange)            // source exchange
                .with("staff.member.added");            // routing key pattern
    }

    /**
     * Binds the staff-member-updated queue to the staff-management exchange
     * with routing key "staff.member.updated".
     */
    @Bean
    public Binding bindStaffMemberUpdated(Queue staffMemberUpdatedQueue,
                                           TopicExchange staffManagementExchange) {
        return BindingBuilder
                .bind(staffMemberUpdatedQueue)
                .to(staffManagementExchange)
                .with("staff.member.updated");
    }

    /**
     * Binds the manager notification queue to the leave-notifications exchange
     * with routing key "notification.manager.pending".
     */
    @Bean
    public Binding bindManagerNotification(Queue managerNotificationQueue,
                                            TopicExchange leaveNotificationsExchange) {
        return BindingBuilder
                .bind(managerNotificationQueue)
                .to(leaveNotificationsExchange)
                .with("notification.manager.pending");
    }

    /**
     * Binds the staff notification queue to the leave-notifications exchange
     * with routing key "notification.staff.decided".
     */
    @Bean
    public Binding bindStaffNotification(Queue staffNotificationQueue,
                                          TopicExchange leaveNotificationsExchange) {
        return BindingBuilder
                .bind(staffNotificationQueue)
                .to(leaveNotificationsExchange)
                .with("notification.staff.decided");
    }
}
