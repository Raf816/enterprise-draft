package com.staffs.leavebooking.common.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the FullName value object.
 * Tests null/blank rejection, length limits, trimming behaviour, and equality.
 */
@DisplayName("FullName Value Object")
class FullNameTest {

    @Nested
    @DisplayName("First name validation")
    class FirstNameValidation {

        @Test
        @DisplayName("Should reject null first name")
        void shouldRejectNullFirstName() {
            // Arrange & Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new FullName(null, "Smith"));
            assertEquals(FullName.FIRST_NAME_NOT_EMPTY, ex.getMessage());
        }

        @Test
        @DisplayName("Should reject blank first name")
        void shouldRejectBlankFirstName() {
            // Arrange & Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new FullName("   ", "Smith"));
            assertEquals(FullName.FIRST_NAME_NOT_EMPTY, ex.getMessage());
        }

        @Test
        @DisplayName("Should reject empty first name")
        void shouldRejectEmptyFirstName() {
            // Arrange & Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new FullName("", "Smith"));
            assertEquals(FullName.FIRST_NAME_NOT_EMPTY, ex.getMessage());
        }

        @Test
        @DisplayName("Should reject first name exceeding 50 characters")
        void shouldRejectFirstNameTooLong() {
            // Arrange
            String longName = "A".repeat(51);

            // Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new FullName(longName, "Smith"));
            assertEquals(FullName.FIRST_NAME_LENGTH, ex.getMessage());
        }

        @Test
        @DisplayName("Should accept first name at exactly 50 characters")
        void shouldAcceptFirstNameAtMaxLength() {
            // Arrange
            String maxName = "A".repeat(50);

            // Act
            FullName fullName = new FullName(maxName, "Smith");

            // Assert
            assertEquals(maxName, fullName.firstName());
        }

        @Test
        @DisplayName("Should trim whitespace from first name")
        void shouldTrimFirstName() {
            // Arrange & Act
            FullName fullName = new FullName("  John  ", "Smith");

            // Assert
            assertEquals("John", fullName.firstName());
        }
    }

    @Nested
    @DisplayName("Surname validation")
    class SurnameValidation {

        @Test
        @DisplayName("Should reject null surname")
        void shouldRejectNullSurname() {
            // Arrange & Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new FullName("John", null));
            assertEquals(FullName.SURNAME_NOT_EMPTY, ex.getMessage());
        }

        @Test
        @DisplayName("Should reject blank surname")
        void shouldRejectBlankSurname() {
            // Arrange & Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new FullName("John", "   "));
            assertEquals(FullName.SURNAME_NOT_EMPTY, ex.getMessage());
        }

        @Test
        @DisplayName("Should reject surname exceeding 50 characters")
        void shouldRejectSurnameTooLong() {
            // Arrange
            String longSurname = "B".repeat(51);

            // Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new FullName("John", longSurname));
            assertEquals(FullName.SURNAME_LENGTH, ex.getMessage());
        }

        @Test
        @DisplayName("Should accept surname at exactly 50 characters")
        void shouldAcceptSurnameAtMaxLength() {
            // Arrange
            String maxSurname = "B".repeat(50);

            // Act
            FullName fullName = new FullName("John", maxSurname);

            // Assert
            assertEquals(maxSurname, fullName.surname());
        }

        @Test
        @DisplayName("Should trim whitespace from surname")
        void shouldTrimSurname() {
            // Arrange & Act
            FullName fullName = new FullName("John", "  Smith  ");

            // Assert
            assertEquals("Smith", fullName.surname());
        }
    }

    @Nested
    @DisplayName("Valid construction")
    class ValidConstruction {

        @Test
        @DisplayName("Should create FullName with valid first name and surname")
        void shouldCreateValidFullName() {
            // Arrange & Act
            FullName fullName = new FullName("John", "Smith");

            // Assert
            assertEquals("John", fullName.firstName());
            assertEquals("Smith", fullName.surname());
        }

        @Test
        @DisplayName("Should accept single character names")
        void shouldAcceptSingleCharNames() {
            // Arrange & Act
            FullName fullName = new FullName("J", "S");

            // Assert
            assertEquals("J", fullName.firstName());
            assertEquals("S", fullName.surname());
        }
    }

    @Nested
    @DisplayName("Equality semantics")
    class EqualitySemantics {

        @Test
        @DisplayName("Two FullNames with same values should be equal")
        void sameValuesShouldBeEqual() {
            // Arrange & Act
            FullName name1 = new FullName("John", "Smith");
            FullName name2 = new FullName("John", "Smith");

            // Assert
            assertEquals(name1, name2);
            assertEquals(name1.hashCode(), name2.hashCode());
        }

        @Test
        @DisplayName("Two FullNames with different values should not be equal")
        void differentValuesShouldNotBeEqual() {
            // Arrange & Act
            FullName name1 = new FullName("John", "Smith");
            FullName name2 = new FullName("Jane", "Doe");

            // Assert
            assertNotEquals(name1, name2);
        }
    }

    @Nested
    @DisplayName("Character validation")
    class CharacterValidation {

        @Test
        @DisplayName("Should reject first name containing numbers")
        void shouldRejectFirstNameWithNumbers() {
            assertThrows(IllegalArgumentException.class,
                    () -> new FullName("John123", "Smith"));
        }

        @Test
        @DisplayName("Should reject first name containing special characters")
        void shouldRejectFirstNameWithSpecialChars() {
            assertThrows(IllegalArgumentException.class,
                    () -> new FullName("John@#$", "Smith"));
        }

        @Test
        @DisplayName("Should reject surname containing numbers")
        void shouldRejectSurnameWithNumbers() {
            assertThrows(IllegalArgumentException.class,
                    () -> new FullName("John", "Smith123"));
        }

        @Test
        @DisplayName("Should reject surname containing special characters")
        void shouldRejectSurnameWithSpecialChars() {
            assertThrows(IllegalArgumentException.class,
                    () -> new FullName("John", "Smith!@#"));
        }

        @Test
        @DisplayName("Should accept hyphenated names (e.g. Smith-Jones)")
        void shouldAcceptHyphenatedNames() {
            FullName fullName = new FullName("Mary-Anne", "Smith-Jones");
            assertEquals("Mary-Anne", fullName.firstName());
            assertEquals("Smith-Jones", fullName.surname());
        }

        @Test
        @DisplayName("Should accept apostrophes (e.g. O'Brien)")
        void shouldAcceptApostrophes() {
            FullName fullName = new FullName("Siobhan", "O'Brien");
            assertEquals("O'Brien", fullName.surname());
        }

        @Test
        @DisplayName("Should accept spaces in names (e.g. Mary Anne)")
        void shouldAcceptSpacesInNames() {
            FullName fullName = new FullName("Mary Anne", "De La Cruz");
            assertEquals("Mary Anne", fullName.firstName());
            assertEquals("De La Cruz", fullName.surname());
        }
    }
}