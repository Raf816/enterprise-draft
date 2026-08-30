package com.staffs.leavebooking.leavemanagement.application.mappers;

import com.staffs.leavebooking.leavemanagement.application.dto.LeaveRequestDTO;
import com.staffs.leavebooking.leavemanagement.infrastructure.entities.LeaveRequestJpa;

import java.util.Objects;

/**
 * Data Mapper (Lecture 4 — Layered Architecture / Anti-Corruption Layer) that converts
 * a {@link LeaveRequestJpa} infrastructure entity directly into a {@link LeaveRequestDTO}
 * for API responses.
 *
 * <p><strong>Direction:</strong> JPA → DTO (query / read path).
 *
 * <p><strong>When used:</strong> Called by the query handler when serving read requests
 * (e.g. "get leave request by ID", "list all leave requests for a staff member"). This
 * mapper is part of the CQRS read-side optimisation — it bypasses the domain aggregate
 * entirely and maps straight from the JPA entity to the DTO, avoiding the overhead of
 * reconstituting a full aggregate when no domain logic needs to execute.
 *
 * <p><strong>Why bypass the domain?</strong> On the read path there are no invariants to
 * enforce, no state transitions to validate, and no domain events to raise. Going directly
 * from JPA → DTO is simpler and more efficient. The domain aggregate is only needed on the
 * command (write) path where business rules must be checked.
 *
 * <p><strong>Key conversions:</strong> None — all fields in {@link LeaveRequestJpa} are
 * already stored as the primitive/String types that the DTO expects. This mapper is a
 * straight field-to-field copy with no type transformations.
 *
 * <p><strong>Mapper chain (read path):</strong>
 * {@code JpaRepository.findXxx() → LeaveRequestJpa → [this mapper] → LeaveRequestDTO → JSON}
 *
 * @see LeaveRequestJpa                       the source JPA entity (mapped to leave_request table)
 * @see LeaveRequestDTO                       the target DTO record returned as JSON
 * @see LeaveRequestJpaToDomainMapper         the command-path mapper (JPA → Domain) — used when
 *                                            domain logic is required
 * @see LeaveRequestDomainToJpaMapper         the write-path mapper (Domain → JPA)
 */
public class LeaveRequestJpaToDTOMapper {

    /**
     * Converts a {@link LeaveRequestJpa} entity into a {@link LeaveRequestDTO} record
     * for direct serialisation to JSON on the read path.
     *
     * <p>This is a flat, field-for-field copy — no value-object unwrapping or enum
     * conversion is needed because the JPA entity already stores all values as the
     * primitive types the DTO expects.
     *
     * @param jpa the JPA entity loaded from the database; must not be {@code null}
     * @return a {@link LeaveRequestDTO} containing all leave request data for the API response
     * @throws NullPointerException if {@code jpa} is {@code null}
     */
    public static LeaveRequestDTO toDTO(LeaveRequestJpa jpa) {
        Objects.requireNonNull(jpa, "LeaveRequest JPA entity cannot be null");

        return new LeaveRequestDTO(
                // String → String: aggregate identity UUID
                jpa.getId(),

                // String → String: the requesting staff member's UUID
                jpa.getStaffMemberId(),

                // String → String: the assigned manager's UUID
                jpa.getManagerId(),

                // String → String: leave type stored as its enum name (e.g. "ANNUAL")
                jpa.getLeaveType(),

                // LocalDate → LocalDate: start of the requested leave period
                jpa.getStartDate(),

                // LocalDate → LocalDate: end of the requested leave period
                jpa.getEndDate(),

                // int → int: number of working days in the leave period
                jpa.getNumberOfDays(),

                // String → String (nullable): optional reason given by the requester
                jpa.getReason(),

                // String → String: current status stored as its enum name (e.g. "PENDING")
                jpa.getStatus(),

                // LocalDate → LocalDate: the date the request was submitted
                jpa.getSubmittedOn(),

                // LocalDate → LocalDate (nullable): date of approval/rejection, if decided
                jpa.getDecidedOn(),

                // String → String (nullable): UUID of the manager who decided, if applicable
                jpa.getDecidedBy(),

                // String → String (nullable): reason given for the approval/rejection
                jpa.getDecisionReason(),

                // String → String (nullable): reason given for the cancellation, if cancelled
                jpa.getCancellationReason()
        );
    }
}
