package com.staffs.leavebooking.leavemanagement.application.handlers;

import com.staffs.leavebooking.common.domain.Identity;
import com.staffs.leavebooking.leavemanagement.application.commands.AmendEntitlementCommand;
import com.staffs.leavebooking.leavemanagement.application.mappers.LeaveAllowanceDomainToJpaMapper;
import com.staffs.leavebooking.leavemanagement.application.mappers.LeaveAllowanceJpaToDomainMapper;
import com.staffs.leavebooking.leavemanagement.domain.LeaveAllowance;
import com.staffs.leavebooking.leavemanagement.infrastructure.entities.LeaveAllowanceJpa;
import com.staffs.leavebooking.leavemanagement.infrastructure.repositories.LeaveAllowanceRepository;
import com.staffs.leavebooking.leavemanagement.ui.exceptions.LeaveAllowanceNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * CQRS Command Handler (Application Service) for Leave Allowance write operations
 * (Lecture 5/6 — CQRS Command Side, Lecture 3 — Application Services, Lecture 7 — Domain Events).
 *
 * <p><strong>CQRS Command Handler:</strong> This service handles all write operations for
 * the {@link LeaveAllowance} aggregate. It is driven by three sources:
 * <ol>
 *   <li><strong>Local domain events</strong> from LeaveRequest (reserve/confirm/release/credit-back):
 *       When a leave request is submitted, approved, rejected, or cancelled, local event
 *       listeners call this service to adjust the allowance balance accordingly.</li>
 *   <li><strong>Remote events via RabbitMQ</strong> from Staff Management (create/update):
 *       When a staff member is activated or their department/manager changes, RabbitMQ
 *       listeners call this service to create or update the allowance.</li>
 *   <li><strong>Admin commands</strong> (amend entitlement): Direct CQRS commands from
 *       the facade when an admin adjusts a staff member's leave entitlement.</li>
 * </ol>
 *
 * <p><strong>Pattern:</strong> Each write method follows the standard DDD application service
 * pattern: load JPA entity → map to domain aggregate → execute domain command (which
 * enforces invariants) → map back to JPA → save. This ensures all business rules are
 * enforced by the domain layer, not the application service.
 *
 * <p><strong>Transactional:</strong> Each write method is annotated with
 * {@code @Transactional} to ensure atomicity. If the domain aggregate rejects the
 * command (e.g., over-booking), the transaction is rolled back.
 *
 * @see LeaveAllowanceQueryHandler for the CQRS read-side (query handler)
 * @see com.staffs.leavebooking.leavemanagement.LeaveManagementFacade for the facade that delegates admin commands
 * @see LeaveAllowance for the domain aggregate this service orchestrates
 * @see LeaveAllowanceRepository for the persistence layer
 */
@Service            // Spring stereotype — registers as a service bean in the application context
@Slf4j              // Lombok: generates a static SLF4J logger field named 'log'
@AllArgsConstructor // Lombok: generates constructor with all final fields (enables constructor-based DI)
public class LeaveAllowanceApplicationService {

    /**
     * Spring Data repository for persisting leave allowance JPA entities.
     * Used to load, save, and query allowance records.
     */
    private final LeaveAllowanceRepository leaveAllowanceRepository;

    // ─────────────────────────────────────────────────────────────────
    // EVENT-DRIVEN OPERATIONS (called by local event listeners)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Reserves days when a leave request is submitted (daysPending += numberOfDays).
     *
     * <p><strong>Triggered by:</strong> Local listener for {@code LeaveRequestSubmittedEvent},
     * which fires when a staff member submits a new leave request.
     *
     * <p><strong>Domain invariant enforced:</strong> The domain aggregate's {@code reserveDays()}
     * method checks that {@code daysUsed + daysPending + numberOfDays <= totalEntitlement}.
     * If the staff member doesn't have enough available days, the domain throws an exception
     * and the entire transaction (including the leave request save) is rolled back.
     *
     * @param staffMemberId the UUID of the staff member whose allowance to update
     * @param numberOfDays  the number of leave days to reserve
     * @throws LeaveAllowanceNotFoundException if no allowance exists for the staff member
     * @throws IllegalStateException           if reserving would exceed the total entitlement
     * @see LeaveAllowance#reserveDays(int) for the domain method
     */
    @Transactional // Ensures load + domain command + save happen atomically
    public void reserveDays(String staffMemberId, int numberOfDays) {
        // Load the current business year's allowance JPA entity for this staff member
        LeaveAllowanceJpa jpa = findCurrentAllowance(staffMemberId);
        // Map JPA entity to domain aggregate for invariant enforcement
        LeaveAllowance allowance = LeaveAllowanceJpaToDomainMapper.toDomain(jpa);

        // Domain enforces: daysUsed + daysPending + numberOfDays <= totalEntitlement
        allowance.reserveDays(numberOfDays);

        // Map domain state back to JPA entity (updates fields in-place) and save
        LeaveAllowanceDomainToJpaMapper.updateJpa(allowance, jpa);
        leaveAllowanceRepository.save(jpa);
        // Log the reservation for audit/debugging
        log.info("Reserved {} days for staff member {}", numberOfDays, staffMemberId);
    }

