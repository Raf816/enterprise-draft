package com.staffs.leavebooking.leavemanagement.ui;

import com.staffs.leavebooking.leavemanagement.LeaveManagementFacade;
import com.staffs.leavebooking.leavemanagement.application.commands.AmendEntitlementCommand;
import com.staffs.leavebooking.leavemanagement.application.dto.LeaveAllowanceDTO;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Leave Allowance operations at {@code /leave-allowances}
 * (Lecture 4 — User Interface Layer, Lecture 5/6 — CQRS).
 *
 * <p><strong>Thin controller:</strong> This class is a thin HTTP layer that:
 * <ol>
 *   <li>Validates HTTP input (path variables, query parameters, request bodies)</li>
 *   <li>Extracts the authenticated user's identity from the JWT token</li>
 *   <li>Delegates to the {@link LeaveManagementFacade} for business logic</li>
 *   <li>Returns the response DTO as JSON</li>
 * </ol>
 *
 * <p><strong>Endpoints (5 total):</strong>
 * <ul>
 *   <li>GET /leave-allowances/my — view current user's own allowance</li>
 *   <li>GET /leave-allowances/staff/{staffMemberId} — view a specific staff member's allowance</li>
 *   <li>GET /leave-allowances/team — view all allowances for the manager's team</li>
 *   <li>GET /leave-allowances/all?department={optional} — admin: view all, optionally filtered by department</li>
 *   <li>PATCH /leave-allowances/{id} — admin: amend a staff member's total entitlement</li>
 * </ul>
 *
 * <p><strong>No POST search:</strong> Unlike {@link LeaveRequestController}, allowances
 * don't need complex multi-field filtering. A simple query parameter ({@code department})
 * on the GET /all endpoint is sufficient.
 *
 * @see LeaveManagementFacade for the bounded context facade this controller delegates to
 * @see LeaveAllowanceDTO for the read model returned by all query endpoints
 * @see AmendEntitlementCommand for the CQRS command used by the PATCH endpoint
 */
@RestController                         // Spring MVC: marks as a REST controller (auto-serialises return values to JSON)
@RequestMapping("/leave-allowances")    // Base URL path for all endpoints in this controller
@AllArgsConstructor                     // Lombok: generates constructor with all final fields (enables constructor-based DI)
public class LeaveAllowanceController {

    /** Leave Management facade — the single entry point for all leave operations in this bounded context. */
    private final LeaveManagementFacade facade;

    // ─────────────────────────────────────────────────────────────────
    // QUERIES
    // ─────────────────────────────────────────────────────────────────

    /**
     * GET /leave-allowances/my — View the current user's own remaining leave balance.
     *
     * <p>The staffMemberId is extracted from the authenticated user's JWT token
     * (Firebase UID), ensuring users can only see their own allowance through this endpoint.
     *
     * <p>The returned DTO includes derived fields: {@code remainingDays} (entitlement - used)
     * and {@code availableDays} (entitlement - used - pending), calculated at mapping time.
     *
     * @param authentication Spring Security authentication object containing the user's identity
     * @return the authenticated user's leave allowance as a DTO
     * @throws com.staffs.leavebooking.leavemanagement.ui.exceptions.LeaveAllowanceNotFoundException if no allowance exists
     * @see LeaveManagementFacade#findMyAllowance(String)
     */
    @GetMapping("/my")                  // Maps HTTP GET /leave-allowances/my
    @ResponseStatus(HttpStatus.OK)      // Returns 200 OK on success
    public LeaveAllowanceDTO getMyAllowance(Authentication authentication) {
        // Extract the staff member ID (Firebase UID) from the JWT token
        String staffMemberId = authentication.getName();
        // Delegate to facade — returns the most recent business year's allowance
        return facade.findMyAllowance(staffMemberId);
    }

    /**
     * GET /leave-allowances/staff/{staffMemberId} — View a specific staff member's allowance.
     *
     * <p>Used by managers to view their team members' allowances, and by admins to
     * view anyone's allowance. RBAC at the facade level restricts access to MANAGER and ADMIN.
     *
     * <p>This satisfies the brief requirement: "View the amount of annual leave
     * remaining for a member of staff."
     *
     * @param staffMemberId the UUID of the staff member whose allowance to view
     * @return the staff member's leave allowance as a DTO
     * @throws com.staffs.leavebooking.leavemanagement.ui.exceptions.LeaveAllowanceNotFoundException if not found
     * @see LeaveManagementFacade#findAllowanceForStaffMember(String)
     */
    @GetMapping("/staff/{staffMemberId}") // Maps HTTP GET /leave-allowances/staff/{staffMemberId}
    @ResponseStatus(HttpStatus.OK)        // Returns 200 OK on success
    public LeaveAllowanceDTO getAllowanceForStaff(@PathVariable String staffMemberId) {
        // Delegate directly to facade — RBAC enforced at facade level (MANAGER or ADMIN)
        return facade.findAllowanceForStaffMember(staffMemberId);
    }

