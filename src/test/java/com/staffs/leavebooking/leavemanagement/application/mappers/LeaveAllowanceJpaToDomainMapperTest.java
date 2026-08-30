package com.staffs.leavebooking.leavemanagement.application.mappers;

import com.staffs.leavebooking.leavemanagement.domain.BusinessYear;
import com.staffs.leavebooking.leavemanagement.domain.LeaveAllowance;
import com.staffs.leavebooking.leavemanagement.infrastructure.entities.LeaveAllowanceJpa;
import com.staffs.leavebooking.testfixtures.JpaEntityMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LeaveAllowanceJpaToDomainMapper.
 * Tests correct field mapping from JPA entity to domain aggregate.
 */
@DisplayName("LeaveAllowanceJpaToDomainMapper")
class LeaveAllowanceJpaToDomainMapperTest {

    @Nested
    @DisplayName("toDomain()")
    class ToDomain {

        @Test
        @DisplayName("Should map all fields correctly")
        void shouldMapAllFields() {
            // Arrange
            LeaveAllowanceJpa jpa = JpaEntityMother.leaveAllowanceJpa();

            // Act
            LeaveAllowance domain = LeaveAllowanceJpaToDomainMapper.toDomain(jpa);

            // Assert
            assertEquals(jpa.getId(), domain.id().id());
            assertEquals(jpa.getStaffMemberId(), domain.staffMemberId());
            assertEquals(jpa.getManagerId(), domain.managerId());
            assertEquals(jpa.getFirstName(), domain.firstName());
            assertEquals(jpa.getSurname(), domain.surname());
            assertEquals(jpa.getDepartment(), domain.department());
            assertEquals(new BusinessYear(2026, 2027), domain.businessYear());
            assertEquals(25, domain.totalEntitlement());
            assertEquals(5, domain.daysUsed());
            assertEquals(3, domain.daysPending());
        }

        @Test
        @DisplayName("Should construct BusinessYear from separate int fields")
        void shouldConstructBusinessYear() {
            // Arrange
            LeaveAllowanceJpa jpa = JpaEntityMother.leaveAllowanceJpa();
            jpa.setBusinessYearStart(2025);
            jpa.setBusinessYearEnd(2026);

            // Act
            LeaveAllowance domain = LeaveAllowanceJpaToDomainMapper.toDomain(jpa);

            // Assert
            assertEquals(2025, domain.businessYear().startYear());
            assertEquals(2026, domain.businessYear().endYear());
        }

        @Test
        @DisplayName("Should not raise events (uses reconstitute)")
        void shouldNotRaiseEvents() {
            // Arrange
            LeaveAllowanceJpa jpa = JpaEntityMother.leaveAllowanceJpa();

            // Act
            LeaveAllowance domain = LeaveAllowanceJpaToDomainMapper.toDomain(jpa);

            // Assert
            assertTrue(domain.listOfDomainEvents().isEmpty());
        }

        @Test
        @DisplayName("Should throw NullPointerException for null JPA entity")
        void shouldThrowForNullJpa() {
            // Act & Assert
            assertThrows(NullPointerException.class,
                    () -> LeaveAllowanceJpaToDomainMapper.toDomain(null));
        }
    }
}
