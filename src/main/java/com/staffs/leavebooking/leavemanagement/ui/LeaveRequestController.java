package com.staffs.leavebooking.leavemanagement.ui;

import com.staffs.leavebooking.leavemanagement.LeaveManagementFacade;
import com.staffs.leavebooking.leavemanagement.application.commands.CancelLeaveRequestCommand;
import com.staffs.leavebooking.leavemanagement.application.commands.SubmitLeaveRequestCommand;
import com.staffs.leavebooking.leavemanagement.application.dto.LeaveAllowanceDTO;
import com.staffs.leavebooking.leavemanagement.application.dto.LeaveRequestDTO;
import com.staffs.leavebooking.leavemanagement.application.dto.LeaveRequestSearchCriteria;
import com.staffs.leavebooking.leavemanagement.domain.DateRange;
import com.staffs.leavebooking.staffmanagement.StaffManagementFacade;
import com.staffs.leavebooking.staffmanagement.application.dto.StaffMemberDTO;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * REST controller for Leave Request operations at {@code /leave-requests}
 * (Lecture 4 — User Interface Layer, Lecture 5/6 — CQRS).
 *
 * <p><strong>Thin controller:</strong> This class is a thin HTTP layer that:
 * <ol>
 *   <li>Validates HTTP input (Bean Validation via {@code @Valid}, search criteria checks)</li>
 *   <li>Extracts the authenticated user's identity from the JWT token</li>
 *   <li>Enforces ownership/team access checks (verifyManagerOrAdmin, verifyOwnerOrAdmin, verifyStaffIsActive)</li>
 *   <li>Delegates to the {@link LeaveManagementFacade} for business logic</li>
 *   <li>Returns the response DTO as JSON</li>
 * </ol>
 *
 * <p><strong>API design follows two conventions:</strong>
 * <ul>
 *   <li><strong>GET endpoints</strong> (4 total) — simple unfiltered reads (no query params).
 *       Used when the caller wants all results for a given scope (my, team, all, by ID).</li>
 *   <li><strong>POST /search endpoints</strong> (3 total) — filtered queries via structured JSON body.
 *       This enterprise search pattern (used by Elasticsearch, Stripe, etc.) keeps URLs clean
 *       when multiple optional filters are needed (status, date range, person filters).
 *       The controller validates that at least one filter is provided before delegating.</li>
 *   <li><strong>POST / PATCH endpoints</strong> (4 total) — write operations for submit, approve, reject, cancel.</li>
 * </ul>
 *
 * <p><strong>Security model:</strong> RBAC is enforced at the facade level via
 * {@code @PreAuthorize}. Ownership/team checks (e.g., "only the assigned manager can approve",
 * "staff can only cancel their own") are enforced here in the controller via helper methods
 * because they require inspecting the leave request data, which the facade doesn't do.
 *
 * <p><strong>Cross-context dependency:</strong> This controller depends on
 * {@link StaffManagementFacade} to verify staff member status (ACTIVE/PENDING_SETUP/TERMINATED)
 * before allowing leave submission. This is a controlled cross-context call via the staff
 * management Open Host Service.
 *
 * @see LeaveManagementFacade for the bounded context facade this controller delegates to
 * @see StaffManagementFacade for staff status verification during leave submission
 * @see LeaveRequestDTO for the read model returned by all endpoints
 * @see LeaveRequestSearchCriteria for the POST search filter structure
 * @see SubmitLeaveRequestBody for the POST submit request body with Bean Validation
 */
@RestController                     // Spring MVC: marks as a REST controller (auto-serialises return values to JSON)
@RequestMapping("/leave-requests")  // Base URL path for all endpoints in this controller
@AllArgsConstructor                 // Lombok: generates constructor with all final fields (enables constructor-based DI)
public class LeaveRequestController {

    /** Leave Management facade — the single entry point for all leave operations in this bounded context. */
    private final LeaveManagementFacade facade;

    /**
     * Staff Management facade — used to verify staff member status before allowing
     * leave submission. Cross-context call via Open Host Service pattern (Lecture 4).
     */
    private final StaffManagementFacade staffFacade;

    // ─────────────────────────────────────────────────────────────────
    // QUERIES (GET — simple, unfiltered reads)
    // ─────────────────────────────────────────────────────────────────

