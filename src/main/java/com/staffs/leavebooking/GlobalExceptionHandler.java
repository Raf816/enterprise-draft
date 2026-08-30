package com.staffs.leavebooking;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.validation.FieldError;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Global exception handler that catches all unhandled exceptions across the application
 * and converts them into clean, consistent JSON error responses
 * (Lecture 9 — Error Handling, REST API Best Practices).
 *
 * <p><strong>@ControllerAdvice:</strong> Spring annotation that makes this class a global
 * exception handler. Any exception thrown by any controller (or deeper — services, domain)
 * that isn't caught locally will be caught here. This provides a single, centralised place
 * for error response formatting.
 *
 * <p><strong>Consistent error format:</strong> Every error response follows the same structure:
 * <pre>
 * {
 *   "status": 400,                                    // HTTP status code
 *   "error": "Bad Request",                           // HTTP status reason phrase
 *   "message": "End date must be on or after start date", // Human-readable description
 *   "timestamp": "2026-09-01T10:15:30Z",             // ISO-8601 when error occurred
 *   "errors": { "email": "must not be blank" }       // (optional) field-level validation errors
 * }
 * </pre>
 *
 * <p><strong>Exception type → HTTP status mapping:</strong>
 * <table>
 *   <tr><th>Exception Type</th><th>HTTP Status</th><th>When Thrown</th></tr>
 *   <tr><td>{@link ResponseStatusException}</td><td>Varies (400/403/404/409)</td>
 *       <td>Controllers throw these explicitly with the desired status</td></tr>
 *   <tr><td>{@link MethodArgumentNotValidException}</td><td>400 Bad Request</td>
 *       <td>Bean Validation fails on @Valid request body (e.g., RegisterRequest)</td></tr>
 *   <tr><td>{@link ConstraintViolationException}</td><td>400 Bad Request</td>
 *       <td>JPA/Jakarta validation fails at the persistence layer</td></tr>
 *   <tr><td>{@link DataIntegrityViolationException}</td><td>409 Conflict</td>
 *       <td>Database unique constraint violated (e.g., duplicate email)</td></tr>
 *   <tr><td>{@link IllegalArgumentException}</td><td>400 Bad Request</td>
 *       <td>Domain validation fails (DomainAssertions guard clauses, invalid enum values)</td></tr>
 *   <tr><td>{@link IllegalStateException}</td><td>409 Conflict</td>
 *       <td>State machine violations (e.g., approve a non-PENDING request, reactivate terminated staff)</td></tr>
 *   <tr><td>Any other Exception</td><td>500 Internal Server Error</td>
 *       <td>Unexpected errors — logged for investigation</td></tr>
 * </table>
 *
 * @see com.staffs.leavebooking.identity.dto.ErrorResponse for the AuthController's error factory
 * @see com.staffs.leavebooking.identity.security.UnauthorisedAccessLogger for 401/403 responses
 */
@ControllerAdvice // Spring: this class handles exceptions from ALL controllers globally
@Slf4j            // Lombok: generates a private static final Logger (SLF4J)
public class GlobalExceptionHandler {

