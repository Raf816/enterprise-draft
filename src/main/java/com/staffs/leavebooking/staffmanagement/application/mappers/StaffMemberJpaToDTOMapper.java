package com.staffs.leavebooking.staffmanagement.application.mappers;

import com.staffs.leavebooking.staffmanagement.application.dto.StaffMemberDTO;
import com.staffs.leavebooking.staffmanagement.infrastructure.entities.StaffMemberJpa;

import java.util.Objects;

/**
 * Mapper: JPA entity → DTO (query/read path)
 * (Lecture 4 — Data Mapper Pattern, Lecture 5 — CQRS Queries).
 *
 * <p><strong>Purpose:</strong> Converts a {@link StaffMemberJpa} JPA entity directly
 * into a {@link StaffMemberDTO} for API responses. This is the READ path — it
 * bypasses the domain aggregate entirely because queries don't need business logic.
 *
 * <p><strong>CQRS optimisation:</strong> On the read path, we skip the domain aggregate
 * and map JPA → DTO directly. This is more efficient because:
 * <ul>
 *   <li>No value object construction (FullName, Email) needed for display</li>
 *   <li>No domain validation runs (the data is already valid — it was validated on write)</li>
 *   <li>No aggregate event infrastructure loaded</li>
 * </ul>
 *
 * <p><strong>Mapper chain:</strong>
 * <pre>
 * WRITE: Controller → Command → Facade → Service → Aggregate → DomainToJpa → repo.save()
 * READ:  Controller → Facade → QueryHandler → repo.findX() → THIS MAPPER → DTO → JSON
 * </pre>
 *
 * @see StaffMemberJpaToDomainMapper for the write/command path (JPA → domain)
 */
public class StaffMemberJpaToDTOMapper {

    /**
     * Converts a JPA entity into a flat DTO for API responses.
     * Simple field-by-field mapping — no domain logic involved.
     *
     * @param jpa the JPA entity loaded from the database (must not be null)
     * @return a StaffMemberDTO with all fields mapped from the JPA entity
     * @throws NullPointerException if jpa is null
     */
    public static StaffMemberDTO toDTO(StaffMemberJpa jpa) {
        Objects.requireNonNull(jpa, "StaffMember JPA entity cannot be null");

        // Direct field-by-field mapping — no transformation needed
        return new StaffMemberDTO(
                jpa.getId(),                    // UUID string
                jpa.getFirstName(),             // First name string
                jpa.getSurname(),               // Surname string
                jpa.getEmail(),                 // Email string
                jpa.getDepartment(),            // Department string
                jpa.getLineManagerId(),         // Manager UUID (nullable)
                jpa.getHireDate(),              // LocalDate
                jpa.getCurrentRole(),           // Job title string
                jpa.getStartDateCurrentRole(),  // LocalDate
                jpa.getJobLevel(),              // Level string (nullable)
                jpa.getEmploymentType(),        // Enum as string (e.g., "FULL_TIME")
                jpa.getEmploymentStatus()       // Enum as string (e.g., "ACTIVE")
        );
    }
}
