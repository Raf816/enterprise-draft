package com.staffs.leavebooking.staffmanagement.application.mappers;

import com.staffs.leavebooking.common.domain.Email;
import com.staffs.leavebooking.common.domain.FullName;
import com.staffs.leavebooking.common.domain.Identity;
import com.staffs.leavebooking.staffmanagement.domain.EmploymentStatus;
import com.staffs.leavebooking.staffmanagement.domain.EmploymentType;
import com.staffs.leavebooking.staffmanagement.domain.StaffMember;
import com.staffs.leavebooking.staffmanagement.infrastructure.entities.StaffMemberJpa;

import java.util.Objects;

/**
 * Mapper: JPA entity → Domain aggregate (read/command path)
 * (Lecture 4 — Data Mapper Pattern, Lecture 7 — Reconstitution vs Creation).
 *
 * <p><strong>Purpose:</strong> Converts a {@link StaffMemberJpa} JPA entity back into
 * a {@link StaffMember} domain aggregate. This is used when the application service
 * needs to load an existing aggregate to execute a command (e.g., update department).
 *
 * <p><strong>Uses reconstitute():</strong> Calls the aggregate's {@code reconstitute()}
 * factory method (not {@code createNew()}). The difference is critical:
 * <ul>
 *   <li>{@code createNew()} — validates business rules, raises events (write path for NEW aggregates)</li>
 *   <li>{@code reconstitute()} — skips creation-time validation, no events (read path for EXISTING data)</li>
 * </ul>
 * This is the split factory method pattern from Lecture 7.
 *
 * @see StaffMemberDomainToJpaMapper for the reverse direction (domain → JPA)
 * @see StaffMemberJpaToDTOMapper for the query path (JPA → DTO)
 */
public class StaffMemberJpaToDomainMapper {

    /**
     * Converts a JPA entity into a domain aggregate via reconstitute().
     *
     * <p><strong>Key conversions:</strong>
     * <ul>
     *   <li>String id → {@code Identity.of(id)} (wraps in typed Identity)</li>
     *   <li>String firstName + surname → {@code new FullName(first, surname)} (value object)</li>
     *   <li>String email → {@code new Email(email)} (validated value object)</li>
     *   <li>String employmentType → {@code EmploymentType.valueOf(type)} (string → enum)</li>
     *   <li>String employmentStatus → {@code EmploymentStatus.valueOf(status)} (string → enum)</li>
     * </ul>
     *
     * @param jpa the JPA entity loaded from the database (must not be null)
     * @return the reconstituted domain aggregate
     * @throws NullPointerException if jpa is null
     */
    public static StaffMember toDomain(StaffMemberJpa jpa) {
        Objects.requireNonNull(jpa, "StaffMember JPA entity cannot be null");

        // Use reconstitute() — not createNew() — because this data already exists
        // reconstitute() does NOT raise events or validate creation-time rules
        return StaffMember.reconstitute(
                Identity.of(jpa.getId()),                                    // String → Identity<StaffMember>
                new FullName(jpa.getFirstName(), jpa.getSurname()),         // Strings → FullName value object
                new Email(jpa.getEmail()),                                   // String → Email value object
                jpa.getDepartment(),                                         // Direct mapping
                jpa.getLineManagerId(),                                      // Direct mapping (nullable)
                jpa.getHireDate(),                                           // Direct mapping
                jpa.getCurrentRole(),                                        // Direct mapping
                jpa.getStartDateCurrentRole(),                               // Direct mapping
                jpa.getJobLevel(),                                           // Direct mapping (nullable)
                EmploymentType.valueOf(jpa.getEmploymentType()),             // String → enum
                EmploymentStatus.valueOf(jpa.getEmploymentStatus())          // String → enum
        );
    }
}
