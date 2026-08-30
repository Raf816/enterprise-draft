package com.staffs.leavebooking.leavemanagement.application.dto;

import java.time.LocalDate;

/**
 * Data Transfer Object (DTO) record for Leave Request — returned to API consumers as JSON
 * (Lecture 5/6 — CQRS Read Model).
 *
 * <p><strong>CQRS Read Model:</strong> This DTO is the read-side representation of a
 * leave request. It is a flat, immutable projection of the data stored in
 * {@link com.staffs.leavebooking.leavemanagement.infrastructure.entities.LeaveRequestJpa}.
 * It contains no business logic — only the data needed for display by the frontend.
 *
 * <p><strong>Mapping:</strong> Created by
 * {@link com.staffs.leavebooking.leavemanagement.application.mappers.LeaveRequestJpaToDTOMapper},
 * which maps JPA entity fields directly to this record's components. The read path
 * bypasses the domain aggregate entirely (JPA → Mapper → DTO), which is a CQRS
 * optimisation — there's no need to reconstitute the full aggregate for queries.
 *
 * <p><strong>14 fields:</strong> This record carries all columns from the leave_request
 * table, including nullable decision-related fields that are only populated after a
 * manager acts on the request (decidedOn, decidedBy, decisionReason) and the
 * cancellationReason that's only populated on cancellation.
 *
 * <p><strong>Java record:</strong> Records are immutable data carriers (since Java 16).
 * The compiler generates the constructor, getters, equals(), hashCode(), and toString().
 * Jackson serialises record components directly to JSON fields.
 *
 * @param id                 the UUID of the leave request (primary key)
 * @param staffMemberId      the UUID of the staff member who submitted this request
 * @param managerId          the UUID of the manager assigned to approve/reject this request
 * @param leaveType          the type of leave (e.g., "ANNUAL") — stored as a string
 * @param startDate          the first day of the leave period
 * @param endDate            the last day of the leave period
 * @param numberOfDays       the calculated number of leave days in the period
 * @param reason             the optional reason provided when the request was submitted (may be null)
 * @param status             the current status: PENDING, APPROVED, REJECTED, or CANCELLED
 * @param submittedOn        the date the request was submitted
 * @param decidedOn          the date the request was approved/rejected (null if still PENDING)
 * @param decidedBy          the UUID of the manager/admin who approved/rejected (null if still PENDING)
 * @param decisionReason     the optional reason given when approving/rejecting (may be null)
 * @param cancellationReason the optional reason given when cancelling (may be null)
 * @see com.staffs.leavebooking.leavemanagement.application.mappers.LeaveRequestJpaToDTOMapper for the JPA → DTO mapping logic
 * @see com.staffs.leavebooking.leavemanagement.infrastructure.entities.LeaveRequestJpa for the JPA entity this DTO maps from
 * @see com.staffs.leavebooking.leavemanagement.application.handlers.LeaveRequestQueryHandler for the query handler that returns this DTO
 */
public record LeaveRequestDTO(
        String id,                  // UUID primary key — generated when the request is submitted
        String staffMemberId,       // UUID of the submitter — matches the Firebase UID
        String managerId,           // UUID of the assigned manager — determines who can approve/reject
        String leaveType,           // Leave type as string (e.g., "ANNUAL") — decoupled from the enum
        LocalDate startDate,        // First day of leave — validated as today or future at submission
        LocalDate endDate,          // Last day of leave — validated as >= startDate by the domain
        int numberOfDays,           // Calculated business days between startDate and endDate (inclusive)
        String reason,              // Optional reason provided at submission — nullable
        String status,              // Current lifecycle status: PENDING → APPROVED/REJECTED → CANCELLED
        LocalDate submittedOn,      // Date the request was created — set automatically at submission
        LocalDate decidedOn,        // Date of approval/rejection — null while PENDING
        String decidedBy,           // UUID of the approver/rejector — null while PENDING
        String decisionReason,      // Optional reason for the approval/rejection decision — nullable
        String cancellationReason   // Optional reason for cancellation — nullable, only set on CANCELLED
) {
}
