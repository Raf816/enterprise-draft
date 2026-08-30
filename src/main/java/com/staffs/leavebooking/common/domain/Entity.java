package com.staffs.leavebooking.common.domain;

import java.util.Objects;

/**
 * Base class for all domain entities (Lecture 2 — DDD Building Blocks).
 *
 * <p><strong>DDD Concept:</strong> An Entity is an object defined by its identity,
 * not by its attributes. Two entities with the same ID are the same entity,
 * even if all their other fields differ. This is the opposite of a {@link ValueObject},
 * which is defined by its attribute values.
 *
 * <p><strong>Example:</strong> Two {@code StaffMember} objects with the same UUID
 * are the same staff member, even if one has department="Networks" and the other
 * has department="Digital" (e.g., before and after a department change).
 *
 * <p><strong>Type parameter:</strong> The generic {@code T} flows through to
 * {@link Identity}{@code <T>}, creating a typed identity system. This means
 * {@code Identity<LeaveRequest>} and {@code Identity<StaffMember>} are different
 * types — the compiler prevents accidentally using a staff member's ID where
 * a leave request's ID is expected.
 *
 * <p><strong>equals/hashCode:</strong> Compares ONLY by identity (id field).
 * State fields (name, department, etc.) are deliberately excluded.
 * This is the fundamental distinction between Entity and ValueObject in DDD.
 *
 * @param <T> the specific entity/aggregate type (for typed identity)
 * @see AggregateRoot which extends this class and adds domain event support
 */
public abstract class Entity<T> {

    // Error message constant — used in constructor guard
    public static final String IDENTITY_CANNOT_BE_NULL = "Identity cannot be null";

    /**
     * The entity's unique identity. Protected so subclasses (aggregates) can access it.
     * Final because an entity's identity never changes after creation.
     */
    protected final Identity<T> id;

    /**
     * Constructor that sets the entity's identity.
     * Validates that the identity is not null — every entity MUST have an identity.
     *
     * @param id the unique identity for this entity
     * @throws IllegalArgumentException if id is null
     */
    protected Entity(Identity<T> id) {
        // Guard clause: reject null identity (an entity without identity is meaningless in DDD)
        if (id == null) {
            throw new IllegalArgumentException(IDENTITY_CANNOT_BE_NULL);
        }
        this.id = id;
    }

    /**
     * Returns this entity's identity.
     * Used by application services and mappers to get the ID for persistence/DTOs.
     *
     * @return the Identity wrapping the UUID string
     */
    public Identity<T> id() {
        return id;
    }

    /**
     * Equality comparison based ONLY on identity (DDD Entity semantics).
     * Two entities of the same class with the same ID are considered equal,
     * regardless of the values of their other fields.
     *
     * <p>The method checks:
     * 1. Same reference → true (optimisation)
     * 2. Null or different class → false
     * 3. Same ID → true (identity-based equality)
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;                            // Same object reference — always equal
        if (o == null || getClass() != o.getClass()) return false; // Different type or null — never equal
        Entity<?> entity = (Entity<?>) o;                      // Safe cast after class check
        return Objects.equals(id, entity.id);                  // Compare by identity ONLY
    }

    /**
     * Hash code based ONLY on identity (must be consistent with equals).
     * Since equals compares only the ID, hashCode must also use only the ID.
     */
    @Override
    public int hashCode() {
        return Objects.hash(id); // Hash the identity field only — matches equals() contract
    }
}
