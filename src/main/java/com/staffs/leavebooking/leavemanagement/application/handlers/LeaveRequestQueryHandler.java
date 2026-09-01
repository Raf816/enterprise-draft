package com.staffs.leavebooking.leavemanagement.application.handlers;

import com.staffs.leavebooking.leavemanagement.application.dto.LeaveRequestDTO;
import com.staffs.leavebooking.leavemanagement.application.dto.LeaveRequestSearchCriteria;
import com.staffs.leavebooking.leavemanagement.application.mappers.LeaveRequestJpaToDTOMapper;
import com.staffs.leavebooking.leavemanagement.infrastructure.entities.LeaveRequestJpa;
import com.staffs.leavebooking.leavemanagement.infrastructure.repositories.LeaveRequestRepository;
import com.staffs.leavebooking.leavemanagement.ui.exceptions.LeaveRequestNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.StreamSupport;

import static java.util.stream.Collectors.toList;

/**
 * CQRS Query Handler — handles all read-only operations for Leave Request
 * (Lecture 5/6 — CQRS Read Side).
 *
 * <p><strong>CQRS Read Path:</strong> This handler does NOT interact with the domain layer.
 * The read path goes directly: Repository → JPA Entity → Mapper → DTO. This is a CQRS
 * optimisation — there's no need to reconstitute the full domain aggregate for queries,
 * which would add unnecessary overhead (domain validation, event tracking, etc.).
 *
 * <p><strong>Two categories of methods:</strong>
 * <ul>
 *   <li><strong>Basic queries</strong> (used by GET endpoints): simple, unfiltered reads
 *       by staffMemberId, managerId, or all. No criteria parameters needed.</li>
 *   <li><strong>Search queries</strong> (used by POST /search endpoints): filtered reads
 *       using {@link LeaveRequestSearchCriteria}. Support optional combinations of status,
 *       date range, staffMemberId, and managerId filters. The handler routes to the
 *       appropriate repository method based on which criteria fields are populated.</li>
 * </ul>
 *
 * <p><strong>Mapping:</strong> All methods map JPA entities to DTOs using
 * {@link LeaveRequestJpaToDTOMapper}, which performs a direct field-by-field copy.
 * The mapper is stateless and used as a static utility.
 *
 * @see LeaveRequestApplicationService for the CQRS write-side (command handler)
 * @see com.staffs.leavebooking.leavemanagement.LeaveManagementFacade for the facade that delegates to this handler
 * @see LeaveRequestJpaToDTOMapper for the JPA-to-DTO mapping logic
 * @see LeaveRequestDTO for the read model returned to API consumers
 * @see LeaveRequestSearchCriteria for the search filter structure
 */
@Service            // Spring stereotype — registers as a service bean in the application context
@AllArgsConstructor // Lombok: generates constructor with all final fields (enables constructor-based DI)
public class LeaveRequestQueryHandler {

    /**
     * Spring Data repository for querying leave request JPA entities.
     * All queries in this handler delegate to this repository's finder methods.
     *
     * @see LeaveRequestRepository for the available query methods
     */
    private final LeaveRequestRepository leaveRequestRepository;

    // ─────────────────────────────────────────────────────────────────
    // BASIC QUERIES (used by GET endpoints — no filters)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Finds all leave requests submitted by a specific staff member.
     *
     * <p>Used by GET /leave-requests/my — returns all requests (all statuses, all dates)
     * for the authenticated user. The staffMemberId comes from the JWT token.
     *
     * @param staffMemberId the UUID of the staff member
     * @return list of the staff member's leave requests as DTOs
     * @see LeaveRequestRepository#findByStaffMemberId(String)
     * @see LeaveRequestJpaToDTOMapper#toDTO
     */
    public List<LeaveRequestDTO> findRequestsByStaffMemberId(String staffMemberId) {
        // Query the repository for all requests by this staff member
        return leaveRequestRepository.findByStaffMemberId(staffMemberId).stream()
                .map(LeaveRequestJpaToDTOMapper::toDTO) // Map each JPA entity to a DTO
                .collect(toList());                      // Collect into a List
    }

