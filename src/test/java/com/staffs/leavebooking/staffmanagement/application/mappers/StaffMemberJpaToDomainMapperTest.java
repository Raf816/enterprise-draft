package com.staffs.leavebooking.staffmanagement.application.mappers;

import com.staffs.leavebooking.staffmanagement.domain.EmploymentStatus;
import com.staffs.leavebooking.staffmanagement.domain.EmploymentType;
import com.staffs.leavebooking.staffmanagement.domain.StaffMember;
import com.staffs.leavebooking.staffmanagement.infrastructure.entities.StaffMemberJpa;
import com.staffs.leavebooking.testfixtures.JpaEntityMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StaffMemberJpaToDomainMapper.
 * Tests correct field mapping from JPA entity to domain aggregate.
 */
@DisplayName("StaffMemberJpaToDomainMapper")
class StaffMemberJpaToDomainMapperTest {

    @Nested
    @DisplayName("toDomain()")
    class ToDomain {

        @Test
        @DisplayName("Should map all fields correctly")
        void shouldMapAllFields() {
            // Arrange
            StaffMemberJpa jpa = JpaEntityMother.staffMemberJpa();

            // Act
            StaffMember domain = StaffMemberJpaToDomainMapper.toDomain(jpa);

            // Assert
            assertEquals(jpa.getId(), domain.id().id());
            assertEquals(jpa.getFirstName(), domain.fullName().firstName());
            assertEquals(jpa.getSurname(), domain.fullName().surname());
            assertEquals(jpa.getEmail(), domain.email().address());
            assertEquals(jpa.getDepartment(), domain.department());
            assertEquals(jpa.getLineManagerId(), domain.lineManagerId());
            assertEquals(jpa.getHireDate(), domain.hireDate());
            assertEquals(jpa.getCurrentRole(), domain.currentRole());
            assertEquals(jpa.getStartDateCurrentRole(), domain.startDateOfCurrentRole());
            assertEquals(jpa.getJobLevel(), domain.jobLevel());
            assertEquals(EmploymentType.FULL_TIME, domain.employmentType());
            assertEquals(EmploymentStatus.ACTIVE, domain.employmentStatus());
        }

        @Test
        @DisplayName("Should construct FullName value object from separate fields")
        void shouldConstructFullName() {
            // Arrange
            StaffMemberJpa jpa = JpaEntityMother.staffMemberJpa();
            jpa.setFirstName("Alice");
            jpa.setSurname("Johnson");

            // Act
            StaffMember domain = StaffMemberJpaToDomainMapper.toDomain(jpa);

            // Assert
            assertEquals("Alice", domain.fullName().firstName());
            assertEquals("Johnson", domain.fullName().surname());
        }

        @Test
        @DisplayName("Should construct Email value object from string")
        void shouldConstructEmail() {
            // Arrange
            StaffMemberJpa jpa = JpaEntityMother.staffMemberJpa();
            jpa.setEmail("alice@corp.com");

            // Act
            StaffMember domain = StaffMemberJpaToDomainMapper.toDomain(jpa);

            // Assert
            assertEquals("alice@corp.com", domain.email().address());
        }

        @Test
        @DisplayName("Should convert string to EmploymentType enum")
        void shouldConvertEmploymentType() {
            // Arrange
            StaffMemberJpa jpa = JpaEntityMother.staffMemberJpa();
            jpa.setEmploymentType("CONTRACT");

            // Act
            StaffMember domain = StaffMemberJpaToDomainMapper.toDomain(jpa);

            // Assert
            assertEquals(EmploymentType.CONTRACT, domain.employmentType());
        }

        @Test
        @DisplayName("Should convert string to EmploymentStatus enum")
        void shouldConvertEmploymentStatus() {
            // Arrange
            StaffMemberJpa jpa = JpaEntityMother.staffMemberJpa();
            jpa.setEmploymentStatus("ON_LEAVE");

            // Act
            StaffMember domain = StaffMemberJpaToDomainMapper.toDomain(jpa);

            // Assert
            assertEquals(EmploymentStatus.ON_LEAVE, domain.employmentStatus());
        }

        @Test
        @DisplayName("Should not raise events (uses reconstitute)")
        void shouldNotRaiseEvents() {
            // Arrange
            StaffMemberJpa jpa = JpaEntityMother.staffMemberJpa();

            // Act
            StaffMember domain = StaffMemberJpaToDomainMapper.toDomain(jpa);

            // Assert
            assertTrue(domain.listOfDomainEvents().isEmpty());
        }

        @Test
        @DisplayName("Should throw NullPointerException for null JPA entity")
        void shouldThrowForNullJpa() {
            // Act & Assert
            assertThrows(NullPointerException.class,
                    () -> StaffMemberJpaToDomainMapper.toDomain(null));
        }
    }
}
