package com.staffs.leavebooking.testfixtures;

import com.staffs.leavebooking.common.domain.Email;
import com.staffs.leavebooking.common.domain.FullName;
import com.staffs.leavebooking.common.domain.Identity;
import com.staffs.leavebooking.staffmanagement.domain.EmploymentStatus;
import com.staffs.leavebooking.staffmanagement.domain.EmploymentType;
import com.staffs.leavebooking.staffmanagement.domain.StaffMember;

import java.time.LocalDate;

/**
 * Object Mother providing pre-configured StaffMember aggregates for unit tests.
 */
public final class StaffMemberMother {

    private StaffMemberMother() {}

    // ─── Default valid values ────────────────────────────────────────────────────

    public static final Identity<StaffMember> DEFAULT_ID = Identity.generateId();
    public static final FullName DEFAULT_FULL_NAME = new FullName("Jane", "Doe");
    public static final Email DEFAULT_EMAIL = new Email("jane.doe@company.com");
    public static final String DEFAULT_DEPARTMENT = "Engineering";
    public static final String DEFAULT_LINE_MANAGER_ID = "550e8400-e29b-41d4-a716-446655440099";
    public static final LocalDate DEFAULT_HIRE_DATE = LocalDate.of(2023, 6, 1);
    public static final String DEFAULT_ROLE = "Senior Developer";
    public static final LocalDate DEFAULT_ROLE_START = LocalDate.of(2024, 1, 1);
    public static final String DEFAULT_JOB_LEVEL = "L5";
    public static final EmploymentType DEFAULT_EMPLOYMENT_TYPE = EmploymentType.FULL_TIME;
    public static final int DEFAULT_LEAVE_ENTITLEMENT = 25;

    /**
     * Creates a valid ACTIVE staff member via reconstitute (no events, no date checks).
     */
    public static StaffMember activeStaffMember() {
        return StaffMember.reconstitute(
                Identity.generateId(),
                DEFAULT_FULL_NAME,
                DEFAULT_EMAIL,
                DEFAULT_DEPARTMENT,
                DEFAULT_LINE_MANAGER_ID,
                DEFAULT_HIRE_DATE,
                DEFAULT_ROLE,
                DEFAULT_ROLE_START,
                DEFAULT_JOB_LEVEL,
                DEFAULT_EMPLOYMENT_TYPE,
                EmploymentStatus.ACTIVE
        );
    }

    /**
     * Creates a TERMINATED staff member.
     */
    public static StaffMember terminatedStaffMember() {
        return StaffMember.reconstitute(
                Identity.generateId(),
                DEFAULT_FULL_NAME,
                DEFAULT_EMAIL,
                DEFAULT_DEPARTMENT,
                DEFAULT_LINE_MANAGER_ID,
                DEFAULT_HIRE_DATE,
                DEFAULT_ROLE,
                DEFAULT_ROLE_START,
                DEFAULT_JOB_LEVEL,
                DEFAULT_EMPLOYMENT_TYPE,
                EmploymentStatus.TERMINATED
        );
    }

    /**
     * Creates an ON_LEAVE staff member.
     */
    public static StaffMember onLeaveStaffMember() {
        return StaffMember.reconstitute(
                Identity.generateId(),
                DEFAULT_FULL_NAME,
                DEFAULT_EMAIL,
                DEFAULT_DEPARTMENT,
                DEFAULT_LINE_MANAGER_ID,
                DEFAULT_HIRE_DATE,
                DEFAULT_ROLE,
                DEFAULT_ROLE_START,
                DEFAULT_JOB_LEVEL,
                DEFAULT_EMPLOYMENT_TYPE,
                EmploymentStatus.ON_LEAVE
        );
    }

    /**
     * Creates a PENDING_SETUP staff member (awaiting admin activation).
     */
    public static StaffMember pendingSetupStaffMember() {
        return StaffMember.reconstitute(
                Identity.generateId(),
                DEFAULT_FULL_NAME,
                DEFAULT_EMAIL,
                "Unassigned",
                null,
                DEFAULT_HIRE_DATE,
                "Pending Setup",
                DEFAULT_ROLE_START,
                null,
                DEFAULT_EMPLOYMENT_TYPE,
                EmploymentStatus.PENDING_SETUP
        );
    }
}
