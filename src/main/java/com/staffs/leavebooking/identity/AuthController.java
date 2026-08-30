package com.staffs.leavebooking.identity;

import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.staffs.leavebooking.identity.authService.FirebaseAuthService;
import com.staffs.leavebooking.identity.dto.*;
import com.staffs.leavebooking.staffmanagement.StaffManagementFacade;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.stream.Collectors;

/**
 * REST controller for the Identity and Access Control module
 * (Lecture 9 — Identity, Authentication, Registration).
 *
 * <p><strong>Architecture:</strong> This controller belongs to the Generic context
 * (Identity & Access). It is NOT domain-driven — it talks directly to
 * {@link FirebaseAuthService} without a facade pattern. This follows the brief's
 * architecture where Identity is a generic supporting context, not a core DDD context.
 *
 * <p><strong>Cross-context coordination:</strong> On self-registration, this controller
 * also creates a skeleton staff record in the Staff Management context via
 * {@link StaffManagementFacade#createSkeletonStaffMember}. The skeleton record starts
 * with status PENDING_SETUP — the admin must fill in department, manager, role, etc.
 * and activate the user before they can submit leave requests.
 *
 * <p><strong>Five endpoints:</strong>
 * <table>
 *   <tr><th>Method</th><th>Path</th><th>Auth</th><th>Purpose</th></tr>
 *   <tr><td>POST</td><td>/auth/register</td><td>Public</td><td>Register a new user</td></tr>
 *   <tr><td>POST</td><td>/auth/login</td><td>Public</td><td>Authenticate and get JWT</td></tr>
 *   <tr><td>GET</td><td>/auth/role-check</td><td>Any authenticated</td><td>Check current user's role</td></tr>
 *   <tr><td>GET</td><td>/auth/users/{email}</td><td>ADMIN only</td><td>Look up user by email</td></tr>
 *   <tr><td>PATCH</td><td>/auth/password</td><td>Any authenticated</td><td>Change own password</td></tr>
 * </table>
 *
 * @see FirebaseAuthService for the underlying Firebase operations
 * @see com.staffs.leavebooking.identity.security.SecurityConfig for URL-level auth rules
 */
@RestController     // Spring: this class handles HTTP requests and returns JSON responses
@RequestMapping("/auth") // Base path: all endpoints start with /auth
@AllArgsConstructor // Lombok: constructor injection for all final fields
@Slf4j              // Lombok: generates a private static final Logger
public class AuthController {

    /** Confirmation message returned on successful registration */
    public static final String USER_CREATED_CONFIRMATION = "User created successfully";

    /** Firebase service for registration, login, password changes, and role management */
    private final FirebaseAuthService firebaseAuthService;

    /** Facade to Staff Management context — used to create skeleton staff records on registration */
    private final StaffManagementFacade staffManagementFacade;

    // ─────────────────────────────────────────────────────────────────
    // ENDPOINTS
    // ─────────────────────────────────────────────────────────────────

