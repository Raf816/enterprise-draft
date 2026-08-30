package com.staffs.leavebooking.staffmanagement.ui;

import java.time.Instant;

/**
 * Response body DTO for successful staff member creation (POST /staff returns 201).
 *
 * <p><strong>Contains:</strong>
 * <ul>
 *   <li>{@code id} — the Firebase UID used as the staff record ID</li>
 *   <li>{@code email} — echo back the registered email for confirmation</li>
 *   <li>{@code message} — success confirmation text</li>
 *   <li>{@code timestamp} — ISO-8601 timestamp of creation</li>
 * </ul>
 *
 * @param id        the staff member's ID (= Firebase UID)
 * @param email     the staff member's email address
 * @param message   a human-readable success message
 * @param timestamp ISO-8601 timestamp of when the record was created
 */
public record StaffMemberCreatedResponse(
        String id,         // Staff record ID (= Firebase UID)
        String email,      // Echo back the registered email
        String message,    // Success message
        String timestamp   // ISO-8601 creation timestamp
) {
    /**
     * Static factory method for creating a success response with auto-generated timestamp.
     *
     * @param id    the created staff member's ID
     * @param email the created staff member's email
     * @return a new StaffMemberCreatedResponse with success message and current timestamp
     */
    public static StaffMemberCreatedResponse of(String id, String email) {
        return new StaffMemberCreatedResponse(
                id, email, "Staff member created successfully", Instant.now().toString()
        );
    }
}
