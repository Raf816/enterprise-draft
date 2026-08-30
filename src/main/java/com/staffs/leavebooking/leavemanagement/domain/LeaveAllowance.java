package com.staffs.leavebooking.leavemanagement.domain;

import com.staffs.leavebooking.common.domain.AggregateRoot;
import com.staffs.leavebooking.common.domain.Identity;

import static com.staffs.leavebooking.common.domain.DomainAssertions.*;

/**
 * Aggregate Root tracking annual leave entitlement and balance for a single staff member
 * within a specific business year (Lecture 3 — Aggregates, Lecture 7 — Domain Events).
 *
 * <p><strong>DDD Concept (Lecture 3):</strong> This is the root of the Leave Allowance aggregate.
 * It encapsulates the leave balance calculation logic and enforces the invariant that a staff
 * member cannot book more leave than their entitlement allows. All balance mutations go through
 * command methods on this aggregate — no external code can directly modify daysUsed or daysPending.
 *
 * <p><strong>Relationship to LeaveRequest:</strong> This aggregate is updated reactively via
 * local domain events raised by the {@link LeaveRequest} aggregate:
 * <ul>
 *   <li>{@code LeaveRequestSubmittedEvent} → {@link #reserveDays(int)} — hold days as pending</li>
 *   <li>{@code LeaveRequestApprovedEvent}  → {@link #confirmDays(int)} — move pending → used</li>
 *   <li>{@code LeaveRequestRejectedEvent}  → {@link #releasePendingDays(int)} — release pending → available</li>
 *   <li>{@code LeaveRequestCancelledEvent} → {@link #creditBackDays(int)} or {@link #releasePendingDays(int)}
 *       depending on whether the request was previously approved</li>
 * </ul>
 *
 * <p><strong>Creation:</strong> A new {@code LeaveAllowance} is created when a
 * {@code StaffMemberAddedEvent} (remote event from the Staff Management bounded context)
 * is received. The default entitlement is set at that point.
 *
 * <p><strong>Balance model (three buckets):</strong>
 * <pre>
 *   totalEntitlement = daysUsed + daysPending + availableDays
 *
 *   Where:
 *     daysUsed     = days from APPROVED leave requests
 *     daysPending  = days from PENDING leave requests (reserved but not yet confirmed)
 *     availableDays = totalEntitlement - daysUsed - daysPending (derived, not stored)
 * </pre>
 *
 * <p><strong>Invariants enforced by this aggregate:</strong>
 * <ul>
 *   <li>totalEntitlement must be greater than zero</li>
 *   <li>daysUsed cannot be negative</li>
 *   <li>daysPending cannot be negative</li>
 *   <li>daysUsed + daysPending cannot exceed totalEntitlement (the over-booking invariant)</li>
 *   <li>Cannot release more days than are currently pending</li>
 *   <li>Cannot credit back more days than have been used</li>
 *   <li>Amended entitlement cannot be less than days already used</li>
 * </ul>
 *
 * @see AggregateRoot for the base class that manages domain event collection
 * @see LeaveRequest for the aggregate whose events drive balance changes
 * @see BusinessYear for the fiscal year value object
 */
public class LeaveAllowance extends AggregateRoot<LeaveAllowance> {

    // ─────────────────────────────────────────────────────────────────
    // VALIDATION MESSAGE CONSTANTS
    // ─────────────────────────────────────────────────────────────────
    // Public so that tests can assert against the exact error messages.

    /** Error message when staff member ID is null or blank. */
    public static final String STAFF_MEMBER_ID_REQUIRED = "Staff member ID is required";

    /** Error message when manager ID is null or blank. */
    public static final String MANAGER_ID_REQUIRED = "Manager ID is required";

    /** Error message when business year is null. */
    public static final String BUSINESS_YEAR_REQUIRED = "Business year is required";

    /** Error message when total entitlement is zero or negative. */
    public static final String ENTITLEMENT_MUST_BE_POSITIVE = "Total entitlement must be greater than zero";

