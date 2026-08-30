package com.staffs.leavebooking.common.events;

import java.time.LocalDate;

/**
 * Remote event published when a staff member needs to be notified about their
 * leave request decision (approved/rejected)
 * (Lecture 8 — Remote Events, Notification Pattern).
 *
 * <p><strong>Producer:</strong> {@code StaffNotificationPublisher} in Leave Management.
 * This listener reacts to {@code LeaveRequestApprovedEvent} and
 * {@code LeaveRequestRejectedEvent} (local events) and raises this remote
 * notification event. Two-stage pattern:
 * <pre>
 * LeaveRequest.approve() → LeaveRequestApprovedEvent (local)
 *     → StaffNotificationPublisher (@TransactionalEventListener)
 *         → raises StaffNotificationEvent (remote)
 *             → RemoteOutboxListener → RabbitMQ
 *                 → StaffNotificationConsumer (@RabbitListener)
 * </pre>
 *
 * <p><strong>Consumer:</strong> {@code StaffNotificationConsumer} (via RabbitMQ queue:
 * {@code notifications.staff-request-decided}). Currently logs the notification —
 * in production, this would send an email or push notification to the staff member.
 *
 * <p><strong>Routing:</strong> Published to exchange {@code leave-notifications} with
 * routing key {@code notification.staff.decided} (configured in application.yaml).
 *
 * <p><strong>Brief requirement:</strong> "Staff alerts for approved/cancelled requests" —
 * this event satisfies that requirement by alerting the staff member when their
 * manager approves or rejects their leave request.
 *
 * @param id             the event store surrogate ID (null before persistence)
 * @param occurredOn     the date the decision notification was created
 * @param staffMemberId  the UUID of the staff member to notify
 * @param leaveRequestId the UUID of the leave request that was decided
 * @param decision       the decision made ("APPROVED" or "REJECTED")
 * @param decidedBy      the UUID of the manager/admin who made the decision
 * @param numberOfDays   the number of days in the leave request
 */
public record StaffNotificationEvent(
        Long id,                    // Event store surrogate ID
        LocalDate occurredOn,       // When the decision notification was created
        String staffMemberId,       // Staff member to notify (their UUID)
        String leaveRequestId,      // The leave request that was decided
        String decision,            // "APPROVED" or "REJECTED"
        String decidedBy,           // Manager/admin who made the decision (their UUID)
        int numberOfDays            // Number of days in the request
) implements RemoteEvent {

    /**
     * Convenience constructor — used when the event is first created (no ORM id yet).
     */
    public StaffNotificationEvent(LocalDate occurredOn, String staffMemberId, String leaveRequestId,
                                   String decision, String decidedBy, int numberOfDays) {
        this(null, occurredOn, staffMemberId, leaveRequestId, decision, decidedBy, numberOfDays);
    }

    /**
     * Wither method — creates a copy with the database-assigned ID attached.
     */
    @Override
    public StaffNotificationEvent withId(Long newId) {
        return new StaffNotificationEvent(newId, this.occurredOn, this.staffMemberId,
                this.leaveRequestId, this.decision, this.decidedBy, this.numberOfDays);
    }
}