    /**
     * Catches ALL exceptions thrown by any controller or service in the application.
     *
     * <p><strong>Flow:</strong>
     * <ol>
     *   <li>Determine the HTTP status based on the exception type (using instanceof checks)</li>
     *   <li>Extract or format the error message</li>
     *   <li>For validation errors, collect field-level error details into a map</li>
     *   <li>Log the exception with the determined status</li>
     *   <li>Build and return a consistent JSON error response</li>
     * </ol>
     *
     * @param ex the exception that was thrown (any type)
     * @return a ResponseEntity with the appropriate HTTP status and JSON error body
     */
    @ExceptionHandler(Exception.class) // Catches ALL exception types
    public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception ex) {
        // Default to 500 Internal Server Error — overridden below for known exception types
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = ex.getMessage();
        Map<String, String> validationErrors = null; // Only populated for validation exceptions

        // ── Determine HTTP status based on exception type ──

        if (ex instanceof org.springframework.security.access.AccessDeniedException
                || ex instanceof org.springframework.security.authorization.AuthorizationDeniedException) {
            // Spring Security access denied — should be 403 Forbidden
            // This catches @PreAuthorize failures that bubble up through the controller
            status = HttpStatus.FORBIDDEN;
            message = "Access denied. You do not have permission to access this resource.";

        } else if (ex instanceof org.springframework.security.core.AuthenticationException) {
            // Spring Security authentication failure — should be 401 Unauthorized
            status = HttpStatus.UNAUTHORIZED;
            message = "Authentication required. Please provide a valid Bearer token.";

        } else if (ex instanceof ResponseStatusException rse) {
            // ResponseStatusException: controllers throw these explicitly with a specific status
            status = HttpStatus.valueOf(rse.getStatusCode().value());
            message = rse.getReason();

        } else if (ex instanceof com.staffs.leavebooking.staffmanagement.ui.exceptions.StaffMemberNotFoundException
                || ex instanceof com.staffs.leavebooking.leavemanagement.ui.exceptions.LeaveRequestNotFoundException
                || ex instanceof com.staffs.leavebooking.leavemanagement.ui.exceptions.LeaveAllowanceNotFoundException) {
            // Not-found exceptions from our domain — 404
            status = HttpStatus.NOT_FOUND;
            message = ex.getMessage();

        } else if (ex instanceof MethodArgumentNotValidException manve) {
            // MethodArgumentNotValidException: Bean Validation (@Valid) failed on request body
            // Example: RegisterRequest with blank username or invalid email
            status = HttpStatus.BAD_REQUEST; // 400
            message = "Validation failed for one or more fields.";
            // Collect each field's error message into a map: { "email": "must not be blank", "password": "too short" }
            validationErrors = manve.getBindingResult().getFieldErrors().stream()
                    .collect(Collectors.toMap(
                            FieldError::getField, // Key: the field name (e.g., "email")
                            error -> Objects.requireNonNullElse(error.getDefaultMessage(), "Invalid value"), // Value: error message
                            (existing, replacement) -> existing // If duplicate field, keep first error
                    ));

        } else if (ex instanceof ConstraintViolationException cve) {
            // ConstraintViolationException: JPA/Jakarta validation failed at persistence layer
            // Safety net — domain validation should catch these first
            status = HttpStatus.BAD_REQUEST; // 400
            message = "Database constraint validation failed.";
            // Collect constraint violations into a map: { "firstName": "must not be blank" }
            validationErrors = cve.getConstraintViolations().stream()
                    .collect(Collectors.toMap(
                            violation -> violation.getPropertyPath().toString(), // Property path
                            ConstraintViolation::getMessage // Constraint message
                    ));

        } else if (ex instanceof DataIntegrityViolationException dive) {
            // DataIntegrityViolationException: database unique constraint violated
            // Example: trying to create a staff member with a duplicate email
            status = HttpStatus.CONFLICT; // 409
            message = dive.getMessage() != null ? dive.getMessage() : "A duplicate record already exists.";

        } else if (ex instanceof IllegalArgumentException) {
            // IllegalArgumentException: domain validation failed (DomainAssertions guard clauses)
            // Examples: "End date must be on or after start date", "Invalid leave type: SICK"
            status = HttpStatus.BAD_REQUEST; // 400
            message = ex.getMessage(); // Use the domain's validation message directly

        } else if (ex instanceof IllegalStateException) {
            // IllegalStateException: state machine or business rule violation
            // Examples: "Only PENDING requests can be approved", "A terminated staff member cannot be reactivated"
            status = HttpStatus.CONFLICT; // 409
            message = ex.getMessage(); // Use the domain's invariant violation message directly
        }
        // Any other exception type falls through with 500 Internal Server Error

        // ── Log the exception for debugging and operational monitoring ──
        log.error("Exception handled: [{}] {}", status.value(), message, ex);

        // ── Build the JSON response body ──
        Map<String, Object> responseBody = new HashMap<>(Map.of(
                "status", status.value(),                                              // HTTP status code (e.g., 400)
                "error", status.getReasonPhrase(),                                     // Status text (e.g., "Bad Request")
                "message", Objects.requireNonNullElse(message, "No message provided"), // Error description
                "timestamp", Instant.now().toString()                                  // ISO-8601 timestamp
        ));

        // Optionally add field-level validation errors (only for Bean Validation / constraint violations)
        if (validationErrors != null) {
            responseBody.put("errors", validationErrors); // e.g., { "email": "must not be blank" }
        }

        // Return the response with the determined HTTP status and JSON body
        return ResponseEntity.status(status).body(responseBody);
    }
}
