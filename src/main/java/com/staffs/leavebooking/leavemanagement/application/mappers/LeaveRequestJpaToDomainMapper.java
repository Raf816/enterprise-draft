package com.staffs.leavebooking.leavemanagement.application.mappers;

import com.staffs.leavebooking.common.domain.Identity;
import com.staffs.leavebooking.leavemanagement.domain.*;
import com.staffs.leavebooking.leavemanagement.infrastructure.entities.LeaveRequestJpa;

import java.util.Objects;

/**
 * Data Mapper (Lecture 4 — Layered Architecture / Anti-Corruption Layer) that converts
 * a {@link LeaveRequestJpa} infrastructure entity back into a {@link LeaveRequest} domain aggregate.
 *
 * <p><strong>Direction:</strong> JPA → Domain (command loading path).
 *
 * <p><strong>When used:</strong> Called by the application service when an existing leave request
 * is loaded from the database for command processing (e.g. approve, reject, cancel). The mapper
 * rebuilds the full domain aggregate so that domain invariants and state-machine rules can be
 * enforced before persisting any changes.
 *
 * <p><strong>Important:</strong> This mapper uses {@link LeaveRequest#reconstitute} rather than
 * {@link LeaveRequest#submitNew}. The {@code reconstitute} factory method rebuilds the aggregate
 * without raising domain events and without re-validating creation-time rules (such as
 * future-start-date checks). This is the correct choice when hydrating from persistence because
 * the aggregate already passed validation at creation time.
 *
 * <p><strong>Key conversions performed (reverse of DomainToJpa):</strong>
 * <ul>
 *   <li>{@code String} → {@code Identity<LeaveRequest>} (wraps the UUID string via {@code Identity.of()})</li>
 *   <li>{@code String} → {@code LeaveType} enum (via {@code LeaveType.valueOf()})</li>
 *   <li>Separate {@code startDate} / {@code endDate} → {@code DateRange} value object</li>
 *   <li>{@code String} → {@code LeaveRequestStatus} enum (via {@code LeaveRequestStatus.valueOf()})</li>
 * </ul>
 *
 * <p><strong>Parameter count:</strong> The {@code reconstitute} factory accepts 13 parameters
 * matching every field of the aggregate. This is intentional — it ensures a complete, consistent
 * aggregate is rebuilt from persistence without partial state.
 *
 * <p><strong>Mapper chain (command loading path):</strong>
 * {@code JpaRepository.findById() → LeaveRequestJpa → [this mapper] → LeaveRequest}
 *
 * @see LeaveRequestJpa                       the source JPA entity (mapped to leave_request table)
 * @see LeaveRequest                          the target domain aggregate
 * @see LeaveRequest#reconstitute             the factory method used for persistence hydration
 * @see LeaveRequestDomainToJpaMapper         the reverse mapper (Domain → JPA) for the write path
 * @see LeaveRequestJpaToDTOMapper            the read-path mapper (JPA → DTO) for queries
 */
public class LeaveRequestJpaToDomainMapper {

    /**
     * Converts a {@link LeaveRequestJpa} entity into a fully hydrated {@link LeaveRequest}
     * domain aggregate using the {@code reconstitute} factory method.
     *
     * <p>All 13 parameters of the aggregate are mapped from the JPA entity, converting
     * flat database columns back into domain value objects and enums.
     *
     * @param jpa the JPA entity loaded from the database; must not be {@code null}
     * @return a reconstituted {@link LeaveRequest} aggregate with no pending domain events
     * @throws NullPointerException     if {@code jpa} is {@code null}
     * @throws IllegalArgumentException if the stored leaveType or status string does not
     *                                  match any enum constant (indicates data corruption)
     */
    public static LeaveRequest toDomain(LeaveRequestJpa jpa) {
        Objects.requireNonNull(jpa, "LeaveRequest JPA entity cannot be null");

        return LeaveRequest.reconstitute(
                // String → Identity<LeaveRequest>: wrap the plain UUID string from the @Id
                // column back into the typed Identity value object for compile-time safety
                Identity.of(jpa.getId()),

                // String → String: staff member UUID, passed through unchanged
                jpa.getStaffMemberId(),

                // String → String: manager UUID, passed through unchanged
                jpa.getManagerId(),

                // String → LeaveType enum: convert the stored name (e.g. "ANNUAL") back
                // into the domain enum; valueOf() will throw if the value is unrecognised
                LeaveType.valueOf(jpa.getLeaveType()),

                // LocalDate + LocalDate → DateRange value object: recompose the two
                // separate date columns back into the single immutable value object
                // that encapsulates date-range invariants (start <= end, working-day calculation)
                new DateRange(jpa.getStartDate(), jpa.getEndDate()),

                // int → int: pre-calculated working days, passed through unchanged
                jpa.getNumberOfDays(),

                // String → String: optional free-text reason, passed through unchanged
                jpa.getReason(),

                // String → LeaveRequestStatus enum: convert the stored status name
                // (e.g. "PENDING", "APPROVED") back into the domain enum for state-machine logic
                LeaveRequestStatus.valueOf(jpa.getStatus()),

                // LocalDate → LocalDate: the original submission date
                jpa.getSubmittedOn(),

                // LocalDate → LocalDate (nullable): the date a decision was made, if any
                jpa.getDecidedOn(),

                // String → String (nullable): UUID of the decision maker, if any
                jpa.getDecidedBy(),

                // String → String (nullable): reason given for the approval/rejection
                jpa.getDecisionReason(),

                // String → String (nullable): reason given for cancellation
                jpa.getCancellationReason()
        );
    }
}