    /**
     * Confirms days when a leave request is approved (daysPending -= numberOfDays, daysUsed += numberOfDays).
     *
     * <p><strong>Triggered by:</strong> Local listener for {@code LeaveRequestApprovedEvent},
     * which fires when a manager approves a pending leave request.
     *
     * <p><strong>Balance shift:</strong> Days move from the "pending" bucket to the "used"
     * bucket. The total commitment doesn't change — it was already accounted for when the
     * days were reserved at submission time.
     *
     * @param staffMemberId the UUID of the staff member whose allowance to update
     * @param numberOfDays  the number of leave days to confirm
     * @throws LeaveAllowanceNotFoundException if no allowance exists for the staff member
     * @see LeaveAllowance#confirmDays(int) for the domain method
     */
    @Transactional // Ensures load + domain command + save happen atomically
    public void confirmDays(String staffMemberId, int numberOfDays) {
        // Load the current business year's allowance JPA entity
        LeaveAllowanceJpa jpa = findCurrentAllowance(staffMemberId);
        // Map to domain aggregate
        LeaveAllowance allowance = LeaveAllowanceJpaToDomainMapper.toDomain(jpa);

        // Domain shifts days: daysPending -= numberOfDays, daysUsed += numberOfDays
        allowance.confirmDays(numberOfDays);

        // Map back to JPA and save
        LeaveAllowanceDomainToJpaMapper.updateJpa(allowance, jpa);
        leaveAllowanceRepository.save(jpa);
        // Log the confirmation for audit/debugging
        log.info("Confirmed {} days for staff member {}", numberOfDays, staffMemberId);
    }

    /**
     * Releases pending days when a PENDING request is rejected or cancelled (daysPending -= numberOfDays).
     *
     * <p><strong>Triggered by:</strong> Local listeners for {@code LeaveRequestRejectedEvent}
     * and {@code LeaveRequestCancelledEvent} (when the cancelled request was in PENDING status).
     *
     * <p><strong>Effect:</strong> The reserved days are released back to the available pool,
     * increasing the staff member's available days for future bookings.
     *
     * @param staffMemberId the UUID of the staff member whose allowance to update
     * @param numberOfDays  the number of pending leave days to release
     * @throws LeaveAllowanceNotFoundException if no allowance exists for the staff member
     * @see LeaveAllowance#releasePendingDays(int) for the domain method
     */
    @Transactional // Ensures load + domain command + save happen atomically
    public void releasePendingDays(String staffMemberId, int numberOfDays) {
        // Load the current business year's allowance JPA entity
        LeaveAllowanceJpa jpa = findCurrentAllowance(staffMemberId);
        // Map to domain aggregate
        LeaveAllowance allowance = LeaveAllowanceJpaToDomainMapper.toDomain(jpa);

        // Domain releases pending days: daysPending -= numberOfDays
        allowance.releasePendingDays(numberOfDays);

        // Map back to JPA and save
        LeaveAllowanceDomainToJpaMapper.updateJpa(allowance, jpa);
        leaveAllowanceRepository.save(jpa);
        // Log the release for audit/debugging
        log.info("Released {} pending days for staff member {}", numberOfDays, staffMemberId);
    }

