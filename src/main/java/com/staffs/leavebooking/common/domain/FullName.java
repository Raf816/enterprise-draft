package com.staffs.leavebooking.common.domain;

import jakarta.persistence.Embeddable;

import static com.staffs.leavebooking.common.domain.DomainAssertions.argumentLength;
import static com.staffs.leavebooking.common.domain.DomainAssertions.argumentMatchesPattern;
import static com.staffs.leavebooking.common.domain.DomainAssertions.argumentNotEmpty;

/**
 * Value Object representing a person's full name (Lecture 2 — DDD Value Objects).
 *
 * <p><strong>@Embeddable:</strong> JPA annotation meaning this record's fields
 * (firstName, surname) are stored directly in the parent entity's table as columns,
 * rather than in a separate table. When a StaffMember JPA entity has a FullName,
 * JPA creates columns {@code first_name} and {@code surname} in the staff_member table.
 *
 * <p><strong>Validation rules:</strong>
 * <ul>
 *   <li>Neither first name nor surname can be null or blank</li>
 *   <li>Both must be between 1 and 50 characters</li>
 *   <li>Both must contain only letters, hyphens, apostrophes, and spaces
 *       (supports names like "O'Brien", "Mary-Jane", "Van Der Berg")</li>
 * </ul>
 *
 * <p><strong>Self-validating:</strong> Like all value objects in this system,
 * the compact constructor validates on creation. A FullName object in an
 * invalid state cannot exist.
 *
 * @param firstName the person's first name (validated, trimmed)
 * @param surname   the person's surname (validated, trimmed)
 */
@Embeddable // Tells JPA to embed these fields into the parent entity's table
public record FullName(
        String firstName,
        String surname
) implements ValueObject {

    // ─── Validation constants (public so tests can reference them) ───

    /** Maximum allowed length for a first name */
    public static final int MAX_FIRST_NAME_LENGTH = 50;

    /** Maximum allowed length for a surname */
    public static final int MAX_SURNAME_LENGTH = 50;

    // ─── Error message constants ───

    public static final String FIRST_NAME_NOT_EMPTY = "First name cannot be empty";
    public static final String SURNAME_NOT_EMPTY = "Surname cannot be empty";
    public static final String FIRST_NAME_LENGTH = "First name must be between 1 and " + MAX_FIRST_NAME_LENGTH + " characters";
    public static final String SURNAME_LENGTH = "Surname must be between 1 and " + MAX_SURNAME_LENGTH + " characters";

    /**
     * Regex pattern allowing only letters, hyphens, apostrophes, and spaces.
     * This supports names like: O'Brien, Mary-Jane, Van Der Berg, Raf
     * But rejects: Raf123, @admin, etc.
     */
    public static final String NAME_PATTERN = "^[a-zA-Z' \\-]+$";

    public static final String FIRST_NAME_INVALID_CHARS = "First name must contain only letters, hyphens, apostrophes, and spaces";
    public static final String SURNAME_INVALID_CHARS = "Surname must contain only letters, hyphens, apostrophes, and spaces";

    /**
     * Compact constructor — runs automatically every time a FullName is created.
     * Applies three layers of validation to each name component:
     * 1. Not empty (also trims whitespace)
     * 2. Length within bounds (1-50 characters)
     * 3. Pattern match (only allowed characters)
     */
    public FullName {
        // Validate firstName: not null/blank, trim whitespace, store trimmed value
        firstName = argumentNotEmpty(firstName, FIRST_NAME_NOT_EMPTY);
        // Validate surname: not null/blank, trim whitespace, store trimmed value
        surname = argumentNotEmpty(surname, SURNAME_NOT_EMPTY);

        // Validate firstName length is between 1 and 50 characters (after trimming)
        argumentLength(firstName, 1, MAX_FIRST_NAME_LENGTH, FIRST_NAME_LENGTH);
        // Validate surname length is between 1 and 50 characters (after trimming)
        argumentLength(surname, 1, MAX_SURNAME_LENGTH, SURNAME_LENGTH);

        // Validate firstName contains only allowed characters (letters, hyphens, apostrophes, spaces)
        argumentMatchesPattern(firstName, NAME_PATTERN, FIRST_NAME_INVALID_CHARS);
        // Validate surname contains only allowed characters
        argumentMatchesPattern(surname, NAME_PATTERN, SURNAME_INVALID_CHARS);
    }
}