    /**
     * GET /leave-requests/my — View current user's own leave requests (all, unfiltered).
     *
     * <p>The staffMemberId is extracted from the authenticated user's JWT token
     * (Firebase UID), ensuring users can only see their own requests through this endpoint.
     *
     * @param authentication Spring Security authentication object containing the user's identity
     * @return list of the authenticated user's leave requests as DTOs
     * @see LeaveManagementFacade#findMyRequests(String)
     */
    @GetMapping("/my")                  // Maps HTTP GET /leave-requests/my
    @ResponseStatus(HttpStatus.OK)      // Returns 200 OK on success
    public List<LeaveRequestDTO> getMyRequests(Authentication authentication) {
        // Extract the staff member ID (Firebase UID) from the JWT token and delegate to facade
        return facade.findMyRequests(extractStaffMemberId(authentication));
    }

    /**
     * GET /leave-requests/team — View all requests for the authenticated manager's team (unfiltered).
     *
     * <p>The managerId is extracted from the authenticated user's JWT token,
     * so managers can only see their own team's requests through this endpoint.
     *
     * @param authentication Spring Security authentication object containing the manager's identity
     * @return list of the manager's team leave requests as DTOs
     * @see LeaveManagementFacade#findTeamRequests(String)
     */
    @GetMapping("/team")                // Maps HTTP GET /leave-requests/team
    @ResponseStatus(HttpStatus.OK)      // Returns 200 OK on success
    public List<LeaveRequestDTO> getTeamRequests(Authentication authentication) {
        // Extract the manager's ID from the JWT token and delegate to facade
        return facade.findTeamRequests(extractStaffMemberId(authentication));
    }

    /**
     * GET /leave-requests/all — Admin: view all requests company-wide (unfiltered).
     *
     * <p>No authentication parameter needed — RBAC at the facade level ensures
     * only admins can call this. No ownership check required for admin-only endpoints.
     *
     * @return list of all leave requests in the system as DTOs
     * @see LeaveManagementFacade#findAllRequests()
     */
    @GetMapping("/all")                 // Maps HTTP GET /leave-requests/all
    @ResponseStatus(HttpStatus.OK)      // Returns 200 OK on success
    public List<LeaveRequestDTO> getAllRequests() {
        // No user extraction needed — admin-only endpoint, RBAC enforced at facade level
        return facade.findAllRequests();
    }

    /**
     * GET /leave-requests/{id} — View a specific leave request by its UUID.
     *
     * <p>Any authenticated user can look up a request by ID. The data returned
     * is used by the frontend to display request details and determine which
     * actions (approve/reject/cancel) are available for the current user.
     *
     * @param id the UUID of the leave request to retrieve
     * @return the leave request data as a DTO
     * @throws com.staffs.leavebooking.leavemanagement.ui.exceptions.LeaveRequestNotFoundException if not found
     * @see LeaveManagementFacade#findRequestById(String)
     */
    @GetMapping("/{id}")                // Maps HTTP GET /leave-requests/{id} with path variable
    @ResponseStatus(HttpStatus.OK)      // Returns 200 OK on success
    public LeaveRequestDTO getRequestById(@PathVariable String id) {
        // Delegate directly to facade — no ownership check for read-by-ID
        return facade.findRequestById(id);
    }

    // ─────────────────────────────────────────────────────────────────
    // SEARCH (POST — filtered queries via JSON body)
    // ─────────────────────────────────────────────────────────────────

    /**
     * POST /leave-requests/my/search — Search the current user's own leave requests with optional filters.
     *
     * <p>The staffMemberId is derived from the JWT — not accepted in the body. This prevents
     * a user from searching another person's requests by injecting a different staffMemberId.
     *
     * <p><strong>Supported filters:</strong> {@code status}
     * <p><strong>Example body:</strong> {@code {"status": "PENDING"}}
     *
     * <p>Validates that at least one filter is provided; returns 400 with a helpful message
     * pointing to the GET endpoint if no filters are set.
     *
     * @param authentication Spring Security authentication object containing the user's identity
     * @param criteria       the search criteria JSON body with optional filters
     * @return list of matching leave requests as DTOs
     * @throws ResponseStatusException 400 if no filters are provided
     * @see LeaveManagementFacade#searchMyRequests(String, LeaveRequestSearchCriteria)
     * @see LeaveRequestSearchCriteria#hasFilters()
     */
    @PostMapping("/my/search")          // Maps HTTP POST /leave-requests/my/search
    @ResponseStatus(HttpStatus.OK)      // Returns 200 OK on success
    public List<LeaveRequestDTO> searchMyRequests(
            Authentication authentication,
            @RequestBody LeaveRequestSearchCriteria criteria) {

        // Validate that at least one search filter is present — directs user to GET endpoint otherwise
        validateSearchCriteria(criteria, "GET /leave-requests/my");
        // Reject person-scope filters — /my/search derives the scope from the JWT
        rejectPersonFilters(criteria, "/my/search");
        // Extract staffMemberId from JWT and delegate to facade with the validated criteria
        return facade.searchMyRequests(extractStaffMemberId(authentication), criteria);
    }

