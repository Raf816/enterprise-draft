package com.staffs.leavebooking.identity.dto;

/**
 * Response body DTO for successful user registration (POST /auth/register returns 201).
 *
 * <p><strong>Contains:</strong>
 * <ul>
 *   <li>{@code uid} — the Firebase-assigned unique ID (also used as the staff member record ID)</li>
 *   <li>{@code email} — the registered email address (echo back for confirmation)</li>
 *   <li>{@code username} — the display name (echo back for confirmation)</li>
 *   <li>{@code message} — confirmation message (e.g., "User created successfully")</li>
 * </ul>
 *
 * @param uid      the Firebase UID assigned to the new user
 * @param email    the email address that was registered
 * @param username the display name that was set
 * @param message  a human-readable success message
 */
public record RegisterResponse(
        String uid,       // Firebase UID — also becomes the staff member record ID
        String email,     // Echo back the registered email
        String username,  // Echo back the display name
        String message    // Confirmation message (e.g., "User created successfully")
) {
}
