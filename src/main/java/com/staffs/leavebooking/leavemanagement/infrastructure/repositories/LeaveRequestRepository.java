package com.staffs.leavebooking.leavemanagement.infrastructure.repositories;

import com.staffs.leavebooking.leavemanagement.infrastructure.entities.LeaveRequestJpa;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * Spring Data repository for the {@code leave_request} table
 * (Lecture 3 — Repository Pattern, Infrastructure Layer).
 *
 * <p><strong>Date range filtering:</strong> All date-range queries use proper overlap
 * detection: {@code startDate <= :to AND endDate >= :from}. This catches leave requests
 * that start before the search range but end within it, which a simple
 * {@code startDate BETWEEN :from AND :to} would miss.
 *
 * @see LeaveRequestJpa for the JPA entity this repository manages
 * @see com.staffs.leavebooking.leavemanagement.application.handlers.LeaveRequestQueryHandler for the query handler
 */
@Repository
public interface LeaveRequestRepository extends CrudRepository<LeaveRequestJpa, String> {

    // ─────────────────────────────────────────────────────────────────
    // BASIC QUERIES (used by GET endpoints — no filters)
    // ─────────────────────────────────────────────────────────────────

    List<LeaveRequestJpa> findByStaffMemberId(String staffMemberId);

    List<LeaveRequestJpa> findByManagerId(String managerId);

    List<LeaveRequestJpa> findByStatus(String status);

    List<LeaveRequestJpa> findAll();

    // ─────────────────────────────────────────────────────────────────
    // SEARCH QUERIES — by staff member
    // ─────────────────────────────────────────────────────────────────

    List<LeaveRequestJpa> findByStaffMemberIdAndStatus(String staffMemberId, String status);

    /** Date overlap: startDate <= to AND endDate >= from (catches requests spanning the range boundary). */
    @Query("SELECT r FROM leave_request r WHERE r.staffMemberId = :staffMemberId AND r.startDate <= :to AND r.endDate >= :from")
    List<LeaveRequestJpa> findByStaffMemberIdAndDateOverlap(@Param("staffMemberId") String staffMemberId,
                                                             @Param("from") LocalDate from,
                                                             @Param("to") LocalDate to);

    /** Date overlap + status filter. */
    @Query("SELECT r FROM leave_request r WHERE r.staffMemberId = :staffMemberId AND r.status = :status AND r.startDate <= :to AND r.endDate >= :from")
    List<LeaveRequestJpa> findByStaffMemberIdAndStatusAndDateOverlap(@Param("staffMemberId") String staffMemberId,
                                                                      @Param("status") String status,
                                                                      @Param("from") LocalDate from,
                                                                      @Param("to") LocalDate to);

    // ─────────────────────────────────────────────────────────────────
    // SEARCH QUERIES — by manager
    // ─────────────────────────────────────────────────────────────────

    List<LeaveRequestJpa> findByManagerIdAndStatus(String managerId, String status);

    /** Date overlap: startDate <= to AND endDate >= from. */
    @Query("SELECT r FROM leave_request r WHERE r.managerId = :managerId AND r.startDate <= :to AND r.endDate >= :from")
    List<LeaveRequestJpa> findByManagerIdAndDateOverlap(@Param("managerId") String managerId,
                                                        @Param("from") LocalDate from,
                                                        @Param("to") LocalDate to);

    /** Date overlap + status filter. */
    @Query("SELECT r FROM leave_request r WHERE r.managerId = :managerId AND r.status = :status AND r.startDate <= :to AND r.endDate >= :from")
    List<LeaveRequestJpa> findByManagerIdAndStatusAndDateOverlap(@Param("managerId") String managerId,
                                                                  @Param("status") String status,
                                                                  @Param("from") LocalDate from,
                                                                  @Param("to") LocalDate to);

    // ─────────────────────────────────────────────────────────────────
    // SEARCH QUERIES — company-wide
    // ─────────────────────────────────────────────────────────────────

    /** Date overlap: startDate <= to AND endDate >= from. */
    @Query("SELECT r FROM leave_request r WHERE r.startDate <= :to AND r.endDate >= :from")
    List<LeaveRequestJpa> findByDateOverlap(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Date overlap + status filter. */
    @Query("SELECT r FROM leave_request r WHERE r.status = :status AND r.startDate <= :to AND r.endDate >= :from")
    List<LeaveRequestJpa> findByStatusAndDateOverlap(@Param("status") String status,
                                                      @Param("from") LocalDate from,
                                                      @Param("to") LocalDate to);
}
