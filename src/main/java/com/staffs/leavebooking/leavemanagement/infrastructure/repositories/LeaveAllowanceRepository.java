package com.staffs.leavebooking.leavemanagement.infrastructure.repositories;

import com.staffs.leavebooking.leavemanagement.infrastructure.entities.LeaveAllowanceJpa;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data repository for the {@code leave_allowance} table
 * (Lecture 3 — Repository Pattern, Infrastructure Layer).
 *
 * <p><strong>Pattern:</strong> Extends {@link CrudRepository} which provides standard
 * CRUD operations (save, findById, findAll, delete, etc.) without any implementation.
 * Spring Data generates the SQL at runtime from the method signatures.
 *
 * <p><strong>Type parameters:</strong>
 * <ul>
 *   <li>{@link LeaveAllowanceJpa} — the JPA entity this repository manages</li>
 *   <li>{@code String} — the primary key type (UUID as string, not Long)</li>
 * </ul>
 *
 * <p><strong>Custom finder methods:</strong> Spring Data derives SQL queries from
 * the method names automatically. For example:
 * <ul>
 *   <li>{@code findByManagerId("abc")} → {@code WHERE manager_id = 'abc'}</li>
 *   <li>{@code findByDepartment("Networks")} → {@code WHERE department = 'Networks'}</li>
 *   <li>{@code findFirstByStaffMemberIdOrderByBusinessYearStartDesc("abc")} →
 *       {@code WHERE staff_member_id = 'abc' ORDER BY business_year_start DESC LIMIT 1}</li>
 *   <li>{@code existsByStaffMemberIdAndBusinessYearStart("abc", 2025)} →
 *       {@code SELECT COUNT(*) > 0 WHERE staff_member_id = 'abc' AND business_year_start = 2025}</li>
 * </ul>
 *
 * <p><strong>Usage:</strong> This repository is used by:
 * <ul>
 *   <li>{@link com.staffs.leavebooking.leavemanagement.application.handlers.LeaveAllowanceQueryHandler} — for all read-only queries</li>
 *   <li>{@link com.staffs.leavebooking.leavemanagement.application.handlers.LeaveAllowanceApplicationService} — for write operations (load, save, existence checks)</li>
 * </ul>
 *
 * @see LeaveAllowanceJpa for the JPA entity this repository manages
 * @see com.staffs.leavebooking.leavemanagement.application.handlers.LeaveAllowanceQueryHandler for the query handler that uses this repository
 * @see com.staffs.leavebooking.leavemanagement.application.handlers.LeaveAllowanceApplicationService for the command handler that uses this repository
 */
@Repository // Spring stereotype — marks as a persistence component for component scanning
public interface LeaveAllowanceRepository extends CrudRepository<LeaveAllowanceJpa, String> {

    /**
     * Finds the allowance for a specific staff member in a specific business year.
     *
     * <p>Used by event listeners to update the allowance when a leave request changes
     * (reserve/confirm/release/credit-back days), and by query handlers.
     *
     * @param staffMemberId    the UUID of the staff member
     * @param businessYearStart the start year of the business year (e.g., 2025)
     * @return the allowance if found, or empty if no allowance exists for this staff/year
     */
    Optional<LeaveAllowanceJpa> findByStaffMemberIdAndBusinessYearStart(String staffMemberId, Integer businessYearStart);

    /**
     * Finds the most recent (current year) allowance for a staff member.
     *
     * <p>Convenience method — returns the first match ordered by business year start
     * descending, which gives the current (or most recent) year's allowance. This is
     * the primary lookup method used by both query handlers and command handlers.
     *
     * @param staffMemberId the UUID of the staff member
     * @return the most recent allowance if found, or empty
     */
    Optional<LeaveAllowanceJpa> findFirstByStaffMemberIdOrderByBusinessYearStartDesc(String staffMemberId);

    /**
     * Finds all allowances managed by a specific manager.
     *
     * <p>Used by the "View team allowances" query (GET /leave-allowances/team).
     * The managerId on the allowance is a denormalised snapshot synced from Staff Management.
     * Satisfies the brief: "View the amount of annual leave remaining for a member of staff."
     *
     * @param managerId the UUID of the manager
     * @return list of allowances for the manager's direct reports
     */
    List<LeaveAllowanceJpa> findByManagerId(String managerId);

    /**
     * Returns all allowances (overrides CrudRepository.findAll() to return List instead of Iterable).
     *
     * <p>Used by the admin "View all allowances" query (GET /leave-allowances/all without
     * a department filter).
     *
     * @return list of all leave allowances in the system
     */
    List<LeaveAllowanceJpa> findAll();

    /**
     * Finds all allowances filtered by department.
     *
     * <p>Used by the admin "View allowances for a department" query
     * (GET /leave-allowances/all?department={department}). The department on the allowance
     * is a denormalised snapshot synced from Staff Management.
     *
     * @param department the department name to filter by (e.g., "Networks", "Digital")
     * @return list of allowances for staff in the specified department
     */
    List<LeaveAllowanceJpa> findByDepartment(String department);

    /**
     * Checks if an allowance already exists for a staff member in a given business year.
     *
     * <p>Used as an idempotency guard in
     * {@link com.staffs.leavebooking.leavemanagement.application.handlers.LeaveAllowanceApplicationService#createAllowanceForNewStaff}
     * to prevent duplicate allowance creation when RabbitMQ delivers the same
     * {@code StaffMemberAddedEvent} more than once (at-least-once delivery semantics).
     *
     * @param staffMemberId    the UUID of the staff member
     * @param businessYearStart the start year of the business year (e.g., 2025)
     * @return true if an allowance already exists for this staff/year combination
     */
    boolean existsByStaffMemberIdAndBusinessYearStart(String staffMemberId, Integer businessYearStart);
}
