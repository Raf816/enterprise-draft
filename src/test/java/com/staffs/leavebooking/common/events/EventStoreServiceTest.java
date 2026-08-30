package com.staffs.leavebooking.common.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.staffs.leavebooking.leavemanagement.domain.events.LeaveRequestSubmittedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EventStoreService — event persistence and status management.
 * Mocks EventStoreRepository and ObjectMapper to test in isolation.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Event Store Service")
class EventStoreServiceTest {

    @Mock
    private EventStoreRepository eventsStore;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EventStoreService eventStoreService;

    @Nested
    @DisplayName("append")
    class Append {

        @Test
        @DisplayName("Should persist a LocalEvent with status LOCAL")
        void shouldPersistLocalEventWithLocalStatus() throws Exception {
            // Arrange
            var localEvent = new LeaveRequestSubmittedEvent(
                    LocalDate.now(), "req-1", "staff-1", 5);
            when(objectMapper.writeValueAsString(localEvent)).thenReturn("{\"test\":true}");
            when(eventsStore.save(any(EventStoreJpa.class))).thenAnswer(inv -> {
                EventStoreJpa jpa = inv.getArgument(0);
                jpa.setId(1L);
                return jpa;
            });

            // Act
            EventStoreJpa result = eventStoreService.append(localEvent, "TestContext");

            // Assert
            ArgumentCaptor<EventStoreJpa> captor = ArgumentCaptor.forClass(EventStoreJpa.class);
            verify(eventsStore).save(captor.capture());
            EventStoreJpa saved = captor.getValue();
            assertEquals("LOCAL", saved.getStatus());
            assertEquals("LeaveRequestSubmittedEvent", saved.getEventType());
            assertEquals("{\"test\":true}", saved.getEventBody());
            assertEquals("TestContext", saved.getSourceContext());
            assertEquals(0, saved.getRetryCount());
        }

        @Test
        @DisplayName("Should persist a RemoteEvent with status PENDING")
        void shouldPersistRemoteEventWithPendingStatus() throws Exception {
            // Arrange
            var remoteEvent = new StaffMemberAddedEvent(
                    LocalDate.now(), "staff-1", "James", "Wilson",
                    "james@company.com", "mgr-1", "Engineering", 25);
            when(objectMapper.writeValueAsString(remoteEvent)).thenReturn("{\"remote\":true}");
            when(eventsStore.save(any(EventStoreJpa.class))).thenAnswer(inv -> {
                EventStoreJpa jpa = inv.getArgument(0);
                jpa.setId(2L);
                return jpa;
            });

            // Act
            eventStoreService.append(remoteEvent, "StaffContext");

            // Assert
            ArgumentCaptor<EventStoreJpa> captor = ArgumentCaptor.forClass(EventStoreJpa.class);
            verify(eventsStore).save(captor.capture());
            assertEquals("PENDING", captor.getValue().getStatus());
            assertEquals("StaffMemberAddedEvent", captor.getValue().getEventType());
        }

