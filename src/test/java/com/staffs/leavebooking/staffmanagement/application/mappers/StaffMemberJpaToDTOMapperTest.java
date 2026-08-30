package com.staffs.leavebooking.staffmanagement.application.mappers;

import com.staffs.leavebooking.staffmanagement.application.dto.StaffMemberDTO;
import com.staffs.leavebooking.staffmanagement.infrastructure.entities.StaffMemberJpa;
import com.staffs.leavebooking.testfixtures.JpaEntityMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StaffMemberJpaToDTOMapper.
 * Tests correct field mapping from JPA entity to DTO.
 */
@DisplayName("StaffMemberJpaToDTOMapper")
class StaffMemberJpaToDTOMapperTest {

    @Nested
    @DisplayName("toDTO()")
    class ToDTO {

        @Test
        @DisplayName("Should map all fields correctly")
        void shouldMapAllFields() {
            // Arrange
            StaffMemberJpa jpa = JpaEntityMother.staffMemberJpa();

            // Act
            StaffMemberDTO dto = StaffMemberJpaToDTOMapper.toDTO(jpa);

            // Assert
            assertEquals(jpa.getId(), dto.id());
            assertEquals("Jane", dto.firstName());
            assertEquals("Doe", dto.surname());
            assertEquals("jane.doe@company.com", dto.email());
            assertEquals("Engineering", dto.department());
            assertEquals(jpa.getLineManagerId(), dto.lineManagerId());
            assertEquals(LocalDate.of(2023, 6, 1), dto.hireDate());
            assertEquals("Senior Developer", dto.currentRole());
            assertEquals(LocalDate.of(2024, 1, 1), dto.startDateOfCurrentRole());
            assertEquals("L5", dto.jobLevel());
            assertEquals("FULL_TIME", dto.employmentType());
            assertEquals("ACTIVE", dto.employmentStatus());
        }

        @Test
        @DisplayName("Should map nullable fields (lineManagerId, jobLevel)")
        void shouldMapNullableFields() {
            // Arrange
            StaffMemberJpa jpa = JpaEntityMother.staffMemberJpa();
            jpa.setLineManagerId(null);
            jpa.setJobLevel(null);

            // Act
            StaffMemberDTO dto = StaffMemberJpaToDTOMapper.toDTO(jpa);

            // Assert
            assertNull(dto.lineManagerId());
            assertNull(dto.jobLevel());
        }

        @Test
        @DisplayName("Should throw NullPointerException for null JPA entity")
        void shouldThrowForNullJpa() {
            // Act & Assert
            assertThrows(NullPointerException.class,
                    () -> StaffMemberJpaToDTOMapper.toDTO(null));
        }
    }
}
