package com.staffs.leavebooking.common.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the RabbitMQ message converter to use Jackson for JSON serialisation/deserialisation
 * (Lecture 8 — Remote Events, Message Broker).
 *
 * <p><strong>Why custom converter?</strong> By default, Spring AMQP uses Java serialisation
 * (ObjectOutputStream) for messages, which:
 * <ul>
 *   <li>Produces binary (non-human-readable) messages</li>
 *   <li>Requires both producer and consumer to have the exact same class version</li>
 *   <li>Is a known security risk (deserialisation attacks)</li>
 * </ul>
 *
 * <p><strong>Jackson2JsonMessageConverter:</strong> Instead, we use Jackson to serialise
 * events as JSON. This means:
 * <ul>
 *   <li>Messages are human-readable in the RabbitMQ management console</li>
 *   <li>Consumers can evolve independently (add fields without breaking producers)</li>
 *   <li>No deserialisation security risks</li>
 *   <li>Messages include a {@code __TypeId__} header with the fully-qualified class name,
 *       allowing the consumer to deserialise back to the correct Java record type</li>
 * </ul>
 *
 * <p><strong>setAlwaysConvertToInferredType(true):</strong> This tells the converter to
 * use the type information from the {@code __TypeId__} message header rather than
 * requiring the consumer to specify the expected type explicitly. This is necessary
 * because our {@code @RabbitListener} methods accept specific event record types
 * (e.g., {@code StaffMemberAddedEvent}) and the converter needs to know which
 * record class to deserialise the JSON into.
 *
 * <p><strong>Shared ObjectMapper:</strong> We inject Spring's auto-configured
 * {@link ObjectMapper} bean, which already has the correct settings for
 * LocalDate serialisation (ISO format), record support, etc.
 *
 * @see RemoteOutboxListener where events are published (serialised to JSON)
 * @see com.staffs.leavebooking.leavemanagement.application.listeners.StaffMemberAddedListener where events are consumed (deserialised from JSON)
 */
@Configuration // Spring configuration class — beans defined here are added to the application context
public class CustomMessageConverter {

    /**
     * Creates a Jackson-based message converter for RabbitMQ.
     * This bean replaces Spring AMQP's default Java serialisation converter.
     *
     * <p>Spring auto-detects this {@link MessageConverter} bean and uses it for
     * all RabbitTemplate operations (publishing) and @RabbitListener operations (consuming).
     *
     * @param objectMapper Spring's auto-configured Jackson ObjectMapper
     *                     (includes Java 8 date/time module, record support, etc.)
     * @return a Jackson2JsonMessageConverter configured for event serialisation
     */
    @Bean // Register this as a Spring-managed bean — replaces the default converter
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        // Create a Jackson-based converter using Spring's ObjectMapper
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);

        // Always use the __TypeId__ header to determine the target class for deserialisation
        // Without this, the converter might try to deserialise to a generic Map instead
        // of the specific event record class (e.g., StaffMemberAddedEvent)
        converter.setAlwaysConvertToInferredType(true);

        return converter;
    }
}