    /**
     * POST /leave-requests/team/search — Search the authenticated manager's team requests with optional filters.
     *
     * <p>The managerId is derived from the JWT — not accepted in the body. This prevents
     * a manager from searching another manager's team by injecting a different managerId.
     *
     * <p><strong>Supported filters:</strong> {@code status}, {@code from}, {@code to}
     * <p><strong>Example body:</strong> {@code {"status": "PENDING", "from": "2026-09-01", "to": "2026-12-31"}}
     *
     * <p>This satisfies the brief's optional enhancement: "could be enhanced with start
     * and end dates to reduce the reporting period."
     *
     * @param authentication Spring Security authentication object containing the manager's identity
     * @param criteria       the search criteria JSON body with optional filters
     * @return list of matching team leave requests as DTOs
     * @throws ResponseStatusException 400 if no filters are provided
     * @see LeaveManagementFacade#searchTeamRequests(String, LeaveRequestSearchCriteria)
     */
    @PostMapping("/team/search")        // Maps HTTP POST /leave-requests/team/search
    @ResponseStatus(HttpStatus.OK)      // Returns 200 OK on success
    public List<LeaveRequestDTO> searchTeamRequests(
            Authentication authentication,
            @RequestBody LeaveRequestSearchCriteria criteria) {

        // Validate that at least one search filter is present — directs user to GET endpoint otherwise
        validateSearchCriteria(criteria, "GET /leave-requests/team");
        // Reject person-scope filters — /team/search derives the scope from the JWT
        rejectPersonFilters(criteria, "/team/search");
        // Extract managerId from JWT and delegate to facade with the validated criteria
        return facade.searchTeamRequests(extractStaffMemberId(authentication), criteria);
    }

    /**
     * POST /leave-requests/all/search — Admin: search all requests company-wide with optional filters.
     *
     * <p><strong>Supported filters:</strong> {@code status}, {@code staffMemberId}, {@code managerId},
     * {@code from}, {@code to} — all optional. staffMemberId and managerId are mutually exclusive.
     *
     * <p>This satisfies the brief requirement: "View all outstanding leave requests
     * filtered by staff member, manager's team or across the company."
     *
     * <p><strong>Example bodies:</strong>
     * <pre>
     * {"status": "PENDING"}
     * {"staffMemberId": "abc-123"}
     * {"managerId": "def-456", "status": "PENDING"}
     * {"from": "2026-01-01", "to": "2026-06-30"}
     * {"staffMemberId": "abc-123", "status": "APPROVED", "from": "2026-09-01", "to": "2026-09-30"}
     * </pre>
     *
     * @param criteria the search criteria JSON body with optional filters (staffMemberId and managerId are mutually exclusive)
     * @return list of matching leave requests as DTOs
     * @throws ResponseStatusException 400 if no filters are provided
     * @see LeaveManagementFacade#searchAllRequests(LeaveRequestSearchCriteria)
     */
    @PostMapping("/all/search")         // Maps HTTP POST /leave-requests/all/search
    @ResponseStatus(HttpStatus.OK)      // Returns 200 OK on success
    public List<LeaveRequestDTO> searchAllRequests(
            @RequestBody LeaveRequestSearchCriteria criteria) {

        // Validate that at least one search filter is present — directs user to GET endpoint otherwise
        validateSearchCriteria(criteria, "GET /leave-requests/all");
        // Delegate to facade — admin endpoint, no user extraction needed
        return facade.searchAllRequests(criteria);
    }

    // ─────────────────────────────────────────────────────────────────
    // COMMANDS (POST/PATCH — write operations)
    // ─────────────────────────────────────────────────────────────────

