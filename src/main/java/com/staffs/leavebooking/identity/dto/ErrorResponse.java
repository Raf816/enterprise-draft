package com.staffs.leavebooking.identity.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Standardised error response record for consistent error formatting across the API
 * (Lecture 9 — Identity, Error Handling).
 *
 * <p><strong>Consistent error format:</strong> Every error response in the application
 * follows this structure:
 * <pre>
 * {
 *   "status": 400,
 *   "error": "Bad Request",
 *   "message": "A user with email admin@admin.com already exists",
 *   "timestamp": "2026-09-01T10:15:30Z"
 * }
 * </pre>
 *
 * <p><strong>Why a static factory method?</strong> The {@link #of(int, String, String)}
 * method returns a {@code Map<String, Object>} instead of an ErrorResponse instance.
 * This is because Spring's {@code ResponseEntity.body()} serialises Maps to JSON
 * naturally, and we want the timestamp to be generated at creation time.
 * Using a Map also avoids the immutability constraint of records when we want
 * to add the timestamp dynamically.
 *
 * <p><strong>Used by:</strong>
 * <ul>
 *   <li>{@code AuthController} — for Firebase registration/login errors</li>
 *   <li>{@code GlobalExceptionHandler} — for domain validation errors (400, 404, 409)</li>
 *   <li>{@code UnauthorisedAccessLogger} — for 401/403 responses</li>
 * </ul>
 *
 * @param status    HTTP status code (e.g., 400, 404, 409)
 * @param error     HTTP status reason phrase (e.g., "Bad Request", "Not Found")
 * @param message   human-readable description of what went wrong
 * @param timestamp ISO-8601 timestamp of when the error occurred
 */
public record ErrorResponse(
        int status,        // HTTP status code
        String error,      // HTTP status reason phrase
        String message,    // Human-readable error description
        String timestamp   // ISO-8601 timestamp
) {
    /**
     * Static factory method for creating error response maps with consistent format.
     * Generates the timestamp automatically at the moment of creation.
     *
     * <p><strong>Returns Map instead of ErrorResponse</strong> because ResponseEntity
     * serialises Maps directly to JSON, and this avoids needing a separate constructor
     * for the dynamic timestamp.
     *
     * @param status  HTTP status code (e.g., 400)
     * @param error   HTTP status reason phrase (e.g., "Bad Request")
     * @param message human-readable error description
     * @return a Map with status, error, message, and auto-generated timestamp
     */
    public static Map<String, Object> of(int status, String error, String message) {
        return Map.of(
                "status", status,                        // HTTP status code
                "error", error,                          // Status reason phrase
                "message", message,                      // Error description
                "timestamp", Instant.now().toString()    // Auto-generated ISO timestamp
        );
    }
}
