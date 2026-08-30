package com.staffs.leavebooking.leavemanagement.application.dto;

/**
 * Data Transfer Object (DTO) record for Leave Allowance — returned to API consumers as JSON
 * (Lecture 5/6 — CQRS Read Model).
 *
 * <p><strong>CQRS Read Model:</strong> This DTO is the read-side representation of a
 * staff member's annual leave balance. It is a flat, immutable projection of the data
 * stored in {@link com.staffs.leavebooking.leavemanagement.infrastructure.entities.LeaveAllowanceJpa},
 * enriched with two derived fields ({@code remainingDays}, {@code availableDays}) that
 * are calculated at mapping time rather than stored in the database.
 *
 * <p><strong>Derived fields:</strong>
 * <ul>
 *   <li>{@code remainingDays} = totalEntitlement - daysUsed (ignores pending days;
 *       shows how many days the staff member has left if all pending requests are rejected)</li>
 *   <li>{@code availableDays} = totalEntitlement - daysUsed - daysPending (includes pending days;
 *       shows how many additional days the staff member can book right now)</li>
 * </ul>
 *
 * <p><strong>Mapping:</strong> Created by
 * {@link com.staffs.leavebooking.leavemanagement.application.mappers.LeaveAllowanceJpaToDTOMapper},
 * which maps JPA entity fields and calculates the derived fields.
 *
 * <p><strong>Denormalised staff details:</strong> The {@code staffName}, {@code managerId},
 * and {@code department} fields are snapshots synced from Staff Management via RabbitMQ
 * events. They are denormalised onto the allowance to avoid cross-context queries when
 * displaying allowance data.
 *
 * @param id               the UUID of the leave allowance record (primary key)
 * @param staffMemberId    the UUID of the staff member this allowance belongs to
 * @param staffName        the staff member's full name (denormalised: firstName + " " + surname)
 * @param managerId        the UUID of the staff member's line manager (denormalised from Staff Management)
 * @param department       the staff member's department (denormalised from Staff Management)
 * @param businessYear     the business year this allowance covers (e.g., "2025/2026")
 * @param totalEntitlement the total number of leave days the staff member is entitled to for this year
 * @param daysUsed         the number of leave days already used (from APPROVED requests)
 * @param daysPending      the number of leave days currently reserved by PENDING requests
 * @param remainingDays    derived: totalEntitlement - daysUsed (days remaining if all pending are rejected)
 * @param availableDays    derived: totalEntitlement - daysUsed - daysPending (days available to book now)
 * @see com.staffs.leavebooking.leavemanagement.application.mappers.LeaveAllowanceJpaToDTOMapper for the JPA → DTO mapping logic
 * @see com.staffs.leavebooking.leavemanagement.infrastructure.entities.LeaveAllowanceJpa for the JPA entity this DTO maps from
 * @see com.staffs.leavebooking.leavemanagement.application.handlers.LeaveAllowanceQueryHandler for the query handler that returns this DTO
 */
public record LeaveAllowanceDTO(
        String id,              // UUID primary key — generated when the allowance is created
        String staffMemberId,   // UUID of the staff member — matches the Firebase UID
        String staffName,       // Full name (firstName + surname) — derived at mapping time
        String managerId,       // UUID of the line manager — denormalised from Staff Management
        String department,      // Department name — denormalised from Staff Management
        String businessYear,    // Formatted business year string (e.g., "2025/2026")
        int totalEntitlement,   // Total annual entitlement in days — set at creation, amendable by admin
        int daysUsed,           // Days consumed by APPROVED requests — incremented on approval
        int daysPending,        // Days reserved by PENDING requests — incremented on submission, decremented on decision
        int remainingDays,      // Derived: totalEntitlement - daysUsed (optimistic remaining)
        int availableDays       // Derived: totalEntitlement - daysUsed - daysPending (available to book now)
) {
}
