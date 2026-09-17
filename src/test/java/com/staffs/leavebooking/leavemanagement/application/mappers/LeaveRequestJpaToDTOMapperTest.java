package com.staffs.leavebooking.leavemanagement.application.mappers;

import com.staffs.leavebooking.leavemanagement.application.dto.LeaveRequestDTO;
import com.staffs.leavebooking.leavemanagement.infrastructure.entities.LeaveRequestJpa;
import com.staffs.leavebooking.testfixtures.JpaEntityMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LeaveRequestJpaToDTOMapper.
 * Tests correct field mapping from JPA entity to DTO.
 */
@DisplayName("LeaveRequestJpaToDTOMapper")
class LeaveRequestJpaToDTOMapperTest {

    @Nested
    @DisplayName("toDTO()")
    class ToDTO {

        @Test
        @DisplayName("Should map all fields correctly")
        void shouldMapAllFields() {
            // Arrange
            LeaveRequestJpa jpa = JpaEntityMother.leaveRequestJpa();

            // Act
            LeaveRequestDTO dto = LeaveRequestJpaToDTOMapper.toDTO(jpa);

            // Assert
            assertEquals(jpa.getId(), dto.id());
            assertEquals(jpa.getStaffMemberId(), dto.staffMemberId());
            assertEquals(jpa.getManagerId(), dto.managerId());
            assertEquals("ANNUAL", dto.leaveType());
            assertEquals(jpa.getStartDate(), dto.startDate());
            assertEquals(jpa.getEndDate(), dto.endDate());
            assertEquals(jpa.getNumberOfDays(), dto.numberOfDays());
            assertEquals(jpa.getReason(), dto.reason());
            assertEquals("PENDING", dto.status());
            assertEquals(jpa.getSubmittedOn(), dto.submittedOn());
            assertNull(dto.decidedOn());
            assertNull(dto.decidedBy());
            assertNull(dto.cancellationReason());
        }

        @Test
        @DisplayName("Should throw NullPointerException for null JPA entity")
        void shouldThrowForNullJpa() {
            // Act & Assert
            assertThrows(NullPointerException.class,
                    () -> LeaveRequestJpaToDTOMapper.toDTO(null));
        }
    }
}
