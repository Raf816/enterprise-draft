package com.staffs.leavebooking.leavemanagement.application.mappers;

import com.staffs.leavebooking.leavemanagement.domain.LeaveRequest;
import com.staffs.leavebooking.leavemanagement.domain.LeaveRequestStatus;
import com.staffs.leavebooking.leavemanagement.domain.LeaveType;
import com.staffs.leavebooking.leavemanagement.infrastructure.entities.LeaveRequestJpa;
import com.staffs.leavebooking.testfixtures.JpaEntityMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LeaveRequestJpaToDomainMapper.
 * Tests correct field mapping from JPA entity to domain aggregate.
 */
@DisplayName("LeaveRequestJpaToDomainMapper")
class LeaveRequestJpaToDomainMapperTest {

    @Nested
    @DisplayName("toDomain()")
    class ToDomain {

        @Test
        @DisplayName("Should map all fields correctly")
        void shouldMapAllFields() {
            // Arrange
            LeaveRequestJpa jpa = JpaEntityMother.leaveRequestJpa();

            // Act
            LeaveRequest domain = LeaveRequestJpaToDomainMapper.toDomain(jpa);

            // Assert
            assertEquals(jpa.getId(), domain.id().id());
            assertEquals(jpa.getStaffMemberId(), domain.staffMemberId());
            assertEquals(jpa.getManagerId(), domain.managerId());
            assertEquals(LeaveType.ANNUAL, domain.leaveType());
            assertEquals(jpa.getStartDate(), domain.dateRange().startDate());
            assertEquals(jpa.getEndDate(), domain.dateRange().endDate());
            assertEquals(jpa.getNumberOfDays(), domain.numberOfDays());
            assertEquals(jpa.getReason(), domain.reason());
            assertEquals(LeaveRequestStatus.PENDING, domain.status());
            assertEquals(jpa.getSubmittedOn(), domain.submittedOn());
        }

        @Test
        @DisplayName("Should map APPROVED status with decided fields")
        void shouldMapApprovedStatus() {
            // Arrange
            LeaveRequestJpa jpa = JpaEntityMother.leaveRequestJpa();
            jpa.setStatus("APPROVED");
            jpa.setDecidedOn(LocalDate.of(2027, 3, 5));
            jpa.setDecidedBy("approver-id");

            // Act
            LeaveRequest domain = LeaveRequestJpaToDomainMapper.toDomain(jpa);

            // Assert
            assertEquals(LeaveRequestStatus.APPROVED, domain.status());
            assertEquals(LocalDate.of(2027, 3, 5), domain.decidedOn());
            assertEquals("approver-id", domain.decidedBy());
        }

        @Test
        @DisplayName("Should not raise events (uses reconstitute)")
        void shouldNotRaiseEvents() {
            // Arrange
            LeaveRequestJpa jpa = JpaEntityMother.leaveRequestJpa();

            // Act
            LeaveRequest domain = LeaveRequestJpaToDomainMapper.toDomain(jpa);

            // Assert
            assertTrue(domain.listOfDomainEvents().isEmpty());
        }

        @Test
        @DisplayName("Should throw NullPointerException for null JPA entity")
        void shouldThrowForNullJpa() {
            // Act & Assert
            assertThrows(NullPointerException.class,
                    () -> LeaveRequestJpaToDomainMapper.toDomain(null));
        }
    }
}
