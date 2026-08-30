package com.staffs.leavebooking.common.domain;

/**
 * Marker interface for Value Objects in the domain model (Lecture 2 — DDD Building Blocks).
 *
 * <p><strong>DDD Concept:</strong> A Value Object is defined by its attributes, not by an identity.
 * Two value objects with the same state are considered equal (structural equality).
 * Value objects should be immutable — once created, their state never changes.
 *
 * <p><strong>Examples in this system:</strong>
 * <ul>
 *   <li>{@link Email} — an email address is a value (two "admin@admin.com" are the same)</li>
 *   <li>{@link FullName} — a name is a value (two "Raf Ahmed" are the same)</li>
 *   <li>{@link Identity} — a UUID wrapper is a value (two identical UUIDs are the same)</li>
 * </ul>
 *
 * <p><strong>Contrast with Entity:</strong> An {@link Entity} is defined by its identity —
 * two entities with the same state but different IDs are different objects.
 *
 * <p>This interface carries no methods. It exists purely as a semantic marker so that
 * developers can see at a glance which classes are value objects in the domain model.
 * Java records are used for all value objects, which automatically provides immutability
 * and structural equals/hashCode — exactly what DDD value objects require.
 *
 * @see Entity for identity-based equality
 * @see IdentifiedValueObject for value objects that need a surrogate ORM id
 */
public interface ValueObject {
    // Marker interface — no methods.
    // Implementing classes (records) get immutability + structural equality from Java records.
}
