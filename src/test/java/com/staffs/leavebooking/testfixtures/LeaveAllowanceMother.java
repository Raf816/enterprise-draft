package com.staffs.leavebooking.testfixtures;

import com.staffs.leavebooking.common.domain.Identity;
import com.staffs.leavebooking.leavemanagement.domain.BusinessYear;
import com.staffs.leavebooking.leavemanagement.domain.LeaveAllowance;

/**
 * Object Mother providing pre-configured LeaveAllowance aggregates for unit tests.
 */
public final class LeaveAllowanceMother {

    private LeaveAllowanceMother() {}

    // ─── Default valid values ────────────────────────────────────────────────────

    public static final String STAFF_MEMBER_ID = "550e8400-e29b-41d4-a716-446655440001";
    public static final String MANAGER_ID = "550e8400-e29b-41d4-a716-446655440002";
    public static final String FIRST_NAME = "John";
    public static final String SURNAME = "Smith";
    public static final String DEPARTMENT = "Engineering";
    public static final int DEFAULT_ENTITLEMENT = 25;

    /**
     * Creates a fresh allowance (0 days used, 0 days pending, 25 entitlement).
     */
    public static LeaveAllowance freshAllowance() {
        return LeaveAllowance.reconstitute(
                Identity.generateId(),
                STAFF_MEMBER_ID,
                MANAGER_ID,
                FIRST_NAME,
                SURNAME,
                DEPARTMENT,
                BusinessYear.current(),
                DEFAULT_ENTITLEMENT,
                0,
                0
        );
    }

    /**
     * Creates an allowance with some days used and pending.
     */
    public static LeaveAllowance partiallyUsedAllowance(int daysUsed, int daysPending) {
        return LeaveAllowance.reconstitute(
                Identity.generateId(),
                STAFF_MEMBER_ID,
                MANAGER_ID,
                FIRST_NAME,
                SURNAME,
                DEPARTMENT,
                BusinessYear.current(),
                DEFAULT_ENTITLEMENT,
                daysUsed,
                daysPending
        );
    }

    /**
     * Creates an allowance with a custom entitlement.
     */
    public static LeaveAllowance allowanceWithEntitlement(int entitlement, int daysUsed, int daysPending) {
        return LeaveAllowance.reconstitute(
                Identity.generateId(),
                STAFF_MEMBER_ID,
                MANAGER_ID,
                FIRST_NAME,
                SURNAME,
                DEPARTMENT,
                BusinessYear.current(),
                entitlement,
                daysUsed,
                daysPending
        );
    }
}
