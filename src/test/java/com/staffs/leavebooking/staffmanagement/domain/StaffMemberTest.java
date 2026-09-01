package com.staffs.leavebooking.staffmanagement.domain;

import com.staffs.leavebooking.common.domain.Email;
import com.staffs.leavebooking.common.domain.FullName;
import com.staffs.leavebooking.common.domain.Identity;
import com.staffs.leavebooking.common.events.Event;
import com.staffs.leavebooking.common.events.StaffMemberAddedEvent;
import com.staffs.leavebooking.common.events.StaffMemberUpdatedEvent;
import com.staffs.leavebooking.testfixtures.StaffMemberMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the StaffMember aggregate root.
 * Tests factory methods, command methods, PENDING_SETUP flow,
 * terminal state invariant, and event raising.
 */
@DisplayName("StaffMember Aggregate Root")
class StaffMemberTest {

    // ═══════════════════════════════════════════════════════════════════
    // FACTORY METHOD: createNew()
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createNew() factory method")
    class CreateNew {

        @Test
        @DisplayName("Should create staff member with PENDING_SETUP status")
        void shouldCreateWithPendingSetupStatus() {
            // Arrange & Act
            StaffMember staff = createDefaultStaff();

            // Assert
            assertEquals(EmploymentStatus.PENDING_SETUP, staff.employmentStatus());
        }

        @Test
        @DisplayName("Should NOT raise any events (event fires on activation)")
        void shouldNotRaiseEventsOnCreation() {
            // Arrange & Act
            StaffMember staff = createDefaultStaff();

            // Assert — no events until admin activates
            assertTrue(staff.listOfDomainEvents().isEmpty());
        }

        @Test
        @DisplayName("Should set all fields correctly")
        void shouldSetAllFieldsCorrectly() {
            // Arrange
            Identity<StaffMember> id = Identity.generateId();
            FullName name = new FullName("John", "Doe");
            Email email = new Email("john.doe@company.com");
            LocalDate hireDate = LocalDate.of(2023, 6, 1);
            LocalDate roleStart = LocalDate.of(2024, 1, 1);

            // Act
            StaffMember staff = StaffMember.createNew(
                    id, name, email, "Engineering", "manager-id-123",
                    hireDate, "Developer", roleStart, "L4", EmploymentType.FULL_TIME);

            // Assert
            assertEquals(id, staff.id());
            assertEquals(name, staff.fullName());
            assertEquals(email, staff.email());
            assertEquals("Engineering", staff.department());
            assertEquals("manager-id-123", staff.lineManagerId());
            assertEquals(hireDate, staff.hireDate());
            assertEquals("Developer", staff.currentRole());
            assertEquals(roleStart, staff.startDateOfCurrentRole());
            assertEquals("L4", staff.jobLevel());
            assertEquals(EmploymentType.FULL_TIME, staff.employmentType());
        }

