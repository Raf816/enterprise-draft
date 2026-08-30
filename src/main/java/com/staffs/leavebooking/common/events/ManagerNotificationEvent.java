package com.staffs.leavebooking.common.events;

import java.time.LocalDate;

/**
 * Remote event published when a manager needs to be notified about a pending leave request
 * (Lecture 8 — Remote Events, Notification Pattern).
 *
 * <p><strong>Producer:</strong> {@code ManagerNotificationPublisher} in Leave Management.
 * This listener reacts to {@code LeaveRequestSubmittedEvent} (a local event) and
 * raises this remote notification event. This is a two-stage pattern:
 * <pre>
 * LeaveRequest.submitNew() → LeaveRequestSubmittedEvent (local)
 *     → ManagerNotificationPublisher (@TransactionalEventListener)
 *         → raises ManagerNotificationEvent (remote)
 *             → RemoteOutboxListener → RabbitMQ
 *                 → ManagerNotificationConsumer (@RabbitListener)
 * </pre>
 *
 * <p><strong>Consumer:</strong> {@code ManagerNotificationConsumer} (via RabbitMQ queue:
 * {@code notifications.manager-pending-request}). Currently logs the notification —
 * in production, this would send an email, push notification, or update a dashboard.
 *
 * <p><strong>Routing:</strong> Published to exchange {@code leave-notifications} with
 * routing key {@code notification.manager.pending} (configured in application.yaml).
 *
 * <p><strong>Brief requirement:</strong> "Manager alerts for pending requests" —
 * this event satisfies that requirement by alerting the assigned manager when
 * a team member submits a new leave request.
 *
 * @param id             the event store surrogate ID (null before persistence)
 * @param occurredOn     the date the notification was created
 * @param managerId      the UUID of the manager who should be notified
 * @param staffMemberId  the UUID of the staff member who submitted the request
 * @param staffName      the full name of the staff member (for display in notification)
 * @param leaveRequestId the UUID of the leave request that triggered this notification
 * @param startDate      the leave start date (for display in notification)
 * @param endDate        the leave end date (for display in notification)
 * @param numberOfDays   the number of working days requested
 * @param reason         the staff member's reason for the leave request
 */
public record ManagerNotificationEvent(
        Long id,                    // Event store surrogate ID
        LocalDate occurredOn,       // When the notification was created
        String managerId,           // Manager to notify (their UUID)
        String staffMemberId,       // Staff member who submitted the request
        String staffName,           // Staff member's display name
        String leaveRequestId,      // The leave request that triggered this
        LocalDate startDate,        // Requested leave start date
        LocalDate endDate,          // Requested leave end date
        int numberOfDays,           // Number of working days
        String reason               // Staff member's reason for leave
) implements RemoteEvent {

    /**
     * Convenience constructor — used when the event is first created (no ORM id yet).
     */
    public ManagerNotificationEvent(LocalDate occurredOn, String managerId, String staffMemberId,
                                     String staffName, String leaveRequestId, LocalDate startDate,
                                     LocalDate endDate, int numberOfDays, String reason) {
        this(null, occurredOn, managerId, staffMemberId, staffName, leaveRequestId,
                startDate, endDate, numberOfDays, reason);
    }

    /**
     * Wither method — creates a copy with the database-assigned ID attached.
     */
    @Override
    public ManagerNotificationEvent withId(Long newId) {
        return new ManagerNotificationEvent(newId, this.occurredOn, this.managerId, this.staffMemberId,
                this.staffName, this.leaveRequestId, this.startDate, this.endDate, this.numberOfDays, this.reason);
    }
}
