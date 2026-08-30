package com.staffs.leavebooking.staffmanagement.ui.exceptions;

/**
 * Exception thrown when a staff member cannot be found by their ID.
 *
 * <p><strong>Caught by:</strong> {@code GlobalExceptionHandler} which returns a clean
 * 404 Not Found JSON response with the staff member ID in the error message.
 *
 * <p><strong>Thrown by:</strong>
 * <ul>
 *   <li>{@code StaffQueryHandler.findStaffMemberById()} — when the ID doesn't exist in the repository</li>
 *   <li>{@code StaffApplicationService.loadDomainAggregate()} — when loading for update operations</li>
 * </ul>
 *
 * @see com.staffs.leavebooking.GlobalExceptionHandler for the error response mapping
 */
public class StaffMemberNotFoundException extends RuntimeException {

    /**
     * Creates a not-found exception with a message containing the staff member ID.
     *
     * @param staffMemberId the UUID of the staff member that was not found
     */
    public StaffMemberNotFoundException(String staffMemberId) {
        super("Staff member not found: " + staffMemberId);
    }
}
