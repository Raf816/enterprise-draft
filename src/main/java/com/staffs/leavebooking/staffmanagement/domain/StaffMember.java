package com.staffs.leavebooking.staffmanagement.domain;

import com.staffs.leavebooking.common.domain.AggregateRoot;
import com.staffs.leavebooking.common.domain.Email;
import com.staffs.leavebooking.common.domain.FullName;
import com.staffs.leavebooking.common.domain.Identity;
import com.staffs.leavebooking.common.events.StaffMemberAddedEvent;
import com.staffs.leavebooking.common.events.StaffMemberUpdatedEvent;

import java.time.LocalDate;

import static com.staffs.leavebooking.common.domain.DomainAssertions.argumentNotEmpty;
import static com.staffs.leavebooking.common.domain.DomainAssertions.argumentNotNull;

/**
 * Aggregate Root for the Staff Management supporting context
 * (Lecture 3 — Aggregates, Lecture 7 — Domain Events, Lecture 8 — Remote Events).
 *
 * <p><strong>DDD Role:</strong> StaffMember is the aggregate root for all staff-related
 * data. All external access goes through this class. It enforces invariants
 * (hire date not in future, TERMINATED is terminal, entitlement must be positive)
 * and raises domain events when state changes need to be communicated to other contexts.
 *
 * <p><strong>Brief context:</strong> "Staff management... is a facade to a bigger
 * HR information system." This aggregate represents the minimal staff data that
 * the leave booking system needs.
 *
 * <p><strong>Three factory methods (Lecture 7 — split creation):</strong>
 * <ul>
 *   <li>{@link #createNew} — admin creates fully-specified staff via POST /staff</li>
 *   <li>{@link #createSkeleton} — self-registration creates minimal record via POST /auth/register</li>
 *   <li>{@link #reconstitute} — loads existing data from database (no events, no validation)</li>
 * </ul>
 *
 * <p><strong>Three command methods (Lecture 6 — Commands):</strong>
 * <ul>
 *   <li>{@link #updateDepartment} — changes dept/manager, raises {@link StaffMemberUpdatedEvent}</li>
 *   <li>{@link #updatePlacement} — changes role/level/type, no events</li>
 *   <li>{@link #updateStatus} — changes lifecycle status, enforces TERMINATED terminal invariant,
 *       raises {@link StaffMemberAddedEvent} on PENDING_SETUP → ACTIVE activation</li>
 * </ul>
 *
 * <p><strong>Event timing design decision:</strong> The StaffMemberAddedEvent is NOT raised
 * on creation (createNew/createSkeleton). It fires when the admin ACTIVATES the staff member
 * (PENDING_SETUP → ACTIVE). This ensures the LeaveAllowance is created with the correct
 * department, manager, and entitlement — which may be set after initial creation.
 *
 * @see com.staffs.leavebooking.staffmanagement.application.handlers.StaffApplicationService for the command handler
 * @see StaffMemberAddedEvent for the activation event consumed by Leave Management
 * @see StaffMemberUpdatedEvent for the department change event consumed by Leave Management
 */
public class StaffMember extends AggregateRoot<StaffMember> {

    // ─── Validation error message constants (public so tests can assert on them) ───
    public static final String FULL_NAME_REQUIRED = "Full name is required";
    public static final String EMAIL_REQUIRED = "Email is required";
    public static final String DEPARTMENT_REQUIRED = "Department is required";
    public static final String HIRE_DATE_REQUIRED = "Hire date is required";
    public static final String HIRE_DATE_IN_FUTURE = "Hire date cannot be in the future";
    public static final String CURRENT_ROLE_REQUIRED = "Current role is required";
    public static final String ROLE_START_DATE_REQUIRED = "Role start date is required";
    public static final String EMPLOYMENT_TYPE_REQUIRED = "Employment type is required";
    public static final String EMPLOYMENT_STATUS_REQUIRED = "Employment status is required";
    public static final String CANNOT_REACTIVATE_TERMINATED = "A terminated staff member cannot be reactivated";
    public static final String ENTITLEMENT_MUST_BE_POSITIVE = "Default leave entitlement must be a positive number";

