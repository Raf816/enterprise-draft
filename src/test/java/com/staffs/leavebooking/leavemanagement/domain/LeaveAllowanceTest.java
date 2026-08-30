package com.staffs.leavebooking.leavemanagement.domain;

import com.staffs.leavebooking.common.domain.Identity;
import com.staffs.leavebooking.testfixtures.LeaveAllowanceMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the LeaveAllowance aggregate root.
 * Tests factory methods, command methods, invariants, and derived accessors.
 */
@DisplayName("LeaveAllowance Aggregate Root")
class LeaveAllowanceTest {

    // ─────────────────────────────────────────────────────────────────────────────
    // FACTORY METHOD: createNew()
    // ─────────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createNew() factory method")
    class CreateNew {

        @Test
        @DisplayName("Should create allowance with zero days used and pending")
        void shouldCreateWithZeroDays() {
            // Arrange
            Identity<LeaveAllowance> id = Identity.generateId();

            // Act
            LeaveAllowance allowance = LeaveAllowance.createNew(
                    id, "staff-id-123", "manager-id-456",
                    "John", "Smith", "Engineering", 25);

            // Assert
            assertEquals(0, allowance.daysUsed());
            assertEquals(0, allowance.daysPending());
            assertEquals(25, allowance.totalEntitlement());
            assertEquals(BusinessYear.current(), allowance.businessYear());
        }

