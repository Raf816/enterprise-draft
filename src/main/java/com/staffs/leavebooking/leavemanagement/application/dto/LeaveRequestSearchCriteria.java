package com.staffs.leavebooking.leavemanagement.application.dto;

import java.time.LocalDate;

/**
 * Search criteria record for filtering leave requests via POST /search endpoints
 * (Lecture 5/6 — CQRS Query Model, Enterprise POST Search Pattern).
 *
 * <p><strong>Enterprise POST search pattern:</strong> This follows the convention where
 * complex queries are expressed as a structured JSON body sent via POST rather than
 * multiple query parameters on a GET request. This approach is used by major APIs
 * (Elasticsearch, Stripe) when the number of optional filters makes query-param URLs
 * unwieldy. All fields are optional — null fields are ignored during filtering.
 *
 * <p><strong>Usage per role:</strong>
 * <ul>
 *   <li><strong>Staff</strong> (POST /leave-requests/my/search): may use {@code status} only.
 *       The staffMemberId is derived from the JWT — never from this criteria body.</li>
 *   <li><strong>Manager</strong> (POST /leave-requests/team/search): may use {@code status},
 *       {@code from}, {@code to}. The managerId is derived from the JWT.</li>
 *   <li><strong>Admin</strong> (POST /leave-requests/all/search): may use all fields including
 *       {@code staffMemberId} and {@code managerId} to search across the entire company.</li>
 * </ul>
 *
 * <p><strong>Date range semantics:</strong> When both {@code from} and {@code to} are provided,
 * the query filters by leave requests that overlap with the range — any request where
 * {@code startDate <= to AND endDate >= from}. This catches requests that start before the
 * range but end within it, or span the entire range.
 * Both dates must be provided for the range to take effect — a single date is ignored.
 *
 * <p><strong>Immutability:</strong> As a Java record, this is immutable after construction.
 * Jackson deserialises the JSON body directly into this record's constructor.
 *
 * @param status         filter by leave request status (PENDING, APPROVED, REJECTED, CANCELLED) — optional
 * @param staffMemberId  filter by the staff member who submitted the request (admin only) — optional
 * @param managerId      filter by the manager assigned to approve the request (admin only) — optional
 * @param from           filter by leave start date on or after this date (inclusive) — optional
 * @param to             filter by leave start date on or before this date (inclusive) — optional
 * @see com.staffs.leavebooking.leavemanagement.application.handlers.LeaveRequestQueryHandler for the query handler that uses this criteria
 * @see com.staffs.leavebooking.leavemanagement.ui.LeaveRequestController for the controller endpoints that accept this criteria
 */
public record LeaveRequestSearchCriteria(
        String status,          // Optional status filter — matched against LeaveRequestStatus values
        String staffMemberId,   // Optional staff member filter — only used by admin /all/search
        String managerId,       // Optional manager filter — only used by admin /all/search
        LocalDate from,         // Optional range start — inclusive; requires 'to' to take effect
        LocalDate to            // Optional range end — inclusive; requires 'from' to take effect
) {

    /**
     * Returns true if any filter field is populated (non-null and non-blank for strings).
     *
     * <p>Used by the controller's {@code validateSearchCriteria()} method to reject
     * POST /search calls with no filters, directing the user to the simpler GET endpoint
     * for unfiltered results instead.
     *
     * @return true if at least one filter is set, false if all filters are null/blank
     * @see com.staffs.leavebooking.leavemanagement.ui.LeaveRequestController#searchMyRequests for usage
     */
    public boolean hasFilters() {
        // Check each field — strings must be non-null and non-blank, dates must be non-null
        return (status != null && !status.isBlank())
                || (staffMemberId != null && !staffMemberId.isBlank())
                || (managerId != null && !managerId.isBlank())
                || from != null
                || to != null;
    }

    /**
     * Returns the status in upper case for case-insensitive matching, or null if not set.
     *
     * <p>Normalises the status to upper case so that queries like {@code "pending"},
     * {@code "Pending"}, and {@code "PENDING"} all match the stored enum value.
     * Returns null if the status is null or blank, indicating no status filter.
     *
     * @return the status string in upper case, or null if no status filter is set
     */
    public String normalizedStatus() {
        // Normalise to upper case for case-insensitive matching against stored enum values
        return (status != null && !status.isBlank()) ? status.toUpperCase() : null;
    }
}
