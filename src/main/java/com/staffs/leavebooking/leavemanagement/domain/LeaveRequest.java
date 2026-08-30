package com.staffs.leavebooking.leavemanagement.domain;

import com.staffs.leavebooking.common.domain.AggregateRoot;
import com.staffs.leavebooking.common.domain.Identity;
import com.staffs.leavebooking.leavemanagement.domain.events.LeaveRequestApprovedEvent;
import com.staffs.leavebooking.leavemanagement.domain.events.LeaveRequestCancelledEvent;
import com.staffs.leavebooking.leavemanagement.domain.events.LeaveRequestRejectedEvent;
import com.staffs.leavebooking.leavemanagement.domain.events.LeaveRequestSubmittedEvent;

import java.time.LocalDate;

import static com.staffs.leavebooking.common.domain.DomainAssertions.argumentNotEmpty;
import static com.staffs.leavebooking.common.domain.DomainAssertions.argumentNotNull;

/**
 * Aggregate Root representing a leave request within the Leave Management bounded context
 * (Lecture 3 — Aggregates, Lecture 7 — Domain Events).
 *
 * <p><strong>DDD Concept (Lecture 3):</strong> This is the root of the Leave Request aggregate.
 * All external access to leave request data and behaviour goes through this class.
 * The aggregate enforces all business invariants (state machine rules, required fields)
 * so that a {@code LeaveRequest} can never exist in an invalid state.
 *
 * <p><strong>State Machine (Lecture 4 — Domain Modelling):</strong> A leave request follows
 * a strict state machine with the following valid transitions:
 * <pre>
 *   PENDING  → APPROVED   (via {@link #approve(String, String)})
 *   PENDING  → REJECTED   (via {@link #reject(String, String)})
 *   PENDING  → CANCELLED  (via {@link #cancel(String, String)})
 *   APPROVED → CANCELLED  (via {@link #cancel(String, String)})
 * </pre>
 * Terminal states (REJECTED, CANCELLED) allow no further transitions.
 *
 * <p><strong>Two factory methods — write path vs read path:</strong>
 * <ul>
 *   <li>{@link #submitNew} — the <em>write path</em>: validates future-start dates,
 *       calculates working days from the {@link DateRange}, sets status to PENDING,
 *       and raises a {@link LeaveRequestSubmittedEvent} that triggers
 *       {@code LeaveAllowance.reserveDays()} via a local event listener.</li>
 *   <li>{@link #reconstitute} — the <em>read path</em>: rebuilds the aggregate from
 *       persisted data without any validation or event raising. Used by the repository
 *       when loading from the database.</li>
 * </ul>
 *
 * <p><strong>Domain Events (Lecture 7):</strong> Each command method raises a local event:
 * <ul>
 *   <li>{@link LeaveRequestSubmittedEvent} → reserves pending days on the allowance</li>
 *   <li>{@link LeaveRequestApprovedEvent} → confirms days (pending → used)</li>
 *   <li>{@link LeaveRequestRejectedEvent} → releases pending days back to available</li>
 *   <li>{@link LeaveRequestCancelledEvent} → credits days back or releases pending days</li>
 * </ul>
 *
 * <p><strong>Invariants enforced by this aggregate:</strong>
 * <ul>
 *   <li>staffMemberId and managerId must not be empty</li>
 *   <li>leaveType and dateRange must not be null</li>
 *   <li>State transitions must follow the state machine (e.g., cannot approve a REJECTED request)</li>
 *   <li>decidedBy is required for approve/reject; cancelledBy is required for cancel</li>
 *   <li>At submission time, the date range must start in the future and include at least one working day</li>
 * </ul>
 *
 * @see AggregateRoot for the base class that manages domain event collection
 * @see LeaveAllowance for the aggregate that tracks leave balance (updated by events from this aggregate)
 * @see LeaveRequestStatus for the state machine enum
 * @see DateRange for the value object that calculates working days
 */
public class LeaveRequest extends AggregateRoot<LeaveRequest> {

