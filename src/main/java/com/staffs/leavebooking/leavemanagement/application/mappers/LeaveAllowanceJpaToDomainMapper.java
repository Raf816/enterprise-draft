package com.staffs.leavebooking.leavemanagement.application.mappers;

import com.staffs.leavebooking.common.domain.Identity;
import com.staffs.leavebooking.leavemanagement.domain.BusinessYear;
import com.staffs.leavebooking.leavemanagement.domain.LeaveAllowance;
import com.staffs.leavebooking.leavemanagement.infrastructure.entities.LeaveAllowanceJpa;

import java.util.Objects;

/**
 * Data Mapper (Lecture 4 — Layered Architecture / Anti-Corruption Layer) that converts
 * a {@link LeaveAllowanceJpa} infrastructure entity back into a {@link LeaveAllowance}
 * domain aggregate.
 *
 * <p><strong>Direction:</strong> JPA → Domain (command loading path).
 *
 * <p><strong>When used:</strong> Called by the application service when an existing leave
 * allowance is loaded from the database for command processing (e.g. reserveDays, confirmDays,
 * releasePendingDays, creditBackDays, amendEntitlement, updateStaffDetails). The mapper
 * rebuilds the full domain aggregate so that invariants (such as the over-booking check)
 * can be enforced before persisting changes.
 *
 * <p><strong>Important:</strong> This mapper uses {@link LeaveAllowance#reconstitute} rather
 * than {@link LeaveAllowance#createNew}. The {@code reconstitute} factory method rebuilds
 * the aggregate without raising domain events and restores the exact persisted state
 * (including non-zero daysUsed/daysPending). This is the correct choice when hydrating
 * from persistence because the aggregate already passed validation at creation time.
 *
 * <p><strong>Key conversions performed (reverse of DomainToJpa):</strong>
 * <ul>
 *   <li>{@code String} → {@code Identity<LeaveAllowance>} (wraps the UUID string via {@code Identity.of()})</li>
 *   <li>Separate {@code businessYearStart} / {@code businessYearEnd} Integer columns →
 *       {@code BusinessYear} value object</li>
 * </ul>
 *
 * <p><strong>Parameter count:</strong> The {@code reconstitute} factory accepts 10 parameters
 * matching every field of the aggregate, ensuring a complete and consistent rebuild.
 *
 * <p><strong>Mapper chain (command loading path):</strong>
 * {@code JpaRepository.findById() → LeaveAllowanceJpa → [this mapper] → LeaveAllowance}
 *
 * @see LeaveAllowanceJpa                      the source JPA entity (mapped to leave_allowance table)
 * @see LeaveAllowance                         the target domain aggregate
 * @see LeaveAllowance#reconstitute            the factory method used for persistence hydration
 * @see LeaveAllowanceDomainToJpaMapper        the reverse mapper (Domain → JPA) for the write path
 * @see LeaveAllowanceJpaToDTOMapper           the read-path mapper (JPA → DTO) for queries
 */
public class LeaveAllowanceJpaToDomainMapper {

    /**
     * Converts a {@link LeaveAllowanceJpa} entity into a fully hydrated {@link LeaveAllowance}
     * domain aggregate using the {@code reconstitute} factory method.
     *
     * <p>All 10 parameters of the aggregate are mapped from the JPA entity, converting
     * flat database columns back into domain value objects where needed.
     *
     * @param jpa the JPA entity loaded from the database; must not be {@code null}
     * @return a reconstituted {@link LeaveAllowance} aggregate with no pending domain events
     * @throws NullPointerException     if {@code jpa} is {@code null}
     * @throws IllegalArgumentException if the stored UUID string is not a valid UUID format
     *                                  (indicates data corruption)
     */
    public static LeaveAllowance toDomain(LeaveAllowanceJpa jpa) {
        Objects.requireNonNull(jpa, "LeaveAllowance JPA entity cannot be null");

        return LeaveAllowance.reconstitute(
                // String → Identity<LeaveAllowance>: wrap the plain UUID string from the @Id
                // column back into the typed Identity value object for compile-time safety
                Identity.of(jpa.getId()),

                // String → String: staff member UUID, passed through unchanged
                jpa.getStaffMemberId(),

                // String → String: manager UUID, passed through unchanged
                jpa.getManagerId(),

                // String → String: first name (denormalised from Staff Management)
                jpa.getFirstName(),

                // String → String: surname (denormalised from Staff Management)
                jpa.getSurname(),

                // String → String: department (denormalised from Staff Management)
                jpa.getDepartment(),

                // Integer + Integer → BusinessYear value object: recompose the two separate
                // year columns (e.g. 2024, 2025) back into the single immutable value object
                // that encapsulates business-year logic
                new BusinessYear(jpa.getBusinessYearStart(), jpa.getBusinessYearEnd()),

                // int → int: total annual leave entitlement in days
                jpa.getTotalEntitlement(),

                // int → int: number of leave days already confirmed (approved requests)
                jpa.getDaysUsed(),

                // int → int: number of leave days reserved but awaiting approval
                jpa.getDaysPending()
        );
    }
}
