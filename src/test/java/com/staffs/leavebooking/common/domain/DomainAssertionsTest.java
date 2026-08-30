package com.staffs.leavebooking.common.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DomainAssertions utility class.
 * Tests all static guard methods for correct exception behaviour.
 */
@DisplayName("DomainAssertions Utility")
class DomainAssertionsTest {

    private static final String ERROR_MSG = "Validation failed";

    @Nested
    @DisplayName("argumentNotEmpty(String)")
    class ArgumentNotEmptyString {

        @Test
        @DisplayName("Should throw for null string")
        void shouldThrowForNull() {
            // Arrange & Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> DomainAssertions.argumentNotEmpty((String) null, ERROR_MSG));
            assertEquals(ERROR_MSG, ex.getMessage());
        }

        @Test
        @DisplayName("Should throw for blank string")
        void shouldThrowForBlank() {
            // Arrange & Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> DomainAssertions.argumentNotEmpty("   ", ERROR_MSG));
            assertEquals(ERROR_MSG, ex.getMessage());
        }

        @Test
        @DisplayName("Should throw for empty string")
        void shouldThrowForEmpty() {
            // Arrange & Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> DomainAssertions.argumentNotEmpty("", ERROR_MSG));
        }

        @Test
        @DisplayName("Should return trimmed value for valid string")
        void shouldReturnTrimmedValue() {
            // Arrange & Act
            String result = DomainAssertions.argumentNotEmpty("  hello  ", ERROR_MSG);

            // Assert
            assertEquals("hello", result);
        }

        @Test
        @DisplayName("Should pass for non-blank string")
        void shouldPassForNonBlank() {
            // Arrange & Act
            String result = DomainAssertions.argumentNotEmpty("valid", ERROR_MSG);

            // Assert
            assertEquals("valid", result);
        }
    }

    @Nested
    @DisplayName("argumentNotNull")
    class ArgumentNotNull {

        @Test
        @DisplayName("Should throw for null object")
        void shouldThrowForNull() {
            // Arrange & Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> DomainAssertions.argumentNotNull(null, ERROR_MSG));
            assertEquals(ERROR_MSG, ex.getMessage());
        }

        @Test
        @DisplayName("Should pass for non-null object")
        void shouldPassForNonNull() {
            // Arrange & Act & Assert
            assertDoesNotThrow(() -> DomainAssertions.argumentNotNull("object", ERROR_MSG));
        }
    }

    @Nested
    @DisplayName("argumentLength")
    class ArgumentLength {

        @Test
        @DisplayName("Should throw when string is too short")
        void shouldThrowWhenTooShort() {
            // Arrange & Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> DomainAssertions.argumentLength("ab", 3, 10, ERROR_MSG));
            assertEquals(ERROR_MSG, ex.getMessage());
        }

        @Test
        @DisplayName("Should throw when string is too long")
        void shouldThrowWhenTooLong() {
            // Arrange & Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> DomainAssertions.argumentLength("abcdefghijk", 3, 10, ERROR_MSG));
            assertEquals(ERROR_MSG, ex.getMessage());
        }

        @Test
        @DisplayName("Should pass when string length is at minimum")
        void shouldPassAtMin() {
            // Arrange & Act & Assert
            assertDoesNotThrow(() -> DomainAssertions.argumentLength("abc", 3, 10, ERROR_MSG));
        }

        @Test
        @DisplayName("Should pass when string length is at maximum")
        void shouldPassAtMax() {
            // Arrange & Act & Assert
            assertDoesNotThrow(() -> DomainAssertions.argumentLength("abcdefghij", 3, 10, ERROR_MSG));
        }

        @Test
        @DisplayName("Should pass when string length is within range")
        void shouldPassWithinRange() {
            // Arrange & Act & Assert
            assertDoesNotThrow(() -> DomainAssertions.argumentLength("hello", 3, 10, ERROR_MSG));
        }
    }

    @Nested
    @DisplayName("argumentPositive")
    class ArgumentPositive {

        @Test
        @DisplayName("Should throw for zero")
        void shouldThrowForZero() {
            // Arrange & Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> DomainAssertions.argumentPositive(0, ERROR_MSG));
            assertEquals(ERROR_MSG, ex.getMessage());
        }

        @Test
        @DisplayName("Should throw for negative value")
        void shouldThrowForNegative() {
            // Arrange & Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> DomainAssertions.argumentPositive(-1, ERROR_MSG));
        }

        @Test
        @DisplayName("Should pass for positive value")
        void shouldPassForPositive() {
            // Arrange & Act & Assert
            assertDoesNotThrow(() -> DomainAssertions.argumentPositive(1, ERROR_MSG));
        }
    }

    @Nested
    @DisplayName("argumentNotNegative")
    class ArgumentNotNegative {

        @Test
        @DisplayName("Should throw for negative value")
        void shouldThrowForNegative() {
            // Arrange & Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> DomainAssertions.argumentNotNegative(-1, ERROR_MSG));
            assertEquals(ERROR_MSG, ex.getMessage());
        }

        @Test
        @DisplayName("Should pass for zero")
        void shouldPassForZero() {
            // Arrange & Act & Assert
            assertDoesNotThrow(() -> DomainAssertions.argumentNotNegative(0, ERROR_MSG));
        }

        @Test
        @DisplayName("Should pass for positive value")
        void shouldPassForPositive() {
            // Arrange & Act & Assert
            assertDoesNotThrow(() -> DomainAssertions.argumentNotNegative(5, ERROR_MSG));
        }
    }

    @Nested
    @DisplayName("argumentNotEmpty(BigDecimal)")
    class ArgumentNotEmptyBigDecimal {

        @Test
        @DisplayName("Should throw for null BigDecimal")
        void shouldThrowForNull() {
            // Arrange & Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> DomainAssertions.argumentNotEmpty((BigDecimal) null, ERROR_MSG));
        }

        @Test
        @DisplayName("Should pass for non-null BigDecimal")
        void shouldPassForNonNull() {
            // Arrange & Act & Assert
            assertDoesNotThrow(() -> DomainAssertions.argumentNotEmpty(BigDecimal.ONE, ERROR_MSG));
        }
    }

    @Nested
    @DisplayName("argumentMatchesPattern")
    class ArgumentMatchesPattern {

        @Test
        @DisplayName("Should throw for null string")
        void shouldThrowForNull() {
            // Arrange & Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> DomainAssertions.argumentMatchesPattern(null, ".*", ERROR_MSG));
        }

        @Test
        @DisplayName("Should throw when pattern does not match")
        void shouldThrowWhenNoMatch() {
            // Arrange & Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> DomainAssertions.argumentMatchesPattern("abc", "^\\d+$", ERROR_MSG));
            assertEquals(ERROR_MSG, ex.getMessage());
        }

        @Test
        @DisplayName("Should pass when pattern matches")
        void shouldPassWhenMatches() {
            // Arrange & Act & Assert
            assertDoesNotThrow(
                    () -> DomainAssertions.argumentMatchesPattern("123", "^\\d+$", ERROR_MSG));
        }
    }
}
