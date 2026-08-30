package com.staffs.leavebooking.leavemanagement.ui.exceptions;

/**
 * Unchecked exception thrown when a leave allowance cannot be found for a given identifier.
 *
 * <p><strong>DDD context:</strong> This exception is raised in the UI/application layer
 * when a query or command references a staff member (or allowance ID) for which no
 * {@code LeaveAllowance} aggregate exists in the persistence store. This typically
 * occurs when a leave request is submitted before the staff member's allowance has been
 * provisioned (via the {@code StaffMemberAddedEvent} from Staff Management).
 *
 * <p><strong>HTTP mapping:</strong> Caught by the {@link com.staffs.leavebooking.GlobalExceptionHandler}
 * via the {@code ResponseStatusException} or general exception handler and returned to the
 * client as an HTTP 404 (Not Found) response. The exception message (e.g.
 * "Leave allowance not found for: &lt;identifier&gt;") is included in the JSON error body.
 *
 * <p><strong>Why RuntimeException?</strong> Unchecked exceptions are used here because
 * a missing entity is a recoverable, expected scenario (not a programming error) but
 * should not force every caller to handle a checked exception. The global exception
 * handler catches it centrally and returns the appropriate HTTP status.
 *
 * @see com.staffs.leavebooking.GlobalExceptionHandler  handles this exception and returns HTTP 404
 * @see LeaveRequestNotFoundException                   the equivalent exception for leave requests
 */
public class LeaveAllowanceNotFoundException extends RuntimeException {

    /**
     * Constructs a new {@code LeaveAllowanceNotFoundException} with a message containing
     * the identifier that could not be resolved to an existing leave allowance.
     *
     * <p>The message format is: {@code "Leave allowance not found for: <identifier>"}.
     * The {@code identifier} may be a staff member UUID, an allowance aggregate UUID,
     * or a composite key depending on the calling context. This message is propagated
     * to the API response body by the global exception handler.
     *
     * @param identifier the UUID or descriptive key that was used to look up the allowance;
     *                   included in the exception message for debugging and logging
     */
    public LeaveAllowanceNotFoundException(String identifier) {
        super("Leave allowance not found for: " + identifier);
    }
}
