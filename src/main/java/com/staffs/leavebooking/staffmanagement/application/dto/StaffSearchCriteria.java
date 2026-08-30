package com.staffs.leavebooking.staffmanagement.application.dto;

/**
 * Search criteria record for filtering staff members via POST /staff/search
 * (Lecture 5/6 — CQRS Queries, Enterprise Search Pattern).
 *
 * <p><strong>Enterprise POST search pattern:</strong> Instead of using query parameters
 * (GET /staff?department=Networks&status=ACTIVE), we use a POST request with a
 * JSON body. This is the same pattern used by Elasticsearch and Stripe — it keeps
 * URLs clean when multiple optional filters are needed.
 *
 * <p><strong>Validation:</strong> At least one filter must be provided.
 * The controller checks {@link #hasFilters()} and returns 400 if no filters are set.
 * For unfiltered results, callers should use GET /staff instead.
 *
 * <p><strong>Supported filters (all optional, any combination):</strong>
 * <ul>
 *   <li>{@code department} — filter by department name (e.g., "Networks", "Digital")</li>
 *   <li>{@code status} — filter by employment status (PENDING_SETUP, ACTIVE, TERMINATED, etc.)</li>
 * </ul>
 *
 * @param department filter by department name (null = no filter)
 * @param status     filter by employment status string (null = no filter)
 */
public record StaffSearchCriteria(
        String department,  // Department filter (null = no department filter)
        String status       // Employment status filter (null = no status filter)
) {
    /**
     * Checks whether at least one filter field is populated.
     * Returns false if both fields are null or blank — the controller uses this
     * to reject empty search requests (400 Bad Request).
     *
     * @return true if at least one filter is set
     */
    public boolean hasFilters() {
        return (department != null && !department.isBlank())
                || (status != null && !status.isBlank());
    }
}
