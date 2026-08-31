package com.staffs.leavebooking.identity.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body DTO for POST /auth/login.
 *
 * <p>Sent to the Firebase Identity Toolkit REST API for authentication.
 * On success, Firebase returns an ID token (JWT) that the client uses
 * for subsequent authenticated API requests.
 *
 * @param emailOrUsername the user's email address (required)
 * @param password        the user's password (required)
 */
public record LoginRequest(

        @NotBlank(message = "Email is required")
        String emailOrUsername,

        @NotBlank(message = "Password is required")
        String password
) {
}
