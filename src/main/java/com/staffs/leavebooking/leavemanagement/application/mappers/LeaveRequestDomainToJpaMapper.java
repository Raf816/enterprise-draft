package com.staffs.leavebooking.leavemanagement.application.mappers;

import com.staffs.leavebooking.leavemanagement.domain.LeaveRequest;
import com.staffs.leavebooking.leavemanagement.infrastructure.entities.LeaveRequestJpa;

import java.util.Objects;

/**
 * Data Mapper (Lecture 4 — Layered Architecture / Anti-Corruption Layer) that converts
 * a {@link LeaveRequest} domain aggregate into a {@link LeaveRequestJpa} infrastructure entity.
 *
 * <p><strong>Direction:</strong> Domain → JPA (write path).
 *
 * <p><strong>When used:</strong> Called by the application service after the aggregate has
 * been created or mutated and needs to be persisted via the JPA repository. This mapper
 * sits at the boundary between the domain layer and the infrastructure layer, ensuring
 * the domain model never leaks persistence concerns.
 *
 * <p><strong>Key conversions performed:</strong>
 * <ul>
 *   <li>{@code Identity<LeaveRequest>} → {@code String} (unwraps the UUID wrapper via {@code id().id()})</li>
 *   <li>{@code DateRange} value object → separate {@code startDate} / {@code endDate} fields</li>
 *   <li>{@code LeaveType} enum → {@code String} (via {@code .name()})</li>
 *   <li>{@code LeaveRequestStatus} enum → {@code String} (via {@code .name()})</li>
 * </ul>
 *
 * <p><strong>Mapper chain (write path):</strong>
 * {@code LeaveRequest → [this mapper] → LeaveRequestJpa → JpaRepository.save()}
 *
 * @see LeaveRequest                          the source domain aggregate
 * @see LeaveRequestJpa                       the target JPA entity (mapped to leave_request table)
 * @see LeaveRequestJpaToDomainMapper         the reverse mapper (JPA → Domain) for command loading
 * @see LeaveRequestJpaToDTOMapper            the read-path mapper (JPA → DTO) for queries
 */
public class LeaveRequestDomainToJpaMapper {

    /**
     * Converts a {@link LeaveRequest} domain aggregate into a {@link LeaveRequestJpa} entity
     * suitable for persistence via JPA.
     *
     * <p>Creates a new JPA entity instance and copies every field from the domain aggregate,
     * performing type conversions where the domain model uses value objects or enums that
     * the JPA layer stores as primitive/String columns.
     *
     * @param domain the domain aggregate to convert; must not be {@code null}
     * @return a fully populated {@link LeaveRequestJpa} entity ready for
     *         {@code JpaRepository.save()}
     * @throws NullPointerException if {@code domain} is {@code null}
     */
    public static LeaveRequestJpa toJpa(LeaveRequest domain) {
        Objects.requireNonNull(domain, "LeaveRequest domain entity cannot be null");

        LeaveRequestJpa jpa = new LeaveRequestJpa();

        // Identity<LeaveRequest> → String: unwrap the typed UUID wrapper to a plain String
        // for the JPA @Id column (VARCHAR(36))
        jpa.setId(domain.id().id());

        // String → String: staff member UUID, no conversion needed
        jpa.setStaffMemberId(domain.staffMemberId());

        // String → String: manager UUID, no conversion needed
        jpa.setManagerId(domain.managerId());

        // LeaveType enum → String: persist the enum constant name (e.g. "ANNUAL", "SICK")
        // so the database stores a human-readable string rather than an ordinal
        jpa.setLeaveType(domain.leaveType().name());

        // DateRange value object → LocalDate: extract the start date from the composite
        // value object; the JPA entity stores start and end as separate date columns
        jpa.setStartDate(domain.dateRange().startDate());

        // DateRange value object → LocalDate: extract the end date from the composite
        // value object; paired with startDate above to flatten the value object
        jpa.setEndDate(domain.dateRange().endDate());

        // int → int: pre-calculated working days within the date range; stored directly
        jpa.setNumberOfDays(domain.numberOfDays());

        // String → String: optional free-text reason provided by the requester
        jpa.setReason(domain.reason());

        // LeaveRequestStatus enum → String: persist the status name (e.g. "PENDING",
        // "APPROVED", "REJECTED", "CANCELLED") as a readable string column
        jpa.setStatus(domain.status().name());

        // LocalDate → LocalDate: the date the request was originally submitted
        jpa.setSubmittedOn(domain.submittedOn());

        // LocalDate → LocalDate (nullable): the date a decision was made; null if still PENDING
        jpa.setDecidedOn(domain.decidedOn());

        // String → String (nullable): UUID of the manager who approved/rejected; null if PENDING
        jpa.setDecidedBy(domain.decidedBy());

        // String → String (nullable): optional reason given by the decision maker
        jpa.setDecisionReason(domain.decisionReason());

        // String → String (nullable): optional reason given when the request was cancelled
        jpa.setCancellationReason(domain.cancellationReason());

        return jpa;
    }
}