    /**
     * Finds all leave requests assigned to a specific manager (their team's requests).
     *
     * <p>Used by GET /leave-requests/team — returns all requests (all statuses, all dates)
     * for the manager's direct reports. The managerId comes from the JWT token.
     *
     * @param managerId the UUID of the manager
     * @return list of the manager's team leave requests as DTOs
     * @see LeaveRequestRepository#findByManagerId(String)
     */
    public List<LeaveRequestDTO> findRequestsByManagerId(String managerId) {
        // Query the repository for all requests assigned to this manager
        return leaveRequestRepository.findByManagerId(managerId).stream()
                .map(LeaveRequestJpaToDTOMapper::toDTO) // Map each JPA entity to a DTO
                .collect(toList());                      // Collect into a List
    }

    /**
     * Finds all leave requests company-wide (admin view, unfiltered).
     *
     * <p>Used by GET /leave-requests/all — returns every leave request in the system.
     * The CrudRepository's {@code findAll()} returns an Iterable, so we use
     * {@link StreamSupport} to convert it to a Stream for mapping.
     *
     * @return list of all leave requests in the system as DTOs
     */
    public List<LeaveRequestDTO> findAllRequests() {
        // CrudRepository.findAll() returns Iterable — convert to Stream via StreamSupport
        return StreamSupport.stream(leaveRequestRepository.findAll().spliterator(), false)
                .map(LeaveRequestJpaToDTOMapper::toDTO) // Map each JPA entity to a DTO
                .collect(toList());                      // Collect into a List
    }

    /**
     * Finds a single leave request by its UUID.
     *
     * <p>Used by GET /leave-requests/{id} — also called internally after write operations
     * to return the updated DTO to the client.
     *
     * @param leaveRequestId the UUID of the leave request
     * @return the leave request as a DTO
     * @throws LeaveRequestNotFoundException if no request exists with the given ID
     * @see LeaveRequestRepository#findById(Object)
     */
    public LeaveRequestDTO findRequestById(String leaveRequestId) {
        // Find by ID, map to DTO, or throw not-found exception
        return leaveRequestRepository.findById(leaveRequestId)
                .map(LeaveRequestJpaToDTOMapper::toDTO)                     // Map JPA entity to DTO
                .orElseThrow(() -> new LeaveRequestNotFoundException(leaveRequestId)); // 404 if not found
    }

    // ─────────────────────────────────────────────────────────────────
    // SEARCH QUERIES (used by POST /search endpoints)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Searches the current user's own requests with optional filters.
     *
     * <p>Used by POST /leave-requests/my/search. The staffMemberId is always derived
     * from the JWT — never from the criteria body (security best practice).
     *
     * <p><strong>Filter routing:</strong> Routes to the appropriate repository method
     * based on which criteria fields are populated:
     * <ul>
     *   <li>status + date range → {@code findByStaffMemberIdAndStatusAndDateOverlap}</li>
     *   <li>status only → {@code findByStaffMemberIdAndStatus}</li>
     *   <li>date range only → {@code findByStaffMemberIdAndDateOverlap}</li>
     *   <li>no filters → {@code findByStaffMemberId} (fallback to basic query)</li>
     * </ul>
     *
     * @param staffMemberId the UUID of the authenticated staff member (from JWT)
     * @param criteria      the search criteria containing optional status and date range filters
     * @return list of matching leave requests as DTOs
     * @see LeaveRequestSearchCriteria#normalizedStatus() for case-insensitive status matching
     */
    public List<LeaveRequestDTO> searchByStaffMember(String staffMemberId, LeaveRequestSearchCriteria criteria) {
        // Normalise the status to upper case for case-insensitive matching (or null if not set)
        String status = criteria.normalizedStatus();
        // Check if both from and to dates are provided (both required for a valid date range)
        boolean hasDateRange = criteria.from() != null && criteria.to() != null;

        List<LeaveRequestJpa> results;

        // Route to the appropriate repository method based on which filters are set
        if (status != null && hasDateRange) {
            // Both status and date range filters — most specific query
            results = leaveRequestRepository.findByStaffMemberIdAndStatusAndDateOverlap(
                    staffMemberId, status, criteria.from(), criteria.to());
        } else if (status != null) {
            // Status filter only — e.g., "show my PENDING requests"
            results = leaveRequestRepository.findByStaffMemberIdAndStatus(staffMemberId, status);
        } else if (hasDateRange) {
            // Date range filter only — e.g., "show my requests in September"
            results = leaveRequestRepository.findByStaffMemberIdAndDateOverlap(
                    staffMemberId, criteria.from(), criteria.to());
        } else {
            // No filters — fallback to basic query (same as GET /my)
            results = leaveRequestRepository.findByStaffMemberId(staffMemberId);
        }

        // Map all JPA entities to DTOs and return
        return results.stream().map(LeaveRequestJpaToDTOMapper::toDTO).collect(toList());
    }