    /**
     * POST /leave-requests — Submit a new leave request.
     *
     * <p>The staffMemberId is derived from the JWT (security best practice — never from the body).
     * The request body is validated with Bean Validation ({@code @Valid}) for early feedback
     * before reaching the domain layer.
     *
     * <p><strong>Pre-condition:</strong> The staff member must be ACTIVE (not PENDING_SETUP
     * or TERMINATED). This is checked via a cross-context call to Staff Management.
     *
     * <p><strong>Response:</strong> Returns 201 Created with the full leave request DTO
     * (fetched after creation so the client has the generated ID, status, and submittedOn date).
     *
     * @param authentication Spring Security authentication object containing the user's identity
     * @param body           the validated request body containing dates, leaveType, and reason
     * @return the newly created leave request as a DTO
     * @throws ResponseStatusException 403 if the staff member is not active
     * @see SubmitLeaveRequestBody for the request body with Bean Validation annotations
     * @see LeaveManagementFacade#submitLeaveRequest(SubmitLeaveRequestCommand)
     */
    @PostMapping                        // Maps HTTP POST /leave-requests
    @ResponseStatus(HttpStatus.CREATED) // Returns 201 Created on success
    public LeaveRequestDTO submitLeaveRequest(
            Authentication authentication,
            @Valid @RequestBody SubmitLeaveRequestBody body) {

        // Extract the staff member ID (Firebase UID) from the JWT token
        String staffMemberId = extractStaffMemberId(authentication);

        // Block PENDING_SETUP and TERMINATED staff from submitting leave requests
        verifyStaffIsActive(staffMemberId);

        // Resolve managerId from the staff member's assigned lineManagerId
        String managerId = resolveLineManager(staffMemberId);

        // Check for date overlap with existing PENDING or APPROVED requests
        verifyNoDateOverlap(staffMemberId, body.startDate(), body.endDate());

        // Check the staff member has enough available leave days for this request
        verifyAllowanceSufficiency(staffMemberId, body.startDate(), body.endDate());

        // Build the CQRS command from the validated body and the JWT-derived staffMemberId
        SubmitLeaveRequestCommand command = new SubmitLeaveRequestCommand(
                staffMemberId,          // From JWT — not from the request body (security)
                managerId,              // Resolved from staff record's lineManagerId
                body.startDate(),       // First day of leave (validated: must be today or future)
                body.endDate(),         // Last day of leave (validated: must be today or future)
                body.leaveType(),       // Type of leave (e.g., "ANNUAL")
                body.reason()           // Optional reason for the leave request
        );

        // Submit via facade and get the generated ID back
        String id = facade.submitLeaveRequest(command);
        // Return the full DTO so the client has the complete record (including generated fields)
        return facade.findRequestById(id);
    }

    /**
     * PATCH /leave-requests/{id}/approve — Approve a pending leave request.
     *
     * <p><strong>Access check:</strong> Only the assigned manager for this request or
     * an admin can approve. This is verified via {@link #verifyManagerOrAdmin} before
     * calling the facade.
     *
     * <p><strong>Optional body:</strong> The request body may contain a {@code "reason"}
     * field explaining the approval decision. If no body is provided, reason is null.
     *
     * @param id             the UUID of the leave request to approve
     * @param authentication Spring Security authentication object for the approver
     * @param body           optional JSON body with a "reason" field
     * @return the updated leave request as a DTO (showing APPROVED status)
     * @throws ResponseStatusException 403 if the user is not the assigned manager or admin
     * @see LeaveManagementFacade#approveLeaveRequest(String, String, String)
     */
    @PatchMapping("/{id}/approve")      // Maps HTTP PATCH /leave-requests/{id}/approve
    @ResponseStatus(HttpStatus.OK)      // Returns 200 OK on success
    public LeaveRequestDTO approveRequest(
            @PathVariable String id,
            Authentication authentication,
            @RequestBody(required = false) Map<String, String> body) {

        // Extract the approver's staff member ID from the JWT token
        String decidedBy = extractStaffMemberId(authentication);
        // Verify that the caller is either the assigned manager for this request or an admin
        verifyManagerOrAdmin(id, decidedBy, authentication);

        // Extract the optional decision reason from the body (null if no body provided)
        String reason = (body != null) ? body.get("reason") : null;
        validateReasonLength(reason);
        // Delegate to facade — transitions status PENDING → APPROVED
        facade.approveLeaveRequest(id, decidedBy, reason);
        // Return the updated DTO so the client sees the new APPROVED status
        return facade.findRequestById(id);
    }

