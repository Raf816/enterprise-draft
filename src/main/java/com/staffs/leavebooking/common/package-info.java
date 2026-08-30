/**
 * Shared Kernel module — contains supertypes, event infrastructure, and shared value objects
 * used by multiple bounded contexts (Lecture 4 — Shared Kernel, Spring Modulith).
 *
 * <p><strong>@ApplicationModule(type = OPEN):</strong> Spring Modulith annotation that marks
 * this module as "open" — its classes can be imported by any other module in the application.
 * This is necessary because the Shared Kernel contains:
 * <ul>
 *   <li><strong>Domain supertypes:</strong> {@code AggregateRoot}, {@code Entity}, {@code Identity},
 *       {@code ValueObject}, {@code Email}, {@code FullName}, {@code DomainAssertions}</li>
 *   <li><strong>Event infrastructure:</strong> {@code Event}, {@code LocalEvent}, {@code RemoteEvent},
 *       {@code DomainEventManager}, {@code EventStoreService}, {@code RemoteOutboxListener},
 *       {@code RabbitOutboxRouter}, {@code RabbitInfrastructureConfig}, {@code CustomMessageConverter}</li>
 *   <li><strong>Shared event records:</strong> {@code StaffMemberAddedEvent}, {@code StaffMemberUpdatedEvent},
 *       {@code ManagerNotificationEvent}, {@code StaffNotificationEvent} — these must be visible
 *       to both the producing context (Staff/Leave Management) and the consuming context
 *       (Leave Management/Notification consumers)</li>
 * </ul>
 *
 * <p><strong>Without OPEN:</strong> Spring Modulith would prevent other modules from importing
 * classes in this package, breaking the Shared Kernel pattern. The OPEN type explicitly
 * declares that this module's classes are shared infrastructure, not internal implementation.
 *
 * <p><strong>DDD Shared Kernel (Lecture 4):</strong> A Shared Kernel is a subset of the domain
 * model that is shared between bounded contexts. It contains types that both contexts need
 * (like event records and base classes). Changes to the Shared Kernel affect all contexts,
 * so it should be kept small and stable.
 *
 * @see org.springframework.modulith.ApplicationModule for the Spring Modulith module annotation
 */
@org.springframework.modulith.ApplicationModule(type = org.springframework.modulith.ApplicationModule.Type.OPEN)
package com.staffs.leavebooking.common;