    // ─────────────────────────────────────────────────────────────────
    // VALIDATION MESSAGE CONSTANTS
    // ─────────────────────────────────────────────────────────────────
    // Public so that tests can assert against the exact error messages
    // without duplicating magic strings.

    /** Error message when the staff member ID is null or blank. */
    public static final String STAFF_MEMBER_ID_REQUIRED = "Staff member ID is required";

    /** Error message when the manager ID is null or blank. */
    public static final String MANAGER_ID_REQUIRED = "Manager ID is required";

    /** Error message when the leave type is null. */
    public static final String LEAVE_TYPE_REQUIRED = "Leave type is required";

    /** Error message when the date range is null. */
    public static final String DATE_RANGE_REQUIRED = "Date range is required";

    /** Error message when attempting to approve a request that is not PENDING. */
    public static final String CANNOT_APPROVE_NON_PENDING = "Only PENDING requests can be approved";

    /** Error message when attempting to reject a request that is not PENDING. */
    public static final String CANNOT_REJECT_NON_PENDING = "Only PENDING requests can be rejected";

    /** Error message when attempting to cancel a request in a terminal state. */
    public static final String CANNOT_CANCEL_TERMINAL = "Cannot cancel a request that is already REJECTED or CANCELLED";

    /** Error message when the approver/rejector ID is null or blank. */
    public static final String DECIDED_BY_REQUIRED = "Decided by (approver/rejector ID) is required";

    /** Error message when the cancelling user ID is null or blank. */
    public static final String CANCELLED_BY_REQUIRED = "Cancelled by (user ID) is required";

    // ─────────────────────────────────────────────────────────────────
    // FIELDS
    // ─────────────────────────────────────────────────────────────────

    /** The ID of the staff member who submitted this leave request. Immutable after creation. */
    private final String staffMemberId;

    /** The ID of the staff member's manager who is responsible for approving/rejecting. Immutable after creation. */
    private final String managerId;

    /** The type of leave being requested (e.g., ANNUAL). Immutable after creation. */
    private final LeaveType leaveType;

    /**
     * The date range for the leave (start date to end date, inclusive).
     * Encapsulates working-day calculation logic. Immutable after creation.
     *
     * @see DateRange#workingDays() for how numberOfDays is derived
     */
    private final DateRange dateRange;

    /**
     * The number of working days in the date range (excludes weekends).
     * Calculated once at submission time and stored so that allowance operations
     * always use the same value. Immutable after creation.
     */
    private final int numberOfDays;

    /** Optional free-text reason provided by the staff member at submission time. */
    private final String reason;

    /**
     * The current status in the state machine. Starts as PENDING and transitions
     * to APPROVED, REJECTED, or CANCELLED via command methods.
     * This is the only field that changes during the aggregate's lifecycle
     * (along with the decision/cancellation metadata below).
     */
    private LeaveRequestStatus status;

    /** The date this leave request was submitted. Set once at creation, never changes. */
    private final LocalDate submittedOn;

    /**
     * The date the manager made an approve/reject decision.
     * Null until {@link #approve} or {@link #reject} is called.
     */
    private LocalDate decidedOn;

    /**
     * The ID of the person who approved or rejected this request.
     * Null until {@link #approve} or {@link #reject} is called.
     */
    private String decidedBy;

    /**
     * Optional reason given by the manager when approving or rejecting.
     * Null until {@link #approve} or {@link #reject} is called.
     */
    private String decisionReason;

    /**
     * Optional reason given when the request is cancelled.
     * Null until {@link #cancel} is called.
     */
    private String cancellationReason;

    // ─────────────────────────────────────────────────────────────────
    // PRIVATE CONSTRUCTOR
    // ─────────────────────────────────────────────────────────────────