    /**
     * GET /leave-allowances/team — View allowances for all team members (manager's direct reports).
     *
     * <p>The managerId is extracted from the authenticated user's JWT token, so managers
     * can only see their own team's allowances through this endpoint.
     *
     * @param authentication Spring Security authentication object containing the manager's identity
     * @return list of leave allowances for the manager's direct reports
     * @see LeaveManagementFacade#findTeamAllowances(String)
     */
    @GetMapping("/team")                // Maps HTTP GET /leave-allowances/team
    @ResponseStatus(HttpStatus.OK)      // Returns 200 OK on success
    public List<LeaveAllowanceDTO> getTeamAllowances(Authentication authentication) {
        // Extract the manager's ID from the JWT token
        String managerId = authentication.getName();
        // Delegate to facade — uses repository.findByManagerId()
        return facade.findTeamAllowances(managerId);
    }

    /**
     * GET /leave-allowances/all?department={optional} — Admin: view all allowances company-wide,
     * optionally filtered by department.
     *
     * <p>If the {@code department} query parameter is provided and non-blank, returns only
     * allowances for staff in that department. Otherwise, returns all allowances.
     *
     * <p>This uses a simple query parameter rather than a POST search body because
     * department is the only filter needed for allowances.
     *
     * @param department optional department name to filter by (e.g., "Networks", "Digital")
     * @return list of leave allowances (all or filtered by department)
     * @see LeaveManagementFacade#findAllAllowances()
     * @see LeaveManagementFacade#findAllowancesByDepartment(String)
     */
    @GetMapping("/all")                 // Maps HTTP GET /leave-allowances/all
    @ResponseStatus(HttpStatus.OK)      // Returns 200 OK on success
    public List<LeaveAllowanceDTO> getAllAllowances(
            @RequestParam(required = false) String department) {

        // Check if the optional department filter is provided and non-blank
        if (department != null && !department.isBlank()) {
            // Filter by department — uses repository.findByDepartment()
            return facade.findAllowancesByDepartment(department);
        }
        // No filter — return all allowances company-wide
        return facade.findAllAllowances();
    }

    // ─────────────────────────────────────────────────────────────────
    // COMMANDS
    // ─────────────────────────────────────────────────────────────────

    /**
     * PATCH /leave-allowances/{id} — Admin: amend a staff member's total leave entitlement.
     *
     * <p><strong>Access:</strong> ADMIN only — entitlement changes are administrative actions.
     * RBAC is enforced at the facade level.
     *
     * <p><strong>Domain validation:</strong> The domain aggregate validates that the new
     * entitlement is not less than the days already used. If it is, an exception is thrown.
     *
     * <p><strong>Response:</strong> Returns the updated allowance DTO so the admin
     * can see the new entitlement and recalculated derived fields.
     *
     * @param id   the UUID of the leave allowance to amend
     * @param body the request body containing the new entitlement value
     * @return the updated leave allowance as a DTO
     * @throws com.staffs.leavebooking.leavemanagement.ui.exceptions.LeaveAllowanceNotFoundException if not found
     * @see LeaveManagementFacade#amendEntitlement(AmendEntitlementCommand)
     * @see AmendEntitlementCommand
     */
    @PatchMapping("/{id}")              // Maps HTTP PATCH /leave-allowances/{id}
    @ResponseStatus(HttpStatus.OK)      // Returns 200 OK on success
    public LeaveAllowanceDTO amendEntitlement(
            @PathVariable String id,
            @RequestBody AmendEntitlementBody body) {

        // Build the CQRS command from the path variable (allowance ID) and request body (new entitlement)
        AmendEntitlementCommand command = new AmendEntitlementCommand(id, body.newEntitlement());
        // Delegate to facade — loads aggregate, amends entitlement, saves
        facade.amendEntitlement(command);

        // Return the updated allowance so the admin sees the new entitlement and derived fields
        return facade.findMyAllowance(id);
    }

    // ─────────────────────────────────────────────────────────────────
    // REQUEST BODY RECORDS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Request body record for PATCH /leave-allowances/{id}.
     *
     * <p>Contains only the new entitlement value. The allowance ID comes from the
     * URL path variable, not the body (RESTful convention).
     *
     * <p><strong>Note:</strong> This is an inner record rather than a separate file
     * because it's only used by this controller's single PATCH endpoint. If more
     * command endpoints were added, it would be extracted to its own file.
     *
     * @param newEntitlement the new total leave entitlement (in days) for the staff member
     */
    public record AmendEntitlementBody(int newEntitlement) {}
}