    /** Default annual leave entitlement in days (used when not specified) */
    public static final int DEFAULT_LEAVE_ENTITLEMENT = 25;

    // ─── Private state fields (encapsulated — only accessible via accessors) ───
    private FullName fullName;              // Value object: first name + surname
    private Email email;                    // Value object: validated email address
    private String department;              // Department name (e.g., "Networks")
    private String lineManagerId;           // UUID of the line manager (nullable)
    private LocalDate hireDate;             // When the staff member was hired
    private String currentRole;             // Job title (e.g., "Software Engineer")
    private LocalDate startDateOfCurrentRole; // When the current role started
    private String jobLevel;                // Seniority level (e.g., "JUNIOR") (nullable)
    private EmploymentType employmentType;  // Contract type (FULL_TIME, PART_TIME, CONTRACT)
    private EmploymentStatus employmentStatus; // Lifecycle state (PENDING_SETUP, ACTIVE, etc.)
    private int defaultLeaveEntitlement;    // Annual leave days (carried in StaffMemberAddedEvent)

    /**
     * Private constructor — all creation goes through factory methods.
     * Validates all required fields using DomainAssertions guard clauses.
     * This ensures a StaffMember object can never exist in an invalid state.
     */
    private StaffMember(Identity<StaffMember> id, FullName fullName, Email email,
                        String department, String lineManagerId, LocalDate hireDate,
                        String currentRole, LocalDate startDateOfCurrentRole,
                        String jobLevel, EmploymentType employmentType,
                        EmploymentStatus employmentStatus, int defaultLeaveEntitlement) {
        super(id); // Entity base class validates id is not null

        // Validate all required fields (guard clauses from DomainAssertions)
        argumentNotNull(fullName, FULL_NAME_REQUIRED);
        argumentNotNull(email, EMAIL_REQUIRED);
        argumentNotEmpty(department, DEPARTMENT_REQUIRED);
        argumentNotNull(hireDate, HIRE_DATE_REQUIRED);
        argumentNotEmpty(currentRole, CURRENT_ROLE_REQUIRED);
        argumentNotNull(startDateOfCurrentRole, ROLE_START_DATE_REQUIRED);
        argumentNotNull(employmentType, EMPLOYMENT_TYPE_REQUIRED);
        argumentNotNull(employmentStatus, EMPLOYMENT_STATUS_REQUIRED);

        // Set all fields
        this.fullName = fullName;
        this.email = email;
        this.department = department;
        this.lineManagerId = lineManagerId;
        this.hireDate = hireDate;
        this.currentRole = currentRole;
        this.startDateOfCurrentRole = startDateOfCurrentRole;
        this.jobLevel = jobLevel;
        this.employmentType = employmentType;
        this.employmentStatus = employmentStatus;
        this.defaultLeaveEntitlement = defaultLeaveEntitlement;
    }

    // ─────────────────────────────────────────────────────────────────
    // FACTORY METHODS (Lecture 7 — split creation: write-path vs read-path)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Factory method: creates a new fully-specified staff member (admin path via POST /staff).
     *
     * <p><strong>Status:</strong> Always starts as PENDING_SETUP. The admin must activate
     * via PATCH /staff/{id} with {@code {"employmentStatus": "ACTIVE"}}.
     *
     * <p><strong>No event raised here:</strong> The StaffMemberAddedEvent fires when
     * the admin activates the staff member (PENDING_SETUP → ACTIVE). This ensures the
     * LeaveAllowance is created with the correct department and manager.
     *
     * <p><strong>Additional validation:</strong> Hire date cannot be in the future,
     * and leave entitlement must be positive.
     *
     * @return a new StaffMember in PENDING_SETUP status
     * @throws IllegalArgumentException if hireDate is in the future or entitlement is not positive
     */
    public static StaffMember createNew(Identity<StaffMember> id, FullName fullName, Email email,
                                         String department, String lineManagerId, LocalDate hireDate,
                                         String currentRole, LocalDate startDateOfCurrentRole,
                                         String jobLevel, EmploymentType employmentType,
                                         int defaultLeaveEntitlement) {
        // Business rule: hire date cannot be in the future
        if (hireDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(HIRE_DATE_IN_FUTURE);
        }
        // Business rule: leave entitlement must be a positive number
        if (defaultLeaveEntitlement <= 0) {
            throw new IllegalArgumentException(ENTITLEMENT_MUST_BE_POSITIVE);
        }

        // Create with PENDING_SETUP status — admin activates later
        return new StaffMember(id, fullName, email, department, lineManagerId,
                hireDate, currentRole, startDateOfCurrentRole, jobLevel,
                employmentType, EmploymentStatus.PENDING_SETUP, defaultLeaveEntitlement);
    }

