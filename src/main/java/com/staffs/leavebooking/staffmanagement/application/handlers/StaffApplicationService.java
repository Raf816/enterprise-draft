package com.staffs.leavebooking.staffmanagement.application.handlers;

import com.staffs.leavebooking.common.domain.Email;
import com.staffs.leavebooking.common.domain.FullName;
import com.staffs.leavebooking.common.domain.Identity;
import com.staffs.leavebooking.common.events.DomainEventManager;
import com.staffs.leavebooking.staffmanagement.application.commands.AddStaffMemberCommand;
import com.staffs.leavebooking.staffmanagement.application.commands.UpdateDepartmentCommand;
import com.staffs.leavebooking.staffmanagement.application.commands.UpdatePlacementCommand;
import com.staffs.leavebooking.staffmanagement.application.commands.UpdateStatusCommand;
import com.staffs.leavebooking.staffmanagement.application.mappers.StaffMemberDomainToJpaMapper;
import com.staffs.leavebooking.staffmanagement.application.mappers.StaffMemberJpaToDomainMapper;
import com.staffs.leavebooking.staffmanagement.domain.EmploymentStatus;
import com.staffs.leavebooking.staffmanagement.domain.EmploymentType;
import com.staffs.leavebooking.staffmanagement.domain.StaffMember;
import com.staffs.leavebooking.staffmanagement.infrastructure.repositories.StaffMemberRepository;
import com.staffs.leavebooking.staffmanagement.ui.exceptions.StaffMemberNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * CQRS Command Handler (Application Service) for the Staff Management bounded context
 * (Lecture 6 — CQRS Commands, Lecture 7 — Domain Events, Lecture 8 — Remote Events).
 *
 * <p><strong>CQRS Write Side:</strong> This class handles ALL write operations for staff
 * management. The read side is handled by {@link StaffQueryHandler}. This separation
 * means write operations go through the full domain model (aggregate → events → persistence)
 * while reads bypass the domain entirely (JPA → DTO directly).
 *
 * <p><strong>Command processing pattern (every write method follows this):</strong>
 * <pre>
 * 1. Load the domain aggregate from the database (JPA → Mapper → Domain)
 * 2. Execute the command on the aggregate (validates invariants, raises events)
 * 3. Save the updated aggregate back to the database (Domain → Mapper → JPA)
 * 4. Dispatch any domain events via DomainEventManager (Store-and-Forward)
 * 5. Clear the aggregate's event list (prevent double-dispatch)
 * </pre>
 *
 * <p><strong>@Transactional:</strong> Every command method is wrapped in a database
 * transaction. This ensures that the aggregate state change AND the event store
 * write happen atomically — if either fails, both roll back.
 *
 * <p><strong>Six command methods:</strong>
 * <ul>
 *   <li>{@link #addNewStaffMember} — creates with auto-generated ID</li>
 *   <li>{@link #addNewStaffMemberWithId} — creates with specific ID (Firebase UID)</li>
 *   <li>{@link #createSkeletonStaffMember} — self-registration minimal record</li>
 *   <li>{@link #updateDepartment} — changes dept/manager, raises StaffMemberUpdatedEvent</li>
 *   <li>{@link #updatePlacement} — changes role/level/type, no events</li>
 *   <li>{@link #updateStatus} — changes lifecycle status, may raise StaffMemberAddedEvent</li>
 * </ul>
 *
 * @see StaffQueryHandler for the CQRS read side
 * @see StaffMember for the domain aggregate with business rules
 * @see DomainEventManager for event persistence and dispatch
 */
@Service            // Spring-managed singleton — injected into StaffManagementFacade
@Slf4j              // Lombok: generates a private static final Logger (SLF4J)
@AllArgsConstructor // Lombok: constructor injection for all final fields
public class StaffApplicationService {

    /** Repository for persisting StaffMemberJpa entities to the database */
    private final StaffMemberRepository staffMemberRepository;

    /**
     * Centrally manages domain event dispatch (Store-and-Forward pattern).
     * Events are persisted to the event_store table and published via Spring's
     * ApplicationEventPublisher within the same transaction.
     */
    private final DomainEventManager domainEventManager;

    // ─────────────────────────────────────────────────────────────────
    // CREATE COMMANDS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Creates a new fully-specified staff member with an auto-generated UUID.
     * Delegates to {@link #addNewStaffMemberWithId} with a new random UUID.
     *
     * <p><strong>Used when:</strong> The ID doesn't need to match an external system
     * (rare — most staff are created via the Firebase coordination path).
     *
     * @param command the creation command with all staff details
     * @return the generated UUID string for the new staff member
     */
    @Transactional // Wraps in a database transaction
    public String addNewStaffMember(AddStaffMemberCommand command) {
        // Generate a new random UUID and delegate to the ID-specific method
        return addNewStaffMemberWithId(Identity.generateId().id(), command);
    }

    /**
     * Creates a new fully-specified staff member using a specific ID (Firebase UID).
     * Called by StaffController after Firebase user creation to ensure ID consistency:
     * Firebase UID = staff record ID = leave allowance staffMemberId.
     *
     * <p><strong>Flow:</strong>
     * <ol>
     *   <li>Check for duplicate email (prevent duplicates before hitting the domain)</li>
     *   <li>Determine leave entitlement (use command value or default to 25 days)</li>
     *   <li>Create domain aggregate via {@code StaffMember.createNew()} (status: PENDING_SETUP)</li>
     *   <li>Map domain → JPA and save to database</li>
     * </ol>
     *
     * <p><strong>No events raised:</strong> The StaffMemberAddedEvent fires on ACTIVATION
     * (PENDING_SETUP → ACTIVE), not on creation. This ensures the LeaveAllowance gets
     * the correct department and manager (which may be updated after creation).
     *
     * @param staffId the specific ID to use (typically the Firebase UID)
     * @param command the creation command with all staff details
     * @return the staff member ID that was used
     * @throws DataIntegrityViolationException if a staff member with the same email already exists
     */
    @Transactional
    public String addNewStaffMemberWithId(String staffId, AddStaffMemberCommand command) {
        // Guard: check for duplicate email before creating the domain aggregate
        // This prevents a database unique constraint violation later
        if (staffMemberRepository.existsByEmail(command.email())) {
            throw new DataIntegrityViolationException(
                    "A staff member with email " + command.email() + " already exists.");
        }

        // Wrap the string ID as a typed Identity<StaffMember>
        Identity<StaffMember> id = Identity.of(staffId);

        // All staff start with the default 25-day entitlement.
        // Admin can amend via PATCH /leave-allowances/{id} after activation.

        // Create the domain aggregate via the write-path factory method
        // This validates all business rules (hire date, required fields)
        StaffMember staffMember = StaffMember.createNew(
                id,
                new FullName(command.firstName(), command.surname()),  // Value object creation
                new Email(command.email()),                            // Value object creation
                command.department(),
                command.lineManagerId(),
                command.hireDate(),
                command.currentRole(),
                command.startDateOfCurrentRole(),
                command.jobLevel(),
                parseEmploymentType(command.employmentType())          // String → enum conversion
        );

        // Map domain aggregate → JPA entity and save to database
        staffMemberRepository.save(StaffMemberDomainToJpaMapper.toJpa(staffMember));
        log.info("Staff member {} created with id {} (status: PENDING_SETUP)", command.email(), staffId);
        return staffId;
    }

    /**
     * Creates a skeleton staff member from self-registration (POST /auth/register).
     * Uses the Firebase UID as the staff record ID for cross-context consistency.
     *
     * <p><strong>Skeleton means:</strong> Only name and email are known. Department,
     * manager, role, etc. are set to defaults. The admin fills these in later
     * and activates the staff member.
     *
     * <p><strong>Idempotent:</strong> If a staff record already exists for this email
     * (e.g., admin created it via POST /staff before the user self-registered),
     * this method logs a warning and returns without creating a duplicate.
     *
     * @param firebaseUid the Firebase UID to use as the staff record ID
     * @param firstName   the user's first name from the registration form
     * @param surname     the user's surname from the registration form
     * @param email       the user's email from the registration form
     * @return the Firebase UID (same as input)
     */
    @Transactional
    public String createSkeletonStaffMember(String firebaseUid, String firstName, String surname, String email) {
        // Idempotent check: skip if a record already exists for this email
        if (staffMemberRepository.existsByEmail(email)) {
            log.warn("Staff record already exists for email {}. Skipping skeleton creation.", email);
            return firebaseUid;
        }

        // Wrap the Firebase UID as a typed Identity
        Identity<StaffMember> id = Identity.of(firebaseUid);

        // Create a skeleton aggregate with minimal data and sensible defaults
        StaffMember staffMember = StaffMember.createSkeleton(
                id,
                new FullName(firstName, surname),
                new Email(email)
        );

        // Map domain → JPA and save
        staffMemberRepository.save(StaffMemberDomainToJpaMapper.toJpa(staffMember));
        log.info("Skeleton staff record created for {} (uid: {}, status: PENDING_SETUP)", email, firebaseUid);
        return firebaseUid;
    }

    // ─────────────────────────────────────────────────────────────────
    // UPDATE COMMANDS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Updates a staff member's department and/or line manager.
     * Raises {@code StaffMemberUpdatedEvent} which syncs denormalised data
     * on the LeaveAllowance in the Leave Management context.
     *
     * <p><strong>Partial update support:</strong> If department or lineManagerId is null
     * in the command, the current value is retained (null = "no change").
     *
     * @param command contains staffMemberId, department (nullable), lineManagerId (nullable)
     * @throws StaffMemberNotFoundException if the staff member doesn't exist
     */
    @Transactional
    public void updateDepartment(UpdateDepartmentCommand command) {
        // Step 1: Load the domain aggregate from the database
        StaffMember staffMember = loadDomainAggregate(command.staffMemberId());

        // Step 2: Resolve effective values (null in command = keep current value)
        String effectiveDepartment = command.department() != null ? command.department() : staffMember.department();
        String effectiveManager = command.lineManagerId() != null ? command.lineManagerId() : staffMember.lineManagerId();

        // Step 3: Execute the domain command (validates, updates state, raises event)
        staffMember.updateDepartment(effectiveDepartment, effectiveManager);

        // Step 4: Save the updated aggregate back to the database
        staffMemberRepository.save(StaffMemberDomainToJpaMapper.toJpa(staffMember));

        // Step 5: Dispatch domain events (StaffMemberUpdatedEvent → RabbitMQ → Leave Management)
        dispatchAndClear(staffMember);

        log.info("Staff member {} department updated to {}", command.staffMemberId(), effectiveDepartment);
    }

    /**
     * Updates a staff member's placement details (role, job level, employment type).
     * No events are raised because the Leave Management context doesn't need this data.
     *
     * <p><strong>Partial update support:</strong> Null fields in the command retain
     * the current values.
     *
     * @param command contains staffMemberId and optional role/level/type fields
     * @throws StaffMemberNotFoundException if the staff member doesn't exist
     */
    @Transactional
    public void updatePlacement(UpdatePlacementCommand command) {
        // Step 1: Load the domain aggregate
        StaffMember staffMember = loadDomainAggregate(command.staffMemberId());

        // Step 2: Resolve effective values (null = keep current)
        String effectiveRole = command.currentRole() != null ? command.currentRole() : staffMember.currentRole();
        LocalDate effectiveStartDate = command.startDateOfCurrentRole() != null
                ? command.startDateOfCurrentRole() : staffMember.startDateOfCurrentRole();
        String effectiveJobLevel = command.jobLevel() != null ? command.jobLevel() : staffMember.jobLevel();
        String effectiveType = command.employmentType() != null ? command.employmentType() : staffMember.employmentType().name();

        // Step 3: Execute the domain command (validates required fields)
        staffMember.updatePlacement(
                effectiveRole,
                effectiveStartDate,
                effectiveJobLevel,
                EmploymentType.valueOf(effectiveType)  // String → enum
        );

        // Step 4: Save back to database (no events to dispatch for placement changes)
        staffMemberRepository.save(StaffMemberDomainToJpaMapper.toJpa(staffMember));
        log.info("Staff member {} placement updated", command.staffMemberId());
    }

    /**
     * Updates a staff member's employment status.
     *
     * <p><strong>Key behaviours:</strong>
     * <ul>
     *   <li>TERMINATED → anything: throws IllegalStateException (terminal state invariant)</li>
     *   <li>PENDING_SETUP → ACTIVE: raises StaffMemberAddedEvent which creates
     *       the LeaveAllowance in Leave Management with correct dept/manager/entitlement</li>
     *   <li>All other transitions: updates status without events</li>
     * </ul>
     *
     * @param command contains staffMemberId and the new employmentStatus string
     * @throws StaffMemberNotFoundException if the staff member doesn't exist
     * @throws IllegalStateException        if trying to reactivate a TERMINATED staff member
     */
    @Transactional
    public void updateStatus(UpdateStatusCommand command) {
        // Step 1: Load the domain aggregate
        StaffMember staffMember = loadDomainAggregate(command.staffMemberId());

        // Step 2: Execute the domain command (enforces terminal state invariant,
        // may raise StaffMemberAddedEvent on activation)
        staffMember.updateStatus(EmploymentStatus.valueOf(command.employmentStatus()));

        // Step 3: Save the updated aggregate
        staffMemberRepository.save(StaffMemberDomainToJpaMapper.toJpa(staffMember));

        // Step 4: Dispatch events (StaffMemberAddedEvent on activation → RabbitMQ → LeaveAllowance creation)
        dispatchAndClear(staffMember);

        log.info("Staff member {} status updated to {}", command.staffMemberId(), command.employmentStatus());
    }

    // ─────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Dispatches any pending domain events from an aggregate, then clears the list.
     * Called after every command that may raise events (department update, status update).
     *
     * <p><strong>Pattern:</strong>
     * 1. Check if the aggregate has any pending events
     * 2. If yes, pass them to DomainEventManager (persists + publishes)
     * 3. Clear the event list to prevent double-dispatch
     *
     * @param aggregate the aggregate that may have raised events
     */
    private void dispatchAndClear(StaffMember aggregate) {
        if (aggregate.domainEventsExist()) {
            // Pass events to DomainEventManager which persists them to event_store
            // and publishes them via Spring's ApplicationEventPublisher
            domainEventManager.manageDomainEvents(
                    this.getClass().getSimpleName(),     // Source context for audit logging
                    aggregate.listOfDomainEvents()       // The list of pending events
            );
            // Clear the event list so they're not dispatched again if the aggregate is reused
            aggregate.clearDomainEvents();
        }
    }

    /**
     * Loads a StaffMember domain aggregate from the database by ID.
     * Used by all update commands to get the current state before applying changes.
     *
     * <p><strong>Mapper chain:</strong> Repository returns StaffMemberJpa →
     * StaffMemberJpaToDomainMapper.toDomain() reconstitutes the domain aggregate.
     *
     * @param staffMemberId the UUID of the staff member to load
     * @return the reconstituted StaffMember domain aggregate
     * @throws StaffMemberNotFoundException if no staff member exists with this ID
     */
    private StaffMember loadDomainAggregate(String staffMemberId) {
        return staffMemberRepository.findById(staffMemberId)
                .map(StaffMemberJpaToDomainMapper::toDomain)  // JPA entity → domain aggregate
                .orElseThrow(() -> new StaffMemberNotFoundException(staffMemberId));
    }

    /**
     * Parses an employment type string into the EmploymentType enum.
     * Provides a clean error message listing valid values if the string is invalid.
     *
     * @param type the employment type string (e.g., "FULL_TIME")
     * @return the matching EmploymentType enum value
     * @throws IllegalArgumentException if the string doesn't match any valid type
     */
    private EmploymentType parseEmploymentType(String type) {
        try {
            return EmploymentType.valueOf(type);  // String → enum (case-sensitive)
        } catch (IllegalArgumentException e) {
            // Provide a helpful error message listing valid values
            throw new IllegalArgumentException(
                    "Invalid employment type: '" + type + "'. Valid values are: FULL_TIME, PART_TIME, CONTRACT");
        }
    }
}
