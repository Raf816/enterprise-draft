package com.staffs.leavebooking.leavemanagement.application.mappers;

import com.staffs.leavebooking.leavemanagement.application.dto.LeaveAllowanceDTO;
import com.staffs.leavebooking.leavemanagement.infrastructure.entities.LeaveAllowanceJpa;

import java.util.Objects;

/**
 * Data Mapper (Lecture 4 — Layered Architecture / Anti-Corruption Layer) that converts
 * a {@link LeaveAllowanceJpa} infrastructure entity directly into a {@link LeaveAllowanceDTO}
 * for API responses.
 *
 * <p><strong>Direction:</strong> JPA → DTO (query / read path).
 *
 * <p><strong>When used:</strong> Called by the query handler when serving read requests
 * (e.g. "get leave allowance for a staff member"). This mapper is part of the CQRS
 * read-side optimisation — it bypasses the domain aggregate entirely and maps straight
 * from the JPA entity to the DTO, avoiding the overhead of reconstituting a full aggregate
 * when no domain logic needs to execute.
 *
 * <p><strong>Calculated / derived fields:</strong> Unlike the LeaveRequest DTO mapper,
 * this mapper computes additional derived fields at mapping time that do not exist as
 * stored columns in the database:
 * <ul>
 *   <li>{@code remainingDays} = totalEntitlement − daysUsed (days left ignoring pending)</li>
 *   <li>{@code availableDays} = totalEntitlement − daysUsed − daysPending (days actually bookable)</li>
 *   <li>{@code staffName} = firstName + " " + surname (concatenated for display convenience)</li>
 *   <li>{@code businessYear} = startYear + "-" + endYear (formatted as a single string, e.g. "2024-2025")</li>
 * </ul>
 *
 * <p><strong>Why calculate here rather than in the domain?</strong> The domain aggregate
 * does have {@code remainingDays()} and {@code availableDays()} methods, but since this
 * mapper intentionally bypasses the domain (CQRS read-side), it replicates those simple
 * calculations directly from the JPA entity's raw values.
 *
 * <p><strong>Mapper chain (read path):</strong>
 * {@code JpaRepository.findXxx() → LeaveAllowanceJpa → [this mapper] → LeaveAllowanceDTO → JSON}
 *
 * @see LeaveAllowanceJpa                      the source JPA entity (mapped to leave_allowance table)
 * @see LeaveAllowanceDTO                      the target DTO record returned as JSON
 * @see LeaveAllowanceJpaToDomainMapper        the command-path mapper (JPA → Domain) — used when
 *                                             domain logic is required
 * @see LeaveAllowanceDomainToJpaMapper        the write-path mapper (Domain → JPA)
 */
public class LeaveAllowanceJpaToDTOMapper {

    /**
     * Converts a {@link LeaveAllowanceJpa} entity into a {@link LeaveAllowanceDTO} record
     * for direct serialisation to JSON on the read path.
     *
     * <p>In addition to copying stored fields, this method calculates four derived values
     * ({@code remainingDays}, {@code availableDays}, {@code staffName}, {@code businessYear})
     * that provide convenient, pre-computed data for the UI layer.
     *
     * @param jpa the JPA entity loaded from the database; must not be {@code null}
     * @return a {@link LeaveAllowanceDTO} containing all leave allowance data plus derived
     *         fields for the API response
     * @throws NullPointerException if {@code jpa} is {@code null}
     */
    public static LeaveAllowanceDTO toDTO(LeaveAllowanceJpa jpa) {
        Objects.requireNonNull(jpa, "LeaveAllowance JPA entity cannot be null");

        // Derived field: remaining days = entitlement minus confirmed used days.
        // Mirrors LeaveAllowance.remainingDays() but calculated from JPA fields directly
        // to avoid reconstituting the domain aggregate on the read path.
        int remaining = jpa.getTotalEntitlement() - jpa.getDaysUsed();

        // Derived field: available days = entitlement minus used minus pending.
        // This is the number of days the staff member can still request.
        // Mirrors LeaveAllowance.availableDays() calculated from JPA fields directly.
        int available = jpa.getTotalEntitlement() - jpa.getDaysUsed() - jpa.getDaysPending();

        // Derived field: business year formatted as a readable string (e.g. "2024-2025")
        // by concatenating the two separate Integer columns from the JPA entity.
        String businessYear = jpa.getBusinessYearStart() + "-" + jpa.getBusinessYearEnd();

        // Derived field: full staff name concatenated from first name and surname
        // for display convenience in the UI, avoiding a separate lookup to Staff Management.
        String staffName = jpa.getFirstName() + " " + jpa.getSurname();

        return new LeaveAllowanceDTO(
                // String → String: aggregate identity UUID
                jpa.getId(),

                // String → String: the staff member's UUID
                jpa.getStaffMemberId(),

                // String (derived): concatenated full name for display (e.g. "John Smith")
                staffName,

                // String → String: the assigned manager's UUID
                jpa.getManagerId(),

                // String → String: department name (denormalised from Staff Management)
                jpa.getDepartment(),

                // String (derived): formatted business year string (e.g. "2024-2025")
                businessYear,

                // int → int: total annual leave entitlement in days
                jpa.getTotalEntitlement(),

                // int → int: number of leave days already used (confirmed/approved)
                jpa.getDaysUsed(),

                // int → int: number of leave days reserved but not yet approved
                jpa.getDaysPending(),

                // int (derived): remaining days = entitlement − used (ignores pending)
                remaining,

                // int (derived): available days = entitlement − used − pending (bookable balance)
                available
        );
    }
}
