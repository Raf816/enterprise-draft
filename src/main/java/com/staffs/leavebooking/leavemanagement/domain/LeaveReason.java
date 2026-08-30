package com.staffs.leavebooking.leavemanagement.domain;

import com.staffs.leavebooking.common.domain.ValueObject;

import static com.staffs.leavebooking.common.domain.DomainAssertions.argumentLength;
import static com.staffs.leavebooking.common.domain.DomainAssertions.argumentNotEmpty;

/**
 * Value Object representing the textual reason provided with a leave request
 * (Lecture 2 — Value Objects).
 *
 * <p><strong>DDD Concept (Lecture 2):</strong> A Value Object is defined by its attributes,
 * not by an identity. Two {@code LeaveReason} instances wrapping the same string are
 * considered equal. Implemented as a Java {@code record} which provides immutability
 * and structural equality automatically.
 *
 * <p><strong>Self-validating:</strong> The compact constructor enforces two invariants:
 * <ul>
 *   <li>The reason string must not be null or blank</li>
 *   <li>The reason string must not exceed {@value #MAX_LENGTH} characters</li>
 * </ul>
 *
 * <p><strong>Encapsulation benefit:</strong> By wrapping the reason string in a value object,
 * the validation rules (non-empty, max length) are enforced once in a single place rather
 * than being duplicated wherever a reason is accepted. This is the "Tiny Types" or
 * "Whole Value" pattern from DDD.
 *
 * @param reason the leave reason text (must not be blank, max {@value #MAX_LENGTH} characters)
 * @see ValueObject for the marker interface
 */
public record LeaveReason(String reason) implements ValueObject {

    // ─────────────────────────────────────────────────────────────────
    // CONSTANTS
    // ─────────────────────────────────────────────────────────────────

    /** Maximum allowed length for a leave reason (in characters). */
    public static final int MAX_LENGTH = 500;

    /** Error message when reason is null or blank. */
    public static final String REASON_NOT_EMPTY = "Leave reason cannot be empty";

    /** Error message when reason exceeds the maximum length. */
    public static final String REASON_TOO_LONG = "Leave reason must not exceed " + MAX_LENGTH + " characters";

    // ─────────────────────────────────────────────────────────────────
    // COMPACT CONSTRUCTOR (self-validation)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Compact constructor — validates invariants at creation time.
     *
     * <p>Ensures the reason is a non-blank string within the allowed length.
     * The {@code argumentNotEmpty} call also trims the string, so leading/trailing
     * whitespace is normalised.
     *
     * @throws IllegalArgumentException if reason is null, blank, or exceeds {@value #MAX_LENGTH} characters
     */
    public LeaveReason {
        reason = argumentNotEmpty(reason, REASON_NOT_EMPTY);        // Guard: non-blank, returns trimmed value
        argumentLength(reason, 1, MAX_LENGTH, REASON_TOO_LONG);    // Guard: length within [1, 500]
    }
}
