package com.staffs.leavebooking.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body DTO for POST /auth/register (Lecture 9 — Identity and Authentication).
 *
 * <p><strong>Bean Validation:</strong> Uses Jakarta Bean Validation annotations
 * to enforce input constraints. When the controller has {@code @Valid} on the
 * parameter, Spring automatically validates before the method body executes.
 * If validation fails, Spring returns a 400 Bad Request with field-level errors.
 *
 * <p><strong>Role behaviour:</strong> The {@code role} field is optional.
 * <ul>
 *   <li>Public callers (no JWT) → always get STAFF regardless of what they pass</li>
 *   <li>Admin callers (with valid ADMIN JWT) → can assign any role (STAFF, MANAGER, ADMIN)</li>
 * </ul>
 * This logic is in {@code AuthController.determineEffectiveRole()}, not here.
 *
 * @param username the display name for the user (e.g., "Raf Ahmed")
 * @param email    the user's email address (must be valid format)
 * @param password the user's password (minimum 6 characters — Firebase requirement)
 * @param role     optional role assignment (defaults to STAFF for non-admin callers)
 */
public record RegisterRequest(

        @NotBlank(message = "Username is required") // Fails if null, empty, or whitespace-only
        String username,

        @NotBlank(message = "Email is required")                    // Must not be blank
        @Email(message = "Email must be a valid email address")     // Must match email format
        String email,

        @NotBlank(message = "Password is required")                 // Must not be blank
        @Size(min = 6, message = "Password must be at least 6 characters") // Firebase minimum
        String password,

        String role  // Optional — no @NotBlank, so null is allowed. Defaults handled in controller.
) {
}
