package com.staffs.leavebooking.leavemanagement.application.handlers;

import com.staffs.leavebooking.leavemanagement.application.dto.LeaveAllowanceDTO;
import com.staffs.leavebooking.leavemanagement.application.mappers.LeaveAllowanceJpaToDTOMapper;
import com.staffs.leavebooking.leavemanagement.infrastructure.repositories.LeaveAllowanceRepository;
import com.staffs.leavebooking.leavemanagement.ui.exceptions.LeaveAllowanceNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.stream.Collectors.toList;

/**
 * CQRS Query Handler — handles all read-only operations for Leave Allowance
 * (Lecture 5/6 — CQRS Read Side).
 *
 * <p><strong>CQRS Read Path:</strong> This handler does NOT interact with the domain layer.
 * The read path goes directly: Repository → JPA Entity → Mapper → DTO. This is a CQRS
 * optimisation — there's no need to reconstitute the full domain aggregate for queries.
 * The mapper calculates derived fields ({@code remainingDays}, {@code availableDays})
 * at mapping time from the stored balance fields.
 *
 * <p><strong>Query methods:</strong>
 * <ul>
 *   <li>{@code findAllowanceByStaffMemberId} — returns the most recent business year's allowance
 *       for a specific staff member (used by "my" and "staff/{id}" endpoints)</li>
 *   <li>{@code findAllowanceById} — returns a specific allowance by its UUID</li>
 *   <li>{@code findAllowancesByManagerId} — returns all allowances for a manager's team</li>
 *   <li>{@code findAllAllowances} — returns all allowances company-wide (admin)</li>
 *   <li>{@code findAllowancesByDepartment} — returns allowances filtered by department (admin)</li>
 * </ul>
 *
 * <p><strong>No POST search:</strong> Unlike leave requests, allowances don't need complex
 * multi-field filtering. The simple query methods here (by staffMemberId, managerId,
 * department, or all) cover all the use cases in the brief.
 *
 * @see LeaveAllowanceApplicationService for the CQRS write-side (command handler)
 * @see com.staffs.leavebooking.leavemanagement.LeaveManagementFacade for the facade that delegates to this handler
 * @see LeaveAllowanceJpaToDTOMapper for the JPA-to-DTO mapping logic (including derived field calculation)
 * @see LeaveAllowanceDTO for the read model returned to API consumers
 * @see LeaveAllowanceRepository for the persistence layer
 */
@Service            // Spring stereotype — registers as a service bean in the application context
@AllArgsConstructor // Lombok: generates constructor with all final fields (enables constructor-based DI)
public class LeaveAllowanceQueryHandler {

    /**
     * Spring Data repository for querying leave allowance JPA entities.
     * All queries in this handler delegate to this repository's finder methods.
     *
     * @see LeaveAllowanceRepository for the available query methods
     */
    private final LeaveAllowanceRepository leaveAllowanceRepository;

    /**
     * Finds the current (most recent business year) allowance for a specific staff member.
     *
     * <p>Used by GET /leave-allowances/my and GET /leave-allowances/staff/{staffMemberId}.
     * Returns the most recent business year's allowance by ordering by business year start
     * descending and taking the first result.
     *
     * @param staffMemberId the UUID of the staff member
     * @return the staff member's current leave allowance as a DTO
     * @throws LeaveAllowanceNotFoundException if no allowance exists for the staff member
     * @see LeaveAllowanceRepository#findFirstByStaffMemberIdOrderByBusinessYearStartDesc(String)
     * @see LeaveAllowanceJpaToDTOMapper#toDTO
     */
    public LeaveAllowanceDTO findAllowanceByStaffMemberId(String staffMemberId) {
        // Find the most recent allowance (ordered by business year descending), map to DTO, or throw
        return leaveAllowanceRepository.findFirstByStaffMemberIdOrderByBusinessYearStartDesc(staffMemberId)
                .map(LeaveAllowanceJpaToDTOMapper::toDTO)                       // Map JPA entity to DTO (calculates derived fields)
                .orElseThrow(() -> new LeaveAllowanceNotFoundException(staffMemberId)); // 404 if not found
    }

    /**
     * Finds a specific allowance by its UUID.
     *
     * <p>Used internally when a direct allowance ID lookup is needed (e.g., after amending
     * entitlement to return the updated record).
     *
     * @param allowanceId the UUID of the leave allowance
     * @return the leave allowance as a DTO
     * @throws LeaveAllowanceNotFoundException if no allowance exists with the given ID
     * @see LeaveAllowanceRepository#findById(Object)
     */
    public LeaveAllowanceDTO findAllowanceById(String allowanceId) {
        // Find by ID, map to DTO, or throw not-found exception
        return leaveAllowanceRepository.findById(allowanceId)
                .map(LeaveAllowanceJpaToDTOMapper::toDTO)                       // Map JPA entity to DTO
                .orElseThrow(() -> new LeaveAllowanceNotFoundException(allowanceId)); // 404 if not found
    }

    /**
     * Finds all allowances for a manager's team (the manager's direct reports).
     *
     * <p>Used by GET /leave-allowances/team — returns all allowances where the managerId
     * matches the authenticated manager. This lets managers see their team's leave balances.
     *
     * @param managerId the UUID of the manager
     * @return list of leave allowances for the manager's direct reports
     * @see LeaveAllowanceRepository#findByManagerId(String)
     */
    public List<LeaveAllowanceDTO> findAllowancesByManagerId(String managerId) {
        // Query the repository for all allowances managed by this manager
        return leaveAllowanceRepository.findByManagerId(managerId).stream()
                .map(LeaveAllowanceJpaToDTOMapper::toDTO) // Map each JPA entity to a DTO
                .collect(toList());                        // Collect into a List
    }

    /**
     * Finds all allowances company-wide (admin view, unfiltered).
     *
     * <p>Used by GET /leave-allowances/all (without the department query parameter).
     * Returns every leave allowance in the system.
     *
     * @return list of all leave allowances in the system as DTOs
     * @see LeaveAllowanceRepository#findAll()
     */
    public List<LeaveAllowanceDTO> findAllAllowances() {
        // Query the repository for all allowances (custom findAll() returns List, not Iterable)
        return leaveAllowanceRepository.findAll().stream()
                .map(LeaveAllowanceJpaToDTOMapper::toDTO) // Map each JPA entity to a DTO
                .collect(toList());                        // Collect into a List
    }

    /**
     * Finds all allowances filtered by department (admin view).
     *
     * <p>Used by GET /leave-allowances/all?department={department}. Allows admins to
     * see leave balances for an entire department (e.g., "Networks", "Digital").
     *
     * @param department the department name to filter by
     * @return list of leave allowances for staff in the specified department
     * @see LeaveAllowanceRepository#findByDepartment(String)
     */
    public List<LeaveAllowanceDTO> findAllowancesByDepartment(String department) {
        // Query the repository for allowances in the specified department
        return leaveAllowanceRepository.findByDepartment(department).stream()
                .map(LeaveAllowanceJpaToDTOMapper::toDTO) // Map each JPA entity to a DTO
                .collect(toList());                        // Collect into a List
    }
}
