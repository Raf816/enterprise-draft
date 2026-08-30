package com.staffs.leavebooking.leavemanagement.infrastructure.repositories;

import com.staffs.leavebooking.leavemanagement.infrastructure.entities.LeaveRequestJpa;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Data repository for the {@code leave_request} table
 * (Lecture 3 — Repository Pattern, Infrastructure Layer).
 *
 * <p><strong>Pattern:</strong> Extends {@link CrudRepository} which provides standard
 * CRUD operations (save, findById, findAll, delete, etc.) without any implementation.
 * Spring Data generates the SQL at runtime from the method signatures.
 *
 * <p><strong>Type parameters:</strong>
 * <ul>
 *   <li>{@link LeaveRequestJpa} — the JPA entity this repository manages</li>
 *   <li>{@code String} — the primary key type (UUID as string, not Long)</li>
 * </ul>
 *
 * <p><strong>Custom finder methods:</strong> Spring Data derives SQL queries from
 * the method names automatically. For example:
 * <ul>
 *   <li>{@code findByStaffMemberId("abc")} → {@code WHERE staff_member_id = 'abc'}</li>
 *   <li>{@code findByStaffMemberIdAndStatus("abc", "PENDING")} → {@code WHERE staff_member_id = 'abc' AND status = 'PENDING'}</li>
 *   <li>{@code findByStartDateBetween(from, to)} → {@code WHERE start_date BETWEEN from AND to}</li>
 * </ul>
 *
 * <p><strong>Organisation:</strong> Methods are grouped by the search scope they support:
 * <ol>
 *   <li><strong>Basic queries</strong> — used by GET endpoints (no filters beyond the person scope)</li>
 *   <li><strong>Staff member search</strong> — used by POST /my/search and POST /all/search (with staffMemberId)</li>
 *   <li><strong>Manager search</strong> — used by POST /team/search and POST /all/search (with managerId)</li>
 *   <li><strong>Company-wide search</strong> — used by POST /all/search (no person filter)</li>
 * </ol>
 *
 * @see LeaveRequestJpa for the JPA entity this repository manages
 * @see com.staffs.leavebooking.leavemanagement.application.handlers.LeaveRequestQueryHandler for the query handler that uses this repository
 * @see com.staffs.leavebooking.leavemanagement.application.handlers.LeaveRequestApplicationService for the command handler that uses this repository
 */
@Repository // Spring stereotype — marks as a persistence component for component scanning
public interface LeaveRequestRepository extends CrudRepository<LeaveRequestJpa, String> {

    // ─────────────────────────────────────────────────────────────────
    // BASIC QUERIES (used by GET endpoints — no filters)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Finds all leave requests submitted by a specific staff member.
     * Used by GET /leave-requests/my (via the query handler).
     *
     * @param staffMemberId the UUID of the staff member
     * @return list of leave requests for the staff member
     */
    List<LeaveRequestJpa> findByStaffMemberId(String staffMemberId);

    /**
     * Finds all leave requests assigned to a specific manager.
     * Used by GET /leave-requests/team (via the query handler).
     *
     * @param managerId the UUID of the manager
     * @return list of leave requests for the manager's team
     */
    List<LeaveRequestJpa> findByManagerId(String managerId);

    /**
     * Finds all leave requests with a specific status (company-wide).
     * Used by the admin search when filtering by status only.
     *
     * @param status the status to filter by (e.g., "PENDING", "APPROVED")
     * @return list of leave requests with the given status
     */
    List<LeaveRequestJpa> findByStatus(String status);

    /**
     * Returns all leave requests (overrides CrudRepository.findAll() to return List instead of Iterable).
     * Used by GET /leave-requests/all (admin view).
     *
     * @return list of all leave requests in the system
     */
    List<LeaveRequestJpa> findAll();

    // ─────────────────────────────────────────────────────────────────
    // SEARCH QUERIES — by staff member (POST /my/search, POST /all/search)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Finds leave requests by staff member and status.
     * Example: "Show my PENDING requests."
     *
     * @param staffMemberId the UUID of the staff member
     * @param status        the status to filter by
     * @return list of matching leave requests
     */
    List<LeaveRequestJpa> findByStaffMemberIdAndStatus(String staffMemberId, String status);

    /**
     * Finds leave requests by staff member and date range (startDate between from and to, inclusive).
     * Example: "Show my requests in September."
     *
     * @param staffMemberId the UUID of the staff member
     * @param from          the start of the date range (inclusive)
     * @param to            the end of the date range (inclusive)
     * @return list of matching leave requests
     */
    List<LeaveRequestJpa> findByStaffMemberIdAndStartDateBetween(String staffMemberId, LocalDate from, LocalDate to);

    /**
     * Finds leave requests by staff member, status, and date range — the most specific staff-scoped query.
     * Example: "Show my APPROVED requests in Q3."
     *
     * @param staffMemberId the UUID of the staff member
     * @param status        the status to filter by
     * @param from          the start of the date range (inclusive)
     * @param to            the end of the date range (inclusive)
     * @return list of matching leave requests
     */
    List<LeaveRequestJpa> findByStaffMemberIdAndStatusAndStartDateBetween(String staffMemberId, String status, LocalDate from, LocalDate to);

    // ─────────────────────────────────────────────────────────────────
    // SEARCH QUERIES — by manager (POST /team/search, POST /all/search)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Finds leave requests by manager and status.
     * Example: "Show PENDING requests for my team."
     *
     * @param managerId the UUID of the manager
     * @param status    the status to filter by
     * @return list of matching leave requests
     */
    List<LeaveRequestJpa> findByManagerIdAndStatus(String managerId, String status);

    /**
     * Finds leave requests by manager and date range (startDate between from and to, inclusive).
     * Example: "Show my team's requests in Q4."
     *
     * @param managerId the UUID of the manager
     * @param from      the start of the date range (inclusive)
     * @param to        the end of the date range (inclusive)
     * @return list of matching leave requests
     */
    List<LeaveRequestJpa> findByManagerIdAndStartDateBetween(String managerId, LocalDate from, LocalDate to);

    /**
     * Finds leave requests by manager, status, and date range — the most specific manager-scoped query.
     * Example: "Show PENDING team requests in September."
     *
     * @param managerId the UUID of the manager
     * @param status    the status to filter by
     * @param from      the start of the date range (inclusive)
     * @param to        the end of the date range (inclusive)
     * @return list of matching leave requests
     */
    List<LeaveRequestJpa> findByManagerIdAndStatusAndStartDateBetween(String managerId, String status, LocalDate from, LocalDate to);

    // ─────────────────────────────────────────────────────────────────
    // SEARCH QUERIES — company-wide (POST /all/search)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Finds leave requests by date range only (company-wide, no person filter).
     * Example: "Show all requests in Q3 company-wide."
     *
     * @param from the start of the date range (inclusive)
     * @param to   the end of the date range (inclusive)
     * @return list of matching leave requests
     */
    List<LeaveRequestJpa> findByStartDateBetween(LocalDate from, LocalDate to);

    /**
     * Finds leave requests by status and date range (company-wide, no person filter).
     * Example: "Show all PENDING requests in September company-wide."
     *
     * @param status the status to filter by
     * @param from   the start of the date range (inclusive)
     * @param to     the end of the date range (inclusive)
     * @return list of matching leave requests
     */
    List<LeaveRequestJpa> findByStatusAndStartDateBetween(String status, LocalDate from, LocalDate to);
}
