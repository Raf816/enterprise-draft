package com.staffs.leavebooking.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body DTO for PATCH /auth/password.
 *
 * @param newPassword the new password (required, minimum 6 characters — Firebase requirement)
 */
public record ChangePasswordRequest(

        @NotBlank(message = "New password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        String newPassword
) {
}
