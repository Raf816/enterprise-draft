package com.staffs.leavebooking.leavemanagement.application.handlers;

import com.staffs.leavebooking.common.domain.Identity;
import com.staffs.leavebooking.common.events.DomainEventManager;
import com.staffs.leavebooking.leavemanagement.application.commands.CancelLeaveRequestCommand;
import com.staffs.leavebooking.leavemanagement.application.commands.SubmitLeaveRequestCommand;
import com.staffs.leavebooking.leavemanagement.application.mappers.LeaveRequestDomainToJpaMapper;
import com.staffs.leavebooking.leavemanagement.application.mappers.LeaveRequestJpaToDomainMapper;
import com.staffs.leavebooking.leavemanagement.domain.DateRange;
import com.staffs.leavebooking.leavemanagement.domain.LeaveRequest;
import com.staffs.leavebooking.leavemanagement.domain.LeaveType;
import com.staffs.leavebooking.leavemanagement.infrastructure.repositories.LeaveRequestRepository;
import com.staffs.leavebooking.leavemanagement.ui.exceptions.LeaveRequestNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CQRS Command Handler (Application Service) for Leave Request write operations
 * (Lecture 5/6 — CQRS Command Side, Lecture 3 — Application Services).
 *
 * <p><strong>CQRS Command Handler:</strong> This service handles all write operations for
 * the {@link LeaveRequest} aggregate. Commands go through the domain model (aggregate)
 * for validation and invariant enforcement, following the standard DDD pattern:
 * <ol>
 *   <li>Receive the command (from the facade)</li>
 *   <li>Load or create the domain aggregate</li>
 *   <li>Execute the domain command (which validates business rules and raises domain events)</li>
 *   <li>Save the aggregate back to the repository (via JPA mapper)</li>
 *   <li>Dispatch domain events via {@link DomainEventManager}</li>
 * </ol>
 *
 * <p><strong>Domain events:</strong> Each command method dispatches domain events that
 * trigger side effects in the Leave Allowance aggregate (via local event listeners):
 * <ul>
 *   <li>{@code submitNewRequest} → {@code LeaveRequestSubmittedEvent} → reserves days on allowance</li>
 *   <li>{@code approveRequest} → {@code LeaveRequestApprovedEvent} → confirms days (pending → used)</li>
 *   <li>{@code rejectRequest} → {@code LeaveRequestRejectedEvent} → releases pending days</li>
 *   <li>{@code cancelRequest} → {@code LeaveRequestCancelledEvent} → releases pending or credits back used days</li>
 * </ul>
 *
 * <p><strong>Transactional:</strong> Each command method is annotated with
 * {@code @Transactional} to ensure that the aggregate save and event dispatch happen
 * within a single database transaction. If any step fails, the entire operation is
 * rolled back.
 *
 * @see LeaveRequestQueryHandler for the CQRS read-side (query handler)
 * @see com.staffs.leavebooking.leavemanagement.LeaveManagementFacade for the facade that delegates to this service
 * @see LeaveRequest for the domain aggregate this service orchestrates
 * @see DomainEventManager for the event dispatch mechanism
 */
@Service            // Spring stereotype — registers as a service bean in the application context
@Slf4j              // Lombok: generates a static SLF4J logger field named 'log'
@AllArgsConstructor // Lombok: generates constructor with all final fields (enables constructor-based DI)
public class LeaveRequestApplicationService {

    /**
     * Spring Data repository for persisting leave request JPA entities.
     * Used to save domain aggregates (after mapping to JPA) and to load them (before mapping to domain).
     */
    private final LeaveRequestRepository leaveRequestRepository;

    /**
     * Domain event manager — dispatches domain events to the event store and RabbitMQ outbox.
     * Events raised by the LeaveRequest aggregate are dispatched after the aggregate is saved.
     *
     * @see DomainEventManager#manageDomainEvents(String, java.util.List) for the dispatch mechanism
     */
    private final DomainEventManager domainEventManager;

