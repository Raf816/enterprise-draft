package com.staffs.leavebooking.leavemanagement.application.mappers;

import com.staffs.leavebooking.leavemanagement.domain.LeaveAllowance;
import com.staffs.leavebooking.leavemanagement.infrastructure.entities.LeaveAllowanceJpa;

import java.util.Objects;

/**
 * Data Mapper (Lecture 4 — Layered Architecture / Anti-Corruption Layer) that converts
 * a {@link LeaveAllowance} domain aggregate into a {@link LeaveAllowanceJpa} infrastructure entity.
 *
 * <p><strong>Direction:</strong> Domain → JPA (write path).
 *
 * <p><strong>When used:</strong> Called by the application service after the aggregate has
 * been created or mutated (e.g. reserveDays, confirmDays, releasePendingDays, creditBackDays,
 * amendEntitlement) and needs to be persisted. This mapper sits at the boundary between
 * the domain layer and the infrastructure layer.
 *
 * <p><strong>Two methods:</strong>
 * <ul>
 *   <li>{@link #toJpa(LeaveAllowance)} — creates a new JPA entity (used for first-time persistence)</li>
 *   <li>{@link #updateJpa(LeaveAllowance, LeaveAllowanceJpa)} — updates an existing JPA entity
 *       in-place (used when modifying an existing allowance so JPA change tracking applies)</li>
 * </ul>
 *
 * <p><strong>Key conversions performed:</strong>
 * <ul>
 *   <li>{@code Identity<LeaveAllowance>} → {@code String} (unwraps the UUID wrapper via {@code id().id()})</li>
 *   <li>{@code BusinessYear} value object → separate {@code businessYearStart} / {@code businessYearEnd} Integer fields</li>
 * </ul>
 *
 * <p><strong>Mapper chain (write path):</strong>
 * {@code LeaveAllowance → [this mapper] → LeaveAllowanceJpa → JpaRepository.save()}
 *
 * @see LeaveAllowance                         the source domain aggregate
 * @see LeaveAllowanceJpa                      the target JPA entity (mapped to leave_allowance table)
 * @see LeaveAllowanceJpaToDomainMapper        the reverse mapper (JPA → Domain) for command loading
 * @see LeaveAllowanceJpaToDTOMapper           the read-path mapper (JPA → DTO) for queries
 */
public class LeaveAllowanceDomainToJpaMapper {

    /**
     * Converts a {@link LeaveAllowance} domain aggregate into a new {@link LeaveAllowanceJpa}
     * entity suitable for first-time persistence via JPA.
     *
     * <p>Creates a new JPA entity instance and copies every field from the domain aggregate,
     * performing type conversions where the domain model uses value objects that the JPA
     * layer stores as primitive columns.
     *
     * @param domain the domain aggregate to convert; must not be {@code null}
     * @return a fully populated {@link LeaveAllowanceJpa} entity ready for
     *         {@code JpaRepository.save()}
     * @throws NullPointerException if {@code domain} is {@code null}
     */
    public static LeaveAllowanceJpa toJpa(LeaveAllowance domain) {
        Objects.requireNonNull(domain, "LeaveAllowance domain entity cannot be null");

        LeaveAllowanceJpa jpa = new LeaveAllowanceJpa();

        // Identity<LeaveAllowance> → String: unwrap the typed UUID wrapper to a plain
        // String for the JPA @Id column (VARCHAR(36))
        jpa.setId(domain.id().id());

        // String → String: staff member UUID, no conversion needed
        jpa.setStaffMemberId(domain.staffMemberId());

        // String → String: manager UUID, no conversion needed
        jpa.setManagerId(domain.managerId());

        // String → String: first name of the staff member (denormalised from Staff Management)
        jpa.setFirstName(domain.firstName());

        // String → String: surname of the staff member (denormalised from Staff Management)
        jpa.setSurname(domain.surname());

        // String → String: department name (denormalised from Staff Management)
        jpa.setDepartment(domain.department());

        // BusinessYear value object → Integer: extract the start year from the composite
        // value object; the JPA entity stores start and end years as separate Integer columns
        jpa.setBusinessYearStart(domain.businessYear().startYear());

        // BusinessYear value object → Integer: extract the end year from the composite
        // value object; paired with businessYearStart above to flatten the value object
        jpa.setBusinessYearEnd(domain.businessYear().endYear());

        // int → int: total annual leave entitlement in days
        jpa.setTotalEntitlement(domain.totalEntitlement());

        // int → int: number of leave days already used (confirmed/approved)
        jpa.setDaysUsed(domain.daysUsed());

        // int → int: number of leave days reserved but not yet approved (pending requests)
        jpa.setDaysPending(domain.daysPending());

        return jpa;
    }

    /**
     * Updates an existing {@link LeaveAllowanceJpa} entity in-place from the domain aggregate.
     *
     * <p>Used when modifying an existing allowance (e.g. after reserve, confirm, release,
     * credit-back, or amend operations). Only mutable fields are updated — the identity,
     * staff member ID, and business year are immutable after creation and are not touched.
     *
     * <p>By updating the managed JPA entity in-place rather than creating a new one,
     * JPA's dirty-checking/change-tracking mechanism detects the modifications and
     * generates an efficient UPDATE statement on flush.
     *
     * @param domain the domain aggregate with updated state; must not be {@code null}
     * @param jpa    the existing managed JPA entity to update; must not be {@code null}
     * @throws NullPointerException if either parameter is {@code null}
     */
    public static void updateJpa(LeaveAllowance domain, LeaveAllowanceJpa jpa) {
        Objects.requireNonNull(domain, "LeaveAllowance domain entity cannot be null");
        Objects.requireNonNull(jpa, "LeaveAllowance JPA entity cannot be null");

        // String → String: manager may change via staff detail sync (updateStaffDetails)
        jpa.setManagerId(domain.managerId());

        // String → String: first name, synced from Staff Management via remote events
        jpa.setFirstName(domain.firstName());

        // String → String: surname, synced from Staff Management via remote events
        jpa.setSurname(domain.surname());

        // String → String: department, may change via staff detail sync
        jpa.setDepartment(domain.department());

        // int → int: entitlement may change via amendEntitlement command
        jpa.setTotalEntitlement(domain.totalEntitlement());

        // int → int: days used, updated when leave is confirmed or credited back
        jpa.setDaysUsed(domain.daysUsed());

        // int → int: days pending, updated when leave is reserved or released
        jpa.setDaysPending(domain.daysPending());
    }
}
