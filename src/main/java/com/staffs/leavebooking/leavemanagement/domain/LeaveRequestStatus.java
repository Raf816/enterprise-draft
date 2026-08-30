package com.staffs.leavebooking.leavemanagement.domain;

/**
 * Enum representing the possible states in the {@link LeaveRequest} state machine
 * (Lecture 4 — Domain Modelling, Lecture 3 — Aggregate Invariants).
 *
 * <p><strong>DDD Concept (Lecture 4):</strong> The leave request lifecycle follows a
 * strict state machine pattern. Each enum constant represents a distinct state, and
 * only specific transitions are allowed. The transitions are enforced by the command
 * methods on {@link LeaveRequest}, not by this enum — the enum simply names the states.
 *
 * <p><strong>State machine transitions:</strong>
 * <pre>
 *   PENDING  ──→ APPROVED   (via {@link LeaveRequest#approve(String, String)})
 *   PENDING  ──→ REJECTED   (via {@link LeaveRequest#reject(String, String)})
 *   PENDING  ──→ CANCELLED  (via {@link LeaveRequest#cancel(String, String)})
 *   APPROVED ──→ CANCELLED  (via {@link LeaveRequest#cancel(String, String)})
 * </pre>
 *
 * <p><strong>Terminal states:</strong> REJECTED and CANCELLED are terminal — no further
 * transitions are allowed once a request reaches either state.
 *
 * <p><strong>Allowance impact per state:</strong>
 * <ul>
 *   <li>PENDING   → days are reserved (daysPending increases)</li>
 *   <li>APPROVED  → days move from pending to used (daysPending decreases, daysUsed increases)</li>
 *   <li>REJECTED  → pending days are released (daysPending decreases)</li>
 *   <li>CANCELLED → depends on previous state: if was APPROVED, days are credited back from used;
 *       if was PENDING, pending days are released</li>
 * </ul>
 *
 * @see LeaveRequest for the aggregate that enforces state transitions
 * @see LeaveAllowance for the aggregate that tracks the balance impact of each state
 */
public enum LeaveRequestStatus {

    /** Initial state — the request has been submitted and awaits a manager's decision. */
    PENDING("Awaiting manager approval"),

    /** The manager has approved the leave request — days move from pending to used. */
    APPROVED("Leave request approved"),

    /** The manager has rejected the leave request — pending days are released. Terminal state. */
    REJECTED("Leave request rejected"),

    /** The request has been cancelled (by staff or admin). Terminal state. */
    CANCELLED("Leave request cancelled");

    // ─────────────────────────────────────────────────────────────────
    // FIELD
    // ─────────────────────────────────────────────────────────────────

    /** Human-readable description of this status, used for display and logging. */
    private final String description;

    // ─────────────────────────────────────────────────────────────────
    // CONSTRUCTOR
    // ─────────────────────────────────────────────────────────────────

    /**
     * Private enum constructor — sets the human-readable description for this status.
     *
     * @param description a short description of what this status means
     */
    LeaveRequestStatus(String description) {
        this.description = description;
    }

    // ─────────────────────────────────────────────────────────────────
    // ACCESSOR
    // ─────────────────────────────────────────────────────────────────

    /**
     * Returns the human-readable description of this status.
     *
     * @return the status description (e.g., "Awaiting manager approval")
     */
    public String description() {
        return description;
    }
}
