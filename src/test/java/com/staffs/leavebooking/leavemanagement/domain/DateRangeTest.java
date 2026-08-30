package com.staffs.leavebooking.leavemanagement.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the DateRange value object.
 * Tests null rejection, end-before-start, working days calculation, and future start validation.
 */
@DisplayName("DateRange Value Object")
class DateRangeTest {

    @Nested
    @DisplayName("Construction validation")
    class ConstructionValidation {

        @Test
        @DisplayName("Should reject null start date")
        void shouldRejectNullStartDate() {
            // Arrange & Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new DateRange(null, LocalDate.of(2027, 1, 10)));
            assertEquals(DateRange.START_DATE_NOT_NULL, ex.getMessage());
        }

        @Test
        @DisplayName("Should reject null end date")
        void shouldRejectNullEndDate() {
            // Arrange & Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new DateRange(LocalDate.of(2027, 1, 5), null));
            assertEquals(DateRange.END_DATE_NOT_NULL, ex.getMessage());
        }

        @Test
        @DisplayName("Should reject end date before start date")
        void shouldRejectEndBeforeStart() {
            // Arrange
            LocalDate start = LocalDate.of(2027, 3, 15);
            LocalDate end = LocalDate.of(2027, 3, 10);

            // Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new DateRange(start, end));
            assertEquals(DateRange.END_BEFORE_START, ex.getMessage());
        }

        @Test
        @DisplayName("Should accept same start and end date")
        void shouldAcceptSameStartAndEnd() {
            // Arrange
            LocalDate date = LocalDate.of(2027, 3, 17); // Monday

            // Act
            DateRange range = new DateRange(date, date);

            // Assert
            assertEquals(date, range.startDate());
            assertEquals(date, range.endDate());
        }

        @Test
        @DisplayName("Should accept valid date range")
        void shouldAcceptValidRange() {
            // Arrange
            LocalDate start = LocalDate.of(2027, 6, 1);
            LocalDate end = LocalDate.of(2027, 6, 5);

            // Act
            DateRange range = new DateRange(start, end);

            // Assert
            assertEquals(start, range.startDate());
            assertEquals(end, range.endDate());
        }
    }

    @Nested
    @DisplayName("validateFutureStart()")
    class ValidateFutureStart {

        @Test
        @DisplayName("Should throw if start date is today")
        void shouldThrowIfStartIsToday() {
            // Arrange
            DateRange range = new DateRange(LocalDate.now(), LocalDate.now().plusDays(5));

            // Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    range::validateFutureStart);
            assertEquals(DateRange.START_DATE_IN_PAST, ex.getMessage());
        }

        @Test
        @DisplayName("Should throw if start date is in the past")
        void shouldThrowIfStartInPast() {
            // Arrange
            DateRange range = new DateRange(LocalDate.now().minusDays(1), LocalDate.now().plusDays(5));

            // Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    range::validateFutureStart);
            assertEquals(DateRange.START_DATE_IN_PAST, ex.getMessage());
        }

        @Test
        @DisplayName("Should pass if start date is tomorrow")
        void shouldPassIfStartIsTomorrow() {
            // Arrange
            DateRange range = new DateRange(LocalDate.now().plusDays(1), LocalDate.now().plusDays(5));

            // Act & Assert
            assertDoesNotThrow(range::validateFutureStart);
        }
    }

    @Nested
    @DisplayName("workingDays() calculation")
    class WorkingDaysCalculation {

        @Test
        @DisplayName("Should count 5 working days for a full Monday-Friday week")
        void shouldCount5DaysForFullWeek() {
            // Arrange - Mon 2027-01-04 to Fri 2027-01-08
            DateRange range = new DateRange(
                    LocalDate.of(2027, 1, 4),   // Monday
                    LocalDate.of(2027, 1, 8));   // Friday

            // Act
            int days = range.workingDays();

            // Assert
            assertEquals(5, days);
        }

        @Test
        @DisplayName("Should exclude weekends from count")
        void shouldExcludeWeekends() {
            // Arrange - Mon 2027-01-04 to Sun 2027-01-10 (7 calendar days, 5 working)
            DateRange range = new DateRange(
                    LocalDate.of(2027, 1, 4),   // Monday
                    LocalDate.of(2027, 1, 10));  // Sunday

            // Act
            int days = range.workingDays();

            // Assert
            assertEquals(5, days);
        }

        @Test
        @DisplayName("Should return 0 for Saturday-Sunday only range")
        void shouldReturnZeroForWeekendOnly() {
            // Arrange - Sat 2027-01-09 to Sun 2027-01-10
            DateRange range = new DateRange(
                    LocalDate.of(2027, 1, 9),   // Saturday
                    LocalDate.of(2027, 1, 10));  // Sunday

            // Act
            int days = range.workingDays();

            // Assert
            assertEquals(0, days);
        }

        @Test
        @DisplayName("Should return 1 for a single weekday")
        void shouldReturn1ForSingleWeekday() {
            // Arrange - Wed 2027-01-06
            LocalDate wednesday = LocalDate.of(2027, 1, 6);
            DateRange range = new DateRange(wednesday, wednesday);

            // Act
            int days = range.workingDays();

            // Assert
            assertEquals(1, days);
        }

        @Test
        @DisplayName("Should return 0 for a single Saturday")
        void shouldReturn0ForSingleSaturday() {
            // Arrange - Sat 2027-01-09
            LocalDate saturday = LocalDate.of(2027, 1, 9);
            DateRange range = new DateRange(saturday, saturday);

            // Act
            int days = range.workingDays();

            // Assert
            assertEquals(0, days);
        }

        @Test
        @DisplayName("Should count 10 working days across two full weeks")
        void shouldCount10DaysForTwoWeeks() {
            // Arrange - Mon 2027-01-04 to Fri 2027-01-15
            DateRange range = new DateRange(
                    LocalDate.of(2027, 1, 4),   // Monday
                    LocalDate.of(2027, 1, 15));  // Friday

            // Act
            int days = range.workingDays();

            // Assert
            assertEquals(10, days);
        }
    }

    @Nested
    @DisplayName("Equality semantics")
    class EqualitySemantics {

        @Test
        @DisplayName("Two date ranges with same dates should be equal")
        void sameDatesShouldBeEqual() {
            // Arrange
            LocalDate start = LocalDate.of(2027, 3, 1);
            LocalDate end = LocalDate.of(2027, 3, 5);

            // Act
            DateRange range1 = new DateRange(start, end);
            DateRange range2 = new DateRange(start, end);

            // Assert
            assertEquals(range1, range2);
            assertEquals(range1.hashCode(), range2.hashCode());
        }
    }
}
