package com.staffs.leavebooking.testfixtures;

import com.staffs.leavebooking.leavemanagement.infrastructure.entities.LeaveAllowanceJpa;
import com.staffs.leavebooking.leavemanagement.infrastructure.entities.LeaveRequestJpa;
import com.staffs.leavebooking.staffmanagement.infrastructure.entities.StaffMemberJpa;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Object Mother providing pre-configured JPA entities for mapper unit tests.
 */
public final class JpaEntityMother {

    private JpaEntityMother() {}

    /**
     * Creates a valid LeaveRequestJpa entity with all fields populated.
     */
    public static LeaveRequestJpa leaveRequestJpa() {
        LeaveRequestJpa jpa = new LeaveRequestJpa();
        jpa.setId(UUID.randomUUID().toString());
        jpa.setStaffMemberId(UUID.randomUUID().toString());
        jpa.setManagerId(UUID.randomUUID().toString());
        jpa.setLeaveType("ANNUAL");
        jpa.setStartDate(LocalDate.of(2027, 3, 10));
        jpa.setEndDate(LocalDate.of(2027, 3, 14));
        jpa.setNumberOfDays(5);
        jpa.setReason("Family holiday");
        jpa.setStatus("PENDING");
        jpa.setSubmittedOn(LocalDate.of(2027, 3, 1));
        jpa.setDecidedOn(null);
        jpa.setDecidedBy(null);
        jpa.setCancellationReason(null);
        return jpa;
    }

    /**
     * Creates a valid LeaveAllowanceJpa entity with all fields populated.
     */
    public static LeaveAllowanceJpa leaveAllowanceJpa() {
        LeaveAllowanceJpa jpa = new LeaveAllowanceJpa();
        jpa.setId(UUID.randomUUID().toString());
        jpa.setStaffMemberId(UUID.randomUUID().toString());
        jpa.setManagerId(UUID.randomUUID().toString());
        jpa.setFirstName("John");
        jpa.setSurname("Smith");
        jpa.setDepartment("Engineering");
        jpa.setBusinessYearStart(2026);
        jpa.setBusinessYearEnd(2027);
        jpa.setTotalEntitlement(25);
        jpa.setDaysUsed(5);
        jpa.setDaysPending(3);
        return jpa;
    }

    /**
     * Creates a valid StaffMemberJpa entity with all fields populated.
     */
    public static StaffMemberJpa staffMemberJpa() {
        StaffMemberJpa jpa = new StaffMemberJpa();
        jpa.setId(UUID.randomUUID().toString());
        jpa.setFirstName("Jane");
        jpa.setSurname("Doe");
        jpa.setEmail("jane.doe@company.com");
        jpa.setDepartment("Engineering");
        jpa.setLineManagerId(UUID.randomUUID().toString());
        jpa.setHireDate(LocalDate.of(2023, 6, 1));
        jpa.setCurrentRole("Senior Developer");
        jpa.setStartDateCurrentRole(LocalDate.of(2024, 1, 1));
        jpa.setJobLevel("L5");
        jpa.setEmploymentType("FULL_TIME");
        jpa.setEmploymentStatus("ACTIVE");
        return jpa;
    }
}
