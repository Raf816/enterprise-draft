package com.staffs.leavebooking.staffmanagement.infrastructure.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

/**
 * JPA entity mapping to the {@code staff_member} database table
 * (Lecture 3 — Infrastructure Layer, Persistence).
 *
 * <p><strong>Separation from domain:</strong> This JPA entity is kept separate from
 * the domain aggregate ({@link com.staffs.leavebooking.staffmanagement.domain.StaffMember}).
 * The domain aggregate has no JPA annotations — it's pure business logic.
 * Mappers convert between the two representations.
 *
 * <p><strong>ID strategy:</strong> The ID is a String (not auto-generated) because
 * it's the Firebase UID. This ensures consistency: Firebase UID = staff record ID =
 * leave allowance staffMemberId.
 *
 * <p><strong>Jakarta Validation annotations:</strong> {@code @NotBlank}, {@code @NotNull},
 * {@code @Size} provide database-level validation as a safety net. The primary validation
 * happens in the domain layer (DomainAssertions), but these annotations catch any
 * data that bypasses the domain (e.g., direct SQL inserts, test data).
 *
 * <p><strong>Lombok annotations:</strong>
 * <ul>
 *   <li>{@code @Getter} — generates getters for all fields (used by mappers)</li>
 *   <li>{@code @Setter} — generates setters for all fields (required by JPA for hydration)</li>
 *   <li>{@code @ToString} — generates toString() for logging/debugging</li>
 * </ul>
 *
 * @see com.staffs.leavebooking.staffmanagement.application.mappers.StaffMemberDomainToJpaMapper for domain → JPA conversion
 * @see com.staffs.leavebooking.staffmanagement.application.mappers.StaffMemberJpaToDomainMapper for JPA → domain conversion
 */
@Entity(name = "staff_member")   // JPA entity name — used in JPQL queries
@Table(name = "staff_member")    // Maps to the staff_member table in the database
@Getter     // Lombok: generates getter methods for all fields
@Setter     // Lombok: generates setter methods for all fields (JPA requires setters)
@ToString   // Lombok: generates toString() for logging
public class StaffMemberJpa {

    /**
     * Primary key — the Firebase UID (not auto-generated).
     * Using the Firebase UID as the primary key ensures a single, consistent ID
     * across Firebase, staff management, and leave management contexts.
     */
    @Id // JPA primary key
    @Column(name = "id", length = 36) // UUID format: 36 chars with hyphens
    private String id;

    /** Staff member's first name (max 50 chars, required) */
    @NotBlank(message = "First name is required")
    @Size(max = 50)
    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    /** Staff member's surname (max 50 chars, required) */
    @NotBlank(message = "Surname is required")
    @Size(max = 50)
    @Column(name = "surname", nullable = false, length = 50)
    private String surname;

    /** Email address (unique across all staff, max 150 chars, required) */
    @NotBlank(message = "Email is required")
    @Size(max = 150)
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    /** Department name (e.g., "Networks", "Digital") (required) */
    @NotBlank(message = "Department is required")
    @Size(max = 100)
    @Column(name = "department", nullable = false, length = 100)
    private String department;

    /** Line manager's UUID (nullable — some staff may not have a manager assigned yet) */
    @Column(name = "line_manager_id", length = 36)
    private String lineManagerId;

    /** Date the staff member was hired (required, cannot be in the future) */
    @NotNull(message = "Hire date is required")
    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    /**
     * Current job title/role (e.g., "Software Engineer", "Apprentice Software Engineer").
     * Backtick-escaped because "current_role" might be a reserved word in some SQL dialects.
     */
    @NotBlank(message = "Current role is required")
    @Size(max = 100)
    @Column(name = "`current_role`", nullable = false, length = 100)
    private String currentRole;

    /** When the current role started (required) */
    @NotNull(message = "Start date of current role is required")
    @Column(name = "start_date_current_role", nullable = false)
    private LocalDate startDateCurrentRole;

    /** Seniority level (e.g., "JUNIOR", "MID", "SENIOR") — nullable */
    @Size(max = 20)
    @Column(name = "job_level", length = 20)
    private String jobLevel;

    /**
     * Contract type stored as string (FULL_TIME, PART_TIME, CONTRACT).
     * Stored as string rather than @Enumerated to decouple the database from the Java enum.
     */
    @NotBlank(message = "Employment type is required")
    @Column(name = "employment_type", nullable = false, length = 20)
    private String employmentType;

    /**
     * Lifecycle status stored as string (PENDING_SETUP, ACTIVE, ON_LEAVE, TERMINATED).
     * Stored as string rather than @Enumerated for flexibility.
     */
    @NotBlank(message = "Employment status is required")
    @Column(name = "employment_status", nullable = false, length = 20)
    private String employmentStatus;
}
