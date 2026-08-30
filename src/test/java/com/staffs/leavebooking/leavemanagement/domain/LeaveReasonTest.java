package com.staffs.leavebooking.leavemanagement.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the LeaveReason value object.
 * Tests null/blank rejection, max length enforcement, trimming, and equality.
 */
@DisplayName("LeaveReason Value Object")
class LeaveReasonTest {

    @Nested
    @DisplayName("Construction validation")
    class ConstructionValidation {

        @Test
        @DisplayName("Should reject null reason")
        void shouldRejectNullReason() {
            // Arrange & Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new LeaveReason(null));
            assertEquals(LeaveReason.REASON_NOT_EMPTY, ex.getMessage());
        }

        @Test
        @DisplayName("Should reject blank reason")
        void shouldRejectBlankReason() {
            // Arrange & Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new LeaveReason("   "));
            assertEquals(LeaveReason.REASON_NOT_EMPTY, ex.getMessage());
        }

        @Test
        @DisplayName("Should reject empty reason")
        void shouldRejectEmptyReason() {
            // Arrange & Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new LeaveReason(""));
            assertEquals(LeaveReason.REASON_NOT_EMPTY, ex.getMessage());
        }

        @Test
        @DisplayName("Should reject reason exceeding 500 characters")
        void shouldRejectReasonTooLong() {
            // Arrange
            String longReason = "A".repeat(501);

            // Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new LeaveReason(longReason));
            assertEquals(LeaveReason.REASON_TOO_LONG, ex.getMessage());
        }

        @Test
        @DisplayName("Should accept reason at exactly 500 characters")
        void shouldAcceptReasonAtMaxLength() {
            // Arrange
            String maxReason = "A".repeat(500);

            // Act
            LeaveReason reason = new LeaveReason(maxReason);

            // Assert
            assertEquals(maxReason, reason.reason());
        }
    }

    @Nested
    @DisplayName("Valid construction")
    class ValidConstruction {

        @Test
        @DisplayName("Should create reason with valid text")
        void shouldCreateValidReason() {
            // Arrange & Act
            LeaveReason reason = new LeaveReason("Family holiday");

            // Assert
            assertEquals("Family holiday", reason.reason());
        }

        @Test
        @DisplayName("Should trim whitespace from reason")
        void shouldTrimReason() {
            // Arrange & Act
            LeaveReason reason = new LeaveReason("  Holiday trip  ");

            // Assert
            assertEquals("Holiday trip", reason.reason());
        }

        @Test
        @DisplayName("Should accept single character reason")
        void shouldAcceptSingleChar() {
            // Arrange & Act
            LeaveReason reason = new LeaveReason("X");

            // Assert
            assertEquals("X", reason.reason());
        }
    }

    @Nested
    @DisplayName("Equality semantics")
    class EqualitySemantics {

        @Test
        @DisplayName("Two reasons with same text should be equal")
        void sameTextShouldBeEqual() {
            // Arrange & Act
            LeaveReason reason1 = new LeaveReason("Holiday");
            LeaveReason reason2 = new LeaveReason("Holiday");

            // Assert
            assertEquals(reason1, reason2);
            assertEquals(reason1.hashCode(), reason2.hashCode());
        }

        @Test
        @DisplayName("Two reasons with different text should not be equal")
        void differentTextShouldNotBeEqual() {
            // Arrange & Act
            LeaveReason reason1 = new LeaveReason("Holiday");
            LeaveReason reason2 = new LeaveReason("Sick leave");

            // Assert
            assertNotEquals(reason1, reason2);
        }
    }
}
