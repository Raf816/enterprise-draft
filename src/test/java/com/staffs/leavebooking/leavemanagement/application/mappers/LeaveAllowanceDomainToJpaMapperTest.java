package com.staffs.leavebooking.leavemanagement.application.mappers;

import com.staffs.leavebooking.leavemanagement.domain.LeaveAllowance;
import com.staffs.leavebooking.leavemanagement.infrastructure.entities.LeaveAllowanceJpa;
import com.staffs.leavebooking.testfixtures.JpaEntityMother;
import com.staffs.leavebooking.testfixtures.LeaveAllowanceMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LeaveAllowanceDomainToJpaMapper.
 * Tests both toJpa() and updateJpa() methods.
 */
@DisplayName("LeaveAllowanceDomainToJpaMapper")
class LeaveAllowanceDomainToJpaMapperTest {

    @Nested
    @DisplayName("toJpa()")
    class ToJpa {

        @Test
        @DisplayName("Should map all fields correctly")
        void shouldMapAllFields() {
            // Arrange
            LeaveAllowance domain = LeaveAllowanceMother.partiallyUsedAllowance(10, 3);

            // Act
            LeaveAllowanceJpa jpa = LeaveAllowanceDomainToJpaMapper.toJpa(domain);

            // Assert
            assertEquals(domain.id().id(), jpa.getId());
            assertEquals(domain.staffMemberId(), jpa.getStaffMemberId());
            assertEquals(domain.managerId(), jpa.getManagerId());
            assertEquals(domain.firstName(), jpa.getFirstName());
            assertEquals(domain.surname(), jpa.getSurname());
            assertEquals(domain.department(), jpa.getDepartment());
            assertEquals(domain.businessYear().startYear(), jpa.getBusinessYearStart());
            assertEquals(domain.businessYear().endYear(), jpa.getBusinessYearEnd());
            assertEquals(25, jpa.getTotalEntitlement());
            assertEquals(10, jpa.getDaysUsed());
            assertEquals(3, jpa.getDaysPending());
        }

        @Test
        @DisplayName("Should throw NullPointerException for null domain")
        void shouldThrowForNullDomain() {
            // Act & Assert
            assertThrows(NullPointerException.class,
                    () -> LeaveAllowanceDomainToJpaMapper.toJpa(null));
        }
    }

    @Nested
    @DisplayName("updateJpa()")
    class UpdateJpa {

        @Test
        @DisplayName("Should update mutable fields on existing JPA entity")
        void shouldUpdateMutableFields() {
            // Arrange
            LeaveAllowance domain = LeaveAllowanceMother.partiallyUsedAllowance(8, 2);
            LeaveAllowanceJpa jpa = JpaEntityMother.leaveAllowanceJpa();
            String originalId = jpa.getId();
            String originalStaffId = jpa.getStaffMemberId();

            // Act
            LeaveAllowanceDomainToJpaMapper.updateJpa(domain, jpa);

            // Assert — id and staffMemberId should NOT be changed
            assertEquals(originalId, jpa.getId());
            assertEquals(originalStaffId, jpa.getStaffMemberId());
            // Mutable fields should be updated
            assertEquals(domain.managerId(), jpa.getManagerId());
            assertEquals(domain.firstName(), jpa.getFirstName());
            assertEquals(domain.surname(), jpa.getSurname());
            assertEquals(domain.department(), jpa.getDepartment());
            assertEquals(25, jpa.getTotalEntitlement());
            assertEquals(8, jpa.getDaysUsed());
            assertEquals(2, jpa.getDaysPending());
        }

        @Test
        @DisplayName("Should throw NullPointerException for null domain")
        void shouldThrowForNullDomain() {
            // Arrange
            LeaveAllowanceJpa jpa = JpaEntityMother.leaveAllowanceJpa();

            // Act & Assert
            assertThrows(NullPointerException.class,
                    () -> LeaveAllowanceDomainToJpaMapper.updateJpa(null, jpa));
        }

        @Test
        @DisplayName("Should throw NullPointerException for null JPA entity")
        void shouldThrowForNullJpa() {
            // Arrange
            LeaveAllowance domain = LeaveAllowanceMother.freshAllowance();

            // Act & Assert
            assertThrows(NullPointerException.class,
                    () -> LeaveAllowanceDomainToJpaMapper.updateJpa(domain, null));
        }
    }
}