    /**
     * Credits back days when an APPROVED request is cancelled (daysUsed -= numberOfDays).
     *
     * <p><strong>Triggered by:</strong> Local listener for {@code LeaveRequestCancelledEvent}
     * when the cancelled request was in APPROVED status.
     *
     * <p><strong>Effect:</strong> The used days are credited back to the staff member's
     * balance, as if the approved leave never happened. This allows the staff member
     * to rebook those days.
     *
     * @param staffMemberId the UUID of the staff member whose allowance to update
     * @param numberOfDays  the number of used leave days to credit back
     * @throws LeaveAllowanceNotFoundException if no allowance exists for the staff member
     * @see LeaveAllowance#creditBackDays(int) for the domain method
     */
    @Transactional // Ensures load + domain command + save happen atomically
    public void creditBackDays(String staffMemberId, int numberOfDays) {
        // Load the current business year's allowance JPA entity
        LeaveAllowanceJpa jpa = findCurrentAllowance(staffMemberId);
        // Map to domain aggregate
        LeaveAllowance allowance = LeaveAllowanceJpaToDomainMapper.toDomain(jpa);

        // Domain credits back used days: daysUsed -= numberOfDays
        allowance.creditBackDays(numberOfDays);

        // Map back to JPA and save
        LeaveAllowanceDomainToJpaMapper.updateJpa(allowance, jpa);
        leaveAllowanceRepository.save(jpa);
        // Log the credit-back for audit/debugging
        log.info("Credited back {} days for staff member {}", numberOfDays, staffMemberId);
    }

    // ─────────────────────────────────────────────────────────────────
    // ADMIN OPERATIONS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Amends the total entitlement for a staff member's leave allowance (admin operation).
     *
     * <p><strong>Triggered by:</strong> The facade's {@code amendEntitlement()} method,
     * called from the PATCH /leave-allowances/{id} endpoint.
     *
     * <p><strong>Domain validation:</strong> The domain aggregate's {@code amendEntitlement()}
     * method validates that the new entitlement is not less than the days already used,
     * preventing the allowance from going into a negative balance.
     *
     * @param command the CQRS command containing the allowance ID and new entitlement value
     * @throws LeaveAllowanceNotFoundException if no allowance exists with the given ID
     * @throws IllegalArgumentException        if the new entitlement is less than days used
     * @see LeaveAllowance#amendEntitlement(int) for the domain method
     * @see AmendEntitlementCommand for the command structure
     */
    @Transactional // Ensures load + domain command + save happen atomically
    public void amendEntitlement(AmendEntitlementCommand command) {
        // Load the allowance JPA entity by its ID (not by staffMemberId — admin uses the allowance ID directly)
        LeaveAllowanceJpa jpa = leaveAllowanceRepository.findById(command.leaveAllowanceId())
                .orElseThrow(() -> new LeaveAllowanceNotFoundException(command.leaveAllowanceId()));

        // Map to domain aggregate for invariant enforcement
        LeaveAllowance allowance = LeaveAllowanceJpaToDomainMapper.toDomain(jpa);
        // Domain validates: newEntitlement >= daysUsed (can't set entitlement below used days)
        allowance.amendEntitlement(command.newEntitlement());

        // Map back to JPA and save
        LeaveAllowanceDomainToJpaMapper.updateJpa(allowance, jpa);
        leaveAllowanceRepository.save(jpa);
        // Log the amendment for audit/debugging
        log.info("Entitlement amended to {} for allowance {}", command.newEntitlement(), command.leaveAllowanceId());
    }