    /**
     * POST /auth/register — Register a new user (public endpoint).
     *
     * <p><strong>Role assignment logic:</strong>
     * <ul>
     *   <li>Public callers (no JWT) → always get STAFF role regardless of request body</li>
     *   <li>Admin callers (with valid ADMIN JWT) → can assign any role (STAFF, MANAGER, ADMIN)</li>
     * </ul>
     *
     * <p><strong>Side effect:</strong> Also creates a skeleton staff record (PENDING_SETUP)
     * in the Staff Management context. The Firebase UID is used as the staff record ID
     * to ensure consistency across Firebase, staff management, and leave management.
     *
     * <p>{@code @Valid} triggers Bean Validation on the {@link RegisterRequest} — checks
     * @NotBlank, @Email, @Size constraints before the method body executes.
     *
     * @param request        the registration details (validated by Bean Validation)
     * @param authentication the current authentication (null for public callers, populated for admins)
     * @return 201 with RegisterResponse on success, 400 with ErrorResponse on failure
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@jakarta.validation.Valid @RequestBody RegisterRequest request,
                                       Authentication authentication) {
        try {
            // Determine the effective role based on caller's authentication status
            // Public callers always get STAFF; admin callers can assign any role
            String effectiveRole = determineEffectiveRole(request.role(), authentication);

            log.info("Registering user: {} with role: {}", request.email(), effectiveRole);

            // Step 1: Create the user in Firebase (gets a Firebase UID back)
            UserRecord userRecord = firebaseAuthService.registerUser(
                    request.username(),
                    request.email(),
                    request.password(),
                    effectiveRole
            );

            // Step 2: Create a skeleton staff record using the Firebase UID as the staff record ID
            // This ensures: Firebase UID = staff record ID = leave allowance staffMemberId
            try {
                staffManagementFacade.createSkeletonStaffMember(
                        userRecord.getUid(),  // Use Firebase UID as the staff record ID
                        // Split the username into first name and surname
                        request.username() != null ? request.username().split(" ")[0] : "Unknown",
                        request.username() != null && request.username().contains(" ")
                                ? request.username().substring(request.username().indexOf(" ") + 1)
                                : "Unknown",
                        request.email()
                );
            } catch (Exception e) {
                // If skeleton creation fails, log but don't fail the registration
                // The Firebase user was created successfully — that's the primary goal
                // An admin can create the staff record manually later
                log.warn("Skeleton staff record creation failed for {}: {} (Firebase user was created successfully)",
                        request.email(), e.getMessage());
            }

            // Return 201 Created with the user details
            return ResponseEntity.status(HttpStatus.CREATED).body(new RegisterResponse(
                    userRecord.getUid(),
                    userRecord.getEmail(),
                    userRecord.getDisplayName(),
                    USER_CREATED_CONFIRMATION
            ));
        } catch (FirebaseAuthException e) {
            // Firebase registration failed (duplicate email, weak password, etc.)
            log.error("Registration failed: {}", e.getMessage());
            // Parse the Firebase error into a clean, user-friendly message
            String cleanMessage = parseFirebaseRegistrationError(e.getMessage(), request.email());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.of(400, "Bad Request", cleanMessage));
        }
    }

    /**
     * POST /auth/login — Authenticate and receive a JWT (public endpoint).
     * Delegates to Firebase Identity Toolkit REST API for authentication.
     *
     * @param request the login credentials (email + password)
     * @return 200 with LoginResponse containing the JWT, or error on failure
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        // Delegate to FirebaseAuthService which calls the Firebase REST API
        // Returns a LoginResponse with the JWT, refresh token, UID, etc.
        LoginResponse response = firebaseAuthService.loginUser(
                request.emailOrUsername(),
                request.password()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * GET /auth/role-check — Verify the current user's role(s).
     * Requires authentication (any role can access this).
     *
     * <p>Useful for frontend clients to check what role the current JWT grants,
     * and for testing/debugging to verify token claims are correct.
     *
     * @param authentication the current user's authentication (injected by Spring Security)
     * @return the user's granted authorities as a string (e.g., "ROLE_ADMIN access granted")
     */
    @PreAuthorize("isAuthenticated()") // Any authenticated user can check their role
    @GetMapping("/role-check")
    public ResponseEntity<String> roleCheck(Authentication authentication) {
        // Extract all granted authorities and join them as a comma-separated string
        String roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)  // e.g., "ROLE_ADMIN"
                .collect(Collectors.joining(", "));   // e.g., "ROLE_ADMIN, ROLE_STAFF"
        return ResponseEntity.ok(roles + " access granted");
    }

    /**
     * GET /auth/users/{email} — Look up a user's details by email (admin only).
     * Returns the Firebase UID, email, display name, and role.
     *
     * @param email the email address to search for
     * @return 200 with user details on success, 404 if not found
     */
    @PreAuthorize("hasRole('ADMIN')") // Only admins can look up other users
    @GetMapping("/users/{email}")
    public ResponseEntity<?> findUserByEmail(@PathVariable String email) {
        try {
            // Look up the user in Firebase by email
            UserRecord user = firebaseAuthService.findUserByEmail(email);
            // Return the user details including their role from custom claims
            return ResponseEntity.ok(java.util.Map.of(
                    "uid", user.getUid(),
                    "email", user.getEmail(),
                    "displayName", user.getDisplayName() != null ? user.getDisplayName() : "",
                    "role", user.getCustomClaims() != null && user.getCustomClaims().get("role") != null
                            ? user.getCustomClaims().get("role") : "STAFF"
            ));
        } catch (FirebaseAuthException e) {
            // No user found with this email in Firebase
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ErrorResponse.of(404, "Not Found", "No user found with email: " + email));
        }
    }

    /**
     * PATCH /auth/password — Change the authenticated user's own password.
     * The user must be logged in — their UID is extracted from the JWT.
     *
     * @param authentication the current user's authentication (provides UID)
     * @param body           request body containing {"newPassword": "..."}
     * @return 200 on success, 400 if password is invalid, 500 if Firebase fails
     */
    @PreAuthorize("isAuthenticated()") // Must be logged in to change own password
    @PatchMapping("/password")
    public ResponseEntity<?> changePassword(Authentication authentication,
                                             @RequestBody java.util.Map<String, String> body) {
        try {
            // Extract the user's Firebase UID from the JWT (set by FirebaseTokenFilter)
            String uid = authentication.getName();
            // Extract the new password from the request body
            String newPassword = body.get("newPassword");

            // Validate that a new password was provided
            if (newPassword == null || newPassword.isBlank()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ErrorResponse.of(400, "Bad Request", "newPassword is required"));
            }

            // Delegate the password change to Firebase
            firebaseAuthService.changePassword(uid, newPassword);

            return ResponseEntity.ok(java.util.Map.of(
                    "message", "Password changed successfully"
            ));
        } catch (IllegalArgumentException e) {
            // Password too short or other validation error
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ErrorResponse.of(400, "Bad Request", e.getMessage()));
        } catch (FirebaseAuthException e) {
            // Firebase failed to update the password
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ErrorResponse.of(500, "Internal Server Error",
                            "Failed to change password. Please try again later."));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Determines the effective role for a new user registration.
     *
     * <p><strong>Logic:</strong>
     * <ul>
     *   <li>If the caller is an authenticated ADMIN → use the requested role (or default to STAFF)</li>
     *   <li>If the caller is NOT an admin (or not authenticated) → always return STAFF</li>
     * </ul>
     *
     * <p>This prevents non-admin users from self-registering as ADMIN or MANAGER.
     *
     * @param requestedRole  the role from the registration request body (may be null)
     * @param authentication the caller's authentication (null for public callers)
     * @return the role to actually assign ("STAFF", "MANAGER", or "ADMIN")
     */
    private String determineEffectiveRole(String requestedRole, Authentication authentication) {
        // Check if the caller is an authenticated ADMIN
        if (authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            // Admin callers can assign any role; default to STAFF if none specified
            return requestedRole != null ? requestedRole : "STAFF";
        }
        // Non-admin or unauthenticated callers ALWAYS get STAFF (security enforcement)
        return "STAFF";

        //temp any role:
        // return requestedRole != null ? requestedRole : "STAFF";
    }

    /**
     * Parses Firebase registration errors into clean, user-friendly messages.
     * Firebase returns verbose error messages with internal error codes — this method
     * translates them into messages suitable for API consumers.
     *
     * @param firebaseMessage the raw error message from Firebase
     * @param email           the email that was being registered (included in error messages)
     * @return a clean, user-friendly error message
     */
    private String parseFirebaseRegistrationError(String firebaseMessage, String email) {
        if (firebaseMessage == null) return "Registration failed. Please try again.";
        // Check for known Firebase error patterns and return clean messages
        if (firebaseMessage.contains("EMAIL_EXISTS") || firebaseMessage.contains("email already exists")) {
            return "A user with email " + email + " already exists";
        }
        if (firebaseMessage.contains("INVALID_EMAIL")) {
            return "The email address " + email + " is not valid";
        }
        if (firebaseMessage.contains("WEAK_PASSWORD")) {
            return "The password is too weak. It must be at least 6 characters";
        }
        // Default message for unrecognised errors
        return "Registration failed. Please check your details and try again.";
    }
}