    /**
     * Submits a new leave request.
     *
     * <p><strong>Flow:</strong>
     * <ol>
     *   <li>Generate a new UUID for the leave request</li>
     *   <li>Call the domain factory method {@link LeaveRequest#submitNew} which validates the data,
     *       calculates the number of days, and raises a {@code LeaveRequestSubmittedEvent}</li>
     *   <li>Map the domain aggregate to a JPA entity and save</li>
     *   <li>Dispatch domain events (the submitted event triggers day reservation on the allowance)</li>
     * </ol>
     *
     * @param command the CQRS command containing staffMemberId, managerId, dates, leaveType, and reason
     * @return the generated UUID of the newly created leave request
     * @throws IllegalArgumentException if the leave type string is invalid
     * @see LeaveRequest#submitNew for the domain factory method
     * @see SubmitLeaveRequestCommand for the command structure
     */
    @Transactional // Ensures aggregate save + event dispatch happen atomically
    public String submitNewRequest(SubmitLeaveRequestCommand command) {
        // Generate a new unique identifier for the leave request
        Identity<LeaveRequest> newId = Identity.generateId();

        // Create the domain aggregate via its factory method — validates data and raises LeaveRequestSubmittedEvent
        LeaveRequest leaveRequest = LeaveRequest.submitNew(
                newId,                                          // Generated UUID wrapped in Identity
                command.staffMemberId(),                        // Who is requesting leave (from JWT)
                command.managerId(),                            // Who will approve/reject (from request body)
                parseLeaveType(command.leaveType()),            // Parse string to LeaveType enum
                new DateRange(command.startDate(), command.endDate()), // Value object for the date range
                command.reason()                                // Optional reason for the request
        );

        // Map the domain aggregate to a JPA entity and persist to the database
        leaveRequestRepository.save(LeaveRequestDomainToJpaMapper.toJpa(leaveRequest));
        // Dispatch domain events — LeaveRequestSubmittedEvent triggers day reservation on the allowance
        dispatchAndClear(leaveRequest);

        // Log the successful submission for audit/debugging
        log.info("Leave request {} submitted by staff member {}",
                newId.id(), command.staffMemberId());
        // Return the generated ID so the controller can fetch the full DTO
        return newId.id();
    }

    /**
     * Approves a pending leave request.
     *
     * <p><strong>Flow:</strong>
     * <ol>
     *   <li>Load the domain aggregate from the repository (JPA → domain mapper)</li>
     *   <li>Call {@link LeaveRequest#approve} which transitions PENDING → APPROVED and
     *       raises a {@code LeaveRequestApprovedEvent}</li>
     *   <li>Map back to JPA and save</li>
     *   <li>Dispatch domain events (the approved event triggers day confirmation on the allowance:
     *       daysPending → daysUsed)</li>
     * </ol>
     *
     * @param leaveRequestId the UUID of the leave request to approve
     * @param decidedBy      the UUID of the manager/admin who is approving
     * @param reason         optional reason for the approval decision (may be null)
     * @throws LeaveRequestNotFoundException if no request exists with the given ID
     * @throws IllegalStateException         if the request is not in PENDING status
     * @see LeaveRequest#approve for the domain method
     */
    @Transactional // Ensures aggregate save + event dispatch happen atomically
    public void approveRequest(String leaveRequestId, String decidedBy, String reason) {
        // Load the domain aggregate from the repository (JPA entity → domain aggregate via mapper)
        LeaveRequest leaveRequest = loadDomainAggregate(leaveRequestId);
        // Execute the domain command — validates status is PENDING, transitions to APPROVED, raises event
        leaveRequest.approve(decidedBy, reason);

        // Map back to JPA and persist the updated state
        leaveRequestRepository.save(LeaveRequestDomainToJpaMapper.toJpa(leaveRequest));
        // Dispatch domain events — LeaveRequestApprovedEvent triggers day confirmation on the allowance
        dispatchAndClear(leaveRequest);

        // Log the approval for audit/debugging
        log.info("Leave request {} approved by {}", leaveRequestId, decidedBy);
    }

    /**
     * Rejects a pending leave request.
     *
     * <p><strong>Flow:</strong>
     * <ol>
     *   <li>Load the domain aggregate from the repository</li>
     *   <li>Call {@link LeaveRequest#reject} which transitions PENDING → REJECTED and
     *       raises a {@code LeaveRequestRejectedEvent}</li>
     *   <li>Map back to JPA and save</li>
     *   <li>Dispatch domain events (the rejected event triggers release of pending days
     *       back to the allowance)</li>
     * </ol>
     *
     * @param leaveRequestId the UUID of the leave request to reject
     * @param decidedBy      the UUID of the manager/admin who is rejecting
     * @param reason         optional reason for the rejection decision (may be null)
     * @throws LeaveRequestNotFoundException if no request exists with the given ID
     * @throws IllegalStateException         if the request is not in PENDING status
     * @see LeaveRequest#reject for the domain method
     */
    @Transactional // Ensures aggregate save + event dispatch happen atomically
    public void rejectRequest(String leaveRequestId, String decidedBy, String reason) {
        // Load the domain aggregate from the repository
        LeaveRequest leaveRequest = loadDomainAggregate(leaveRequestId);
        // Execute the domain command — validates status is PENDING, transitions to REJECTED, raises event
        leaveRequest.reject(decidedBy, reason);

        // Map back to JPA and persist the updated state
        leaveRequestRepository.save(LeaveRequestDomainToJpaMapper.toJpa(leaveRequest));
        // Dispatch domain events — LeaveRequestRejectedEvent triggers release of pending days
        dispatchAndClear(leaveRequest);

        // Log the rejection for audit/debugging
        log.info("Leave request {} rejected by {}", leaveRequestId, decidedBy);
    }