    // ─────────────────────────────────────────────────────────────────
    // REMOTE EVENT-DRIVEN OPERATIONS (called by RabbitMQ listeners)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Creates a new LeaveAllowance when a {@code StaffMemberAddedEvent} is received via RabbitMQ
     * (Lecture 7 — Cross-Context Event-Driven Integration).
     *
     * <p><strong>Triggered by:</strong> RabbitMQ listener for {@code StaffMemberAddedEvent},
     * which fires when a staff member's status is changed to ACTIVE in Staff Management.
     *
     * <p><strong>Idempotent:</strong> Checks if an allowance already exists for this staff
     * member in the current business year before creating. This guards against duplicate
     * event delivery (at-least-once semantics in RabbitMQ) — if the allowance already
     * exists, the method logs a warning and returns without creating a duplicate.
     *
     * <p><strong>Default entitlement:</strong> The entitlement is passed from the Staff
     * Management event (typically the organisation's default, e.g., 25 days).
     *
     * @param staffMemberId     the UUID of the newly activated staff member
     * @param managerId         the UUID of the staff member's line manager
     * @param firstName         the staff member's first name (denormalised onto the allowance)
     * @param surname           the staff member's surname (denormalised onto the allowance)
     * @param department        the staff member's department (denormalised onto the allowance)
     * @param defaultEntitlement the default annual leave entitlement in days
     * @see LeaveAllowance#createNew for the domain factory method
     */
    @Transactional // Ensures idempotency check + save happen atomically
    public void createAllowanceForNewStaff(String staffMemberId, String managerId,
                                            String firstName, String surname,
                                            String department, int defaultEntitlement) {
        // Get the current calendar year for the business year check
        int currentYear = LocalDate.now().getYear();

        // Idempotency guard — don't create a duplicate allowance if one already exists for this year
        if (leaveAllowanceRepository.existsByStaffMemberIdAndBusinessYearStart(staffMemberId, currentYear)) {
            // Log a warning (this can happen with at-least-once message delivery)
            log.warn("Allowance already exists for staff {} in year {}. Skipping.", staffMemberId, currentYear);
            return; // Exit early — no duplicate created
        }

        // Generate a new UUID for the allowance record
        Identity<LeaveAllowance> newId = Identity.generateId();
        // Create the domain aggregate via its factory method — sets up the business year and initial balance
        LeaveAllowance allowance = LeaveAllowance.createNew(
                newId, staffMemberId, managerId, firstName, surname, department, defaultEntitlement
        );

        // Map domain aggregate to JPA entity and persist
        leaveAllowanceRepository.save(LeaveAllowanceDomainToJpaMapper.toJpa(allowance));
        // Log the creation for audit/debugging
        log.info("Created leave allowance {} for new staff member {}", newId.id(), staffMemberId);
    }

    /**
     * Updates staff details on the LeaveAllowance when a {@code StaffMemberUpdatedEvent}
     * is received via RabbitMQ (Lecture 7 — Cross-Context Event-Driven Integration).
     *
     * <p><strong>Triggered by:</strong> RabbitMQ listener for {@code StaffMemberUpdatedEvent},
     * which fires when a staff member's department or manager is changed in Staff Management.
     *
     * <p><strong>Denormalised data sync:</strong> The LeaveAllowance stores a snapshot of
     * the staff member's managerId and department to avoid cross-context queries. When these
     * fields change in Staff Management, this method syncs the snapshot on the allowance.
     *
     * @param staffMemberId the UUID of the staff member whose details changed
     * @param managerId     the new manager UUID (or the same if unchanged)
     * @param department    the new department name (or the same if unchanged)
     * @throws LeaveAllowanceNotFoundException if no allowance exists for the staff member
     * @see LeaveAllowance#updateStaffDetails(String, String) for the domain method
     */
    @Transactional // Ensures load + update + save happen atomically
    public void updateStaffDetails(String staffMemberId, String managerId, String department) {
        // Load the current business year's allowance for this staff member
        LeaveAllowanceJpa jpa = findCurrentAllowance(staffMemberId);
        // Map to domain aggregate
        LeaveAllowance allowance = LeaveAllowanceJpaToDomainMapper.toDomain(jpa);

        // Update the denormalised staff details on the domain aggregate
        allowance.updateStaffDetails(managerId, department);

        // Map back to JPA and save the updated snapshot
        LeaveAllowanceDomainToJpaMapper.updateJpa(allowance, jpa);
        leaveAllowanceRepository.save(jpa);
        // Log the update for audit/debugging
        log.info("Updated staff details on allowance for staff member {}", staffMemberId);
    }

    // ─────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Finds the current (most recent) business year's leave allowance for a staff member.
     *
     * <p>Uses the repository's {@code findFirstByStaffMemberIdOrderByBusinessYearStartDesc}
     * method, which returns the most recent allowance by business year start (descending).
     * This ensures we always operate on the current year's allowance.
     *
     * @param staffMemberId the UUID of the staff member
     * @return the JPA entity for the staff member's current allowance
     * @throws LeaveAllowanceNotFoundException if no allowance exists for the staff member
     */
    private LeaveAllowanceJpa findCurrentAllowance(String staffMemberId) {
        // Find the most recent allowance (ordered by business year descending) or throw not-found
        return leaveAllowanceRepository
                .findFirstByStaffMemberIdOrderByBusinessYearStartDesc(staffMemberId)
                .orElseThrow(() -> new LeaveAllowanceNotFoundException(staffMemberId));
    }
}
