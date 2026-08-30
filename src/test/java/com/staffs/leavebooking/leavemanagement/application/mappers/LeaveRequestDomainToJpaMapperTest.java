package com.staffs.leavebooking.leavemanagement.application.mappers;

import com.staffs.leavebooking.common.domain.Identity;
import com.staffs.leavebooking.leavemanagement.domain.*;
import com.staffs.leavebooking.leavemanagement.infrastructure.entities.LeaveRequestJpa;
import com.staffs.leavebooking.testfixtures.LeaveRequestMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LeaveRequestDomainToJpaMapper.
 * Tests correct field mapping from domain aggregate to JPA entity.
 */
@DisplayName("LeaveRequestDomainToJpaMapper")
class LeaveRequestDomainToJpaMapperTest {

    @Nested
    @DisplayName("toJpa()")
    class ToJpa {

        @Test
        @DisplayName("Should map all fields correctly for a PENDING request")
        void shouldMapPendingRequestFields() {
            // Arrange
            Identity<LeaveRequest> id = Identity.generateId();
            DateRange range = new DateRange(LocalDate.of(2027, 6, 2), LocalDate.of(2027, 6, 6));
            LeaveRequest domain = LeaveRequest.reconstitute(
                    id, "staff-123", "mgr-456", LeaveType.ANNUAL, range, 5,
                    "Holiday", LeaveRequestStatus.PENDING, LocalDate.of(2027, 5, 20),
                    null, null, null, null);

            // Act
            LeaveRequestJpa jpa = LeaveRequestDomainToJpaMapper.toJpa(domain);

            // Assert
            assertEquals(id.id(), jpa.getId());
            assertEquals("staff-123", jpa.getStaffMemberId());
            assertEquals("mgr-456", jpa.getManagerId());
            assertEquals("ANNUAL", jpa.getLeaveType());
            assertEquals(LocalDate.of(2027, 6, 2), jpa.getStartDate());
            assertEquals(LocalDate.of(2027, 6, 6), jpa.getEndDate());
            assertEquals(5, jpa.getNumberOfDays());
            assertEquals("Holiday", jpa.getReason());
            assertEquals("PENDING", jpa.getStatus());
            assertEquals(LocalDate.of(2027, 5, 20), jpa.getSubmittedOn());
            assertNull(jpa.getDecidedOn());
            assertNull(jpa.getDecidedBy());
            assertNull(jpa.getCancellationReason());
        }

        @Test
        @DisplayName("Should map all fields for an APPROVED request")
        void shouldMapApprovedRequestFields() {
            // Arrange
            LeaveRequest domain = LeaveRequestMother.approvedRequest();

            // Act
            LeaveRequestJpa jpa = LeaveRequestDomainToJpaMapper.toJpa(domain);

            // Assert
            assertEquals("APPROVED", jpa.getStatus());
            assertNotNull(jpa.getDecidedOn());
            assertNotNull(jpa.getDecidedBy());
        }

        @Test
        @DisplayName("Should map cancellation reason for CANCELLED request")
        void shouldMapCancellationReason() {
            // Arrange
            LeaveRequest domain = LeaveRequestMother.cancelledRequest();

            // Act
            LeaveRequestJpa jpa = LeaveRequestDomainToJpaMapper.toJpa(domain);

            // Assert
            assertEquals("CANCELLED", jpa.getStatus());
            assertEquals("Changed plans", jpa.getCancellationReason());
        }

        @Test
        @DisplayName("Should throw NullPointerException for null domain")
        void shouldThrowForNullDomain() {
            // Act & Assert
            assertThrows(NullPointerException.class,
                    () -> LeaveRequestDomainToJpaMapper.toJpa(null));
        }
    }
}
