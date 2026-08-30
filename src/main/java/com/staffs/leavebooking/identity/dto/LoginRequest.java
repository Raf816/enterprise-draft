package com.staffs.leavebooking.identity.dto;

/**
 * Request body DTO for POST /auth/login.
 *
 * <p>Sent to the Firebase Identity Toolkit REST API for authentication.
 * On success, Firebase returns an ID token (JWT) that the client uses
 * for subsequent authenticated API requests.
 *
 * @param emailOrUsername the user's email address (Firebase uses email for auth)
 * @param password        the user's password
 */
public record LoginRequest(
        String emailOrUsername, // The email to authenticate with (Firebase key)
        String password         // The user's password (validated by Firebase)
) {
}
