package com.staffs.leavebooking.leavemanagement;

import com.staffs.leavebooking.leavemanagement.application.commands.AmendEntitlementCommand;
import com.staffs.leavebooking.leavemanagement.application.commands.CancelLeaveRequestCommand;
import com.staffs.leavebooking.leavemanagement.application.commands.SubmitLeaveRequestCommand;
import com.staffs.leavebooking.leavemanagement.application.dto.LeaveAllowanceDTO;
import com.staffs.leavebooking.leavemanagement.application.dto.LeaveRequestDTO;
import com.staffs.leavebooking.leavemanagement.application.dto.LeaveRequestSearchCriteria;
import com.staffs.leavebooking.leavemanagement.application.handlers.LeaveAllowanceApplicationService;
import com.staffs.leavebooking.leavemanagement.application.handlers.LeaveAllowanceQueryHandler;
import com.staffs.leavebooking.leavemanagement.application.handlers.LeaveRequestApplicationService;
import com.staffs.leavebooking.leavemanagement.application.handlers.LeaveRequestQueryHandler;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Context Facade (Open Host Service) for the Leave Management bounded context
 * (Lecture 4 — Bounded Context Integration, Open Host Service Pattern).
 *
 * <p><strong>DDD Concept — Open Host Service (Lecture 4):</strong> An Open Host Service
 * defines a well-known protocol (API) that external consumers use to interact with a
 * bounded context. This facade is the single entry point for all Leave Management
 * operations — controllers and other bounded contexts never bypass it to access
 * internal services directly. This is the ONLY public class in this module; all
 * sub-packages (application, domain, infrastructure, ui) are hidden behind it.
 *
 * <p><strong>Facade Pattern:</strong> This class acts as a simplified, unified API that
 * hides the internal complexity of the Leave Management context. Internally, it delegates
 * to four specialised handlers following the CQRS pattern:
 * <ul>
 *   <li>{@link LeaveRequestQueryHandler} — handles all leave request read-only (query) operations</li>
 *   <li>{@link LeaveRequestApplicationService} — handles all leave request write (command) operations</li>
 *   <li>{@link LeaveAllowanceQueryHandler} — handles all leave allowance read-only (query) operations</li>
 *   <li>{@link LeaveAllowanceApplicationService} — handles all leave allowance write (command) operations</li>
 * </ul>
 * External consumers (e.g., {@link com.staffs.leavebooking.leavemanagement.ui.LeaveRequestController})
 * interact only with this facade, never with the handlers directly.
 *
 * <p><strong>CQRS separation (Lecture 5/6):</strong> The methods in this class are
 * organised into four sections — LEAVE REQUEST QUERIES, LEAVE REQUEST SEARCH,
 * LEAVE REQUEST COMMANDS, and LEAVE ALLOWANCE QUERIES/COMMANDS — mirroring the CQRS split.
 * Query methods return DTO objects (read model), while command methods accept command records
 * and return void or an ID string.
 *
 * <p><strong>Security (RBAC via Spring Security):</strong> Every public method is
 * annotated with {@code @PreAuthorize} to enforce role-based access control:
 * <ul>
 *   <li>{@code hasRole('ADMIN')} — admin-only operations (view all, search all, amend entitlement)</li>
 *   <li>{@code hasAnyRole('MANAGER', 'ADMIN')} — managers can view team requests/allowances and approve/reject</li>
 *   <li>{@code hasAnyRole('STAFF', 'MANAGER', 'ADMIN')} — any authenticated staff can view their own data and submit/cancel</li>
 * </ul>
 *
 * <p><strong>Ownership checks:</strong> Note that ownership/team checks (e.g., "staff can
 * only see own requests", "manager can only approve their team's requests") are enforced
 * at the controller level before calling these facade methods. The facade trusts that the
 * controller has already verified the caller's relationship to the resource.
 *
 * <p><strong>Call flow:</strong>
 * <pre>
 * LeaveRequestController / LeaveAllowanceController (HTTP)
 *   → LeaveManagementFacade (security + routing)
 *     → LeaveRequestQueryHandler / LeaveAllowanceQueryHandler (reads — JPA → DTO)
 *     → LeaveRequestApplicationService / LeaveAllowanceApplicationService (writes — command → domain → JPA → events)
 * </pre>
 *
 * @see LeaveRequestQueryHandler for the CQRS read-side handler for leave requests
 * @see LeaveRequestApplicationService for the CQRS write-side handler for leave requests
 * @see LeaveAllowanceQueryHandler for the CQRS read-side handler for leave allowances
 * @see LeaveAllowanceApplicationService for the CQRS write-side handler for leave allowances
 * @see com.staffs.leavebooking.leavemanagement.ui.LeaveRequestController for the REST controller that calls this facade
 * @see com.staffs.leavebooking.leavemanagement.ui.LeaveAllowanceController for the REST controller that calls this facade
 * @see com.staffs.leavebooking.leavemanagement.application.dto.LeaveRequestDTO for the leave request read model
 * @see com.staffs.leavebooking.leavemanagement.application.dto.LeaveAllowanceDTO for the leave allowance read model
 */
@Component      // Spring-managed bean — registered in the application context for dependency injection
@AllArgsConstructor // Lombok: generates a constructor with all final fields (enables constructor-based DI)
public class LeaveManagementFacade {

    /**
     * CQRS query handler for leave requests — handles all read-only operations.
     * Maps JPA entities directly to DTOs without going through the domain aggregate
     * (read-side optimisation: no need to reconstitute the full aggregate for queries).
     */
    private final LeaveRequestQueryHandler leaveRequestQueryHandler;

    /**
     * CQRS command handler (application service) for leave requests — handles all write operations.
     * Follows the pattern: receive command → load/create domain aggregate → execute
     * domain logic → save to repository → dispatch domain events.
     */
    private final LeaveRequestApplicationService leaveRequestApplicationService;

    /**
     * CQRS query handler for leave allowances — handles all read-only allowance operations.
     * Maps JPA entities directly to DTOs, calculating derived fields (remainingDays, availableDays).
     */
    private final LeaveAllowanceQueryHandler leaveAllowanceQueryHandler;

    /**
     * CQRS command handler (application service) for leave allowances — handles all write operations.
     * Manages allowance balance changes (reserve, confirm, release, credit-back) and
     * admin entitlement amendments. Also handles cross-context event-driven creation/updates.
     */
    private final LeaveAllowanceApplicationService leaveAllowanceApplicationService;

    // ═══════════════════════════════════════════════════════════════════
    // LEAVE REQUEST — QUERIES (read-only operations — delegated to LeaveRequestQueryHandler)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Retrieves all leave requests submitted by a specific staff member (unfiltered).
     *
     * <p><strong>Access:</strong> STAFF, MANAGER, or ADMIN — any authenticated staff
     * member can view their own leave requests. The controller verifies that the
     * staffMemberId matches the authenticated user's ID (ownership check).
     *
     * <p><strong>Delegates to:</strong> {@link LeaveRequestQueryHandler#findRequestsByStaffMemberId(String)},
     * which calls {@link com.staffs.leavebooking.leavemanagement.infrastructure.repositories.LeaveRequestRepository#findByStaffMemberId(String)}
     * and maps each JPA entity to a {@link LeaveRequestDTO}.
     *
     * @param staffMemberId the UUID of the staff member whose requests to retrieve
     * @return list of the staff member's leave requests as DTOs
     * @see LeaveRequestQueryHandler#findRequestsByStaffMemberId(String)
     */
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')") // Any authenticated staff can view their own requests
    public List<LeaveRequestDTO> findMyRequests(String staffMemberId) {
        // Delegate to the query handler — it handles JPA-to-DTO mapping
        return leaveRequestQueryHandler.findRequestsByStaffMemberId(staffMemberId);
    }

    /**
     * Retrieves all leave requests assigned to a specific manager's team (unfiltered).
     *
     * <p><strong>Access:</strong> MANAGER or ADMIN — managers see requests from their
     * direct reports; admins can view any manager's team requests.
     *
     * <p><strong>Delegates to:</strong> {@link LeaveRequestQueryHandler#findRequestsByManagerId(String)},
     * which calls {@link com.staffs.leavebooking.leavemanagement.infrastructure.repositories.LeaveRequestRepository#findByManagerId(String)}.
     *
     * @param managerId the UUID of the manager whose team's requests to retrieve
     * @return list of the manager's team leave requests as DTOs
     * @see LeaveRequestQueryHandler#findRequestsByManagerId(String)
     */
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')") // Managers can view their team's requests
    public List<LeaveRequestDTO> findTeamRequests(String managerId) {
        // Delegate to the query handler — uses repository.findByManagerId()
        return leaveRequestQueryHandler.findRequestsByManagerId(managerId);
    }

    /**
     * Retrieves all leave requests company-wide (unfiltered).
     *
     * <p><strong>Access:</strong> ADMIN only — this returns every leave request
     * across all staff and managers in the system.
     *
     * <p><strong>Delegates to:</strong> {@link LeaveRequestQueryHandler#findAllRequests()},
     * which calls {@link com.staffs.leavebooking.leavemanagement.infrastructure.repositories.LeaveRequestRepository#findAll()}.
     *
     * @return list of all leave requests as DTOs
     * @see LeaveRequestQueryHandler#findAllRequests()
     */
    @PreAuthorize("hasRole('ADMIN')") // Only admins can see all leave requests company-wide
    public List<LeaveRequestDTO> findAllRequests() {
        // Delegate to the query handler — returns all requests in the system
        return leaveRequestQueryHandler.findAllRequests();
    }

    /**
     * Retrieves a single leave request by its unique ID.
     *
     * <p><strong>Access:</strong> STAFF, MANAGER, or ADMIN — any authenticated user
     * can look up a leave request by ID. The controller applies ownership/team checks
     * before allowing approve/reject/cancel actions on the returned request.
     *
     * <p><strong>Delegates to:</strong> {@link LeaveRequestQueryHandler#findRequestById(String)},
     * which throws {@link com.staffs.leavebooking.leavemanagement.ui.exceptions.LeaveRequestNotFoundException}
     * if no request exists with the given ID.
     *
     * @param leaveRequestId the UUID of the leave request to retrieve
     * @return the leave request's data as a {@link LeaveRequestDTO}
     * @throws com.staffs.leavebooking.leavemanagement.ui.exceptions.LeaveRequestNotFoundException if not found
     * @see LeaveRequestQueryHandler#findRequestById(String)
     */
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')") // Any authenticated user can look up a request by ID
    public LeaveRequestDTO findRequestById(String leaveRequestId) {
        // Delegate to the query handler — throws LeaveRequestNotFoundException if not found
        return leaveRequestQueryHandler.findRequestById(leaveRequestId);
    }

    // ═══════════════════════════════════════════════════════════════════
    // LEAVE REQUEST — SEARCH (POST /search endpoints — filtered queries)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Searches the current user's own leave requests with optional filters.
     *
     * <p><strong>Enterprise POST search pattern:</strong> Uses a POST request body
     * with {@link LeaveRequestSearchCriteria} rather than query parameters — the same
     * pattern used by Elasticsearch and Stripe for complex, multi-field filtering.
     *
     * <p><strong>Security note:</strong> The staffMemberId is derived from the JWT —
     * never accepted from the search criteria body. This prevents users from searching
     * other people's requests through this endpoint.
     *
     * <p><strong>Supported filters:</strong> {@code status} (for staff's own requests).
     *
     * @param staffMemberId the UUID of the authenticated staff member (from JWT)
     * @param criteria      the search criteria containing optional filters
     * @return list of matching leave requests as DTOs
     * @see LeaveRequestQueryHandler#searchByStaffMember(String, LeaveRequestSearchCriteria)
     * @see LeaveRequestSearchCriteria
     */
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')") // Any authenticated staff can search their own requests
    public List<LeaveRequestDTO> searchMyRequests(String staffMemberId, LeaveRequestSearchCriteria criteria) {
        // Delegate to the query handler — staffMemberId is from JWT, not from the criteria body
        return leaveRequestQueryHandler.searchByStaffMember(staffMemberId, criteria);
    }

    /**
     * Searches a manager's team leave requests with optional filters.
     *
     * <p><strong>Security note:</strong> The managerId is derived from the JWT —
     * never accepted from the search criteria body. This prevents managers from
     * searching other managers' teams through this endpoint.
     *
     * <p><strong>Supported filters:</strong> {@code status}, {@code from}, {@code to}
     * (date range). This satisfies the brief's optional enhancement: "could be enhanced
     * with start and end dates to reduce the reporting period."
     *
     * @param managerId the UUID of the authenticated manager (from JWT)
     * @param criteria  the search criteria containing optional status and date range filters
     * @return list of matching team leave requests as DTOs
     * @see LeaveRequestQueryHandler#searchByManager(String, LeaveRequestSearchCriteria)
     * @see LeaveRequestSearchCriteria
     */
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')") // Managers can search their team's requests
    public List<LeaveRequestDTO> searchTeamRequests(String managerId, LeaveRequestSearchCriteria criteria) {
        // Delegate to the query handler — managerId is from JWT, not from the criteria body
        return leaveRequestQueryHandler.searchByManager(managerId, criteria);
    }

    /**
     * Searches all leave requests company-wide with optional filters (admin only).
     *
     * <p><strong>Supported filters:</strong> {@code status}, {@code staffMemberId},
     * {@code managerId}, {@code from}, {@code to} — all optional, any combination.
     * This single method replaces the need for separate filter endpoints and satisfies
     * the brief requirement: "View all outstanding leave requests filtered by staff member,
     * manager's team or across the company."
     *
     * @param criteria the search criteria containing any combination of optional filters
     * @return list of matching leave requests as DTOs
     * @see LeaveRequestQueryHandler#searchAll(LeaveRequestSearchCriteria)
     * @see LeaveRequestSearchCriteria
     */
    @PreAuthorize("hasRole('ADMIN')") // Only admins can search all leave requests company-wide
    public List<LeaveRequestDTO> searchAllRequests(LeaveRequestSearchCriteria criteria) {
        // Delegate to the query handler — routes to the right repository method based on filter combination
        return leaveRequestQueryHandler.searchAll(criteria);
    }

    // ═══════════════════════════════════════════════════════════════════
    // LEAVE REQUEST — COMMANDS (write operations — delegated to LeaveRequestApplicationService)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Submits a new leave request on behalf of the authenticated staff member.
     *
     * <p><strong>Access:</strong> STAFF, MANAGER, or ADMIN — any authenticated staff
     * member can submit a leave request. The controller verifies that the staff member's
     * status is ACTIVE before allowing submission.
     *
     * <p><strong>Domain flow:</strong> The application service creates a new
     * {@link com.staffs.leavebooking.leavemanagement.domain.LeaveRequest} aggregate,
     * which validates the data and raises a
     * {@link com.staffs.leavebooking.leavemanagement.domain.events.LeaveRequestSubmittedEvent}.
     * A local event listener then reserves days on the staff member's
     * {@link com.staffs.leavebooking.leavemanagement.domain.LeaveAllowance}.
     *
     * @param command the command containing staffMemberId, managerId, dates, leaveType, and reason
     * @return the generated UUID of the newly created leave request
     * @see LeaveRequestApplicationService#submitNewRequest(SubmitLeaveRequestCommand)
     * @see SubmitLeaveRequestCommand
     */
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')") // Any authenticated staff can submit a leave request
    public String submitLeaveRequest(SubmitLeaveRequestCommand command) {
        // Delegate to the application service — creates domain aggregate + saves to JPA + dispatches events
        return leaveRequestApplicationService.submitNewRequest(command);
    }

    /**
     * Approves a pending leave request.
     *
     * <p><strong>Access:</strong> MANAGER or ADMIN — only managers (or admins) can approve
     * leave requests. The controller verifies that the approver is the assigned manager
     * or an admin before calling this method.
     *
     * <p><strong>Domain flow:</strong> The application service loads the
     * {@link com.staffs.leavebooking.leavemanagement.domain.LeaveRequest} aggregate,
     * calls {@code approve()}, which transitions status PENDING → APPROVED and raises a
     * {@link com.staffs.leavebooking.leavemanagement.domain.events.LeaveRequestApprovedEvent}.
     * A local event listener then confirms days on the allowance (daysPending → daysUsed).
     *
     * @param leaveRequestId the UUID of the leave request to approve
     * @param decidedBy      the UUID of the manager/admin who is approving
     * @param reason         optional reason for the approval decision (may be null)
     * @see LeaveRequestApplicationService#approveRequest(String, String, String)
     */
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')") // Only managers or admins can approve requests
    public void approveLeaveRequest(String leaveRequestId, String decidedBy, String reason) {
        // Delegate to the application service — loads aggregate, executes approve, saves, dispatches events
        leaveRequestApplicationService.approveRequest(leaveRequestId, decidedBy, reason);
    }

    /**
     * Rejects a pending leave request.
     *
     * <p><strong>Access:</strong> MANAGER or ADMIN — only managers (or admins) can reject
     * leave requests. The controller verifies that the rejector is the assigned manager
     * or an admin before calling this method.
     *
     * <p><strong>Domain flow:</strong> The application service loads the
     * {@link com.staffs.leavebooking.leavemanagement.domain.LeaveRequest} aggregate,
     * calls {@code reject()}, which transitions status PENDING → REJECTED and raises a
     * {@link com.staffs.leavebooking.leavemanagement.domain.events.LeaveRequestRejectedEvent}.
     * A local event listener then releases the pending days back to the allowance.
     *
     * @param leaveRequestId the UUID of the leave request to reject
     * @param decidedBy      the UUID of the manager/admin who is rejecting
     * @param reason         optional reason for the rejection decision (may be null)
     * @see LeaveRequestApplicationService#rejectRequest(String, String, String)
     */
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')") // Only managers or admins can reject requests
    public void rejectLeaveRequest(String leaveRequestId, String decidedBy, String reason) {
        // Delegate to the application service — loads aggregate, executes reject, saves, dispatches events
        leaveRequestApplicationService.rejectRequest(leaveRequestId, decidedBy, reason);
    }

    /**
     * Cancels a pending or approved leave request.
     *
     * <p><strong>Access:</strong> STAFF, MANAGER, or ADMIN — staff members can cancel
     * their own requests; admins can cancel any request. The controller verifies
     * ownership before calling this method.
     *
     * <p><strong>Domain flow:</strong> The application service loads the
     * {@link com.staffs.leavebooking.leavemanagement.domain.LeaveRequest} aggregate,
     * calls {@code cancel()}, which transitions status to CANCELLED and raises a
     * {@link com.staffs.leavebooking.leavemanagement.domain.events.LeaveRequestCancelledEvent}.
     * A local event listener then either releases pending days (if was PENDING) or
     * credits back used days (if was APPROVED) on the allowance.
     *
     * @param command the command containing leaveRequestId, cancelledBy, and optional reason
     * @see LeaveRequestApplicationService#cancelRequest(CancelLeaveRequestCommand)
     * @see CancelLeaveRequestCommand
     */
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')") // Staff can cancel their own; admins can cancel any
    public void cancelLeaveRequest(CancelLeaveRequestCommand command) {
        // Delegate to the application service — loads aggregate, executes cancel, saves, dispatches events
        leaveRequestApplicationService.cancelRequest(command);
    }

    // ═══════════════════════════════════════════════════════════════════
    // LEAVE ALLOWANCE — QUERIES (read-only operations — delegated to LeaveAllowanceQueryHandler)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Retrieves the current leave allowance for the authenticated staff member.
     *
     * <p><strong>Access:</strong> STAFF, MANAGER, or ADMIN — any authenticated staff
     * member can view their own remaining leave balance.
     *
     * <p><strong>Delegates to:</strong> {@link LeaveAllowanceQueryHandler#findAllowanceByStaffMemberId(String)},
     * which retrieves the most recent business year's allowance and maps it to a
     * {@link LeaveAllowanceDTO} including derived fields (remainingDays, availableDays).
     *
     * @param staffMemberId the UUID of the staff member whose allowance to retrieve
     * @return the staff member's leave allowance as a DTO
     * @throws com.staffs.leavebooking.leavemanagement.ui.exceptions.LeaveAllowanceNotFoundException if not found
     * @see LeaveAllowanceQueryHandler#findAllowanceByStaffMemberId(String)
     */
    @PreAuthorize("hasAnyRole('STAFF', 'MANAGER', 'ADMIN')") // Any authenticated staff can view their own allowance
    public LeaveAllowanceDTO findMyAllowance(String staffMemberId) {
        // Delegate to the query handler — returns the most recent year's allowance
        return leaveAllowanceQueryHandler.findAllowanceByStaffMemberId(staffMemberId);
    }

    /**
     * Retrieves a leave allowance by its unique allowance ID (not staffMemberId).
     * Used after amendEntitlement to return the updated allowance.
     */
    @PreAuthorize("hasRole('ADMIN')")
    public LeaveAllowanceDTO findAllowanceById(String allowanceId) {
        return leaveAllowanceQueryHandler.findAllowanceById(allowanceId);
    }

    /**
     * Retrieves the leave allowance for a specific staff member (manager/admin viewing another's allowance).
     *
     * <p><strong>Access:</strong> MANAGER or ADMIN — managers can view their team members'
     * allowances; admins can view anyone's. This satisfies the brief requirement:
     * "View the amount of annual leave remaining for a member of staff."
     *
     * @param staffMemberId the UUID of the staff member whose allowance to view
     * @return the staff member's leave allowance as a DTO
     * @throws com.staffs.leavebooking.leavemanagement.ui.exceptions.LeaveAllowanceNotFoundException if not found
     * @see LeaveAllowanceQueryHandler#findAllowanceByStaffMemberId(String)
     */
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')") // Managers can view their team's allowances
    public LeaveAllowanceDTO findAllowanceForStaffMember(String staffMemberId) {
        // Delegate to the query handler — same underlying query as findMyAllowance
        return leaveAllowanceQueryHandler.findAllowanceByStaffMemberId(staffMemberId);
    }

    /**
     * Retrieves leave allowances for all staff members managed by a specific manager.
     *
     * <p><strong>Access:</strong> MANAGER or ADMIN — the primary query for the manager
     * dashboard showing their team's leave balances.
     *
     * @param managerId the UUID of the manager whose team's allowances to retrieve
     * @return list of leave allowances for the manager's direct reports
     * @see LeaveAllowanceQueryHandler#findAllowancesByManagerId(String)
     */
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')") // Managers can view their team's allowances
    public List<LeaveAllowanceDTO> findTeamAllowances(String managerId) {
        // Delegate to the query handler — uses repository.findByManagerId()
        return leaveAllowanceQueryHandler.findAllowancesByManagerId(managerId);
    }

    /**
     * Retrieves all leave allowances company-wide (unfiltered).
     *
     * <p><strong>Access:</strong> ADMIN only — this returns every leave allowance
     * across all staff members in the system.
     *
     * @return list of all leave allowances as DTOs
     * @see LeaveAllowanceQueryHandler#findAllAllowances()
     */
    @PreAuthorize("hasRole('ADMIN')") // Only admins can see all leave allowances company-wide
    public List<LeaveAllowanceDTO> findAllAllowances() {
        // Delegate to the query handler — returns all allowances in the system
        return leaveAllowanceQueryHandler.findAllAllowances();
    }

    /**
     * Retrieves all leave allowances filtered by department.
     *
     * <p><strong>Access:</strong> ADMIN only — useful for viewing leave balances
     * across an entire department (e.g., "Networks", "Digital").
     *
     * @param department the department name to filter by
     * @return list of leave allowances for staff in the specified department
     * @see LeaveAllowanceQueryHandler#findAllowancesByDepartment(String)
     */
    @PreAuthorize("hasRole('ADMIN')") // Only admins can filter allowances by department
    public List<LeaveAllowanceDTO> findAllowancesByDepartment(String department) {
        // Delegate to the query handler — uses repository.findByDepartment()
        return leaveAllowanceQueryHandler.findAllowancesByDepartment(department);
    }

    // ═══════════════════════════════════════════════════════════════════
    // LEAVE ALLOWANCE — COMMANDS (write operations — delegated to LeaveAllowanceApplicationService)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Amends a staff member's total leave entitlement (admin operation).
     *
     * <p><strong>Access:</strong> ADMIN only — entitlement changes are administrative actions
     * that affect how many days a staff member can book in total for the business year.
     *
     * <p><strong>Domain flow:</strong> The application service loads the
     * {@link com.staffs.leavebooking.leavemanagement.domain.LeaveAllowance} aggregate,
     * calls {@code amendEntitlement()}, which updates the totalEntitlement and
     * recalculates derived fields. The domain validates that the new entitlement
     * is not less than the days already used.
     *
     * @param command the command containing the leaveAllowanceId and the new entitlement value
     * @see LeaveAllowanceApplicationService#amendEntitlement(AmendEntitlementCommand)
     * @see AmendEntitlementCommand
     */
    @PreAuthorize("hasRole('ADMIN')") // Only admins can amend leave entitlements
    public void amendEntitlement(AmendEntitlementCommand command) {
        // Delegate to the application service — loads aggregate, amends entitlement, saves
        leaveAllowanceApplicationService.amendEntitlement(command);
    }
}
