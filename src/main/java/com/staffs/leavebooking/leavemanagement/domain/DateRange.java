package com.staffs.leavebooking.leavemanagement.domain;

import com.staffs.leavebooking.common.domain.ValueObject;

import java.time.DayOfWeek;
import java.time.LocalDate;

import static com.staffs.leavebooking.common.domain.DomainAssertions.argumentNotNull;

/**
 * Value Object representing an inclusive date range (start date to end date)
 * with working-day calculation logic (Lecture 2 — Value Objects).
 *
 * <p><strong>DDD Concept (Lecture 2):</strong> A Value Object is defined by its attributes,
 * not by an identity. Two {@code DateRange} instances with the same start and end dates
 * are considered equal. Value objects are immutable — implemented as a Java {@code record}
 * which provides immutability and structural equality automatically.
 *
 * <p><strong>Self-validating:</strong> The compact constructor enforces two invariants:
 * <ul>
 *   <li>Neither date can be null</li>
 *   <li>endDate must be on or after startDate (no backwards ranges)</li>
 * </ul>
 *
 * <p><strong>Future-start validation is separate:</strong> The {@link #validateFutureStart()}
 * method is only called on the write path ({@link LeaveRequest#submitNew}) because a date
 * range loaded from persistence may legitimately have a start date in the past (the leave
 * was valid when originally submitted).
 *
 * <p><strong>Working days calculation:</strong> The {@link #workingDays()} method counts
 * only Monday–Friday days in the range. This count is used by the {@link LeaveRequest}
 * to determine how many days to reserve/confirm on the {@link LeaveAllowance}.
 *
 * @param startDate the first day of leave (inclusive, must not be null)
 * @param endDate   the last day of leave (inclusive, must not be null, must be &ge; startDate)
 * @see ValueObject for the marker interface
 * @see LeaveRequest#submitNew for where this value object is created and validated
 */
public record DateRange(LocalDate startDate, LocalDate endDate) implements ValueObject {

    // ─────────────────────────────────────────────────────────────────
    // VALIDATION MESSAGE CONSTANTS
    // ─────────────────────────────────────────────────────────────────

    /** Error message when startDate is null. */
    public static final String START_DATE_NOT_NULL = "Start date cannot be null";

    /** Error message when endDate is null. */
    public static final String END_DATE_NOT_NULL = "End date cannot be null";

    /** Error message when endDate is before startDate. */
    public static final String END_BEFORE_START = "End date must be on or after start date";

    /** Error message when startDate is not in the future at submission time. */
    public static final String START_DATE_IN_PAST = "Start date must be in the future";

    // ─────────────────────────────────────────────────────────────────
    // COMPACT CONSTRUCTOR (self-validation)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Compact constructor — validates invariants at creation time.
     *
     * <p>This runs every time a {@code DateRange} is instantiated (both write and read paths).
     * It enforces that neither date is null and that the range is not backwards.
     *
     * <p><strong>Note:</strong> Future-start validation is NOT done here because reconstituted
     * date ranges from the database may legitimately have past start dates.
     *
     * @throws IllegalArgumentException if either date is null or endDate &lt; startDate
     */
    public DateRange {
        argumentNotNull(startDate, START_DATE_NOT_NULL); // Guard: startDate must not be null
        argumentNotNull(endDate, END_DATE_NOT_NULL);     // Guard: endDate must not be null

        // Validate that the range is not backwards (end must be >= start)
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(END_BEFORE_START);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // VALIDATION METHODS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Validates that the start date is strictly in the future (after today).
     *
     * <p><strong>Write-path only:</strong> Called by {@link LeaveRequest#submitNew} to ensure
     * new leave requests cannot be backdated. This is NOT called when reconstituting from
     * persistence because a previously-valid request may now have a past start date.
     *
     * @throws IllegalArgumentException if startDate is today or in the past
     */
    public void validateFutureStart() {
        // startDate must be strictly after today — today itself is not allowed
        if (!startDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException(START_DATE_IN_PAST);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // QUERY METHODS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Calculates the number of working days (Monday–Friday) in this date range.
     *
     * <p><strong>Business logic:</strong> Leave allowances are counted in working days,
     * not calendar days. Weekends (Saturday and Sunday) are excluded from the count.
     * Public holidays are NOT excluded — this is a simplification for the university project.
     *
     * <p><strong>Algorithm:</strong> Iterates day-by-day from startDate to endDate (inclusive),
     * incrementing the count for each day that is not a Saturday or Sunday.
     *
     * <p><strong>Used by:</strong> {@link LeaveRequest#submitNew} to calculate the
     * {@code numberOfDays} field, which is then used for all allowance operations.
     *
     * @return the number of weekdays (Mon–Fri) in the range, zero if the range spans only weekends
     */
    public int workingDays() {
        int count = 0;                       // Accumulator for working days
        LocalDate current = startDate;       // Start iterating from the first day

        // Iterate through every day in the range (inclusive of both start and end)
        while (!current.isAfter(endDate)) {
            DayOfWeek day = current.getDayOfWeek(); // Get the day of the week for the current date

            // Only count weekdays (Monday through Friday)
            if (day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY) {
                count++; // This is a working day — increment the count
            }

            current = current.plusDays(1); // Move to the next calendar day
        }

        return count; // Total number of working days in the range
    }
}
