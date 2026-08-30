package com.staffs.leavebooking.common.domain;

/**
 * Marker interface for Value Objects that require an ORM surrogate id
 * due to their repeating nature (stored in a separate join table).
 *
 * <p><strong>Why this exists:</strong> In pure DDD, value objects have no identity.
 * But JPA sometimes needs a primary key to store value objects in their own table
 * (e.g., when an aggregate has a collection of value objects). In those cases,
 * the JPA entity gets a surrogate {@code @Id} field for ORM purposes only.
 *
 * <p><strong>Important:</strong> Even though the ORM has a surrogate id,
 * equals/hashCode should still compare by value (all domain fields),
 * NOT by the surrogate id. The surrogate id is an infrastructure concern,
 * not a domain concern.
 *
 * <p><strong>Example:</strong> {@code DateRange} in Leave Management — if it were
 * stored in a separate table, it would need a surrogate id for JPA,
 * but two DateRange objects with the same start/end dates are still equal.
 *
 * @see ValueObject for standard value objects without ORM id
 */
public interface IdentifiedValueObject extends ValueObject {
    // Marker interface — extends ValueObject with the semantic meaning
    // that implementors will have a JPA surrogate id that should be
    // excluded from equals/hashCode comparisons.
}
