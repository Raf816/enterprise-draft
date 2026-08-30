package com.staffs.leavebooking.common.domain;

import java.math.BigDecimal;

/**
 * Utility class providing static precondition guard methods for domain validation
 * (Lecture 3 — Aggregate Invariants).
 *
 * <p><strong>Purpose:</strong> Domain objects (aggregates, entities, value objects) call these
 * methods in their constructors and command methods to enforce business rules.
 * If a precondition fails, an {@link IllegalArgumentException} is thrown immediately,
 * preventing the domain object from entering an invalid state.
 *
 * <p><strong>Design pattern:</strong> This follows the "Guard Clause" or "Assertion" pattern
 * (also called "Design by Contract" preconditions). By centralising validation logic here,
 * domain classes stay clean and focused on business behaviour rather than null-checking boilerplate.
 *
 * <p><strong>Why IllegalArgumentException?</strong> These are programming errors or invalid
 * user input — the caller passed bad data. Spring's {@code @ControllerAdvice}
 * ({@link com.staffs.leavebooking.GlobalExceptionHandler}) catches these and returns
 * a clean 400 Bad Request response to the API consumer.
 *
 * <p><strong>Usage example:</strong>
 * <pre>
 * // In a domain constructor:
 * argumentNotNull(email, "Email is required");        // throws if null
 * argumentNotEmpty(department, "Department required"); // throws if null/blank, returns trimmed
 * argumentLength(name, 1, 50, "Name too long");       // throws if outside bounds
 * </pre>
 */
public final class DomainAssertions {

    /**
     * Private constructor prevents instantiation.
     * This is a utility class — all methods are static.
     * Making the constructor private follows the "utility class" pattern
     * and prevents accidental new DomainAssertions().
     */
    private DomainAssertions() {
        // utility class — not instantiable
    }

    /**
     * Asserts that a String argument is not null and not blank (empty or whitespace-only).
     * If valid, returns the trimmed version of the string to normalise whitespace.
     *
     * @param argument the string to validate
     * @param message  the error message if validation fails
     * @return the trimmed string (leading/trailing whitespace removed)
     * @throws IllegalArgumentException if argument is null or blank
     */
    public static String argumentNotEmpty(String argument, String message) {
        // Check for null first, then blank (empty string or only whitespace)
        if (argument == null || argument.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        // Return trimmed version — removes leading/trailing whitespace
        // This means "  Raf  " becomes "Raf" and is stored consistently
        return argument.trim();
    }

    /**
     * Asserts that an Object argument is not null.
     * Used for non-string fields like dates, enums, and other objects.
     *
     * @param argument the object to validate
     * @param message  the error message if validation fails
     * @throws IllegalArgumentException if argument is null
     */
    public static void argumentNotNull(Object argument, String message) {
        if (argument == null) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Asserts that a String's length falls within the specified range (inclusive).
     * Should be called AFTER argumentNotEmpty to avoid NullPointerException.
     *
     * @param argument  the string to check
     * @param minLength minimum allowed length (inclusive)
     * @param maxLength maximum allowed length (inclusive)
     * @param message   the error message if validation fails
     * @throws IllegalArgumentException if length is outside the range
     */
    public static void argumentLength(String argument, int minLength, int maxLength, String message) {
        // Check if the string length falls outside the acceptable range
        if (argument.length() < minLength || argument.length() > maxLength) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Asserts that an integer argument is strictly positive (greater than zero).
     * Used for quantities that must be at least 1 (e.g., leave entitlement days).
     *
     * @param argument the integer to validate
     * @param message  the error message if validation fails
     * @throws IllegalArgumentException if argument is zero or negative
     */
    public static void argumentPositive(int argument, String message) {
        if (argument <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Asserts that an integer argument is not negative (zero or positive).
     * Used for counts that can be zero but not negative (e.g., daysUsed can be 0).
     *
     * @param argument the integer to validate
     * @param message  the error message if validation fails
     * @throws IllegalArgumentException if argument is negative
     */
    public static void argumentNotNegative(int argument, String message) {
        if (argument < 0) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Asserts that a BigDecimal argument is not null.
     * Used for monetary or decimal values that must be present.
     *
     * @param argument the BigDecimal to validate
     * @param message  the error message if validation fails
     * @throws IllegalArgumentException if argument is null
     */
    public static void argumentNotEmpty(BigDecimal argument, String message) {
        if (argument == null) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Asserts that a String argument matches the given regex pattern.
     * Used for format validation (e.g., email addresses, names with allowed characters).
     *
     * @param argument the string to validate
     * @param regex    the regular expression pattern to match against
     * @param message  the error message if validation fails
     * @throws IllegalArgumentException if argument is null or doesn't match the pattern
     */
    public static void argumentMatchesPattern(String argument, String regex, String message) {
        // Null check first to avoid NullPointerException on .matches()
        if (argument == null || !argument.matches(regex)) {
            throw new IllegalArgumentException(message);
        }
    }
}