    /**
     * Searches a manager's team requests with optional filters.
     *
     * <p>Used by POST /leave-requests/team/search. The managerId is always derived
     * from the JWT — never from the criteria body (security best practice).
     *
     * <p><strong>Filter routing:</strong> Routes to the appropriate repository method
     * based on which criteria fields are populated:
     * <ul>
     *   <li>status + date range → {@code findByManagerIdAndStatusAndDateOverlap}</li>
     *   <li>status only → {@code findByManagerIdAndStatus}</li>
     *   <li>date range only → {@code findByManagerIdAndDateOverlap}</li>
     *   <li>no filters → {@code findByManagerId} (fallback to basic query)</li>
     * </ul>
     *
     * @param managerId the UUID of the authenticated manager (from JWT)
     * @param criteria  the search criteria containing optional status and date range filters
     * @return list of matching team leave requests as DTOs
     */
    public List<LeaveRequestDTO> searchByManager(String managerId, LeaveRequestSearchCriteria criteria) {
        // Normalise the status to upper case for case-insensitive matching (or null if not set)
        String status = criteria.normalizedStatus();
        // Check if both from and to dates are provided
        boolean hasDateRange = criteria.from() != null && criteria.to() != null;

        List<LeaveRequestJpa> results;

        // Route to the appropriate repository method based on which filters are set
        if (status != null && hasDateRange) {
            // Both status and date range filters — most specific query
            results = leaveRequestRepository.findByManagerIdAndStatusAndDateOverlap(
                    managerId, status, criteria.from(), criteria.to());
        } else if (status != null) {
            // Status filter only — e.g., "show pending team requests"
            results = leaveRequestRepository.findByManagerIdAndStatus(managerId, status);
        } else if (hasDateRange) {
            // Date range filter only — e.g., "show team requests in Q4"
            results = leaveRequestRepository.findByManagerIdAndDateOverlap(
                    managerId, criteria.from(), criteria.to());
        } else {
            // No filters — fallback to basic query (same as GET /team)
            results = leaveRequestRepository.findByManagerId(managerId);
        }

        // Map all JPA entities to DTOs and return
        return results.stream().map(LeaveRequestJpaToDTOMapper::toDTO).collect(toList());
    }

