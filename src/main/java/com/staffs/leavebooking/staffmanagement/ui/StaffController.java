package com.staffs.leavebooking.staffmanagement.ui;

import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.staffs.leavebooking.identity.authService.FirebaseAuthService;
import com.staffs.leavebooking.staffmanagement.StaffManagementFacade;
import com.staffs.leavebooking.staffmanagement.application.commands.AddStaffMemberCommand;
import com.staffs.leavebooking.staffmanagement.application.commands.UpdateDepartmentCommand;
import com.staffs.leavebooking.staffmanagement.application.commands.UpdatePlacementCommand;
import com.staffs.leavebooking.staffmanagement.application.commands.UpdateStatusCommand;
import com.staffs.leavebooking.staffmanagement.application.dto.StaffMemberDTO;
import com.staffs.leavebooking.staffmanagement.application.dto.StaffSearchCriteria;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

/**
 * REST controller for Staff Management operations.
 * Primarily admin-only CRUD; managers can view their team.
 *
 * <p>On POST /staff, the controller coordinates between two bounded contexts:
 * 1. Identity (Firebase) — creates the user account so they can login
 * 2. Staff Management — creates the staff record using the Firebase UID as the ID
 * This ensures Firebase UID = staff record ID = leave allowance staffMemberId.
 */
@RestController
@RequestMapping("/staff")
@AllArgsConstructor
@Slf4j
public class StaffController {

    private final StaffManagementFacade facade;
    private final FirebaseAuthService firebaseAuthService;

    // ─────────────────────────────────────────────────────────────────
    // QUERIES (GET — unfiltered)
    // ─────────────────────────────────────────────────────────────────

    /**
     * GET /staff — list all staff (unfiltered).
     */
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<StaffMemberDTO> getAllStaff() {
        return facade.findAllStaffMembers();
    }

    /**
     * GET /staff/{id} — view a single staff member.
     */
    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public StaffMemberDTO getStaffMemberById(@PathVariable String id) {
        return facade.findStaffMemberById(id);
    }

    // ─────────────────────────────────────────────────────────────────
    // SEARCH (POST — filtered queries via JSON body)
    // ─────────────────────────────────────────────────────────────────

    /**
     * POST /staff/search — search staff with optional filters.
     * Supports filtering by department and/or status (can combine both).
     */
    @PostMapping("/search")
    @ResponseStatus(HttpStatus.OK)
    public List<StaffMemberDTO> searchStaff(@RequestBody StaffSearchCriteria criteria) {
        if (!criteria.hasFilters()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "At least one search filter is required (department, status). Use GET /staff for unfiltered results.");
        }
        // Validate status against allowed EmploymentStatus enum values if provided
        if (criteria.status() != null && !criteria.status().isBlank()) {
            try {
                com.staffs.leavebooking.staffmanagement.domain.EmploymentStatus.valueOf(criteria.status().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Invalid status filter: '" + criteria.status() + "'. Valid values are: PENDING_SETUP, ACTIVE, ON_LEAVE, TERMINATED.");
            }
        }
        return facade.searchStaff(criteria);
    }

    // ─────────────────────────────────────────────────────────────────
    // COMMANDS
    // ─────────────────────────────────────────────────────────────────

