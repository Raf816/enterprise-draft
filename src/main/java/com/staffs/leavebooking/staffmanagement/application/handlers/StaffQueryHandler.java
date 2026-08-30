package com.staffs.leavebooking.staffmanagement.application.handlers;

import com.staffs.leavebooking.staffmanagement.application.dto.StaffMemberDTO;
import com.staffs.leavebooking.staffmanagement.application.mappers.StaffMemberJpaToDTOMapper;
import com.staffs.leavebooking.staffmanagement.infrastructure.repositories.StaffMemberRepository;
import com.staffs.leavebooking.staffmanagement.ui.exceptions.StaffMemberNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * CQRS Query Handler for the Staff Management bounded context
 * (Lecture 5 — CQRS Read Side, Query Handlers).
 *
 * <p><strong>CQRS Concept (Lecture 5):</strong> In the CQRS (Command Query Responsibility
 * Segregation) pattern, reads and writes are handled by separate objects. This class is
 * the read side — it handles all query operations for staff data. The write side is handled
 * by {@link StaffApplicationService}, which processes commands that change state.
 *
 * <p><strong>Read-side optimisation — JPA straight to DTO:</strong> Unlike the command side,
 * which must load the full domain aggregate ({@link com.staffs.leavebooking.staffmanagement.domain.StaffMember})
 * to enforce business rules, the query side maps JPA entities directly to DTOs using
 * {@link StaffMemberJpaToDTOMapper}. This skips the domain aggregate entirely because:
 * <ul>
 *   <li>No business rules need to be enforced for reads</li>
 *   <li>No domain events are raised during reads</li>
 *   <li>Mapping JPA → DTO is simpler and faster than JPA → Domain → DTO</li>
 * </ul>
 *
 * <p><strong>Mapper chain (read side):</strong>
 * <pre>
 * StaffMemberJpa (database entity)
 *   → StaffMemberJpaToDTOMapper.toDTO()
 *     → StaffMemberDTO (flat API response)
 * </pre>
 *
 * <p><strong>Contrast with command side (write side):</strong>
 * <pre>
 * StaffMemberJpa (database entity)
 *   → StaffMemberJpaToDomainMapper.toDomain()
 *     → StaffMember (domain aggregate — enforces invariants, raises events)
 *       → StaffMemberDomainToJpaMapper.toJpa()
 *         → StaffMemberJpa (saved back to database)
 * </pre>
 *
 * <p><strong>Called by:</strong> {@link com.staffs.leavebooking.staffmanagement.StaffManagementFacade},
 * which enforces RBAC via {@code @PreAuthorize} before delegating queries here.
 * This class has no security annotations — security is the facade's responsibility.
 *
 * <p><strong>Search routing (searchStaff method):</strong> The {@link #searchStaff} method
 * implements a simple filter-combination router: based on which criteria fields are non-null,
 * it delegates to the appropriate repository method (department-only, status-only,
 * department+status, or unfiltered). This avoids complex dynamic query construction
 * while supporting all useful filter combinations.
 *
 * @see StaffApplicationService for the CQRS write-side (command handler)
 * @see com.staffs.leavebooking.staffmanagement.StaffManagementFacade for the facade that delegates to this handler
 * @see StaffMemberJpaToDTOMapper for the JPA-to-DTO mapping logic
 * @see StaffMemberDTO for the read model returned to API consumers
 */
@Service        // Spring stereotype — registers this as a service bean in the application context
@AllArgsConstructor // Lombok: generates constructor with all final fields (enables constructor-based DI)
public class StaffQueryHandler {

    /**
     * Spring Data repository for the {@code staff_member} table.
     * Provides CRUD operations and custom finder methods (findByDepartment, findByEmploymentStatus, etc.).
     * All queries in this handler delegate to this repository.
     *
     * @see StaffMemberRepository for the available query methods
     */
    private final StaffMemberRepository staffMemberRepository;

    /**
     * Retrieves all staff members in the system (unfiltered).
     *
     * <p><strong>Usage:</strong> Called from the facade's {@code findAllStaffMembers()} method
     * when an admin requests GET /staff with no filters.
     *
     * <p><strong>Mapping:</strong> Each JPA entity is individually mapped to a DTO using
     * {@link StaffMemberJpaToDTOMapper#toDTO}. The stream-based approach provides a clean
     * functional pipeline from database entities to API-ready DTOs.
     *
     * @return list of all staff members as {@link StaffMemberDTO} objects
     * @see StaffMemberRepository#findAll()
     * @see StaffMemberJpaToDTOMapper#toDTO
     */
    public List<StaffMemberDTO> findAllStaffMembers() {
        // Fetch all JPA entities from the database via Spring Data's findAll()
        return staffMemberRepository.findAll().stream()
                // Map each JPA entity to a flat DTO (skips domain aggregate — read-side optimisation)
                .map(StaffMemberJpaToDTOMapper::toDTO)
                // Collect the mapped DTOs into a List for the API response
                .collect(toList());
    }

    /**
     * Retrieves a single staff member by their UUID (= Firebase UID).
     *
     * <p><strong>Usage:</strong> Called from the facade when a manager or admin
     * requests GET /staff/{id}, or after a PATCH to return the updated state.
     *
     * <p><strong>Error handling:</strong> If the ID doesn't exist in the database,
     * {@code findById()} returns an empty Optional, and {@code orElseThrow()} throws
     * {@link StaffMemberNotFoundException}, which is mapped to HTTP 404 by the
     * controller advice.
     *
     * @param staffMemberId the UUID of the staff member to retrieve (= Firebase UID)
     * @return the staff member's data as a {@link StaffMemberDTO}
     * @throws StaffMemberNotFoundException if no staff member exists with the given ID
     * @see StaffMemberRepository#findById(Object)
     * @see StaffMemberJpaToDTOMapper#toDTO
     */
    public StaffMemberDTO findStaffMemberById(String staffMemberId) {
        // Look up the JPA entity by its primary key (Firebase UID)
        return staffMemberRepository.findById(staffMemberId)
                // If found, map the JPA entity to a flat DTO for the API response
                .map(StaffMemberJpaToDTOMapper::toDTO)
                // If not found, throw StaffMemberNotFoundException (→ HTTP 404)
                .orElseThrow(() -> new StaffMemberNotFoundException(staffMemberId));
    }

    /**
     * Retrieves all staff members in a specific department.
     *
     * <p><strong>Usage:</strong> Called from the facade's {@code findStaffByDepartment()} method
     * when an admin filters staff by department.
     *
     * <p><strong>Spring Data derived query:</strong> The repository method name
     * {@code findByDepartment()} is automatically translated by Spring Data into
     * {@code SELECT * FROM staff_member WHERE department = ?}.
     *
     * @param department the department name to filter by (e.g., "Networks", "Digital")
     * @return list of staff members in the specified department
     * @see StaffMemberRepository#findByDepartment(String)
     */
    public List<StaffMemberDTO> findByDepartment(String department) {
        // Use Spring Data's derived query to fetch all JPA entities for this department
        return staffMemberRepository.findByDepartment(department).stream()
                // Map each JPA entity to a flat DTO (read-side optimisation — no domain aggregate)
                .map(StaffMemberJpaToDTOMapper::toDTO)
                // Collect into a List for the API response
                .collect(toList());
    }

    /**
     * Retrieves all staff members with a specific employment status.
     *
     * <p><strong>Usage:</strong> Called from the facade's {@code findStaffByStatus()} method
     * when an admin filters staff by employment status (e.g., finding all PENDING_SETUP
     * staff who need activation).
     *
     * <p><strong>Spring Data derived query:</strong> {@code findByEmploymentStatus()} →
     * {@code SELECT * FROM staff_member WHERE employment_status = ?}.
     *
     * @param status the employment status to filter by (e.g., "ACTIVE", "PENDING_SETUP", "TERMINATED")
     * @return list of staff members with the specified status
     * @see StaffMemberRepository#findByEmploymentStatus(String)
     */
    public List<StaffMemberDTO> findByStatus(String status) {
        // Use Spring Data's derived query to fetch all JPA entities with this employment status
        return staffMemberRepository.findByEmploymentStatus(status).stream()
                // Map each JPA entity to a flat DTO (read-side optimisation — no domain aggregate)
                .map(StaffMemberJpaToDTOMapper::toDTO)
                // Collect into a List for the API response
                .collect(toList());
    }

    /**
     * Retrieves all staff members managed by a specific manager ("My Team" query).
     *
     * <p><strong>Usage:</strong> Called from the facade's {@code findMyTeam()} method
     * when a manager views their team dashboard. Returns only direct reports —
     * staff whose {@code lineManagerId} matches the given manager ID.
     *
     * <p><strong>Spring Data derived query:</strong> {@code findByLineManagerId()} →
     * {@code SELECT * FROM staff_member WHERE line_manager_id = ?}.
     *
     * @param managerId the UUID of the manager whose direct reports to retrieve
     * @return list of staff members who report to the specified manager
     * @see StaffMemberRepository#findByLineManagerId(String)
     */
    public List<StaffMemberDTO> findByManagerId(String managerId) {
        // Use Spring Data's derived query to fetch all staff with this line manager
        return staffMemberRepository.findByLineManagerId(managerId).stream()
                // Map each JPA entity to a flat DTO (read-side optimisation — no domain aggregate)
                .map(StaffMemberJpaToDTOMapper::toDTO)
                // Collect into a List for the API response
                .collect(toList());
    }

    /**
     * Search staff with optional combined filters (department + status).
     *
     * <p><strong>Filter-combination routing:</strong> This method acts as a simple
     * query router — it inspects which criteria fields are populated and delegates
     * to the appropriate repository method:
     * <ul>
     *   <li>Both department AND status set → {@code findByDepartmentAndEmploymentStatus()}</li>
     *   <li>Only department set → {@code findByDepartment()}</li>
     *   <li>Only status set → {@code findByEmploymentStatus()}</li>
     *   <li>Neither set → falls back to {@code findAllStaffMembers()} (defensive — controller
     *       should reject empty criteria via {@code StaffSearchCriteria.hasFilters()})</li>
     * </ul>
     *
     * <p><strong>Status normalisation:</strong> The status value is uppercased to match
     * the enum names stored in the database (e.g., "active" → "ACTIVE"). Department
     * is not normalised because department names are free-text strings.
     *
     * <p><strong>Null coalescing:</strong> Blank strings are treated as null (no filter)
     * to handle cases where the JSON body contains {@code "department": ""} — which
     * Jackson deserialises as an empty string rather than null.
     *
     * @param criteria the search criteria containing optional department and status filters
     * @return list of staff members matching all specified filters
     * @see com.staffs.leavebooking.staffmanagement.application.dto.StaffSearchCriteria
     * @see StaffMemberRepository#findByDepartmentAndEmploymentStatus(String, String)
     */
    public List<StaffMemberDTO> searchStaff(com.staffs.leavebooking.staffmanagement.application.dto.StaffSearchCriteria criteria) {
        // Normalise department: treat null and blank as "no filter" (null)
        String dept = (criteria.department() != null && !criteria.department().isBlank()) ? criteria.department() : null;
        // Normalise status: treat null and blank as "no filter" (null), uppercase to match enum names in DB
        String status = (criteria.status() != null && !criteria.status().isBlank()) ? criteria.status().toUpperCase() : null;

        // Route to the correct repository method based on which filters are active
        if (dept != null && status != null) {
            // Both filters active → use the combined AND query
            return staffMemberRepository.findByDepartmentAndEmploymentStatus(dept, status).stream()
                    .map(StaffMemberJpaToDTOMapper::toDTO).collect(toList());
        } else if (dept != null) {
            // Only department filter active → use department-only query
            return staffMemberRepository.findByDepartment(dept).stream()
                    .map(StaffMemberJpaToDTOMapper::toDTO).collect(toList());
        } else if (status != null) {
            // Only status filter active → use status-only query
            return staffMemberRepository.findByEmploymentStatus(status).stream()
                    .map(StaffMemberJpaToDTOMapper::toDTO).collect(toList());
        } else {
            // No filters active — defensive fallback (controller should have rejected this)
            return findAllStaffMembers();
        }
    }
}