    /**
     * Cancels a leave request (from PENDING or APPROVED status).
     *
     * <p><strong>Flow:</strong>
     * <ol>
     *   <li>Load the domain aggregate from the repository</li>
     *   <li>Call {@link LeaveRequest#cancel} which transitions to CANCELLED and
     *       raises a {@code LeaveRequestCancelledEvent} (which includes the previous status)</li>
     *   <li>Map back to JPA and save</li>
     *   <li>Dispatch domain events — the cancelled event's handler checks the previous status:
     *       <ul>
     *         <li>Was PENDING → releases pending days</li>
     *         <li>Was APPROVED → credits back used days</li>
     *       </ul>
     *   </li>
     * </ol>
     *
     * @param command the CQRS command containing leaveRequestId, cancelledBy, and optional reason
     * @throws LeaveRequestNotFoundException if no request exists with the given ID
     * @throws IllegalStateException         if the request is in REJECTED or already CANCELLED status
     * @see LeaveRequest#cancel for the domain method
     * @see CancelLeaveRequestCommand for the command structure
     */
    @Transactional // Ensures aggregate save + event dispatch happen atomically
    public void cancelRequest(CancelLeaveRequestCommand command) {
        // Load the domain aggregate from the repository
        LeaveRequest leaveRequest = loadDomainAggregate(command.leaveRequestId());
        // Execute the domain command — validates status allows cancellation, transitions to CANCELLED
        leaveRequest.cancel(command.cancelledBy(), command.reason());

        // Map back to JPA and persist the updated state
        leaveRequestRepository.save(LeaveRequestDomainToJpaMapper.toJpa(leaveRequest));
        // Dispatch domain events — event handler checks previous status to decide how to adjust allowance
        dispatchAndClear(leaveRequest);

        // Log the cancellation for audit/debugging
        log.info("Leave request {} cancelled by {}", command.leaveRequestId(), command.cancelledBy());
    }

    // ─────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Dispatches all domain events from the aggregate via the {@link DomainEventManager},
     * then clears the aggregate's event list.
     *
     * <p><strong>Guard check:</strong> Only dispatches if the aggregate has pending events.
     * This avoids unnecessary calls to the event manager when no events were raised
     * (though in practice, every command raises at least one event).
     *
     * @param aggregate the LeaveRequest aggregate whose events to dispatch
     * @see DomainEventManager#manageDomainEvents(String, java.util.List)
     */
    private void dispatchAndClear(LeaveRequest aggregate) {
        // Only dispatch if the aggregate has pending domain events
        if (aggregate.domainEventsExist()) {
            // Dispatch events to the event store and RabbitMQ outbox via the DomainEventManager
            domainEventManager.manageDomainEvents(
                    this.getClass().getSimpleName(),    // Source identifier for audit logging
                    aggregate.listOfDomainEvents()      // The list of domain events raised by the aggregate
            );
            // Clear the aggregate's event list to prevent double-dispatch
            aggregate.clearDomainEvents();
        }
    }

    /**
     * Loads a LeaveRequest JPA entity from the repository and maps it to the domain
     * aggregate for command processing.
     *
     * <p><strong>Pattern:</strong> Repository → JPA Entity → Domain Mapper → Domain Aggregate.
     * The domain aggregate is reconstituted from the persisted JPA state so that domain
     * invariants and business rules can be enforced during command execution.
     *
     * @param leaveRequestId the UUID of the leave request to load
     * @return the reconstituted domain aggregate
     * @throws LeaveRequestNotFoundException if no request exists with the given ID
     * @see LeaveRequestJpaToDomainMapper for the JPA → domain mapping logic
     */
    private LeaveRequest loadDomainAggregate(String leaveRequestId) {
        // Find the JPA entity by ID, map to domain aggregate, or throw not-found exception
        return leaveRequestRepository.findById(leaveRequestId)
                .map(LeaveRequestJpaToDomainMapper::toDomain)   // Map JPA entity to domain aggregate
                .orElseThrow(() -> new LeaveRequestNotFoundException(leaveRequestId)); // 404 if not found
    }

    /**
     * Parses a leave type string to the {@link LeaveType} enum with a user-friendly error message.
     *
     * <p>Wraps the standard {@code valueOf()} call to provide a clear error message listing
     * the valid values, rather than the default {@code IllegalArgumentException} message
     * which is cryptic for API consumers.
     *
     * @param type the leave type string to parse (e.g., "ANNUAL")
     * @return the corresponding {@link LeaveType} enum value
     * @throws IllegalArgumentException if the string doesn't match any LeaveType value
     */
    private LeaveType parseLeaveType(String type) {
        try {
            // Attempt to parse the string to the LeaveType enum
            return LeaveType.valueOf(type);
        } catch (IllegalArgumentException e) {
            // Provide a user-friendly error message listing valid values
            throw new IllegalArgumentException(
                    "Invalid leave type: '" + type + "'. Valid values are: ANNUAL");
        }
    }
}
