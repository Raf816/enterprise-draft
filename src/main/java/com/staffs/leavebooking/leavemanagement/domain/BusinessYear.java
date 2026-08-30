package com.staffs.leavebooking.leavemanagement.domain;

import com.staffs.leavebooking.common.domain.ValueObject;

/**
 * Value Object representing a business (fiscal) year period (Lecture 2 — Value Objects).
 *
 * <p><strong>DDD Concept (Lecture 2):</strong> A Value Object is defined by its attributes,
 * not by an identity. Two {@code BusinessYear} instances with the same startYear and endYear
 * are considered equal. Implemented as a Java {@code record} which provides immutability
 * and structural equality automatically.
 *
 * <p><strong>Structure:</strong> A business year spans two consecutive calendar years
 * (e.g., 2026-2027). The endYear is always startYear + 1. This constraint is enforced
 * by the compact constructor's self-validation.
 *
 * <p><strong>Usage:</strong> Each {@link LeaveAllowance} is scoped to a single business year.
 * When a new staff member is added, their allowance is created for the current business year
 * via {@link #current()}.
 *
 * <p><strong>Invariants:</strong>
 * <ul>
 *   <li>startYear must be a positive value (greater than zero)</li>
 *   <li>endYear must equal startYear + 1 (consecutive years only)</li>
 * </ul>
 *
 * @param startYear the starting calendar year of the fiscal period (e.g., 2026)
 * @param endYear   the ending calendar year of the fiscal period (must be startYear + 1, e.g., 2027)
 * @see ValueObject for the marker interface
 * @see LeaveAllowance for the aggregate that uses this value object
 */
public record BusinessYear(int startYear, int endYear) implements ValueObject {

    // ─────────────────────────────────────────────────────────────────
    // VALIDATION MESSAGE CONSTANTS
    // ─────────────────────────────────────────────────────────────────

    /** Error message when startYear is zero or negative. */
    public static final String INVALID_START_YEAR = "Start year must be a positive value";

    /** Error message when endYear is not exactly startYear + 1. */
    public static final String END_YEAR_MUST_FOLLOW_START = "End year must be start year + 1";

    // ─────────────────────────────────────────────────────────────────
    // COMPACT CONSTRUCTOR (self-validation)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Compact constructor — validates invariants at creation time.
     *
     * <p>Ensures the business year represents a valid consecutive-year period.
     * This runs every time a {@code BusinessYear} is instantiated.
     *
     * @throws IllegalArgumentException if startYear &le; 0 or endYear &ne; startYear + 1
     */
    public BusinessYear {
        // Guard: startYear must be positive (no year zero or negative years)
        if (startYear <= 0) {
            throw new IllegalArgumentException(INVALID_START_YEAR);
        }

        // Guard: endYear must be exactly one more than startYear (consecutive years)
        if (endYear != startYear + 1) {
            throw new IllegalArgumentException(END_YEAR_MUST_FOLLOW_START);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // FACTORY METHODS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Convenience factory that creates a {@code BusinessYear} for the current calendar year.
     *
     * <p>Defaults to the current year as startYear and current year + 1 as endYear.
     * For example, if called in 2026, returns {@code BusinessYear(2026, 2027)}.
     *
     * <p><strong>Used by:</strong> {@link LeaveAllowance#createNew} when setting up a
     * new allowance for a newly-added staff member.
     *
     * @return a {@code BusinessYear} representing the current fiscal year
     */
    public static BusinessYear current() {
        int currentYear = java.time.LocalDate.now().getYear(); // Get the current calendar year
        return new BusinessYear(currentYear, currentYear + 1); // Create a consecutive-year pair
    }

    // ─────────────────────────────────────────────────────────────────
    // DISPLAY
    // ─────────────────────────────────────────────────────────────────

    /**
     * Returns a human-readable representation of the business year (e.g., "2026-2027").
     *
     * <p>Overrides the default record toString() to provide a cleaner format
     * for logging, UI display, and debugging.
     *
     * @return the business year as "startYear-endYear" (e.g., "2026-2027")
     */
    @Override
    public String toString() {
        return startYear + "-" + endYear; // Format as "YYYY-YYYY"
    }
}
