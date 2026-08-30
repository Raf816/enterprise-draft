package com.staffs.leavebooking.staffmanagement.application.mappers;

import com.staffs.leavebooking.staffmanagement.domain.EmploymentStatus;
import com.staffs.leavebooking.staffmanagement.domain.EmploymentType;
import com.staffs.leavebooking.staffmanagement.domain.StaffMember;
import com.staffs.leavebooking.staffmanagement.infrastructure.entities.StaffMemberJpa;
import com.staffs.leavebooking.testfixtures.StaffMemberMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StaffMemberDomainToJpaMapper.
 * Tests correct field mapping from domain aggregate to JPA entity.
 */
@DisplayName("StaffMemberDomainToJpaMapper")
class StaffMemberDomainToJpaMapperTest {

    @Nested
    @DisplayName("toJpa()")
    class ToJpa {

        @Test
        @DisplayName("Should map all fields correctly")
        void shouldMapAllFields() {
            // Arrange
            StaffMember domain = StaffMemberMother.activeStaffMember();

            // Act
            StaffMemberJpa jpa = StaffMemberDomainToJpaMapper.toJpa(domain);

            // Assert
            assertEquals(domain.id().id(), jpa.getId());
            assertEquals(domain.fullName().firstName(), jpa.getFirstName());
            assertEquals(domain.fullName().surname(), jpa.getSurname());
            assertEquals(domain.email().address(), jpa.getEmail());
            assertEquals(domain.department(), jpa.getDepartment());
            assertEquals(domain.lineManagerId(), jpa.getLineManagerId());
            assertEquals(domain.hireDate(), jpa.getHireDate());
            assertEquals(domain.currentRole(), jpa.getCurrentRole());
            assertEquals(domain.startDateOfCurrentRole(), jpa.getStartDateCurrentRole());
            assertEquals(domain.jobLevel(), jpa.getJobLevel());
            assertEquals("FULL_TIME", jpa.getEmploymentType());
            assertEquals("ACTIVE", jpa.getEmploymentStatus());
        }

        @Test
        @DisplayName("Should map employment type enum name to string")
        void shouldMapEmploymentTypeAsEnumName() {
            // Arrange
            StaffMember domain = StaffMemberMother.activeStaffMember();

            // Act
            StaffMemberJpa jpa = StaffMemberDomainToJpaMapper.toJpa(domain);

            // Assert
            assertEquals(EmploymentType.FULL_TIME.name(), jpa.getEmploymentType());
        }

        @Test
        @DisplayName("Should map employment status enum name to string")
        void shouldMapEmploymentStatusAsEnumName() {
            // Arrange
            StaffMember domain = StaffMemberMother.terminatedStaffMember();

            // Act
            StaffMemberJpa jpa = StaffMemberDomainToJpaMapper.toJpa(domain);

            // Assert
            assertEquals(EmploymentStatus.TERMINATED.name(), jpa.getEmploymentStatus());
        }

        @Test
        @DisplayName("Should throw NullPointerException for null domain")
        void shouldThrowForNullDomain() {
            // Act & Assert
            assertThrows(NullPointerException.class,
                    () -> StaffMemberDomainToJpaMapper.toJpa(null));
        }
    }
}
