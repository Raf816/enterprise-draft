package com.staffs.leavebooking.staffmanagement.application.commands;

import java.time.LocalDate;

/**
 * CQRS Command record for updating a staff member's placement details
 * (job role, level, employment type) (Lecture 6 — CQRS Commands).
 *
 * <p><strong>No event raised:</strong> Placement changes do not trigger cross-context
 * events because the Leave Management context doesn't need job role or employment type
 * data. Only department/manager changes trigger events (via UpdateDepartmentCommand).
 *
 * <p><strong>Supports partial updates:</strong> Null fields retain current values.
 *
 * @param staffMemberId          the UUID of the staff member to update
 * @param currentRole            the new job title (null = keep current)
 * @param startDateOfCurrentRole when the new role started (null = keep current)
 * @param jobLevel               the new seniority level (null = keep current)
 * @param employmentType         the new contract type (null = keep current)
 */
public record UpdatePlacementCommand(
        String staffMemberId,               // Which staff member to update
        String currentRole,                 // New job title (null = no change)
        LocalDate startDateOfCurrentRole,   // When the new role started (null = no change)
        String jobLevel,                    // New seniority level (null = no change)
        String employmentType               // New contract type (null = no change)
) {
}