        @Test
        @DisplayName("Should reject hire date in the future")
        void shouldRejectFutureHireDate() {
            // Arrange
            LocalDate futureDate = LocalDate.now().plusDays(1);

            // Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> StaffMember.createNew(
                            Identity.generateId(),
                            new FullName("John", "Doe"),
                            new Email("john@company.com"),
                            "Engineering", "mgr-id",
                            futureDate, "Dev",
                            LocalDate.of(2024, 1, 1), "L4",
                            EmploymentType.FULL_TIME));
            assertEquals(StaffMember.HIRE_DATE_IN_FUTURE, ex.getMessage());
        }

        @Test
        @DisplayName("Should accept hire date of today")
        void shouldAcceptTodayHireDate() {
            // Arrange & Act
            StaffMember staff = StaffMember.createNew(
                    Identity.generateId(),
                    new FullName("John", "Doe"),
                    new Email("john@company.com"),
                    "Engineering", "mgr-id",
                    LocalDate.now(), "Dev",
                    LocalDate.of(2024, 1, 1), "L4",
                    EmploymentType.FULL_TIME);

            // Assert
            assertEquals(LocalDate.now(), staff.hireDate());
        }

        @Test
        @DisplayName("Should reject null full name")
        void shouldRejectNullFullName() {
            assertThrows(IllegalArgumentException.class,
                    () -> StaffMember.createNew(
                            Identity.generateId(), null,
                            new Email("john@company.com"),
                            "Engineering", "mgr-id",
                            LocalDate.of(2023, 1, 1), "Dev",
                            LocalDate.of(2024, 1, 1), "L4",
                            EmploymentType.FULL_TIME));
        }

        @Test
        @DisplayName("Should reject null email")
        void shouldRejectNullEmail() {
            assertThrows(IllegalArgumentException.class,
                    () -> StaffMember.createNew(
                            Identity.generateId(),
                            new FullName("John", "Doe"), null,
                            "Engineering", "mgr-id",
                            LocalDate.of(2023, 1, 1), "Dev",
                            LocalDate.of(2024, 1, 1), "L4",
                            EmploymentType.FULL_TIME));
        }

        @Test
        @DisplayName("Should reject blank department")
        void shouldRejectBlankDepartment() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> StaffMember.createNew(
                            Identity.generateId(),
                            new FullName("John", "Doe"),
                            new Email("john@company.com"),
                            "   ", "mgr-id",
                            LocalDate.of(2023, 1, 1), "Dev",
                            LocalDate.of(2024, 1, 1), "L4",
                            EmploymentType.FULL_TIME));
            assertEquals(StaffMember.DEPARTMENT_REQUIRED, ex.getMessage());
        }

        private StaffMember createDefaultStaff() {
            return StaffMember.createNew(
                    Identity.generateId(),
                    new FullName("John", "Doe"),
                    new Email("john.doe@company.com"),
                    "Engineering", "manager-id-123",
                    LocalDate.of(2023, 6, 1), "Developer",
                    LocalDate.of(2024, 1, 1), "L4",
                    EmploymentType.FULL_TIME);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // FACTORY METHOD: createSkeleton()
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("createSkeleton() factory method")
    class CreateSkeleton {

        @Test
        @DisplayName("Should create skeleton with PENDING_SETUP status")
        void shouldCreateWithPendingSetupStatus() {
            // Arrange & Act
            StaffMember staff = StaffMember.createSkeleton(
                    Identity.generateId(),
                    new FullName("Raf", "Ahmed"),
                    new Email("raf@staffs.ac.uk"));

            // Assert
            assertEquals(EmploymentStatus.PENDING_SETUP, staff.employmentStatus());
        }

        @Test
        @DisplayName("Should set default values for unfilled fields")
        void shouldSetDefaultValues() {
            // Arrange & Act
            StaffMember staff = StaffMember.createSkeleton(
                    Identity.generateId(),
                    new FullName("Raf", "Ahmed"),
                    new Email("raf@staffs.ac.uk"));

            // Assert
            assertEquals("Unassigned", staff.department());
            assertNull(staff.lineManagerId());
            assertEquals("Pending Setup", staff.currentRole());
            assertNull(staff.jobLevel());
            assertEquals(EmploymentType.FULL_TIME, staff.employmentType());
        }

        @Test
        @DisplayName("Should preserve name and email")
        void shouldPreserveNameAndEmail() {
            // Arrange
            Identity<StaffMember> id = Identity.generateId();
            FullName name = new FullName("Raf", "Ahmed");
            Email email = new Email("raf@staffs.ac.uk");

            // Act
            StaffMember staff = StaffMember.createSkeleton(id, name, email);

            // Assert
            assertEquals(id, staff.id());
            assertEquals("Raf", staff.fullName().firstName());
            assertEquals("Ahmed", staff.fullName().surname());
            assertEquals("raf@staffs.ac.uk", staff.email().address());
        }

        @Test
        @DisplayName("Should NOT raise any events")
        void shouldNotRaiseEvents() {
            // Arrange & Act
            StaffMember staff = StaffMember.createSkeleton(
                    Identity.generateId(),
                    new FullName("Raf", "Ahmed"),
                    new Email("raf@staffs.ac.uk"));

            // Assert
            assertTrue(staff.listOfDomainEvents().isEmpty());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // FACTORY METHOD: reconstitute()
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("reconstitute() factory method")
    class Reconstitute {

        @Test
        @DisplayName("Should not raise any events")
        void shouldNotRaiseEvents() {
            StaffMember staff = StaffMemberMother.activeStaffMember();
            assertTrue(staff.listOfDomainEvents().isEmpty());
        }

        @Test
        @DisplayName("Should preserve TERMINATED status")
        void shouldPreserveTerminatedStatus() {
            StaffMember staff = StaffMemberMother.terminatedStaffMember();
            assertEquals(EmploymentStatus.TERMINATED, staff.employmentStatus());
        }

        @Test
        @DisplayName("Should preserve PENDING_SETUP status")
        void shouldPreservePendingSetupStatus() {
            StaffMember staff = StaffMemberMother.pendingSetupStaffMember();
            assertEquals(EmploymentStatus.PENDING_SETUP, staff.employmentStatus());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // COMMAND: updateDepartment()
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updateDepartment() command")
    class UpdateDepartment {

        @Test
        @DisplayName("Should update department and line manager")
        void shouldUpdateDepartmentAndManager() {
            StaffMember staff = StaffMemberMother.activeStaffMember();
            staff.updateDepartment("Finance", "new-mgr-id");
            assertEquals("Finance", staff.department());
            assertEquals("new-mgr-id", staff.lineManagerId());
        }

        @Test
        @DisplayName("Should raise StaffMemberUpdatedEvent")
        void shouldRaiseUpdatedEvent() {
            StaffMember staff = StaffMemberMother.activeStaffMember();
            staff.updateDepartment("Finance", "new-mgr-id");

            List<Event> events = staff.listOfDomainEvents();
            assertEquals(1, events.size());
            assertInstanceOf(StaffMemberUpdatedEvent.class, events.get(0));
            StaffMemberUpdatedEvent event = (StaffMemberUpdatedEvent) events.get(0);
            assertEquals(staff.id().id(), event.staffMemberId());
            assertEquals("new-mgr-id", event.managerId());
            assertEquals("Finance", event.department());
        }

        @Test
        @DisplayName("Should reject blank department")
        void shouldRejectBlankDepartment() {
            StaffMember staff = StaffMemberMother.activeStaffMember();
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> staff.updateDepartment("  ", "mgr-id"));
            assertEquals(StaffMember.DEPARTMENT_REQUIRED, ex.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // COMMAND: updatePlacement()
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updatePlacement() command")
    class UpdatePlacement {

        @Test
        @DisplayName("Should update role, start date, job level, and employment type")
        void shouldUpdateAllPlacementFields() {
            StaffMember staff = StaffMemberMother.activeStaffMember();
            LocalDate newStartDate = LocalDate.of(2025, 6, 1);

            staff.updatePlacement("Lead Developer", newStartDate, "L6", EmploymentType.CONTRACT);

            assertEquals("Lead Developer", staff.currentRole());
            assertEquals(newStartDate, staff.startDateOfCurrentRole());
            assertEquals("L6", staff.jobLevel());
            assertEquals(EmploymentType.CONTRACT, staff.employmentType());
        }

        @Test
        @DisplayName("Should reject blank role")
        void shouldRejectBlankRole() {
            StaffMember staff = StaffMemberMother.activeStaffMember();
            assertThrows(IllegalArgumentException.class,
                    () -> staff.updatePlacement("  ", LocalDate.now(), "L5", EmploymentType.FULL_TIME));
        }

        @Test
        @DisplayName("Should reject null start date")
        void shouldRejectNullStartDate() {
            StaffMember staff = StaffMemberMother.activeStaffMember();
            assertThrows(IllegalArgumentException.class,
                    () -> staff.updatePlacement("Dev", null, "L5", EmploymentType.FULL_TIME));
        }

        @Test
        @DisplayName("Should reject null employment type")
        void shouldRejectNullEmploymentType() {
            StaffMember staff = StaffMemberMother.activeStaffMember();
            assertThrows(IllegalArgumentException.class,
                    () -> staff.updatePlacement("Dev", LocalDate.now(), "L5", null));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // COMMAND: updateStatus()
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("updateStatus() command")
    class UpdateStatus {

        @Test
        @DisplayName("Should transition PENDING_SETUP to ACTIVE and raise StaffMemberAddedEvent")
        void shouldActivateAndRaiseEvent() {
            // Arrange
            StaffMember staff = StaffMemberMother.pendingSetupStaffMember();

            // Act
            staff.updateStatus(EmploymentStatus.ACTIVE);

            // Assert
            assertEquals(EmploymentStatus.ACTIVE, staff.employmentStatus());
            List<Event> events = staff.listOfDomainEvents();
            assertEquals(1, events.size());
            assertInstanceOf(StaffMemberAddedEvent.class, events.get(0));
            StaffMemberAddedEvent event = (StaffMemberAddedEvent) events.get(0);
            assertEquals(staff.id().id(), event.staffMemberId());
        }

        @Test
        @DisplayName("Should NOT raise event when transitioning ACTIVE to ON_LEAVE")
        void shouldNotRaiseEventOnNonActivation() {
            StaffMember staff = StaffMemberMother.activeStaffMember();
            staff.updateStatus(EmploymentStatus.ON_LEAVE);

            assertEquals(EmploymentStatus.ON_LEAVE, staff.employmentStatus());
            assertTrue(staff.listOfDomainEvents().isEmpty());
        }

        @Test
        @DisplayName("Should transition ACTIVE to TERMINATED")
        void shouldTransitionActiveToTerminated() {
            StaffMember staff = StaffMemberMother.activeStaffMember();
            staff.updateStatus(EmploymentStatus.TERMINATED);
            assertEquals(EmploymentStatus.TERMINATED, staff.employmentStatus());
        }

        @Test
        @DisplayName("Should transition ON_LEAVE to ACTIVE without event")
        void shouldTransitionOnLeaveToActive() {
            StaffMember staff = StaffMemberMother.onLeaveStaffMember();
            staff.updateStatus(EmploymentStatus.ACTIVE);

            assertEquals(EmploymentStatus.ACTIVE, staff.employmentStatus());
            assertTrue(staff.listOfDomainEvents().isEmpty());
        }

        @Test
        @DisplayName("Should allow TERMINATED to TERMINATED (no-op)")
        void shouldAllowTerminatedToTerminated() {
            StaffMember staff = StaffMemberMother.terminatedStaffMember();
            assertDoesNotThrow(() -> staff.updateStatus(EmploymentStatus.TERMINATED));
        }

        @Test
        @DisplayName("Should reject TERMINATED to ACTIVE (terminal state)")
        void shouldRejectTerminatedToActive() {
            StaffMember staff = StaffMemberMother.terminatedStaffMember();
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> staff.updateStatus(EmploymentStatus.ACTIVE));
            assertEquals(StaffMember.CANNOT_REACTIVATE_TERMINATED, ex.getMessage());
        }

        @Test
        @DisplayName("Should reject TERMINATED to ON_LEAVE (terminal state)")
        void shouldRejectTerminatedToOnLeave() {
            StaffMember staff = StaffMemberMother.terminatedStaffMember();
            assertThrows(IllegalStateException.class,
                    () -> staff.updateStatus(EmploymentStatus.ON_LEAVE));
        }

        @Test
        @DisplayName("Should allow PENDING_SETUP to TERMINATED")
        void shouldAllowPendingSetupToTerminated() {
            StaffMember staff = StaffMemberMother.pendingSetupStaffMember();
            staff.updateStatus(EmploymentStatus.TERMINATED);
            assertEquals(EmploymentStatus.TERMINATED, staff.employmentStatus());
            assertTrue(staff.listOfDomainEvents().isEmpty()); // no activation event
        }

        @Test
        @DisplayName("Should reject null status")
        void shouldRejectNullStatus() {
            StaffMember staff = StaffMemberMother.activeStaffMember();
            assertThrows(IllegalArgumentException.class, () -> staff.updateStatus(null));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // ENTITY EQUALITY
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Entity equality")
    class EntityEquality {

        @Test
        @DisplayName("Same identity means equal regardless of state")
        void sameIdentityShouldBeEqual() {
            Identity<StaffMember> id = Identity.generateId();
            StaffMember s1 = StaffMember.reconstitute(id,
                    new FullName("A", "B"), new Email("a@b.com"),
                    "Dept1", "mgr1", LocalDate.of(2020, 1, 1),
                    "Role1", LocalDate.of(2020, 1, 1), "L1",
                    EmploymentType.FULL_TIME, EmploymentStatus.ACTIVE);
            StaffMember s2 = StaffMember.reconstitute(id,
                    new FullName("C", "D"), new Email("c@d.com"),
                    "Dept2", "mgr2", LocalDate.of(2021, 1, 1),
                    "Role2", LocalDate.of(2021, 1, 1), "L2",
                    EmploymentType.CONTRACT, EmploymentStatus.TERMINATED);

            assertEquals(s1, s2);
            assertEquals(s1.hashCode(), s2.hashCode());
        }
    }
}