    /**
     * POST /staff — Admin creates a new staff member.
     * Coordinates between Identity (Firebase) and Staff Management contexts:
     * 1. Creates Firebase user account (so the staff member can login)
     * 2. Creates staff record using the Firebase UID as the ID
     * 3. Staff starts as PENDING_SETUP — admin activates via PATCH /staff/{id}
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StaffMemberCreatedResponse addStaffMember(@jakarta.validation.Valid @RequestBody AddStaffMemberCommand command) {
        String firebaseUid;

        // Pre-validate domain value objects BEFORE creating the Firebase user.
        // This prevents orphan Firebase accounts when domain validation would fail.
        // Bean Validation (@Valid) catches null/blank/size issues, but the domain
        // has stricter rules (e.g., FullName rejects digits, Email checks regex format).
        try {
            new com.staffs.leavebooking.common.domain.FullName(command.firstName(), command.surname());
            new com.staffs.leavebooking.common.domain.Email(command.email());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        try {
            String displayName = command.firstName() + " " + command.surname();
            UserRecord userRecord = firebaseAuthService.registerUser(
                    displayName,
                    command.email(),
                    command.effectivePassword(),
                    command.effectiveRole()
            );
            firebaseUid = userRecord.getUid();
            log.info("Firebase user created for {} with UID {}", command.email(), firebaseUid);
        } catch (FirebaseAuthException e) {
            log.error("Failed to create Firebase user for {}: {}", command.email(), e.getMessage());
            String cleanMessage = parseFirebaseError(e.getMessage(), command.email());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, cleanMessage);
        }

        String staffId = facade.addStaffMemberWithId(firebaseUid, command);
        return StaffMemberCreatedResponse.of(staffId, command.email());
    }

    /**
     * PATCH /staff/{id} — Update any combination of staff member fields.
     * All fields in the body are optional — only non-null fields are updated.
     *
     * <p>Internally routes to the correct operations based on which fields are present:
     * <ul>
     *   <li>department/lineManagerId → updateDepartment (triggers StaffMemberUpdatedEvent)</li>
     *   <li>currentRole/jobLevel/employmentType → updatePlacement (no event)</li>
     *   <li>employmentStatus → updateStatus (TERMINATED invariant; PENDING_SETUP→ACTIVE creates allowance)</li>
     *   <li>role → updateUserRole on Firebase (updates custom claims for next login)</li>
     * </ul>
     */
    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public StaffMemberDTO updateStaff(
            @PathVariable String id,
            @RequestBody UpdateStaffBody body) {

        // Department change → triggers StaffMemberUpdatedEvent → syncs Leave Management
        if (body.department() != null || body.lineManagerId() != null) {
            facade.updateDepartment(new UpdateDepartmentCommand(
                    id, body.department(), body.lineManagerId()));
        }

        // Placement change → no event
        if (body.currentRole() != null || body.jobLevel() != null || body.employmentType() != null) {
            facade.updatePlacement(new UpdatePlacementCommand(
                    id, body.currentRole(), body.startDateOfCurrentRole(),
                    body.jobLevel(), body.employmentType()));
        }

        // Status change → TERMINATED invariant; PENDING_SETUP→ACTIVE creates allowance
        if (body.employmentStatus() != null) {
            facade.updateStatus(new UpdateStatusCommand(id, body.employmentStatus()));
        }

        // Role change → updates Firebase custom claims (takes effect on next login)
        if (body.role() != null) {
            try {
                firebaseAuthService.updateUserRole(id, body.role());
                log.info("Firebase role updated to {} for staff member {}", body.role(), id);
            } catch (FirebaseAuthException e) {
                log.error("Failed to update Firebase role for {}: {}", id, e.getMessage());
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Failed to update role: " + e.getMessage());
            }
        }

        return facade.findStaffMemberById(id);
    }

    // ─────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Parses Firebase error messages into clean, user-friendly messages.
     */
    private String parseFirebaseError(String firebaseMessage, String email) {
        if (firebaseMessage == null) return "Failed to create user account";
        if (firebaseMessage.contains("EMAIL_EXISTS") || firebaseMessage.contains("email already exists")) {
            return "A user account with email " + email + " already exists";
        }
        if (firebaseMessage.contains("INVALID_EMAIL")) {
            return "The email address " + email + " is not valid";
        }
        if (firebaseMessage.contains("WEAK_PASSWORD")) {
            return "The password is too weak. It must be at least 6 characters";
        }
        return "Failed to create user account. Please check the details and try again";
    }

    // ─────────────────────────────────────────────────────────────────
    // REQUEST BODY RECORDS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Unified update body for PATCH /staff/{id}.
     * All fields are optional — null fields are ignored.
     */
    public record UpdateStaffBody(
            String department,
            String lineManagerId,
            String currentRole,
            LocalDate startDateOfCurrentRole,
            String jobLevel,
            String employmentType,
            String employmentStatus,
            String role               // Updates Firebase custom claim (STAFF, MANAGER, ADMIN)
    ) {}
}
