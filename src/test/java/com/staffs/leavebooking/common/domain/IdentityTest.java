package com.staffs.leavebooking.common.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Identity value object.
 * Tests UUID validation, factory methods, and equality semantics.
 */
@DisplayName("Identity Value Object")
class IdentityTest {

    @Nested
    @DisplayName("Construction validation")
    class ConstructionValidation {

        @Test
        @DisplayName("Should reject null id")
        void shouldRejectNullId() {
            // Arrange & Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new Identity<>(null));
            assertEquals(Identity.IDENTITY_CANNOT_BE_NULL, ex.getMessage());
        }

        @Test
        @DisplayName("Should reject blank id")
        void shouldRejectBlankId() {
            // Arrange & Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new Identity<>("   "));
            assertEquals(Identity.IDENTITY_CANNOT_BE_NULL, ex.getMessage());
        }

        @Test
        @DisplayName("Should reject empty string id")
        void shouldRejectEmptyStringId() {
            // Arrange & Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new Identity<>(""));
            assertEquals(Identity.IDENTITY_CANNOT_BE_NULL, ex.getMessage());
        }

        @Test
        @DisplayName("Should reject non-UUID format string")
        void shouldRejectNonUuidFormat() {
            // Arrange & Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new Identity<>("not-a-uuid"));
            assertEquals(Identity.IDENTITY_MUST_BE_UUID, ex.getMessage());
        }

        @Test
        @DisplayName("Should reject partial UUID format")
        void shouldRejectPartialUuid() {
            // Arrange & Act & Assert
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new Identity<>("550e8400-e29b-41d4"));
            assertEquals(Identity.IDENTITY_MUST_BE_UUID, ex.getMessage());
        }

        @Test
        @DisplayName("Should accept valid UUID format")
        void shouldAcceptValidUuid() {
            // Arrange
            String validUuid = "550e8400-e29b-41d4-a716-446655440000";

            // Act
            Identity<?> identity = new Identity<>(validUuid);

            // Assert
            assertEquals(validUuid, identity.id());
        }
    }

    @Nested
    @DisplayName("Factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("of() should create identity from valid UUID string")
        void ofShouldCreateFromValidString() {
            // Arrange
            String uuid = UUID.randomUUID().toString();

            // Act
            Identity<?> identity = Identity.of(uuid);

            // Assert
            assertEquals(uuid, identity.id());
        }

        @Test
        @DisplayName("generateId() should produce valid UUID identity")
        void generateIdShouldProduceValidUuid() {
            // Act
            Identity<?> identity = Identity.generateId();

            // Assert
            assertNotNull(identity);
            assertNotNull(identity.id());
            assertDoesNotThrow(() -> UUID.fromString(identity.id()));
        }

        @Test
        @DisplayName("generateId() should produce unique identities")
        void generateIdShouldProduceUniqueIds() {
            // Act
            Identity<?> id1 = Identity.generateId();
            Identity<?> id2 = Identity.generateId();

            // Assert
            assertNotEquals(id1, id2);
        }
    }

    @Nested
    @DisplayName("Equality semantics")
    class EqualitySemantics {

        @Test
        @DisplayName("Two identities with same UUID should be equal")
        void sameUuidShouldBeEqual() {
            // Arrange
            String uuid = UUID.randomUUID().toString();

            // Act
            Identity<?> id1 = Identity.of(uuid);
            Identity<?> id2 = Identity.of(uuid);

            // Assert
            assertEquals(id1, id2);
            assertEquals(id1.hashCode(), id2.hashCode());
        }

        @Test
        @DisplayName("Two identities with different UUIDs should not be equal")
        void differentUuidsShouldNotBeEqual() {
            // Arrange & Act
            Identity<?> id1 = Identity.generateId();
            Identity<?> id2 = Identity.generateId();

            // Assert
            assertNotEquals(id1, id2);
        }
    }
}
