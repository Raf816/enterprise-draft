package com.staffs.leavebooking.staffmanagement;

import com.staffs.leavebooking.staffmanagement.application.commands.AddStaffMemberCommand;
import com.staffs.leavebooking.staffmanagement.application.commands.UpdateDepartmentCommand;
import com.staffs.leavebooking.staffmanagement.application.commands.UpdatePlacementCommand;
import com.staffs.leavebooking.staffmanagement.application.commands.UpdateStatusCommand;
import com.staffs.leavebooking.staffmanagement.application.dto.StaffMemberDTO;
import com.staffs.leavebooking.staffmanagement.application.handlers.StaffApplicationService;
import com.staffs.leavebooking.staffmanagement.application.handlers.StaffQueryHandler;
import lombok.AllArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Context Facade (Open Host Service) for the Staff Management bounded context
 * (Lecture 4 — Bounded Context Integration, Open Host Service Pattern).
 *
 * <p><strong>DDD Concept — Open Host Service (Lecture 4):</strong> An Open Host Service
 * defines a well-known protocol (API) that external consumers use to interact with a
 * bounded context. This facade is the single entry point for all Staff Management
 * operations — controllers and other bounded contexts never bypass it to access
 * internal services directly.
 *
 * <p><strong>Facade Pattern:</strong> This class acts as a simplified, unified API that
 * hides the internal complexity of the Staff Management context. Internally, it delegates
 * to two specialised handlers following the CQRS pattern:
 * <ul>
 *   <li>{@link StaffQueryHandler} — handles all read-only (query) operations</li>
 *   <li>{@link StaffApplicationService} — handles all write (command) operations</li>
 * </ul>
 * External consumers (e.g., {@link com.staffs.leavebooking.staffmanagement.ui.StaffController})
 * interact only with this facade, never with the handlers directly.
 *
 * <p><strong>CQRS separation (Lecture 5/6):</strong> The methods in this class are
 * organised into two sections — QUERIES and COMMANDS — mirroring the CQRS split.
 * Query methods return {@link StaffMemberDTO} objects (read model), while command
 * methods accept command records and return void or an ID string.
 *
 * <p><strong>Security (RBAC via Spring Security):</strong> Every public method is
 * annotated with {@code @PreAuthorize} to enforce role-based access control:
 * <ul>
 *   <li>{@code hasRole('ADMIN')} — admin-only operations (CRUD, search, status changes)</li>
 *   <li>{@code hasAnyRole('MANAGER', 'ADMIN')} — managers can view individual staff and their team</li>
 *   <li>No annotation — {@link #createSkeletonStaffMember} is called internally from AuthController
 *       after self-registration, so it bypasses RBAC (the user is not yet authenticated with a role)</li>
 * </ul>
 *
 * <p><strong>Call flow:</strong>
 * <pre>
 * StaffController (HTTP)
 *   → StaffManagementFacade (security + routing)
 *     → StaffQueryHandler (reads — JPA → DTO)
 *     → StaffApplicationService (writes — command → domain → JPA → events)
 * </pre>
 *
 * @see StaffQueryHandler for the CQRS read-side handler
 * @see StaffApplicationService for the CQRS write-side handler
 * @see com.staffs.leavebooking.staffmanagement.ui.StaffController for the REST controller that calls this facade
 * @see com.staffs.leavebooking.staffmanagement.application.dto.StaffMemberDTO for the read model returned by queries
 */
@Component      // Spring-managed bean — registered in the application context for dependency injection
@AllArgsConstructor // Lombok: generates a constructor with all final fields (enables constructor-based DI)
public class StaffManagementFacade {

    /**
     * CQRS query handler — handles all read-only operations.
     * Maps JPA entities directly to DTOs without going through the domain aggregate
     * (read-side optimisation: no need to reconstitute the full aggregate for queries).
     */
    private final StaffQueryHandler staffQueryHandler;

    /**
     * CQRS command handler (application service) — handles all write operations.
     * Follows the pattern: receive command → load/create domain aggregate → execute
     * domain logic → save to repository → dispatch domain events.
     */
    private final StaffApplicationService staffApplicationService;

    // ═══════════════════════════════════════════════════════════════════
    // QUERIES (read-only operations — delegated to StaffQueryHandler)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Retrieves all staff members in the system (unfiltered).
     *
     * <p><strong>Access:</strong> ADMIN only — managers should use {@link #findMyTeam(String)}
     * to see their direct reports, or {@link #findStaffMemberById(String)} for individuals.
     *
     * <p><strong>Delegates to:</strong> {@link StaffQueryHandler#findAllStaffMembers()},
     * which calls {@link com.staffs.leavebooking.staffmanagement.infrastructure.repositories.StaffMemberRepository#findAll()}
     * and maps each JPA entity to a {@link StaffMemberDTO}.
     *
     * @return list of all staff members as DTOs
     * @see StaffQueryHandler#findAllStaffMembers()
     */
    @PreAuthorize("hasRole('ADMIN')") // Only admins can see the full staff list
    public List<StaffMemberDTO> findAllStaffMembers() {
        // Delegate to the query handler — it handles JPA-to-DTO mapping
        return staffQueryHandler.findAllStaffMembers();
    }

    /**
     * Retrieves a single staff member by their ID.
     *
     * <p><strong>Access:</strong> MANAGER or ADMIN — managers need this to view individual
     * team members' details (e.g., when reviewing a leave request).
     *
     * <p><strong>Delegates to:</strong> {@link StaffQueryHandler#findStaffMemberById(String)},
     * which throws {@link com.staffs.leavebooking.staffmanagement.ui.exceptions.StaffMemberNotFoundException}
     * if the ID doesn't exist.
     *
     * @param staffMemberId the UUID of the staff member (= Firebase UID)
     * @return the staff member's data as a DTO
     * @throws com.staffs.leavebooking.staffmanagement.ui.exceptions.StaffMemberNotFoundException if not found
     * @see StaffQueryHandler#findStaffMemberById(String)
     */
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')") // Managers can view individual staff details
    public StaffMemberDTO findStaffMemberById(String staffMemberId) {
        // Delegate to the query handler — throws StaffMemberNotFoundException if not found
        return staffQueryHandler.findStaffMemberById(staffMemberId);
    }

    /**
     * Retrieves all staff members in a specific department.
     *
     * <p><strong>Access:</strong> ADMIN only — this returns staff across all managers
     * within the department, which exceeds a single manager's visibility scope.
     *
     * <p><strong>Delegates to:</strong> {@link StaffQueryHandler#findByDepartment(String)},
     * which uses Spring Data's derived query {@code findByDepartment()} on the repository.
     *
     * @param department the department name to filter by (e.g., "Networks", "Digital")
     * @return list of staff members in the specified department
     * @see StaffQueryHandler#findByDepartment(String)
     */
    @PreAuthorize("hasRole('ADMIN')") // Only admins can view entire departments
    public List<StaffMemberDTO> findStaffByDepartment(String department) {
        // Delegate to the query handler — uses repository.findByDepartment()
        return staffQueryHandler.findByDepartment(department);
    }

    /**
     * Retrieves all staff members with a specific employment status.
     *
     * <p><strong>Access:</strong> ADMIN only — useful for finding all PENDING_SETUP
     * staff who need activation, or all TERMINATED staff for audit purposes.
     *
     * <p><strong>Delegates to:</strong> {@link StaffQueryHandler#findByStatus(String)},
     * which uses Spring Data's derived query {@code findByEmploymentStatus()} on the repository.
     *
     * @param status the employment status to filter by (e.g., "ACTIVE", "PENDING_SETUP", "TERMINATED")
     * @return list of staff members with the specified status
     * @see StaffQueryHandler#findByStatus(String)
     */
    @PreAuthorize("hasRole('ADMIN')") // Only admins can filter by employment status
    public List<StaffMemberDTO> findStaffByStatus(String status) {
        // Delegate to the query handler — uses repository.findByEmploymentStatus()
        return staffQueryHandler.findByStatus(status);
    }

    /**
     * Retrieves all staff members managed by a specific manager ("My Team" view).
     *
     * <p><strong>Access:</strong> MANAGER or ADMIN — the primary query for the manager
     * dashboard. Managers see only their direct reports; admins can view any manager's team.
     *
     * <p><strong>Delegates to:</strong> {@link StaffQueryHandler#findByManagerId(String)},
     * which uses Spring Data's derived query {@code findByLineManagerId()} on the repository.
     *
     * @param managerId the UUID of the manager whose team to retrieve
     * @return list of staff members who report to the specified manager
     * @see StaffQueryHandler#findByManagerId(String)
     */
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')") // Managers can view their own team
    public List<StaffMemberDTO> findMyTeam(String managerId) {
        // Delegate to the query handler — uses repository.findByLineManagerId()
        return staffQueryHandler.findByManagerId(managerId);
    }

    /**
     * Searches staff members using combined optional filters (department + status).
     *
     * <p><strong>Access:</strong> ADMIN only — this is the advanced search endpoint
     * that supports combining multiple filters in a single query.
     *
     * <p><strong>Enterprise POST search pattern:</strong> Uses a POST request body
     * with {@link com.staffs.leavebooking.staffmanagement.application.dto.StaffSearchCriteria}
     * rather than query parameters — the same pattern used by Elasticsearch and Stripe
     * for complex, multi-field filtering.
     *
     * <p><strong>Delegates to:</strong> {@link StaffQueryHandler#searchStaff(com.staffs.leavebooking.staffmanagement.application.dto.StaffSearchCriteria)},
     * which routes to different repository methods based on which criteria fields are set.
     *
     * @param criteria the search criteria containing optional department and status filters
     * @return list of staff members matching all specified filters
     * @see StaffQueryHandler#searchStaff(com.staffs.leavebooking.staffmanagement.application.dto.StaffSearchCriteria)
     * @see com.staffs.leavebooking.staffmanagement.application.dto.StaffSearchCriteria
     */
    @PreAuthorize("hasRole('ADMIN')") // Only admins can perform advanced staff searches
    public List<StaffMemberDTO> searchStaff(com.staffs.leavebooking.staffmanagement.application.dto.StaffSearchCriteria criteria) {
        // Delegate to the query handler — routes to the right repository method based on filter combination
        return staffQueryHandler.searchStaff(criteria);
    }

    // ═══════════════════════════════════════════════════════════════════
    // COMMANDS (write operations — delegated to StaffApplicationService)
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Creates a new staff member with a system-generated UUID.
     *
     * <p><strong>Access:</strong> ADMIN only — staff members are created by administrators.
     *
     * <p><strong>Note:</strong> This method generates a random UUID for the staff member.
     * In the normal admin flow, {@link #addStaffMemberWithId(String, AddStaffMemberCommand)}
     * is preferred because it uses the Firebase UID as the ID for consistency across
     * Identity and Staff Management contexts.
     *
     * <p><strong>Delegates to:</strong> {@link StaffApplicationService#addNewStaffMember(AddStaffMemberCommand)},
     * which creates the domain aggregate, validates, and saves to the repository.
     *
     * @param command the command containing all staff member details
     * @return the generated UUID of the newly created staff member
     * @see StaffApplicationService#addNewStaffMember(AddStaffMemberCommand)
     */
    @PreAuthorize("hasRole('ADMIN')") // Only admins can create staff members
    public String addStaffMember(AddStaffMemberCommand command) {
        // Delegate to the application service — creates domain aggregate + saves to JPA
        return staffApplicationService.addNewStaffMember(command);
    }

    /**
     * Creates a new staff member using a specific ID (the Firebase UID).
     * Called by {@link com.staffs.leavebooking.staffmanagement.ui.StaffController}
     * after Firebase user creation to ensure ID consistency.
     *
     * <p><strong>Access:</strong> ADMIN only — this is the primary creation path
     * used by the admin POST /staff endpoint.
     *
     * <p><strong>ID consistency:</strong> By using the Firebase UID as the staff record ID,
     * we maintain a single identifier across three systems:
     * <ol>
     *   <li>Firebase Authentication (user account)</li>
     *   <li>Staff Management (staff record)</li>
     *   <li>Leave Management (leave allowance via staffMemberId)</li>
     * </ol>
     *
     * <p><strong>Delegates to:</strong> {@link StaffApplicationService#addNewStaffMemberWithId(String, AddStaffMemberCommand)},
     * which checks for email uniqueness, creates the domain aggregate, and saves.
     *
     * @param firebaseUid the Firebase UID to use as the staff record's primary key
     * @param command     the command containing all staff member details
     * @return the Firebase UID (same as the input — confirms successful creation)
     * @see StaffApplicationService#addNewStaffMemberWithId(String, AddStaffMemberCommand)
     */
    @PreAuthorize("hasRole('ADMIN')") // Only admins can create staff via the admin path
    public String addStaffMemberWithId(String firebaseUid, AddStaffMemberCommand command) {
        // Delegate to the application service — uses the Firebase UID as the staff record ID
        return staffApplicationService.addNewStaffMemberWithId(firebaseUid, command);
    }

    /**
     * Creates a skeleton staff record on self-registration (POST /auth/register).
     *
     * <p><strong>No @PreAuthorize — intentionally unprotected:</strong> This method is
     * called internally from AuthController immediately after Firebase user creation
     * during self-registration. At that point, the user has just been created in Firebase
     * but doesn't yet have a role-based custom claim, so RBAC cannot be enforced.
     *
     * <p><strong>Skeleton record:</strong> Only name and email are captured. All other
     * fields (department, manager, role, etc.) default to placeholder values that the
     * admin fills in later when activating the staff member.
     *
     * <p><strong>Status:</strong> PENDING_SETUP — the self-registered user can authenticate
     * but cannot submit leave requests until an admin activates them via PATCH /staff/{id}.
     *
     * <p><strong>Idempotent:</strong> If a staff record with the same email already exists
     * (e.g., concurrent registration), the method logs a warning and returns the Firebase UID
     * without creating a duplicate.
     *
     * <p><strong>Delegates to:</strong> {@link StaffApplicationService#createSkeletonStaffMember(String, String, String, String)}.
     *
     * @param firebaseUid the Firebase UID to use as the staff record's primary key
     * @param firstName   the user's first name from the registration form
     * @param surname     the user's surname from the registration form
     * @param email       the user's email from the registration form
     * @return the Firebase UID (confirms the skeleton record exists)
     * @see StaffApplicationService#createSkeletonStaffMember(String, String, String, String)
     */
    // No @PreAuthorize — called internally from AuthController after Firebase user creation
    public String createSkeletonStaffMember(String firebaseUid, String firstName, String surname, String email) {
        // Delegate to the application service — creates a minimal staff record with placeholder values
        return staffApplicationService.createSkeletonStaffMember(firebaseUid, firstName, surname, email);
    }

    /**
     * Updates a staff member's department and/or line manager.
     *
     * <p><strong>Access:</strong> ADMIN only — department reassignment is an administrative action.
     *
     * <p><strong>Cross-context event:</strong> This operation raises a
     * {@link com.staffs.leavebooking.common.events.StaffMemberUpdatedEvent} via RabbitMQ,
     * which is consumed by Leave Management to sync the denormalised department and
     * manager fields on the staff member's {@code LeaveAllowance}.
     *
     * <p><strong>Delegates to:</strong> {@link StaffApplicationService#updateDepartment(UpdateDepartmentCommand)},
     * which loads the domain aggregate, executes the domain command, saves, and dispatches events.
     *
     * @param command the command containing the staff member ID, new department, and/or new manager ID
     * @see StaffApplicationService#updateDepartment(UpdateDepartmentCommand)
     * @see com.staffs.leavebooking.common.events.StaffMemberUpdatedEvent
     */
    @PreAuthorize("hasRole('ADMIN')") // Only admins can reassign departments
    public void updateDepartment(UpdateDepartmentCommand command) {
        // Delegate to the application service — triggers StaffMemberUpdatedEvent for Leave Management sync
        staffApplicationService.updateDepartment(command);
    }

    /**
     * Updates a staff member's placement details (role, job level, employment type).
     *
     * <p><strong>Access:</strong> ADMIN only — placement changes are administrative.
     *
     * <p><strong>No cross-context event:</strong> Unlike department changes, placement
     * updates don't affect the Leave Management context (it doesn't need job title
     * or employment type data), so no event is raised.
     *
     * <p><strong>Delegates to:</strong> {@link StaffApplicationService#updatePlacement(UpdatePlacementCommand)},
     * which loads the domain aggregate, executes the domain command, and saves.
     *
     * @param command the command containing the staff member ID and updated placement fields
     * @see StaffApplicationService#updatePlacement(UpdatePlacementCommand)
     */
    @PreAuthorize("hasRole('ADMIN')") // Only admins can update placement details
    public void updatePlacement(UpdatePlacementCommand command) {
        // Delegate to the application service — no events raised for placement changes
        staffApplicationService.updatePlacement(command);
    }

    /**
     * Updates a staff member's employment status (e.g., PENDING_SETUP → ACTIVE, ACTIVE → TERMINATED).
     *
     * <p><strong>Access:</strong> ADMIN only — status changes have significant system-wide effects.
     *
     * <p><strong>Domain invariant:</strong> TERMINATED is a terminal state — attempting to
     * transition out of TERMINATED throws {@link IllegalStateException} from the domain aggregate.
     *
     * <p><strong>Activation event:</strong> When transitioning PENDING_SETUP → ACTIVE, the domain
     * aggregate raises a {@link com.staffs.leavebooking.common.events.StaffMemberAddedEvent},
     * which is consumed by Leave Management via RabbitMQ to create the staff member's
     * {@code LeaveAllowance} with the correct department, manager, and entitlement.
     *
     * <p><strong>Delegates to:</strong> {@link StaffApplicationService#updateStatus(UpdateStatusCommand)},
     * which loads the domain aggregate, executes the status transition, saves, and dispatches events.
     *
     * @param command the command containing the staff member ID and new employment status
     * @see StaffApplicationService#updateStatus(UpdateStatusCommand)
     * @see com.staffs.leavebooking.common.events.StaffMemberAddedEvent
     */
    @PreAuthorize("hasRole('ADMIN')") // Only admins can change employment status
    public void updateStatus(UpdateStatusCommand command) {
        // Delegate to the application service — may trigger StaffMemberAddedEvent on activation
        staffApplicationService.updateStatus(command);
    }
}