    /**
     * Private constructor — all instantiation goes through factory methods
     * ({@link #submitNew} or {@link #reconstitute}).
     *
     * <p>Validates mandatory fields (staffMemberId, managerId, leaveType, dateRange)
     * using {@link com.staffs.leavebooking.common.domain.DomainAssertions} guard methods.
     * Optional fields (reason, decidedOn, decidedBy, decisionReason, cancellationReason)
     * are allowed to be null.
     *
     * @param id                 unique identity for this aggregate root
     * @param staffMemberId      ID of the requesting staff member (must not be empty)
     * @param managerId          ID of the approving manager (must not be empty)
     * @param leaveType          type of leave (must not be null)
     * @param dateRange          start-to-end date range (must not be null)
     * @param numberOfDays       pre-calculated number of working days
     * @param reason             optional reason from the staff member
     * @param status             current state machine status
     * @param submittedOn        date the request was submitted
     * @param decidedOn          date of the approve/reject decision (null if still pending)
     * @param decidedBy          ID of the decider (null if still pending)
     * @param decisionReason     optional reason from the decider (null if still pending)
     * @param cancellationReason optional reason for cancellation (null if not cancelled)
     */
    private LeaveRequest(Identity<LeaveRequest> id, String staffMemberId, String managerId,
                         LeaveType leaveType, DateRange dateRange, int numberOfDays,
                         String reason, LeaveRequestStatus status, LocalDate submittedOn,
                         LocalDate decidedOn, String decidedBy, String decisionReason,
                         String cancellationReason) {
        super(id); // Delegate to AggregateRoot → Entity constructor (validates non-null identity)
        this.staffMemberId = argumentNotEmpty(staffMemberId, STAFF_MEMBER_ID_REQUIRED); // Guard: non-blank, returns trimmed
        this.managerId = argumentNotEmpty(managerId, MANAGER_ID_REQUIRED);               // Guard: non-blank, returns trimmed
        argumentNotNull(leaveType, LEAVE_TYPE_REQUIRED);   // Guard: not null
        argumentNotNull(dateRange, DATE_RANGE_REQUIRED);   // Guard: not null
        this.leaveType = leaveType;
        this.dateRange = dateRange;
        this.numberOfDays = numberOfDays;                  // Pre-calculated working days
        this.reason = reason;                              // Optional — may be null
        this.status = status;                              // Initial status (PENDING for new, any for reconstitute)
        this.submittedOn = submittedOn;                    // Snapshot of creation date
        this.decidedOn = decidedOn;                        // Null until decided
        this.decidedBy = decidedBy;                        // Null until decided
        this.decisionReason = decisionReason;              // Null until decided
        this.cancellationReason = cancellationReason;      // Null until cancelled
    }