    /** Error message when a leave request would exceed available balance. */
    public static final String INSUFFICIENT_BALANCE = "Insufficient leave balance";

    /** Error message when a day count argument is zero or negative. */
    public static final String DAYS_MUST_BE_POSITIVE = "Days must be a positive number";

    /** Error message when attempting to release more days than are currently pending. */
    public static final String CANNOT_RELEASE_MORE_THAN_PENDING = "Cannot release more days than currently pending";

    /** Error message when attempting to credit back more days than have been used. */
    public static final String CANNOT_CREDIT_MORE_THAN_USED = "Cannot credit back more days than have been used";

    /** Error message when new entitlement would be less than days already used. */
    public static final String NEW_ENTITLEMENT_TOO_LOW = "New entitlement cannot be less than days already used";

    // ─────────────────────────────────────────────────────────────────
    // FIELDS
    // ─────────────────────────────────────────────────────────────────

    /** The ID of the staff member this allowance belongs to. Immutable — set once at creation. */
    private final String staffMemberId;

    /**
     * The ID of the staff member's line manager. Mutable — can be updated when the staff
     * member's details are synced from the Staff Management bounded context via remote event.
     */
    private String managerId;

    /** The staff member's first name. Mutable — synced from Staff Management. */
    private String firstName;

    /** The staff member's surname. Mutable — synced from Staff Management. */
    private String surname;

    /**
     * The staff member's department. Mutable — can be updated when organisational changes
     * are synced from the Staff Management bounded context.
     */
    private String department;

    /**
     * The business year this allowance applies to (e.g., 2026-2027).
     * Immutable — each business year has its own allowance record.
     *
     * @see BusinessYear for the value object
     */
    private final BusinessYear businessYear;

    /**
     * The total number of leave days the staff member is entitled to for this business year.
     * Starts at the default entitlement and can be amended by an administrator.
     */
    private int totalEntitlement;

    /**
     * The number of days consumed by APPROVED leave requests.
     * Incremented by {@link #confirmDays(int)}, decremented by {@link #creditBackDays(int)}.
     */
    private int daysUsed;

    /**
     * The number of days reserved by PENDING leave requests (not yet approved or rejected).
     * Incremented by {@link #reserveDays(int)}, decremented by {@link #confirmDays(int)}
     * or {@link #releasePendingDays(int)}.
     */
    private int daysPending;

    // ─────────────────────────────────────────────────────────────────
    // PRIVATE CONSTRUCTOR
    // ─────────────────────────────────────────────────────────────────

    /**
     * Private constructor — all instantiation goes through factory methods
     * ({@link #createNew} or {@link #reconstitute}).
     *
     * <p>Validates all mandatory fields and enforces non-negative constraints on
     * daysUsed and daysPending using {@link com.staffs.leavebooking.common.domain.DomainAssertions}.
     *
     * @param id               unique identity for this aggregate root
     * @param staffMemberId    ID of the staff member (must not be empty)
     * @param managerId        ID of the manager (must not be empty)
     * @param firstName        staff member's first name
     * @param surname          staff member's surname
     * @param department       staff member's department
     * @param businessYear     the fiscal year this allowance covers (must not be null)
     * @param totalEntitlement total days allowed (must be positive)
     * @param daysUsed         days already used by approved requests (must not be negative)
     * @param daysPending      days reserved by pending requests (must not be negative)
     */
    private LeaveAllowance(Identity<LeaveAllowance> id, String staffMemberId, String managerId,
                           String firstName, String surname, String department,
                           BusinessYear businessYear, int totalEntitlement,
                           int daysUsed, int daysPending) {
        super(id); // Delegate to AggregateRoot → Entity constructor (validates non-null identity)
        this.staffMemberId = argumentNotEmpty(staffMemberId, STAFF_MEMBER_ID_REQUIRED); // Guard: non-blank, returns trimmed
        this.managerId = argumentNotEmpty(managerId, MANAGER_ID_REQUIRED);               // Guard: non-blank, returns trimmed
        this.firstName = firstName;                                                       // Optional — display data from Staff Management
        this.surname = surname;                                                           // Optional — display data from Staff Management
        this.department = department;                                                     // Optional — display data from Staff Management
        argumentNotNull(businessYear, BUSINESS_YEAR_REQUIRED);                           // Guard: not null
        this.businessYear = businessYear;
        argumentPositive(totalEntitlement, ENTITLEMENT_MUST_BE_POSITIVE);                // Guard: must be > 0
        this.totalEntitlement = totalEntitlement;
        argumentNotNegative(daysUsed, "Days used cannot be negative");                   // Guard: must be >= 0
        argumentNotNegative(daysPending, "Days pending cannot be negative");              // Guard: must be >= 0
        this.daysUsed = daysUsed;
        this.daysPending = daysPending;
    }