    /**
     * Factory method: creates a skeleton staff member from self-registration (POST /auth/register).
     *
     * <p><strong>Minimal data:</strong> Only name and email are known at self-registration time.
     * All other fields get sensible defaults that the admin fills in later:
     * <ul>
     *   <li>department = "Unassigned"</li>
     *   <li>lineManagerId = null</li>
     *   <li>hireDate = today (registration date)</li>
     *   <li>currentRole = "Pending Setup"</li>
     *   <li>employmentType = FULL_TIME (default)</li>
     *   <li>defaultLeaveEntitlement = 25 days</li>
     * </ul>
     *
     * <p><strong>Status:</strong> PENDING_SETUP — user can authenticate but cannot
     * submit leave requests until activated by admin.
     *
     * @param id       the Firebase UID wrapped as an Identity
     * @param fullName the user's name from the registration form
     * @param email    the user's email from the registration form
     * @return a new skeleton StaffMember in PENDING_SETUP status
     */
    public static StaffMember createSkeleton(Identity<StaffMember> id, FullName fullName, Email email) {
        return new StaffMember(id, fullName, email,
                "Unassigned",                    // department — admin fills in later
                null,                            // lineManagerId — admin assigns later
                LocalDate.now(),                 // hireDate — defaults to registration date
                "Pending Setup",                 // currentRole — admin fills in later
                LocalDate.now(),                 // startDateOfCurrentRole — today
                null,                            // jobLevel — admin fills in later
                EmploymentType.FULL_TIME,        // default employment type
                EmploymentStatus.PENDING_SETUP,  // initial status
                DEFAULT_LEAVE_ENTITLEMENT);      // 25 days default
    }

    /**
     * Factory method: reconstitutes an existing staff member from persistence (read path).
     *
     * <p><strong>No events raised, no creation-time validation.</strong>
     * This is the read-path factory (Lecture 7 split pattern). It's used when loading
     * an existing aggregate from the database to execute a command. Since the data
     * was already validated when it was first created, we don't re-validate.
     *
     * @return a reconstituted StaffMember aggregate from existing data
     */
    public static StaffMember reconstitute(Identity<StaffMember> id, FullName fullName, Email email,
                                            String department, String lineManagerId, LocalDate hireDate,
                                            String currentRole, LocalDate startDateOfCurrentRole,
                                            String jobLevel, EmploymentType employmentType,
                                            EmploymentStatus employmentStatus) {
        return new StaffMember(id, fullName, email, department, lineManagerId,
                hireDate, currentRole, startDateOfCurrentRole, jobLevel,
                employmentType, employmentStatus, DEFAULT_LEAVE_ENTITLEMENT);
    }

    // ─────────────────────────────────────────────────────────────────
    // COMMAND METHODS (Lecture 6 — Commands, Lecture 7 — Domain Events)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Updates department and line manager. Raises {@link StaffMemberUpdatedEvent}.
     *
     * <p><strong>Event:</strong> StaffMemberUpdatedEvent is published to RabbitMQ
     * and consumed by Leave Management to sync the denormalised department/manager
     * fields on the staff member's LeaveAllowance.
     *
     * @param newDepartment    the new department name (required — cannot be empty)
     * @param newLineManagerId the new line manager's UUID (nullable)
     */
    public void updateDepartment(String newDepartment, String newLineManagerId) {
        argumentNotEmpty(newDepartment, DEPARTMENT_REQUIRED); // Validate department not empty
        this.department = newDepartment;
        this.lineManagerId = newLineManagerId;

        // Raise remote event → consumed by Leave Management via RabbitMQ
        addDomainEvent(new StaffMemberUpdatedEvent(
                LocalDate.now(), this.id.id(), newLineManagerId, newDepartment
        ));
    }