        @Test
        @DisplayName("Should throw IllegalArgumentException when serialization fails")
        void shouldThrowOnSerializationFailure() throws Exception {
            // Arrange
            var event = new LeaveRequestSubmittedEvent(
                    LocalDate.now(), "req-1", "staff-1", 5);
            when(objectMapper.writeValueAsString(event))
                    .thenThrow(new TestJsonProcessingException("Serialization error"));

            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> eventStoreService.append(event, "TestContext"));
            verify(eventsStore, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatus {

        @Test
        @DisplayName("Should update event status without incrementing retry count")
        void shouldUpdateStatus() {
            // Arrange
            EventStoreJpa existingEvent = new EventStoreJpa();
            existingEvent.setId(1L);
            existingEvent.setStatus("PENDING");
            existingEvent.setRetryCount(0);
            when(eventsStore.findById(1L)).thenReturn(Optional.of(existingEvent));

            // Act
            eventStoreService.updateStatus(1L, EventStoreService.StatusOfMessageDelivery.PUBLISHED, false);

            // Assert
            assertEquals("PUBLISHED", existingEvent.getStatus());
            assertEquals(0, existingEvent.getRetryCount());
            verify(eventsStore).save(existingEvent);
        }

        @Test
        @DisplayName("Should update status and increment retry count when requested")
        void shouldIncrementRetryCount() {
            // Arrange
            EventStoreJpa existingEvent = new EventStoreJpa();
            existingEvent.setId(2L);
            existingEvent.setStatus("PENDING");
            existingEvent.setRetryCount(1);
            when(eventsStore.findById(2L)).thenReturn(Optional.of(existingEvent));

            // Act
            eventStoreService.updateStatus(2L, EventStoreService.StatusOfMessageDelivery.FAILED, true);

            // Assert
            assertEquals("FAILED", existingEvent.getStatus());
            assertEquals(2, existingEvent.getRetryCount());
            verify(eventsStore).save(existingEvent);
        }

        @Test
        @DisplayName("Should do nothing when event ID not found")
        void shouldDoNothingWhenNotFound() {
            // Arrange
            when(eventsStore.findById(999L)).thenReturn(Optional.empty());

            // Act
            eventStoreService.updateStatus(999L, EventStoreService.StatusOfMessageDelivery.PUBLISHED, false);

            // Assert
            verify(eventsStore, never()).save(any());
        }
    }

    @Nested
    @DisplayName("purgeOldEvents")
    class PurgeOldEvents {

        @Test
        @DisplayName("Should delete PUBLISHED events older than retention period")
        void shouldPurgeOldPublishedEvents() {
            // Arrange
            EventStoreJpa publishedEvent = createEvent(1L, "PUBLISHED", LocalDate.now().minusDays(31));
            when(eventsStore.findByStatusAndOccurredOnBefore(eq("PUBLISHED"), any(LocalDate.class)))
                    .thenReturn(java.util.List.of(publishedEvent));
            when(eventsStore.findByStatusAndOccurredOnBefore(eq("LOCAL"), any(LocalDate.class)))
                    .thenReturn(java.util.List.of());

            // Act
            int purged = eventStoreService.purgeOldEvents(30);

            // Assert
            assertEquals(1, purged);
            verify(eventsStore).deleteAll(java.util.List.of(publishedEvent));
        }

        @Test
        @DisplayName("Should delete LOCAL events older than retention period")
        void shouldPurgeOldLocalEvents() {
            // Arrange
            EventStoreJpa localEvent = createEvent(2L, "LOCAL", LocalDate.now().minusDays(45));
            when(eventsStore.findByStatusAndOccurredOnBefore(eq("PUBLISHED"), any(LocalDate.class)))
                    .thenReturn(java.util.List.of());
            when(eventsStore.findByStatusAndOccurredOnBefore(eq("LOCAL"), any(LocalDate.class)))
                    .thenReturn(java.util.List.of(localEvent));

            // Act
            int purged = eventStoreService.purgeOldEvents(30);

            // Assert
            assertEquals(1, purged);
            verify(eventsStore).deleteAll(java.util.List.of(localEvent));
        }

        @Test
        @DisplayName("Should NOT purge FAILED or PENDING events")
        void shouldNotPurgeFailedOrPendingEvents() {
            // Arrange — no PUBLISHED or LOCAL events found
            when(eventsStore.findByStatusAndOccurredOnBefore(eq("PUBLISHED"), any(LocalDate.class)))
                    .thenReturn(java.util.List.of());
            when(eventsStore.findByStatusAndOccurredOnBefore(eq("LOCAL"), any(LocalDate.class)))
                    .thenReturn(java.util.List.of());

            // Act
            int purged = eventStoreService.purgeOldEvents(30);

            // Assert — nothing deleted, FAILED/PENDING never queried
            assertEquals(0, purged);
            verify(eventsStore, never()).findByStatusAndOccurredOnBefore(eq("FAILED"), any());
            verify(eventsStore, never()).findByStatusAndOccurredOnBefore(eq("PENDING"), any());
        }

        @Test
        @DisplayName("Should return zero when no events are older than retention period")
        void shouldReturnZeroWhenNothingToPurge() {
            // Arrange
            when(eventsStore.findByStatusAndOccurredOnBefore(eq("PUBLISHED"), any(LocalDate.class)))
                    .thenReturn(java.util.List.of());
            when(eventsStore.findByStatusAndOccurredOnBefore(eq("LOCAL"), any(LocalDate.class)))
                    .thenReturn(java.util.List.of());

            // Act
            int purged = eventStoreService.purgeOldEvents(30);

            // Assert
            assertEquals(0, purged);
        }

        private EventStoreJpa createEvent(Long id, String status, LocalDate occurredOn) {
            EventStoreJpa jpa = new EventStoreJpa();
            jpa.setId(id);
            jpa.setStatus(status);
            jpa.setOccurredOn(occurredOn);
            jpa.setEventType("TestEvent");
            jpa.setEventBody("{}");
            jpa.setRetryCount(0);
            return jpa;
        }
    }

    // ---------------------------------------------------------------
    // HELPER
    // ---------------------------------------------------------------

    /**
     * Concrete subclass of JsonProcessingException for test stubbing
     * (JsonProcessingException's constructor is protected).
     */
    private static class TestJsonProcessingException extends JsonProcessingException {
        protected TestJsonProcessingException(String msg) {
            super(msg);
        }
    }
}