    /**
     * PATCH /leave-requests/{id}/reject — Reject a pending leave request.
     *
     * <p><strong>Access check:</strong> Only the assigned manager for this request or
     * an admin can reject. This is verified via {@link #verifyManagerOrAdmin} before
     * calling the facade.
     *
     * <p><strong>Optional body:</strong> The request body may contain a {@code "reason"}
     * field explaining the rejection decision. If no body is provided, reason is null.
     *
     * @param id             the UUID of the leave request to reject
     * @param authentication Spring Security authentication object for the rejector
     * @param body           optional JSON body with a "reason" field
     * @return the updated leave request as a DTO (showing REJECTED status)
     * @throws ResponseStatusException 403 if the user is not the assigned manager or admin
     * @see LeaveManagementFacade#rejectLeaveRequest(String, String, String)
     */
    @PatchMapping("/{id}/reject")       // Maps HTTP PATCH /leave-requests/{id}/reject
    @ResponseStatus(HttpStatus.OK)      // Returns 200 OK on success
    public LeaveRequestDTO rejectRequest(
            @PathVariable String id,
            Authentication authentication,
            @RequestBody(required = false) Map<String, String> body) {

        // Extract the rejector's staff member ID from the JWT token
        String decidedBy = extractStaffMemberId(authentication);
        // Verify that the caller is either the assigned manager for this request or an admin
        verifyManagerOrAdmin(id, decidedBy, authentication);

        // Extract the optional decision reason from the body (null if no body provided)
        String reason = (body != null) ? body.get("reason") : null;
        validateReasonLength(reason);
        // Delegate to facade — transitions status PENDING → REJECTED
        facade.rejectLeaveRequest(id, decidedBy, reason);
        // Return the updated DTO so the client sees the new REJECTED status
        return facade.findRequestById(id);
    }

    /**
     * PATCH /leave-requests/{id}/cancel — Cancel a pending or approved leave request.
     *
     * <p><strong>Access check:</strong> Staff can only cancel their own requests;
     * admins can cancel any request. This is verified via {@link #verifyOwnerOrAdmin}
     * before calling the facade.
     *
     * <p><strong>Optional body:</strong> The request body may contain a {@code "reason"}
     * field explaining why the cancellation was needed. If no body is provided, reason is null.
     *
     * <p><strong>Allowance impact:</strong> If the request was PENDING, pending days are released.
     * If the request was APPROVED, used days are credited back. This is handled by local
     * event listeners in the application layer.
     *
     * @param id             the UUID of the leave request to cancel
     * @param authentication Spring Security authentication object for the canceller
     * @param body           optional JSON body with a "reason" field
     * @return the updated leave request as a DTO (showing CANCELLED status)
     * @throws ResponseStatusException 403 if the user is not the owner or admin
     * @see LeaveManagementFacade#cancelLeaveRequest(CancelLeaveRequestCommand)
     * @see CancelLeaveRequestCommand
     */
    @PatchMapping("/{id}/cancel")       // Maps HTTP PATCH /leave-requests/{id}/cancel
    @ResponseStatus(HttpStatus.OK)      // Returns 200 OK on success
    public LeaveRequestDTO cancelRequest(
            @PathVariable String id,
            Authentication authentication,
            @RequestBody(required = false) Map<String, String> body) {

        // Extract the canceller's staff member ID from the JWT token
        String cancelledBy = extractStaffMemberId(authentication);
        // Verify that the caller is either the owner of this request or an admin
        verifyOwnerOrAdmin(id, cancelledBy, authentication);

        // Extract the optional cancellation reason from the body (null if no body provided)
        String reason = (body != null) ? body.get("reason") : null;
        validateReasonLength(reason);

        // Build the CQRS cancel command with the leave request ID, canceller, and reason
        CancelLeaveRequestCommand command = new CancelLeaveRequestCommand(id, cancelledBy, reason);
        // Delegate to facade — transitions status PENDING/APPROVED → CANCELLED
        facade.cancelLeaveRequest(command);
        // Return the updated DTO so the client sees the new CANCELLED status
        return facade.findRequestById(id);
    }

