package com.staffs.leavebooking.identity.security;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Enum representing the three application roles (Lecture 9 — RBAC, @PreAuthorize).
 *
 * <p><strong>Spring Security convention:</strong> Spring Security differentiates
 * between "roles" (prefixed with {@code ROLE_}) and fine-grained "authorities".
 * When you use {@code @PreAuthorize("hasRole('ADMIN')")}, Spring actually checks
 * for an authority named {@code ROLE_ADMIN}. This enum handles that prefix convention.
 *
 * <p><strong>Three roles in the system:</strong>
 * <ul>
 *   <li>{@code STAFF} — can submit leave requests, view own data, cancel own requests</li>
 *   <li>{@code MANAGER} — can view team data, approve/reject team requests</li>
 *   <li>{@code ADMIN} — can view all data, manage staff, amend entitlements, bypass ownership checks</li>
 * </ul>
 *
 * <p><strong>Firebase custom claims:</strong> The role is stored as a custom claim
 * in the Firebase ID token (JWT). When a token is verified, the role claim is
 * extracted and mapped to a Spring Security authority via
 * {@link FirebaseJwtAuthenticationConverter} or {@link com.staffs.leavebooking.identity.authService.FirebaseTokenFilter}.
 *
 * @see FirebaseJwtAuthenticationConverter for JWT → authority mapping
 * @see com.staffs.leavebooking.identity.authService.FirebaseAuthService#registerUser for role assignment
 */
public enum Role {

    STAFF,    // Standard employee — can manage own leave
    MANAGER,  // Team lead — can manage team's leave
    ADMIN;    // System administrator — full access

    /** Spring Security authority prefix — all role-based authorities start with "ROLE_" */
    public static final String PREFIX = "ROLE_";

    /**
     * Returns the Spring Security authority string for this role.
     * e.g., ADMIN → "ROLE_ADMIN", STAFF → "ROLE_STAFF"
     *
     * <p>This is what {@code @PreAuthorize("hasRole('ADMIN')")} checks for.
     *
     * @return the prefixed authority string
     */
    public String getAuthority() {
        return PREFIX + name(); // e.g., "ROLE_" + "ADMIN" = "ROLE_ADMIN"
    }

    /**
     * Converts a string to a Role enum value (case-insensitive).
     * Used during registration to validate and normalise the role from the request body.
     *
     * <p>{@code @JsonCreator} tells Jackson to use this method when deserialising
     * a Role from JSON (though in practice, roles are handled as strings in DTOs).
     *
     * @param roleAsString the role string to convert (e.g., "admin", "STAFF", "Manager")
     * @return the matching Role enum value
     * @throws IllegalArgumentException if the string doesn't match any valid role
     */
    @JsonCreator // Jackson: use this for JSON deserialisation of Role values
    public static Role fromString(String roleAsString) {
        // Guard: null or blank role
        if (roleAsString == null || roleAsString.isBlank()) {
            throw new IllegalArgumentException("Role cannot be null or empty");
        }
        try {
            // Convert to uppercase and look up the enum value
            // This makes the lookup case-insensitive: "admin" → "ADMIN" → Role.ADMIN
            return Role.valueOf(roleAsString.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            // valueOf() throws if no matching enum constant exists
            throw new IllegalArgumentException("Invalid role: " + roleAsString +
                    ". Valid roles are: STAFF, MANAGER, ADMIN");
        }
    }
}