        @Test
        @DisplayName("Should reject blank staff member ID")
        void shouldRejectBlankStaffId() {
            // Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> LeaveAllowance.createNew(
                            Identity.generateId(), "   ", "manager-id",
                            "John", "Smith", "Eng", 25));
            assertEquals(LeaveAllowance.STAFF_MEMBER_ID_REQUIRED, ex.getMessage());
        }

        @Test
        @DisplayName("Should reject blank manager ID")
        void shouldRejectBlankManagerId() {
            // Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> LeaveAllowance.createNew(
                            Identity.generateId(), "staff-id", "   ",
                            "John", "Smith", "Eng", 25));
            assertEquals(LeaveAllowance.MANAGER_ID_REQUIRED, ex.getMessage());
        }

        @Test
        @DisplayName("Should reject zero entitlement")
        void shouldRejectZeroEntitlement() {
            // Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> LeaveAllowance.createNew(
                            Identity.generateId(), "staff-id", "manager-id",
                            "John", "Smith", "Eng", 0));
            assertEquals(LeaveAllowance.ENTITLEMENT_MUST_BE_POSITIVE, ex.getMessage());
        }

        @Test
        @DisplayName("Should reject negative entitlement")
        void shouldRejectNegativeEntitlement() {
            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> LeaveAllowance.createNew(
                            Identity.generateId(), "staff-id", "manager-id",
                            "John", "Smith", "Eng", -5));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // COMMAND: reserveDays()
    // ─────────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("reserveDays() command")
    class ReserveDays {

        @Test
        @DisplayName("Should increase days pending by requested amount")
        void shouldIncreaseDaysPending() {
            // Arrange
            LeaveAllowance allowance = LeaveAllowanceMother.freshAllowance();

            // Act
            allowance.reserveDays(5);

            // Assert
            assertEquals(5, allowance.daysPending());
            assertEquals(0, allowance.daysUsed());
        }

        @Test
        @DisplayName("Should allow multiple reservations up to entitlement")
        void shouldAllowMultipleReservations() {
            // Arrange
            LeaveAllowance allowance = LeaveAllowanceMother.freshAllowance();

            // Act
            allowance.reserveDays(10);
            allowance.reserveDays(10);
            allowance.reserveDays(5);

            // Assert
            assertEquals(25, allowance.daysPending());
        }

        @Test
        @DisplayName("Should throw when over-booking invariant violated")
        void shouldThrowOnOverbooking() {
            // Arrange
            LeaveAllowance allowance = LeaveAllowanceMother.freshAllowance(); // 25 entitlement

            // Act — use 20 pending, then try 6 more
            allowance.reserveDays(20);

            // Assert
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> allowance.reserveDays(6));
            assertTrue(ex.getMessage().startsWith(LeaveAllowance.INSUFFICIENT_BALANCE));
        }

        @Test
        @DisplayName("Should consider daysUsed in over-booking check")
        void shouldConsiderDaysUsedInOverbookingCheck() {
            // Arrange — 10 used, 10 pending, entitlement 25 → only 5 available
            LeaveAllowance allowance = LeaveAllowanceMother.partiallyUsedAllowance(10, 10);

            // Act & Assert
            assertDoesNotThrow(() -> allowance.reserveDays(5));
            assertThrows(IllegalStateException.class, () -> allowance.reserveDays(1));
        }

        @Test
        @DisplayName("Should reject zero days")
        void shouldRejectZeroDays() {
            // Arrange
            LeaveAllowance allowance = LeaveAllowanceMother.freshAllowance();

            // Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> allowance.reserveDays(0));
            assertEquals(LeaveAllowance.DAYS_MUST_BE_POSITIVE, ex.getMessage());
        }

        @Test
        @DisplayName("Should reject negative days")
        void shouldRejectNegativeDays() {
            // Arrange
            LeaveAllowance allowance = LeaveAllowanceMother.freshAllowance();

            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> allowance.reserveDays(-1));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // COMMAND: confirmDays()
    // ─────────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("confirmDays() command")
    class ConfirmDays {

        @Test
        @DisplayName("Should move days from pending to used")
        void shouldMoveDaysFromPendingToUsed() {
            // Arrange
            LeaveAllowance allowance = LeaveAllowanceMother.partiallyUsedAllowance(0, 5);

            // Act
            allowance.confirmDays(5);

            // Assert
            assertEquals(5, allowance.daysUsed());
            assertEquals(0, allowance.daysPending());
        }

        @Test
        @DisplayName("Should throw when confirming more days than pending")
        void shouldThrowWhenConfirmingMoreThanPending() {
            // Arrange
            LeaveAllowance allowance = LeaveAllowanceMother.partiallyUsedAllowance(0, 3);

            // Act & Assert
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> allowance.confirmDays(5));
            assertEquals(LeaveAllowance.CANNOT_RELEASE_MORE_THAN_PENDING, ex.getMessage());
        }

        @Test
        @DisplayName("Should reject zero days")
        void shouldRejectZeroDays() {
            // Arrange
            LeaveAllowance allowance = LeaveAllowanceMother.partiallyUsedAllowance(0, 5);

            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> allowance.confirmDays(0));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // COMMAND: releasePendingDays()
    // ─────────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("releasePendingDays() command")
    class ReleasePendingDays {

        @Test
        @DisplayName("Should reduce days pending")
        void shouldReduceDaysPending() {
            // Arrange
            LeaveAllowance allowance = LeaveAllowanceMother.partiallyUsedAllowance(0, 5);

            // Act
            allowance.releasePendingDays(3);

            // Assert
            assertEquals(2, allowance.daysPending());
        }

        @Test
        @DisplayName("Should throw when releasing more than pending")
        void shouldThrowWhenReleasingMoreThanPending() {
            // Arrange
            LeaveAllowance allowance = LeaveAllowanceMother.partiallyUsedAllowance(0, 3);

            // Act & Assert
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> allowance.releasePendingDays(5));
            assertEquals(LeaveAllowance.CANNOT_RELEASE_MORE_THAN_PENDING, ex.getMessage());
        }

        @Test
        @DisplayName("Should reject zero days")
        void shouldRejectZeroDays() {
            // Arrange
            LeaveAllowance allowance = LeaveAllowanceMother.partiallyUsedAllowance(0, 5);

            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> allowance.releasePendingDays(0));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // COMMAND: creditBackDays()
    // ─────────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("creditBackDays() command")
    class CreditBackDays {

        @Test
        @DisplayName("Should reduce days used")
        void shouldReduceDaysUsed() {
            // Arrange
            LeaveAllowance allowance = LeaveAllowanceMother.partiallyUsedAllowance(10, 0);

            // Act
            allowance.creditBackDays(5);

            // Assert
            assertEquals(5, allowance.daysUsed());
        }

        @Test
        @DisplayName("Should throw when crediting back more than used")
        void shouldThrowWhenCreditingMoreThanUsed() {
            // Arrange
            LeaveAllowance allowance = LeaveAllowanceMother.partiallyUsedAllowance(3, 0);

            // Act & Assert
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> allowance.creditBackDays(5));
            assertEquals(LeaveAllowance.CANNOT_CREDIT_MORE_THAN_USED, ex.getMessage());
        }

        @Test
        @DisplayName("Should reject zero days")
        void shouldRejectZeroDays() {
            // Arrange
            LeaveAllowance allowance = LeaveAllowanceMother.partiallyUsedAllowance(10, 0);

            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> allowance.creditBackDays(0));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // COMMAND: amendEntitlement()
    // ─────────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("amendEntitlement() command")
    class AmendEntitlement {

        @Test
        @DisplayName("Should update total entitlement")
        void shouldUpdateEntitlement() {
            // Arrange
            LeaveAllowance allowance = LeaveAllowanceMother.freshAllowance();

            // Act
            allowance.amendEntitlement(30);

            // Assert
            assertEquals(30, allowance.totalEntitlement());
        }

        @Test
        @DisplayName("Should allow reducing to daysUsed exactly")
        void shouldAllowReducingToDaysUsed() {
            // Arrange
            LeaveAllowance allowance = LeaveAllowanceMother.partiallyUsedAllowance(10, 0);

            // Act
            allowance.amendEntitlement(10);

            // Assert
            assertEquals(10, allowance.totalEntitlement());
        }

        @Test
        @DisplayName("Should reject new entitlement below days used")
        void shouldRejectEntitlementBelowDaysUsed() {
            // Arrange
            LeaveAllowance allowance = LeaveAllowanceMother.partiallyUsedAllowance(10, 0);

            // Act & Assert
            IllegalStateException ex = assertThrows(IllegalStateException.class,
                    () -> allowance.amendEntitlement(9));
            assertEquals(LeaveAllowance.NEW_ENTITLEMENT_TOO_LOW, ex.getMessage());
        }

        @Test
        @DisplayName("Should reject zero entitlement")
        void shouldRejectZeroEntitlement() {
            // Arrange
            LeaveAllowance allowance = LeaveAllowanceMother.freshAllowance();

            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> allowance.amendEntitlement(0));
        }

        @Test
        @DisplayName("Should reject negative entitlement")
        void shouldRejectNegativeEntitlement() {
            // Arrange
            LeaveAllowance allowance = LeaveAllowanceMother.freshAllowance();

            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> allowance.amendEntitlement(-5));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // COMMAND: updateStaffDetails()
    // ─────────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateStaffDetails() command")
    class UpdateStaffDetails {

        @Test
        @DisplayName("Should update manager and department")
        void shouldUpdateManagerAndDepartment() {
            // Arrange
            LeaveAllowance allowance = LeaveAllowanceMother.freshAllowance();

            // Act
            allowance.updateStaffDetails("new-manager-id", "Finance");

            // Assert
            assertEquals("new-manager-id", allowance.managerId());
            assertEquals("Finance", allowance.department());
        }

        @Test
        @DisplayName("Should reject blank manager ID")
        void shouldRejectBlankManagerId() {
            // Arrange
            LeaveAllowance allowance = LeaveAllowanceMother.freshAllowance();

            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> allowance.updateStaffDetails("  ", "Finance"));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // DERIVED ACCESSORS
    // ─────────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Derived accessors")
    class DerivedAccessors {

        @Test
        @DisplayName("remainingDays() should return entitlement minus days used")
        void remainingDaysShouldExcludeUsed() {
            // Arrange
            LeaveAllowance allowance = LeaveAllowanceMother.partiallyUsedAllowance(10, 3);

            // Act & Assert
            assertEquals(15, allowance.remainingDays()); // 25 - 10
        }

        @Test
        @DisplayName("availableDays() should return entitlement minus used and pending")
        void availableDaysShouldExcludeUsedAndPending() {
            // Arrange
            LeaveAllowance allowance = LeaveAllowanceMother.partiallyUsedAllowance(10, 3);

            // Act & Assert
            assertEquals(12, allowance.availableDays()); // 25 - 10 - 3
        }

        @Test
        @DisplayName("Fresh allowance should have full entitlement available")
        void freshAllowanceShouldHaveFullEntitlement() {
            // Arrange
            LeaveAllowance allowance = LeaveAllowanceMother.freshAllowance();

            // Act & Assert
            assertEquals(25, allowance.remainingDays());
            assertEquals(25, allowance.availableDays());
        }
    }
}
