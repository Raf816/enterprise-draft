package com.staffs.leavebooking.staffmanagement.application.mappers;

import com.staffs.leavebooking.staffmanagement.domain.StaffMember;
import com.staffs.leavebooking.staffmanagement.infrastructure.entities.StaffMemberJpa;

import java.util.Objects;

/**
 * Mapper: Domain aggregate → JPA entity (write path)
 * (Lecture 4 — Data Mapper Pattern, Decoupling Domain from Infrastructure).
 *
 * <p><strong>Purpose:</strong> Converts a {@link StaffMember} domain aggregate into a
 * {@link StaffMemberJpa} JPA entity for persistence. This mapper is used on the
 * WRITE path — after the aggregate processes a command, the updated state is
 * mapped to a JPA entity and saved to the database.
 *
 * <p><strong>Why separate mapper?</strong> The domain aggregate should NOT know about
 * JPA annotations or database column names. The mapper bridges the gap between
 * the domain layer (pure business logic) and the infrastructure layer (JPA/database).
 *
 * <p><strong>Mapper chain:</strong>
 * <pre>
 * WRITE PATH: StaffMember (domain) → THIS MAPPER → StaffMemberJpa → repository.save()
 * READ PATH:  repository.findById() → StaffMemberJpa → JpaToDomainMapper → StaffMember
 * DTO PATH:   repository.findById() → StaffMemberJpa → JpaToDTOMapper → StaffMemberDTO
 * </pre>
 */
public class StaffMemberDomainToJpaMapper {

    /**
     * Converts a StaffMember domain aggregate into a StaffMemberJpa entity.
     * Maps all domain fields to their JPA column equivalents.
     *
     * <p><strong>Key conversions:</strong>
     * <ul>
     *   <li>{@code Identity<StaffMember>} → String id (via {@code .id().id()})</li>
     *   <li>{@code FullName} → separate firstName and surname strings</li>
     *   <li>{@code Email} → String email (via {@code .address()})</li>
     *   <li>{@code EmploymentType} enum → String (via {@code .name()})</li>
     *   <li>{@code EmploymentStatus} enum → String (via {@code .name()})</li>
     * </ul>
     *
     * @param domain the domain aggregate to convert (must not be null)
     * @return the JPA entity ready for persistence
     * @throws NullPointerException if domain is null
     */
    public static StaffMemberJpa toJpa(StaffMember domain) {
        Objects.requireNonNull(domain, "StaffMember domain entity cannot be null");

        StaffMemberJpa jpa = new StaffMemberJpa();
        // Map Identity<StaffMember> → String id
        jpa.setId(domain.id().id());
        // Map FullName value object → separate first name and surname strings
        jpa.setFirstName(domain.fullName().firstName());
        jpa.setSurname(domain.fullName().surname());
        // Map Email value object → string address
        jpa.setEmail(domain.email().address());
        // Map plain string fields directly
        jpa.setDepartment(domain.department());
        jpa.setLineManagerId(domain.lineManagerId());
        jpa.setHireDate(domain.hireDate());
        jpa.setCurrentRole(domain.currentRole());
        jpa.setStartDateCurrentRole(domain.startDateOfCurrentRole());
        jpa.setJobLevel(domain.jobLevel());
        // Map enums → their string names for database storage
        jpa.setEmploymentType(domain.employmentType().name());       // e.g., "FULL_TIME"
        jpa.setEmploymentStatus(domain.employmentStatus().name());   // e.g., "ACTIVE"
        return jpa;
    }
}
