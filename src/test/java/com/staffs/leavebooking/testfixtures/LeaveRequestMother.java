package com.staffs.leavebooking.testfixtures;

import com.staffs.leavebooking.common.domain.Identity;
import com.staffs.leavebooking.leavemanagement.domain.*;

import java.time.LocalDate;

/**
 * Object Mother providing pre-configured LeaveRequest aggregates for unit tests.
 */
public final class LeaveRequestMother {

    private LeaveRequestMother() {}

    public static final String STAFF_MEMBER_ID = "550e8400-e29b-41d4-a716-446655440001";
    public static final String MANAGER_ID = "550e8400-e29b-41d4-a716-446655440002";
    public static final String DECIDER_ID = "550e8400-e29b-41d4-a716-446655440003";

    public static LeaveRequest pendingRequest() {
        return LeaveRequest.reconstitute(
                Identity.generateId(), STAFF_MEMBER_ID, MANAGER_ID,
                LeaveType.ANNUAL,
                new DateRange(LocalDate.now().plusDays(5), LocalDate.now().plusDays(9)),
                5, "Annual family holiday", LeaveRequestStatus.PENDING,
                LocalDate.now(), null, null, null, null
        );
    }

    public static LeaveRequest approvedRequest() {
        return LeaveRequest.reconstitute(
                Identity.generateId(), STAFF_MEMBER_ID, MANAGER_ID,
                LeaveType.ANNUAL,
                new DateRange(LocalDate.now().plusDays(5), LocalDate.now().plusDays(9)),
                5, "Annual family holiday", LeaveRequestStatus.APPROVED,
                LocalDate.now().minusDays(2), LocalDate.now(), DECIDER_ID,
                "Approved, enjoy your holiday", null
        );
    }

    public static LeaveRequest rejectedRequest() {
        return LeaveRequest.reconstitute(
                Identity.generateId(), STAFF_MEMBER_ID, MANAGER_ID,
                LeaveType.ANNUAL,
                new DateRange(LocalDate.now().plusDays(5), LocalDate.now().plusDays(9)),
                5, "Annual family holiday", LeaveRequestStatus.REJECTED,
                LocalDate.now().minusDays(2), LocalDate.now(), DECIDER_ID,
                "Team is short-staffed that week", null
        );
    }

    public static LeaveRequest cancelledRequest() {
        return LeaveRequest.reconstitute(
                Identity.generateId(), STAFF_MEMBER_ID, MANAGER_ID,
                LeaveType.ANNUAL,
                new DateRange(LocalDate.now().plusDays(5), LocalDate.now().plusDays(9)),
                5, "Annual family holiday", LeaveRequestStatus.CANCELLED,
                LocalDate.now().minusDays(2), null, null,
                null, "Changed plans"
        );
    }

    public static DateRange futureDateRange(int daysFromNow, int durationDays) {
        return new DateRange(
                LocalDate.now().plusDays(daysFromNow),
                LocalDate.now().plusDays(daysFromNow + durationDays - 1)
        );
    }
}