    /**
     * Searches all requests company-wide with optional filters (admin only).
     *
     * <p>Used by POST /leave-requests/all/search. Supports filtering by staffMemberId,
     * managerId, status, and date range. staffMemberId and managerId are mutually exclusive.
     *
     * <p><strong>Filter routing:</strong> staffMemberId and managerId are mutually exclusive
     * (the controller rejects requests with both). The handler routes to the appropriate
     * query tier based on which filter is present:
     *
     * <p><strong>Three-tier routing:</strong>
     * <ol>
     *   <li>If staffMemberId is set → routes to staff-member-scoped queries</li>
     *   <li>Else if managerId is set → routes to manager-scoped queries</li>
     *   <li>Else → routes to company-wide queries (status and/or date range only)</li>
     * </ol>
     * Within each tier, further routing happens based on status and date range filters.
     *
     * @param criteria the search criteria (staffMemberId and managerId mutually exclusive, validated by controller)
     * @return list of matching leave requests as DTOs
     * @see LeaveRequestSearchCriteria for the full set of available filters
     */
    public List<LeaveRequestDTO> searchAll(LeaveRequestSearchCriteria criteria) {
        // Normalise the status to upper case for case-insensitive matching (or null if not set)
        String status = criteria.normalizedStatus();
        // Check if both from and to dates are provided
        boolean hasDateRange = criteria.from() != null && criteria.to() != null;
        // Extract and validate the staffMemberId filter (null if blank or not set)
        String staffId = (criteria.staffMemberId() != null && !criteria.staffMemberId().isBlank())
                ? criteria.staffMemberId() : null;
        // Extract and validate the managerId filter (null if blank or not set)
        String mgrId = (criteria.managerId() != null && !criteria.managerId().isBlank())
                ? criteria.managerId() : null;

        List<LeaveRequestJpa> results;

        // Staff member filter (search a specific person's requests)
        if (staffId != null) {
            if (status != null && hasDateRange) {
                // Staff + status + date range — most specific staff-scoped query
                results = leaveRequestRepository.findByStaffMemberIdAndStatusAndDateOverlap(
                        staffId, status, criteria.from(), criteria.to());
            } else if (status != null) {
                // Staff + status — e.g., "show this person's APPROVED requests"
                results = leaveRequestRepository.findByStaffMemberIdAndStatus(staffId, status);
            } else if (hasDateRange) {
                // Staff + date range — e.g., "show this person's requests in Q3"
                results = leaveRequestRepository.findByStaffMemberIdAndDateOverlap(
                        staffId, criteria.from(), criteria.to());
            } else {
                // Staff only — show all requests for this person
                results = leaveRequestRepository.findByStaffMemberId(staffId);
            }
        }
        // TIER 2: Manager filter (search a manager's team requests)
        else if (mgrId != null) {
            if (status != null && hasDateRange) {
                // Manager + status + date range — most specific manager-scoped query
                results = leaveRequestRepository.findByManagerIdAndStatusAndDateOverlap(
                        mgrId, status, criteria.from(), criteria.to());
            } else if (status != null) {
                // Manager + status — e.g., "show this manager's PENDING team requests"
                results = leaveRequestRepository.findByManagerIdAndStatus(mgrId, status);
            } else if (hasDateRange) {
                // Manager + date range — e.g., "show this manager's team requests in Q4"
                results = leaveRequestRepository.findByManagerIdAndDateOverlap(
                        mgrId, criteria.from(), criteria.to());
            } else {
                // Manager only — show all requests for this manager's team
                results = leaveRequestRepository.findByManagerId(mgrId);
            }
        }
        // TIER 3: Company-wide (no person filter — status and/or date range only)
        else {
            if (status != null && hasDateRange) {
                // Status + date range — e.g., "show all PENDING requests in September"
                results = leaveRequestRepository.findByStatusAndDateOverlap(
                        status, criteria.from(), criteria.to());
            } else if (status != null) {
                // Status only — e.g., "show all PENDING requests company-wide"
                results = leaveRequestRepository.findByStatus(status);
            } else if (hasDateRange) {
                // Date range only — e.g., "show all requests in Q3 company-wide"
                results = leaveRequestRepository.findByDateOverlap(criteria.from(), criteria.to());
            } else {
                // No filters at all — return everything (same as GET /all)
                results = StreamSupport.stream(leaveRequestRepository.findAll().spliterator(), false)
                        .collect(toList());
            }
        }

        // Map all JPA entities to DTOs and return
        return results.stream().map(LeaveRequestJpaToDTOMapper::toDTO).collect(toList());
    }
}
