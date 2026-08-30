package com.staffs.leavebooking.leavemanagement.application.mappers;

import com.staffs.leavebooking.leavemanagement.application.dto.LeaveAllowanceDTO;
import com.staffs.leavebooking.leavemanagement.infrastructure.entities.LeaveAllowanceJpa;
import com.staffs.leavebooking.testfixtures.JpaEntityMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LeaveAllowanceJpaToDTOMapper.
 * Tests field mapping and derived field calculations.
 */
@DisplayName("LeaveAllowanceJpaToDTOMapper")
class LeaveAllowanceJpaToDTOMapperTest {

    @Nested
    @DisplayName("toDTO()")
    class ToDTO {

        @Test
        @DisplayName("Should map all basic fields correctly")
        void shouldMapBasicFields() {
            // Arrange
            LeaveAllowanceJpa jpa = JpaEntityMother.leaveAllowanceJpa();

            // Act
            LeaveAllowanceDTO dto = LeaveAllowanceJpaToDTOMapper.toDTO(jpa);

            // Assert
            assertEquals(jpa.getId(), dto.id());
            assertEquals(jpa.getStaffMemberId(), dto.staffMemberId());
            assertEquals(jpa.getManagerId(), dto.managerId());
            assertEquals(jpa.getDepartment(), dto.department());
            assertEquals(25, dto.totalEntitlement());
            assertEquals(5, dto.daysUsed());
            assertEquals(3, dto.daysPending());
        }

        @Test
        @DisplayName("Should calculate remainingDays as entitlement minus daysUsed")
        void shouldCalculateRemainingDays() {
            // Arrange — entitlement=25, used=5
            LeaveAllowanceJpa jpa = JpaEntityMother.leaveAllowanceJpa();

            // Act
            LeaveAllowanceDTO dto = LeaveAllowanceJpaToDTOMapper.toDTO(jpa);

            // Assert
            assertEquals(20, dto.remainingDays()); // 25 - 5
        }

        @Test
        @DisplayName("Should calculate availableDays as entitlement minus used minus pending")
        void shouldCalculateAvailableDays() {
            // Arrange — entitlement=25, used=5, pending=3
            LeaveAllowanceJpa jpa = JpaEntityMother.leaveAllowanceJpa();

            // Act
            LeaveAllowanceDTO dto = LeaveAllowanceJpaToDTOMapper.toDTO(jpa);

            // Assert
            assertEquals(17, dto.availableDays()); // 25 - 5 - 3
        }

        @Test
        @DisplayName("Should format staffName as 'firstName surname'")
        void shouldFormatStaffName() {
            // Arrange
            LeaveAllowanceJpa jpa = JpaEntityMother.leaveAllowanceJpa();
            jpa.setFirstName("Alice");
            jpa.setSurname("Johnson");

            // Act
            LeaveAllowanceDTO dto = LeaveAllowanceJpaToDTOMapper.toDTO(jpa);

            // Assert
            assertEquals("Alice Johnson", dto.staffName());
        }

        @Test
        @DisplayName("Should format businessYear as 'start-end' string")
        void shouldFormatBusinessYear() {
            // Arrange
            LeaveAllowanceJpa jpa = JpaEntityMother.leaveAllowanceJpa();
            jpa.setBusinessYearStart(2026);
            jpa.setBusinessYearEnd(2027);

            // Act
            LeaveAllowanceDTO dto = LeaveAllowanceJpaToDTOMapper.toDTO(jpa);

            // Assert
            assertEquals("2026-2027", dto.businessYear());
        }

        @Test
        @DisplayName("Should throw NullPointerException for null JPA entity")
        void shouldThrowForNullJpa() {
            // Act & Assert
            assertThrows(NullPointerException.class,
                    () -> LeaveAllowanceJpaToDTOMapper.toDTO(null));
        }
    }
}