    // ─────────────────────────────────────────────────────────────────
    // FACTORY METHODS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Write-path factory: creates a brand-new leave request and raises a domain event.
     *
     * <p><strong>Business rules enforced:</strong>
     * <ul>
     *   <li>The date range must start in the future (delegates to {@link DateRange#validateFutureStart()})</li>
     *   <li>The date range must contain at least one working day (no weekend-only requests)</li>
     * </ul>
     *
     * <p><strong>Side effects:</strong>
     * <ul>
     *   <li>Status is set to {@link LeaveRequestStatus#PENDING}</li>
     *   <li>submittedOn is set to today's date</li>
     *   <li>A {@link LeaveRequestSubmittedEvent} is raised, which triggers
     *       {@code LeaveAllowance.reserveDays()} via the local event listener</li>
     * </ul>
     *
     * @param id             unique identity for the new leave request
     * @param staffMemberId  ID of the requesting staff member
     * @param managerId      ID of the manager who will approve/reject
     * @param leaveType      type of leave (e.g., ANNUAL)
     * @param dateRange      the requested date range (validated for future start)
     * @param reason         optional free-text reason
     * @return a new {@code LeaveRequest} in PENDING status with a submitted event queued
     * @throws IllegalArgumentException if dateRange starts in the past or has zero working days
     */
    public static LeaveRequest submitNew(Identity<LeaveRequest> id, String staffMemberId,
                                          String managerId, LeaveType leaveType,
                                          DateRange dateRange, String reason) {
        // Validate that the leave starts in the future — not allowed to backdate leave requests
        dateRange.validateFutureStart();

        // Calculate working days (excludes weekends) — this value is stored and used for allowance updates
        int workingDays = dateRange.workingDays();

        // Guard: a leave request spanning only weekends would have zero working days
        if (workingDays <= 0) {
            throw new IllegalArgumentException("Leave request must include at least one working day");
        }

        // Create the aggregate with PENDING status and today as the submission date
        // Decision fields (decidedOn, decidedBy, decisionReason, cancellationReason) are null
        LeaveRequest request = new LeaveRequest(
                id, staffMemberId, managerId, leaveType, dateRange, workingDays,
                reason, LeaveRequestStatus.PENDING, LocalDate.now(),
                null, null, null, null
        );

        // Raise the submitted event — the local event listener will call
        // LeaveAllowance.reserveDays(workingDays) to hold these days as "pending"
        request.addDomainEvent(new LeaveRequestSubmittedEvent(
                LocalDate.now(), id.id(), staffMemberId, workingDays
        ));

        return request;
    }

    /**
     * Read-path factory: reconstitutes a {@code LeaveRequest} from persisted data.
     *
     * <p><strong>Key difference from {@link #submitNew}:</strong> No validation of future dates
     * (the request was valid when originally submitted), no working-day recalculation
     * (the stored value is used), and no domain events are raised (this is a read, not a command).
     *
     * <p>Used by the repository's row mapper / result set extractor when loading from the database.
     *
     * @param id                 the persisted identity
     * @param staffMemberId      the requesting staff member's ID
     * @param managerId          the manager's ID
     * @param leaveType          the type of leave
     * @param dateRange          the date range (start and end dates)
     * @param numberOfDays       the stored number of working days
     * @param reason             the optional submission reason
     * @param status             the persisted status (could be any state)
     * @param submittedOn        the original submission date
     * @param decidedOn          the decision date (null if still PENDING)
     * @param decidedBy          the decider's ID (null if still PENDING)
     * @param decisionReason     the optional decision reason (null if still PENDING)
     * @param cancellationReason the optional cancellation reason (null if not cancelled)
     * @return a reconstituted {@code LeaveRequest} with no pending domain events
     */
    public static LeaveRequest reconstitute(Identity<LeaveRequest> id, String staffMemberId,
                                             String managerId, LeaveType leaveType,
                                             DateRange dateRange, int numberOfDays,
                                             String reason, LeaveRequestStatus status,
                                             LocalDate submittedOn, LocalDate decidedOn,
                                             String decidedBy, String decisionReason,
                                             String cancellationReason) {
        // Straight delegation to the private constructor — no events, no extra validation
        return new LeaveRequest(id, staffMemberId, managerId, leaveType, dateRange,
                numberOfDays, reason, status, submittedOn, decidedOn, decidedBy,
                decisionReason, cancellationReason);
    }

    // ─────────────────────────────────────────────────────────────────
    // COMMAND METHODS (state machine transitions)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Approves this leave request. Transitions status from PENDING → APPROVED.
     *
     * <p><strong>State machine guard:</strong> Only PENDING requests can be approved.
     * Attempting to approve a request in any other state throws {@link IllegalStateException}.
     *
     * <p><strong>Side effects:</strong>
     * <ul>
     *   <li>status changes to {@link LeaveRequestStatus#APPROVED}</li>
     *   <li>decidedOn is set to today's date</li>
     *   <li>decidedBy is set to the approver's ID</li>
     *   <li>decisionReason is set to the optional reason</li>
     *   <li>A {@link LeaveRequestApprovedEvent} is raised, which triggers
     *       {@code LeaveAllowance.confirmDays()} — moving days from pending to used</li>
     * </ul>
     *
     * @param decidedBy the ID of the manager/admin approving the request (must not be empty)
     * @param reason    optional reason for the approval (e.g., "Approved, enjoy your holiday")
     * @throws IllegalArgumentException if decidedBy is null or blank
     * @throws IllegalStateException    if the request is not in PENDING status
     */
    public void approve(String decidedBy, String reason) {
        argumentNotEmpty(decidedBy, DECIDED_BY_REQUIRED); // Guard: approver ID must be provided

        // State machine guard — only PENDING requests can transition to APPROVED
        if (this.status != LeaveRequestStatus.PENDING) {
            throw new IllegalStateException(CANNOT_APPROVE_NON_PENDING);
        }

        // Transition state and record decision metadata
        this.status = LeaveRequestStatus.APPROVED;    // State machine transition: PENDING → APPROVED
        this.decidedOn = LocalDate.now();              // Snapshot the decision date
        this.decidedBy = decidedBy;                    // Record who approved
        this.decisionReason = reason;                  // Record why (optional)

        // Raise the approved event — the local event listener will call
        // LeaveAllowance.confirmDays(numberOfDays) to move days from pending to used
        addDomainEvent(new LeaveRequestApprovedEvent(
                LocalDate.now(), this.id.id(), this.staffMemberId, decidedBy, this.numberOfDays
        ));
    }

    /**
     * Rejects this leave request. Transitions status from PENDING → REJECTED.
     *
     * <p><strong>State machine guard:</strong> Only PENDING requests can be rejected.
     * Attempting to reject a request in any other state throws {@link IllegalStateException}.
     *
     * <p><strong>Side effects:</strong>
     * <ul>
     *   <li>status changes to {@link LeaveRequestStatus#REJECTED}</li>
     *   <li>decidedOn is set to today's date</li>
     *   <li>decidedBy is set to the rejector's ID</li>
     *   <li>decisionReason is set to the optional reason</li>
     *   <li>A {@link LeaveRequestRejectedEvent} is raised, which triggers
     *       {@code LeaveAllowance.releasePendingDays()} — returning days from pending to available</li>
     * </ul>
     *
     * @param decidedBy the ID of the manager/admin rejecting the request (must not be empty)
     * @param reason    optional reason for the rejection (e.g., "Team is short-staffed that week")
     * @throws IllegalArgumentException if decidedBy is null or blank
     * @throws IllegalStateException    if the request is not in PENDING status
     */
    public void reject(String decidedBy, String reason) {
        argumentNotEmpty(decidedBy, DECIDED_BY_REQUIRED); // Guard: rejector ID must be provided

        // State machine guard — only PENDING requests can transition to REJECTED
        if (this.status != LeaveRequestStatus.PENDING) {
            throw new IllegalStateException(CANNOT_REJECT_NON_PENDING);
        }

        // Transition state and record decision metadata
        this.status = LeaveRequestStatus.REJECTED;    // State machine transition: PENDING → REJECTED
        this.decidedOn = LocalDate.now();              // Snapshot the decision date
        this.decidedBy = decidedBy;                    // Record who rejected
        this.decisionReason = reason;                  // Record why (optional)

        // Raise the rejected event — the local event listener will call
        // LeaveAllowance.releasePendingDays(numberOfDays) to return days from pending to available
        addDomainEvent(new LeaveRequestRejectedEvent(
                LocalDate.now(), this.id.id(), this.staffMemberId, decidedBy, this.numberOfDays
        ));
    }

    /**
     * Cancels this leave request. Valid from PENDING or APPROVED states.
     *
     * <p><strong>State machine guard:</strong> Requests in terminal states (REJECTED, CANCELLED)
     * cannot be cancelled. Attempting to do so throws {@link IllegalStateException}.
     *
     * <p><strong>Key business logic — the {@code wasPreviouslyApproved} flag:</strong>
     * The cancellation event carries a boolean indicating whether the request was APPROVED
     * before cancellation. The event listener uses this flag to determine the correct
     * allowance operation:
     * <ul>
     *   <li>{@code wasPreviouslyApproved = true} → call {@code LeaveAllowance.creditBackDays()}
     *       (days move from "used" back to "available")</li>
     *   <li>{@code wasPreviouslyApproved = false} → call {@code LeaveAllowance.releasePendingDays()}
     *       (days move from "pending" back to "available")</li>
     * </ul>
     *
     * <p><strong>Side effects:</strong>
     * <ul>
     *   <li>status changes to {@link LeaveRequestStatus#CANCELLED}</li>
     *   <li>cancellationReason is set to the optional reason</li>
     *   <li>A {@link LeaveRequestCancelledEvent} is raised with the {@code wasPreviouslyApproved} flag</li>
     * </ul>
     *
     * @param cancelledBy the ID of the user cancelling the request (must not be empty)
     * @param reason      optional reason for the cancellation
     * @throws IllegalArgumentException if cancelledBy is null or blank
     * @throws IllegalStateException    if the request is already REJECTED or CANCELLED
     */
    public void cancel(String cancelledBy, String reason) {
        argumentNotEmpty(cancelledBy, CANCELLED_BY_REQUIRED); // Guard: canceller ID must be provided

        // State machine guard — cannot cancel requests that are already in a terminal state
        if (this.status == LeaveRequestStatus.REJECTED || this.status == LeaveRequestStatus.CANCELLED) {
            throw new IllegalStateException(CANNOT_CANCEL_TERMINAL);
        }

        // Capture the current status BEFORE transitioning — the event listener needs to know
        // whether this was an approved request (credit back used days) or a pending request
        // (release pending days)
        boolean wasPreviouslyApproved = (this.status == LeaveRequestStatus.APPROVED);

        // Transition state and record cancellation metadata
        this.status = LeaveRequestStatus.CANCELLED;   // State machine transition: PENDING/APPROVED → CANCELLED
        this.cancellationReason = reason;              // Record why (optional)

        // Raise the cancelled event — the local event listener checks wasPreviouslyApproved
        // to decide whether to call creditBackDays() or releasePendingDays() on the allowance
        addDomainEvent(new LeaveRequestCancelledEvent(
                LocalDate.now(), this.id.id(), this.staffMemberId, cancelledBy,
                this.numberOfDays, wasPreviouslyApproved
        ));
    }

    // ─────────────────────────────────────────────────────────────────
    // ACCESSORS (DDD-style: no get prefix)
    // ─────────────────────────────────────────────────────────────────
    // DDD convention uses noun-style accessors (staffMemberId() not getStaffMemberId())
    // to reflect the ubiquitous language rather than JavaBean convention.

    /** @return the ID of the staff member who submitted this request */
    public String staffMemberId() { return staffMemberId; }

    /** @return the ID of the manager responsible for deciding on this request */
    public String managerId() { return managerId; }

    /** @return the type of leave requested (e.g., ANNUAL) */
    public LeaveType leaveType() { return leaveType; }

    /** @return the date range (start to end, inclusive) for the requested leave */
    public DateRange dateRange() { return dateRange; }

    /** @return the number of working days (excluding weekends) in the date range */
    public int numberOfDays() { return numberOfDays; }

    /** @return the optional free-text reason provided at submission time, or null */
    public String reason() { return reason; }

    /** @return the current status in the state machine (PENDING, APPROVED, REJECTED, or CANCELLED) */
    public LeaveRequestStatus status() { return status; }

    /** @return the date this request was submitted */
    public LocalDate submittedOn() { return submittedOn; }

    /** @return the date the approve/reject decision was made, or null if still pending */
    public LocalDate decidedOn() { return decidedOn; }

    /** @return the ID of the person who approved or rejected, or null if still pending */
    public String decidedBy() { return decidedBy; }

    /** @return the optional reason given for the approve/reject decision, or null if still pending */
    public String decisionReason() { return decisionReason; }

    /** @return the optional reason given for cancellation, or null if not cancelled */
    public String cancellationReason() { return cancellationReason; }
}
