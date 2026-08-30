package com.staffs.leavebooking.staffmanagement.infrastructure.repositories;

import com.staffs.leavebooking.staffmanagement.infrastructure.entities.StaffMemberJpa;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data repository for the {@code staff_member} table
 * (Lecture 3 — Repository Pattern, Infrastructure Layer).
 *
 * <p><strong>Pattern:</strong> Extends {@link CrudRepository} which provides standard
 * CRUD operations (save, findById, findAll, delete, etc.) without any implementation.
 * Spring Data generates the SQL at runtime from the method signatures.
 *
 * <p><strong>Type parameters:</strong>
 * <ul>
 *   <li>{@link StaffMemberJpa} — the JPA entity this repository manages</li>
 *   <li>{@code String} — the primary key type (Firebase UID, not Long)</li>
 * </ul>
 *
 * <p><strong>Custom finder methods:</strong> Spring Data derives SQL queries from
 * the method names automatically. For example:
 * <ul>
 *   <li>{@code findByDepartment("Networks")} → {@code WHERE department = 'Networks'}</li>
 *   <li>{@code findByDepartmentAndEmploymentStatus("Networks", "ACTIVE")} →
 *       {@code WHERE department = 'Networks' AND employment_status = 'ACTIVE'}</li>
 *   <li>{@code existsByEmail("raf@bt.com")} → {@code SELECT COUNT(*) > 0 WHERE email = 'raf@bt.com'}</li>
 * </ul>
 *
 * @see StaffMemberJpa for the JPA entity this repository manages
 */
@Repository // Spring stereotype — marks as a persistence component for component scanning
public interface StaffMemberRepository extends CrudRepository<StaffMemberJpa, String> {

    /** Returns all staff members (overrides CrudRepository.findAll() to return List instead of Iterable) */
    List<StaffMemberJpa> findAll();

    /** Finds all staff members in a specific department */
    List<StaffMemberJpa> findByDepartment(String department);

    /** Finds all staff members with a specific employment status (e.g., "ACTIVE", "TERMINATED") */
    List<StaffMemberJpa> findByEmploymentStatus(String employmentStatus);

    /** Finds all staff members matching both department AND employment status (combined filter) */
    List<StaffMemberJpa> findByDepartmentAndEmploymentStatus(String department, String employmentStatus);

    /** Finds all staff members managed by a specific manager (by their UUID) */
    List<StaffMemberJpa> findByLineManagerId(String lineManagerId);

    /** Checks if a staff member with the given email already exists (used for duplicate prevention) */
    boolean existsByEmail(String email);
}
