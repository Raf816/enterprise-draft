package com.staffs.leavebooking.common.domain;

import java.util.UUID;

/**
 * Generic identity value object wrapping a UUID string
 * (Lecture 2 — DDD Building Blocks: Entity Identity).
 *
 * <p><strong>DDD Concept:</strong> Every Entity and Aggregate Root needs a unique identity.
 * This class wraps a UUID string and provides type safety through generics.
 * The type parameter {@code T} binds the identity to a specific aggregate/entity type,
 * preventing accidental mixing at compile time.
 *
 * <p><strong>Type safety example:</strong>
 * <pre>
 * Identity&lt;LeaveRequest&gt; leaveId = Identity.generateId();
 * Identity&lt;StaffMember&gt; staffId = Identity.generateId();
 * // These are different types — compiler prevents mixing them
 * </pre>
 *
 * <p><strong>Why UUID?</strong> UUIDs can be generated without database coordination
 * (no auto-increment sequence needed). This is important for DDD because the domain
 * layer creates identities — not the database. It also supports the Firebase UID
 * integration where we use external IDs as our aggregate identity.
 *
 * <p><strong>Validation:</strong> The compact constructor validates that the string
 * is not null/blank and is a valid UUID format. This enforces the invariant that
 * every identity in the system is a well-formed UUID.
 *
 * @param <T> the type of entity/aggregate this identity belongs to
 * @param id  the UUID string value
 */
public record Identity<T>(String id) implements ValueObject {

    // Error message constants — public so tests can assert on them
    public static final String IDENTITY_CANNOT_BE_NULL = "Identity cannot be null or blank";
    public static final String IDENTITY_MUST_BE_UUID = "Identity must be a valid UUID format";

    /**
     * Compact constructor — runs automatically when any Identity is created.
     * Validates that the id is not null/blank and is a valid UUID format.
     * This is the "self-validating" pattern: an Identity object cannot exist
     * in an invalid state.
     */
    public Identity {
        // Guard 1: reject null or blank strings
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(IDENTITY_CANNOT_BE_NULL);
        }
        // Guard 2: verify UUID format by attempting to parse it
        // UUID.fromString() throws IllegalArgumentException if the format is wrong
        try {
            UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            // Re-throw with our own message for cleaner error reporting
            throw new IllegalArgumentException(IDENTITY_MUST_BE_UUID);
        }
    }

    /**
     * Factory method: creates an Identity from an existing UUID string.
     * Used when loading entities from persistence (the ID already exists in the database).
     *
     * @param id  the existing UUID string from the database
     * @param <T> the entity/aggregate type this identity belongs to
     * @return a new Identity wrapping the given id
     */
    public static <T> Identity<T> of(String id) {
        return new Identity<>(id);
    }

    /**
     * Factory method: generates a new random UUID identity.
     * Used when creating a brand new aggregate (the ID doesn't exist yet).
     * This is the DDD pattern of "domain generates identity" rather than
     * relying on database auto-increment.
     *
     * @param <T> the entity/aggregate type this identity belongs to
     * @return a new Identity with a randomly generated UUID
     */
    public static <T> Identity<T> generateId() {
        return new Identity<>(UUID.randomUUID().toString());
    }
}