    /**
     * Updates placement details (role, job level, employment type).
     *
     * <p><strong>No event raised:</strong> Placement changes don't affect the
     * Leave Management context (it doesn't need job title or employment type data).
     * Only department/manager changes trigger cross-context events.
     *
     * @param newRole      the new job title (required — cannot be empty)
     * @param newStartDate when the new role started (required)
     * @param newJobLevel  the new seniority level (nullable)
     * @param newType      the new contract type (required)
     */
    public void updatePlacement(String newRole, LocalDate newStartDate,
                                String newJobLevel, EmploymentType newType) {
        argumentNotEmpty(newRole, CURRENT_ROLE_REQUIRED);
        argumentNotNull(newStartDate, ROLE_START_DATE_REQUIRED);
        argumentNotNull(newType, EMPLOYMENT_TYPE_REQUIRED);
        this.currentRole = newRole;
        this.startDateOfCurrentRole = newStartDate;
        this.jobLevel = newJobLevel;
        this.employmentType = newType;
    }

    /**
     * Updates employment status. Enforces the state machine invariants.
     *
     * <p><strong>Invariant 1 — Terminal state:</strong> TERMINATED cannot transition
     * to any other state. Attempting to reactivate a terminated staff member throws
     * {@link IllegalStateException}.
     *
     * <p><strong>Event on activation:</strong> When transitioning from PENDING_SETUP
     * to ACTIVE, raises {@link StaffMemberAddedEvent}. This is consumed by Leave
     * Management via RabbitMQ to create the staff member's {@code LeaveAllowance}
     * with the correct department, manager, and entitlement.
     *
     * @param newStatus the new employment status
     * @throws IllegalStateException    if attempting to transition out of TERMINATED
     * @throws IllegalArgumentException if newStatus is null
     */
    public void updateStatus(EmploymentStatus newStatus) {
        argumentNotNull(newStatus, EMPLOYMENT_STATUS_REQUIRED);

        // Invariant: TERMINATED is a terminal state — cannot transition to anything else
        if (this.employmentStatus == EmploymentStatus.TERMINATED && newStatus != EmploymentStatus.TERMINATED) {
            throw new IllegalStateException(CANNOT_REACTIVATE_TERMINATED);
        }

        // Check if this is an activation (PENDING_SETUP → ACTIVE)
        boolean isActivation = (this.employmentStatus == EmploymentStatus.PENDING_SETUP
                && newStatus == EmploymentStatus.ACTIVE);

        // Update the status
        this.employmentStatus = newStatus;

        // If this is an activation, raise StaffMemberAddedEvent
        // This triggers LeaveAllowance creation in the Leave Management context
        if (isActivation) {
            addDomainEvent(new StaffMemberAddedEvent(
                    LocalDate.now(),                  // when the event occurred
                    this.id.id(),                     // staff member UUID (Firebase UID)
                    this.fullName.firstName(),        // for LeaveAllowance.staffName
                    this.fullName.surname(),          // for LeaveAllowance.staffName
                    this.email.address(),             // for audit/logging
                    this.lineManagerId,               // for LeaveAllowance.managerId
                    this.department,                  // for LeaveAllowance.department
                    this.defaultLeaveEntitlement      // for LeaveAllowance.totalEntitlement
            ));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // ACCESSORS (read-only — expose state for mappers and DTOs)
    // ─────────────────────────────────────────────────────────────────

    public FullName fullName() { return fullName; }
    public Email email() { return email; }
    public String department() { return department; }
    public String lineManagerId() { return lineManagerId; }
    public LocalDate hireDate() { return hireDate; }
    public String currentRole() { return currentRole; }
    public LocalDate startDateOfCurrentRole() { return startDateOfCurrentRole; }
    public String jobLevel() { return jobLevel; }
    public EmploymentType employmentType() { return employmentType; }
    public EmploymentStatus employmentStatus() { return employmentStatus; }
    public int defaultLeaveEntitlement() { return defaultLeaveEntitlement; }
}
