package com.staffs.leavebooking.leavemanagement.ui.exceptions;

/**
 * Unchecked exception thrown when a leave request cannot be found by its unique identifier.
 *
 * <p><strong>DDD context:</strong> This exception is raised in the UI/application layer
 * when a query or command references a leave request ID that does not exist in the
 * persistence store. It signals a "not found" condition rather than a domain invariant
 * violation, which is why it lives in the UI exceptions package rather than the domain layer.
 *
 * <p><strong>HTTP mapping:</strong> Caught by the {@link com.staffs.leavebooking.GlobalExceptionHandler}
 * via the {@code ResponseStatusException} or general exception handler and returned to the
 * client as an HTTP 404 (Not Found) response. The exception message (e.g.
 * "Leave request not found: &lt;uuid&gt;") is included in the JSON error body.
 *
 * <p><strong>Why RuntimeException?</strong> Unchecked exceptions are used here because
 * a missing entity is a recoverable, expected scenario (not a programming error) but
 * should not force every caller to handle a checked exception. The global exception
 * handler catches it centrally and returns the appropriate HTTP status.
 *
 * @see com.staffs.leavebooking.GlobalExceptionHandler  handles this exception and returns HTTP 404
 * @see LeaveAllowanceNotFoundException                  the equivalent exception for leave allowances
 */
public class LeaveRequestNotFoundException extends RuntimeException {

    /**
     * Constructs a new {@code LeaveRequestNotFoundException} with a message containing
     * the leave request ID that could not be found.
     *
     * <p>The message format is: {@code "Leave request not found: <leaveRequestId>"}.
     * This message is propagated to the API response body by the global exception handler.
     *
     * @param leaveRequestId the UUID string of the leave request that was not found;
     *                       included in the exception message for debugging and logging
     */
    public LeaveRequestNotFoundException(String leaveRequestId) {
        super("Leave request not found: " + leaveRequestId);
    }
}