    // ─────────────────────────────────────────────────────────────────
    // FACTORY METHODS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Write-path factory: creates a new leave allowance for a staff member.
     *
     * <p><strong>Triggered by:</strong> A {@code StaffMemberAddedEvent} (remote event from
     * the Staff Management bounded context). When a new staff member is added, the listener
     * creates a fresh allowance with zero days used and zero days pending.
     *
     * <p>Uses {@link BusinessYear#current()} to set the business year to the current
     * calendar year (e.g., 2026-2027).
     *
     * @param id                 unique identity for the new allowance
     * @param staffMemberId      ID of the staff member
     * @param managerId          ID of the staff member's manager
     * @param firstName          staff member's first name
     * @param surname            staff member's surname
     * @param department         staff member's department
     * @param defaultEntitlement the default number of leave days (e.g., 28)
     * @return a new {@code LeaveAllowance} with zero used/pending days for the current business year
     */
    public static LeaveAllowance createNew(Identity<LeaveAllowance> id, String staffMemberId,
                                            String managerId, String firstName, String surname,
                                            String department, int defaultEntitlement) {
        // New allowance starts with the current business year and zero days consumed
        return new LeaveAllowance(id, staffMemberId, managerId, firstName, surname,
                department, BusinessYear.current(), defaultEntitlement, 0, 0);
    }

    /**
     * Read-path factory: reconstitutes a {@code LeaveAllowance} from persisted data.
     *
     * <p>Used by the repository's row mapper when loading from the database.
     * Accepts all fields as-is — no extra validation beyond the constructor guards,
     * and no domain events are raised.
     *
     * @param id               the persisted identity
     * @param staffMemberId    the staff member's ID
     * @param managerId        the manager's ID
     * @param firstName        the staff member's first name
     * @param surname          the staff member's surname
     * @param department       the staff member's department
     * @param businessYear     the business year period
     * @param totalEntitlement the total entitlement
     * @param daysUsed         days consumed by approved requests
     * @param daysPending      days reserved by pending requests
     * @return a reconstituted {@code LeaveAllowance} with no pending domain events
     */
    public static LeaveAllowance reconstitute(Identity<LeaveAllowance> id, String staffMemberId,
                                               String managerId, String firstName, String surname,
                                               String department, BusinessYear businessYear,
                                               int totalEntitlement, int daysUsed, int daysPending) {
        // Straight delegation to the private constructor — no events, no extra validation
        return new LeaveAllowance(id, staffMemberId, managerId, firstName, surname,
                department, businessYear, totalEntitlement, daysUsed, daysPending);
    }

    // ─────────────────────────────────────────────────────────────────
    // COMMAND METHODS (event-driven updates)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Reserves days when a new leave request is submitted (PENDING state).
     *
     * <p><strong>Triggered by:</strong> {@code LeaveRequestSubmittedEvent} via the local event listener.
     *
     * <p><strong>Business rule — the over-booking invariant:</strong> The total of
     * daysUsed + daysPending + requested days must not exceed totalEntitlement.
     * This prevents a staff member from submitting multiple requests that collectively
     * exceed their balance.
     *
     * @param days the number of working days to reserve (must be positive)
     * @throws IllegalArgumentException if days is zero or negative
     * @throws IllegalStateException    if reserving these days would exceed the total entitlement
     */
    public void reserveDays(int days) {
        argumentPositive(days, DAYS_MUST_BE_POSITIVE); // Guard: must be > 0

        // Check the over-booking invariant: used + pending + new request must not exceed entitlement
        if (daysUsed + daysPending + days > totalEntitlement) {
            // Calculate how many days are actually available for a clear error message
            int available = totalEntitlement - daysUsed - daysPending;
            throw new IllegalStateException(
                    INSUFFICIENT_BALANCE + ". Available: " + available + " days, Requested: " + days + " days"
            );
        }

        // Reserve the days — they move into the "pending" bucket
        this.daysPending += days;
    }

    /**
     * Confirms days when a leave request is approved (moves from pending to used).
     *
     * <p><strong>Triggered by:</strong> {@code LeaveRequestApprovedEvent} via the local event listener.
     *
     * <p><strong>Balance transition:</strong> daysPending decreases, daysUsed increases
     * by the same amount. The total entitlement and available days are unchanged.
     *
     * @param days the number of working days to confirm (must be positive)
     * @throws IllegalArgumentException if days is zero or negative
     * @throws IllegalStateException    if days exceeds the current pending count
     */
    public void confirmDays(int days) {
        argumentPositive(days, DAYS_MUST_BE_POSITIVE); // Guard: must be > 0

        // Cannot confirm more days than are currently pending
        if (days > daysPending) {
            throw new IllegalStateException(CANNOT_RELEASE_MORE_THAN_PENDING);
        }

        // Move days from "pending" bucket to "used" bucket
        this.daysPending -= days;  // Decrease pending
        this.daysUsed += days;     // Increase used
    }

    /**
     * Releases pending days when a PENDING request is rejected or cancelled.
     *
     * <p><strong>Triggered by:</strong>
     * <ul>
     *   <li>{@code LeaveRequestRejectedEvent} — request was rejected by a manager</li>
     *   <li>{@code LeaveRequestCancelledEvent} (when {@code wasPreviouslyApproved = false})
     *       — a pending request was cancelled before approval</li>
     * </ul>
     *
     * <p><strong>Balance transition:</strong> daysPending decreases, making those days
     * available again. daysUsed is unchanged.
     *
     * @param days the number of working days to release (must be positive)
     * @throws IllegalArgumentException if days is zero or negative
     * @throws IllegalStateException    if days exceeds the current pending count
     */
    public void releasePendingDays(int days) {
        argumentPositive(days, DAYS_MUST_BE_POSITIVE); // Guard: must be > 0

        // Cannot release more days than are currently in the pending bucket
        if (days > daysPending) {
            throw new IllegalStateException(CANNOT_RELEASE_MORE_THAN_PENDING);
        }

        // Release days from the "pending" bucket — they become available again
        this.daysPending -= days;
    }

    /**
     * Credits back days when an APPROVED request is cancelled.
     *
     * <p><strong>Triggered by:</strong> {@code LeaveRequestCancelledEvent}
     * (when {@code wasPreviouslyApproved = true}).
     *
     * <p><strong>Balance transition:</strong> daysUsed decreases, making those days
     * available again. daysPending is unchanged.
     *
     * @param days the number of working days to credit back (must be positive)
     * @throws IllegalArgumentException if days is zero or negative
     * @throws IllegalStateException    if days exceeds the current used count
     */
    public void creditBackDays(int days) {
        argumentPositive(days, DAYS_MUST_BE_POSITIVE); // Guard: must be > 0

        // Cannot credit back more days than have actually been used
        if (days > daysUsed) {
            throw new IllegalStateException(CANNOT_CREDIT_MORE_THAN_USED);
        }

        // Credit days back from the "used" bucket — they become available again
        this.daysUsed -= days;
    }

    /**
     * Amends the total entitlement for this business year (admin operation).
     *
     * <p><strong>Business rule:</strong> The new entitlement cannot be less than the number
     * of days already used. This prevents the invariant daysUsed &le; totalEntitlement
     * from being violated.
     *
     * <p><strong>Note:</strong> Pending days are not considered in this check. If a manager
     * reduces entitlement below daysUsed + daysPending, the pending requests may need to be
     * rejected separately.
     *
     * @param newEntitlement the new total entitlement (must be positive)
     * @throws IllegalArgumentException if newEntitlement is zero or negative
     * @throws IllegalStateException    if newEntitlement is less than daysUsed
     */
    public void amendEntitlement(int newEntitlement) {
        argumentPositive(newEntitlement, ENTITLEMENT_MUST_BE_POSITIVE); // Guard: must be > 0

        // Cannot reduce entitlement below the days that have already been consumed
        if (newEntitlement < daysUsed) {
            throw new IllegalStateException(NEW_ENTITLEMENT_TOO_LOW);
        }

        // Update the entitlement
        this.totalEntitlement = newEntitlement;
    }

    /**
     * Updates staff details synced from the Staff Management bounded context via a remote event.
     *
     * <p><strong>Triggered by:</strong> A {@code StaffDetailsUpdatedEvent} (remote event)
     * when a staff member's manager or department changes in the Staff Management context.
     *
     * <p>This keeps the denormalised staff data in the Leave Management context eventually
     * consistent with the source of truth in Staff Management.
     *
     * @param managerId  the new manager ID (must not be empty)
     * @param department the new department
     */
    public void updateStaffDetails(String managerId, String department) {
        this.managerId = argumentNotEmpty(managerId, MANAGER_ID_REQUIRED); // Guard: non-blank, returns trimmed
        this.department = department; // Update denormalised department value
    }

    // ─────────────────────────────────────────────────────────────────
    // DERIVED / QUERY ACCESSORS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Calculates the total remaining days (entitlement minus used, ignoring pending).
     *
     * <p>This represents the maximum days the staff member could still use if all
     * pending requests were rejected or cancelled.
     *
     * @return totalEntitlement - daysUsed
     */
    public int remainingDays() {
        return totalEntitlement - daysUsed;
    }

    /**
     * Calculates the days still available to request (excludes both used and pending).
     *
     * <p>This is the number of additional working days the staff member can request
     * without exceeding their entitlement. Used by the UI to show "X days available".
     *
     * @return totalEntitlement - daysUsed - daysPending
     */
    public int availableDays() {
        return totalEntitlement - daysUsed - daysPending;
    }

    // ─────────────────────────────────────────────────────────────────
    // ACCESSORS (DDD-style: no get prefix)
    // ─────────────────────────────────────────────────────────────────
    // DDD convention uses noun-style accessors (staffMemberId() not getStaffMemberId())
    // to reflect the ubiquitous language rather than JavaBean convention.

    /** @return the ID of the staff member this allowance belongs to */
    public String staffMemberId() { return staffMemberId; }

    /** @return the ID of the staff member's manager */
    public String managerId() { return managerId; }

    /** @return the staff member's first name */
    public String firstName() { return firstName; }

    /** @return the staff member's surname */
    public String surname() { return surname; }

    /** @return the staff member's department */
    public String department() { return department; }

    /** @return the business year this allowance covers */
    public BusinessYear businessYear() { return businessYear; }

    /** @return the total leave entitlement in days for this business year */
    public int totalEntitlement() { return totalEntitlement; }

    /** @return the number of days consumed by approved leave requests */
    public int daysUsed() { return daysUsed; }

    /** @return the number of days reserved by pending leave requests */
    public int daysPending() { return daysPending; }
}
