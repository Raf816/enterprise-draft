package com.staffs.leavebooking.identity.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response body DTO from Firebase Identity Toolkit sign-in endpoint
 * (Lecture 9 — Identity, JWT Authentication).
 *
 * <p><strong>@JsonProperty mapping:</strong> Firebase returns its own field names
 * (localId, displayName, idToken, expiresIn) which don't match our preferred naming.
 * Jackson's {@code @JsonProperty} maps Firebase's field names to cleaner names
 * for our API consumers.
 *
 * <p><strong>Key field — accessToken:</strong> This is the Firebase ID token (JWT)
 * that the client must include in the {@code Authorization: Bearer <token>} header
 * on all subsequent API requests. It contains the user's UID (subject), email,
 * and custom claims (including role) embedded in the token payload.
 *
 * @param uid              Firebase UID (mapped from "localId" in Firebase response)
 * @param email            the user's email address
 * @param username         display name (mapped from "displayName")
 * @param accessToken      the JWT/ID token (mapped from "idToken") — used for Bearer auth
 * @param refreshToken     token to obtain new ID tokens when the current one expires
 * @param expiresInSeconds seconds until the ID token expires (mapped from "expiresIn")
 */
public record LoginResponse(
        @JsonProperty("localId") String uid,              // Firebase UID (their field: "localId")
        String email,                                      // Email address (same name in both)
        @JsonProperty("displayName") String username,     // Display name (their field: "displayName")
        @JsonProperty("idToken") String accessToken,      // JWT token (their field: "idToken")
        String refreshToken,                               // Refresh token (same name in both)
        @JsonProperty("expiresIn") String expiresInSeconds // Expiry in seconds (their field: "expiresIn")
) {
}
