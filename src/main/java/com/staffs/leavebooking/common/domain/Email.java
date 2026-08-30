package com.staffs.leavebooking.common.domain;

import static com.staffs.leavebooking.common.domain.DomainAssertions.argumentMatchesPattern;
import static com.staffs.leavebooking.common.domain.DomainAssertions.argumentNotEmpty;

/**
 * Value Object representing a validated email address (Lecture 2 — DDD Value Objects).
 *
 * <p><strong>Self-validating:</strong> An Email object cannot exist in an invalid state.
 * The compact constructor runs validation on every creation — if the email address
 * is null, blank, or doesn't match the regex pattern, construction fails immediately
 * with an {@link IllegalArgumentException}.
 *
 * <p><strong>Why a dedicated Value Object instead of plain String?</strong>
 * <ul>
 *   <li>Type safety — a method taking {@code Email} is more expressive than {@code String}</li>
 *   <li>Validation centralised — the regex check happens once, in the constructor</li>
 *   <li>Immutable — Java records are immutable by default, so the email can never change</li>
 *   <li>DDD semantics — an email address IS a value (two identical addresses are equal)</li>
 * </ul>
 *
 * <p><strong>Regex pattern:</strong> {@code ^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$}
 * allows standard email formats like raf.ahmed@bt.com, admin@staffs.ac.uk, etc.
 *
 * @param address the validated email address string
 */
public record Email(String address) implements ValueObject {

    // Error messages — public constants so unit tests can assert on them
    public static final String EMAIL_NOT_EMPTY = "Email address cannot be empty";
    public static final String EMAIL_INVALID_FORMAT = "Email address must be a valid format";

    // Regex pattern for email validation:
    // ^             = start of string
    // [A-Za-z0-9+_.-]+ = one or more alphanumeric chars, plus, underscore, dot, or hyphen (local part)
    // @             = literal @ symbol
    // [A-Za-z0-9.-]+ = one or more alphanumeric chars, dot, or hyphen (domain part)
    // \.            = literal dot before the TLD
    // [A-Za-z]{2,}  = two or more letters for TLD (e.g., com, ac.uk)
    // $             = end of string
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    /**
     * Compact constructor — runs automatically on every Email creation.
     * Validates the address is not empty and matches the email regex.
     *
     * <p>The argumentNotEmpty call also trims the address, so
     * " raf@bt.com " becomes "raf@bt.com".
     */
    public Email {
        // First check: address must not be null or blank (returns trimmed version)
        address = argumentNotEmpty(address, EMAIL_NOT_EMPTY);
        // Second check: trimmed address must match the email regex pattern
        argumentMatchesPattern(address, EMAIL_REGEX, EMAIL_INVALID_FORMAT);
    }
}