    // ─────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Extracts the staff member ID from the authenticated user's principal.
     *
     * <p>The Firebase UID (subject claim in the JWT) is used as the staffMemberId
     * across all contexts (Identity, Staff Management, Leave Management), ensuring
     * a single consistent identifier.
     *
     * @param authentication the Spring Security authentication object from the request
     * @return the Firebase UID of the authenticated user
     */
    private String extractStaffMemberId(Authentication authentication) {
        // Authentication.getName() returns the principal name — which is the Firebase UID (subject)
        return authentication.getName();
    }

    /**
     * Validates that at least one search filter is provided in the criteria.
     *
     * <p>Returns 400 Bad Request with a helpful message if no filters are set, directing
     * the user to the corresponding GET endpoint for unfiltered results. This prevents
     * wasteful POST /search calls that return the same results as the simpler GET endpoint.
     *
     * @param criteria    the search criteria to validate
     * @param getEndpoint the corresponding GET endpoint name for the error message
     * @throws ResponseStatusException 400 if no filters are provided
     * @see LeaveRequestSearchCriteria#hasFilters()
     */
    private void validateSearchCriteria(LeaveRequestSearchCriteria criteria, String getEndpoint) {
        // Check if any filter field is populated using the criteria's convenience method
        if (!criteria.hasFilters()) {
            // Return 400 with a helpful message pointing to the GET endpoint for unfiltered results
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "At least one search filter is required. Use " + getEndpoint + " for unfiltered results.");
        }
        // Validate status against allowed enum values if provided
        if (criteria.status() != null && !criteria.status().isBlank()) {
            try {
                com.staffs.leavebooking.leavemanagement.domain.LeaveRequestStatus.valueOf(criteria.status().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid status filter: '" + criteria.status() + "'. Valid values are: PENDING, APPROVED, REJECTED, CANCELLED.");
            }
        }
        // Validate date range: if one date is provided, both must be
        if ((criteria.from() != null) != (criteria.to() != null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Both 'from' and 'to' dates must be provided for date range filtering.");
        }
        // Validate from <= to
        if (criteria.from() != null && criteria.to() != null && criteria.from().isAfter(criteria.to())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "'from' date must be on or before 'to' date.");
        }
        // Reject mutually exclusive filters: staffMemberId and managerId cannot be combined
        boolean hasStaffFilter = criteria.staffMemberId() != null && !criteria.staffMemberId().isBlank();
        boolean hasManagerFilter = criteria.managerId() != null && !criteria.managerId().isBlank();
        if (hasStaffFilter && hasManagerFilter) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "staffMemberId and managerId cannot be supplied together. Search by one or the other.");
        }
    }

    /**
     * Rejects staffMemberId and managerId filters on role-scoped search endpoints.
     * These endpoints derive their scope from the JWT — person filters are not applicable.
     *
     * @param criteria the search criteria to check
     * @param endpoint the endpoint name for the error message
     * @throws ResponseStatusException 400 if staffMemberId or managerId is provided
     */
    private void rejectPersonFilters(LeaveRequestSearchCriteria criteria, String endpoint) {
        boolean hasStaffFilter = criteria.staffMemberId() != null && !criteria.staffMemberId().isBlank();
        boolean hasManagerFilter = criteria.managerId() != null && !criteria.managerId().isBlank();
        if (hasStaffFilter || hasManagerFilter) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "staffMemberId and managerId filters are not accepted on " + endpoint +
                    ". The search scope is determined by your authentication token. " +
                    "Use POST /leave-requests/all/search for person-filtered searches (admin only).");
        }
    }

    /**
     * Checks if the authenticated user has the ADMIN role.
     *
     * <p>Used by the ownership/team verification helpers to short-circuit checks
     * for admin users, who have unrestricted access to all operations.
     *
     * @param authentication the Spring Security authentication object to check
     * @return true if the user has the ROLE_ADMIN authority, false otherwise
     */
    private boolean isAdmin(Authentication authentication) {
        // Stream through the user's granted authorities and check for ROLE_ADMIN
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    /**
     * Verifies that the authenticated user is either the assigned manager for this
     * leave request OR an admin.
     *
     * <p>This prevents managers from approving/rejecting requests that aren't assigned
     * to them. Admins bypass this check entirely — they can approve/reject any request.
     *
     * <p><strong>How it works:</strong> Loads the leave request DTO from the facade and
     * compares the request's managerId with the authenticated user's ID. If they don't
     * match and the user is not an admin, throws 403 Forbidden.
     *
     * @param leaveRequestId the UUID of the leave request being acted upon
     * @param userId         the authenticated user's staff member ID (from JWT)
     * @param authentication the authentication object (used to check admin role)
     * @throws ResponseStatusException 403 if the user is not the assigned manager or admin
     */
    private void verifyManagerOrAdmin(String leaveRequestId, String userId, Authentication authentication) {
        if (isAdmin(authentication)) return; // Admins can approve/reject any request — skip the check

        // Load the leave request to check who the assigned manager is
        LeaveRequestDTO request = facade.findRequestById(leaveRequestId);
        // Compare the request's assigned manager with the authenticated user
        if (!request.managerId().equals(userId)) {
            // The user is not the assigned manager and not an admin — deny access
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only the assigned manager or an admin can approve/reject this request.");
        }
    }

    /**
     * Verifies that the authenticated user is either the owner (submitter) of
     * this leave request OR an admin.
     *
     * <p>This prevents staff from cancelling other people's requests. Admins bypass
     * this check entirely — they can cancel any request.
     *
     * <p><strong>How it works:</strong> Loads the leave request DTO from the facade and
     * compares the request's staffMemberId with the authenticated user's ID. If they don't
     * match and the user is not an admin, throws 403 Forbidden.
     *
     * @param leaveRequestId the UUID of the leave request being cancelled
     * @param userId         the authenticated user's staff member ID (from JWT)
     * @param authentication the authentication object (used to check admin role)
     * @throws ResponseStatusException 403 if the user is not the owner or admin
     */
    private void verifyOwnerOrAdmin(String leaveRequestId, String userId, Authentication authentication) {
        if (isAdmin(authentication)) return; // Admins can cancel any request — skip the check

        // Load the leave request to check who submitted it
        LeaveRequestDTO request = facade.findRequestById(leaveRequestId);
        // Compare the request's submitter with the authenticated user
        if (!request.staffMemberId().equals(userId)) {
            // The user is not the owner and not an admin — deny access
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "You can only cancel your own leave requests.");
        }
    }

    /**
     * Verifies the staff member's employment status is ACTIVE before allowing leave operations.
     *
     * <p><strong>Business rule:</strong> Staff in PENDING_SETUP status cannot submit leave
     * requests — their profile must be completed and activated by an admin first. Staff
     * in TERMINATED status are also blocked from submitting.
     *
     * <p><strong>Cross-context call:</strong> Uses the {@link StaffManagementFacade} (Open Host
     * Service) to look up the staff member's current employment status. This is one of the
     * few controlled cross-context dependencies in the system.
     *
     * <p><strong>Error handling:</strong> If the staff record is not found at all (e.g., the
     * Firebase user exists but no staff profile has been created), returns 403 with a
     * message directing the user to contact their administrator.
     *
     * @param staffMemberId the UUID of the staff member to verify
     * @throws ResponseStatusException 403 if the staff member is not active or not found
     * @see StaffManagementFacade#findStaffMemberById(String)
     */
    private void verifyStaffIsActive(String staffMemberId) {
        try {
            // Cross-context call: look up the staff member's details from Staff Management
            StaffMemberDTO staff = staffFacade.findStaffMemberByIdInternal(staffMemberId);
            // Check if the staff member is still in the setup phase — not yet activated by admin
            if ("PENDING_SETUP".equals(staff.employmentStatus())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Your account is pending setup. An administrator must complete your profile " +
                        "and activate your account before you can submit leave requests.");
            }
            // Check if the staff member has been terminated — no longer allowed to submit
            if ("TERMINATED".equals(staff.employmentStatus())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Your account has been terminated. You cannot submit leave requests.");
            }
        } catch (ResponseStatusException e) {
            throw e; // Re-throw our own ResponseStatusException (from the checks above)
        } catch (Exception e) {
            // Staff record not found in Staff Management — may not have been set up yet
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Your staff profile has not been set up. Please contact your administrator.");
        }
    }

    /**
     * Validates that the optional reason string does not exceed 500 characters.
     * Matches the VARCHAR(500) column limits in schema.sql and the @Size(max=500)
     * on the JPA entity. Catches oversized text early at the controller level
     * rather than letting it fail at the database layer.
     *
     * @param reason the optional reason string (null is allowed)
     * @throws ResponseStatusException 400 if reason exceeds 500 characters
     */
    private void validateReasonLength(String reason) {
        if (reason != null && reason.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Reason must not exceed 500 characters (provided: " + reason.length() + ").");
        }
    }

    /**
     * Resolves the managerId from the staff member's assigned lineManagerId.
     * Always resolves from the staff record — managerId is never accepted from the request body.
     * This ensures leave requests can only be directed to the staff member's actual line manager.
     *
     * @param staffMemberId the staff member submitting the request
     * @return the resolved managerId (the staff member's lineManagerId)
     * @throws ResponseStatusException 400 if no line manager is assigned
     */
    private String resolveLineManager(String staffMemberId) {
        try {
            StaffMemberDTO staff = staffFacade.findStaffMemberByIdInternal(staffMemberId);
            if (staff.lineManagerId() != null && !staff.lineManagerId().isBlank()) {
                String managerId = staff.lineManagerId();
                // Verify the assigned manager still exists and is not terminated
                try {
                    StaffMemberDTO manager = staffFacade.findStaffMemberByIdInternal(managerId);
                    if ("TERMINATED".equals(manager.employmentStatus())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Your assigned line manager's account has been terminated. " +
                                "Please contact an administrator to update your line manager assignment.");
                    }
                } catch (ResponseStatusException e) {
                    throw e;
                } catch (Exception e) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Your assigned line manager (ID: " + managerId + ") no longer exists. " +
                            "Please contact an administrator to update your line manager assignment.");
                }
                return managerId;
            }
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            // Staff record lookup failed — fall through to error below
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "No line manager assigned to your profile. An administrator must assign " +
                "a line manager before you can submit leave requests.");
    }

    /**
     * Checks for date overlap with existing PENDING or APPROVED leave requests.
     * A staff member cannot request leave for dates that overlap with an existing
     * active request (unless the existing request is REJECTED or CANCELLED).
     *
     * <p>Two date ranges overlap if: start1 <= end2 AND start2 <= end1
     *
     * @param staffMemberId the staff member submitting the request
     * @param startDate the requested start date
     * @param endDate the requested end date
     * @throws ResponseStatusException 409 if dates overlap with an active request
     */
    private void verifyNoDateOverlap(String staffMemberId, java.time.LocalDate startDate, java.time.LocalDate endDate) {
        // Get all of this staff member's existing requests
        java.util.List<LeaveRequestDTO> existingRequests = facade.findMyRequests(staffMemberId);

        for (LeaveRequestDTO existing : existingRequests) {
            // Only check PENDING and APPROVED requests — REJECTED and CANCELLED don't block
            if (!"PENDING".equals(existing.status()) && !"APPROVED".equals(existing.status())) {
                continue;
            }

            // Two ranges overlap if: start1 <= end2 AND start2 <= end1
            boolean overlaps = !startDate.isAfter(existing.endDate()) && !endDate.isBefore(existing.startDate());

            if (overlaps) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Leave request dates overlap with an existing " + existing.status() +
                        " request (" + existing.startDate() + " to " + existing.endDate() + ")." +
                        " Cancel the existing request first or choose different dates.");
            }
        }
    }

    /**
     * Verifies the staff member has enough available leave days for the requested period.
     * Calculates working days from the date range and compares to the allowance's availableDays
     * (totalEntitlement - daysUsed - daysPending).
     *
     * <p><strong>Why synchronous:</strong> The BEFORE_COMMIT event listener also enforces
     * this invariant atomically within the same transaction. This synchronous controller
     * check catches insufficient balance early and provides a cleaner 400 error message
     * rather than letting the BEFORE_COMMIT listener cause a transaction rollback.
     *
     * @param staffMemberId the staff member submitting the request
     * @param startDate the requested start date
     * @param endDate the requested end date
     * @throws ResponseStatusException 400 if insufficient leave balance
     */
    private void verifyAllowanceSufficiency(String staffMemberId,
                                             java.time.LocalDate startDate,
                                             java.time.LocalDate endDate) {
        try {
            LeaveAllowanceDTO allowance = facade.findMyAllowanceInternal(staffMemberId);
            int requestedDays = new DateRange(startDate, endDate).workingDays();

            if (requestedDays > allowance.availableDays()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Insufficient leave balance. You have " + allowance.availableDays() +
                        " days available but requested " + requestedDays + " days.");
            }
        } catch (ResponseStatusException e) {
            throw e; // Re-throw our own validation error
        } catch (Exception e) {
            // Allowance not found — staff may not be activated yet. Let verifyStaffIsActive handle it.
        }
    }
}
