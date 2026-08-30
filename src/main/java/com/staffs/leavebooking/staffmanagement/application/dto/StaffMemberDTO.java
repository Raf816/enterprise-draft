package com.staffs.leavebooking.staffmanagement.application.dto;

import java.time.LocalDate;

/**
 * Data Transfer Object for staff member data returned by the API
 * (Lecture 4 — DTOs, Decoupling Domain from Presentation).
 *
 * <p><strong>Why a DTO?</strong> The domain aggregate ({@code StaffMember}) contains
 * domain logic, validation, and events — it should not be exposed directly to API consumers.
 * This flat DTO record contains only the data fields needed for the API response,
 * with no domain behaviour or framework annotations.
 *
 * <p><strong>Mapper chain:</strong>
 * <pre>
 * StaffMember (domain) → StaffMemberDomainToJpaMapper → StaffMemberJpa (database)
 * StaffMemberJpa (database) → StaffMemberJpaToDTOMapper → StaffMemberDTO (API response)
 * </pre>
 *
 * <p><strong>All fields are strings/primitives:</strong> This ensures the DTO serialises
 * cleanly to JSON without needing custom serialisers. Enums (EmploymentType, EmploymentStatus)
 * are represented as their string names.
 *
 * @param id                     the staff member's UUID (= Firebase UID)
 * @param firstName              first name
 * @param surname                surname
 * @param email                  email address
 * @param department             department name
 * @param lineManagerId          line manager's UUID (may be null)
 * @param hireDate               date hired
 * @param currentRole            current job title
 * @param startDateOfCurrentRole when the current role started
 * @param jobLevel               seniority level (may be null)
 * @param employmentType         contract type as string (FULL_TIME, PART_TIME, CONTRACT)
 * @param employmentStatus       lifecycle status as string (PENDING_SETUP, ACTIVE, etc.)
 */
public record StaffMemberDTO(
        String id,                          // Staff record ID (= Firebase UID)
        String firstName,                   // First name
        String surname,                     // Surname
        String email,                       // Email address
        String department,                  // Department name
        String lineManagerId,               // Line manager UUID (nullable)
        LocalDate hireDate,                 // Date hired
        String currentRole,                 // Job title
        LocalDate startDateOfCurrentRole,   // When current role started
        String jobLevel,                    // Seniority level (nullable)
        String employmentType,              // Contract type as string
        String employmentStatus             // Lifecycle status as string
) {
}
