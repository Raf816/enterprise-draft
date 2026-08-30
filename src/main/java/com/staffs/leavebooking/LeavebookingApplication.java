package com.staffs.leavebooking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main entry point for the Leave Booking System Spring Boot application
 * (COMP60047 Enterprise Application Development Assignment 1).
 *
 * <p><strong>Architecture:</strong> This application follows Domain-Driven Design (DDD)
 * with three bounded contexts:
 * <ul>
 *   <li><strong>Leave Management</strong> (Core) — leave requests, allowances, state machine,
 *       local events, CQRS handlers</li>
 *   <li><strong>Staff Management</strong> (Supporting) — staff member lifecycle, department
 *       management, remote events to Leave Management</li>
 *   <li><strong>Identity & Access</strong> (Generic) — Firebase authentication, JWT-based
 *       RBAC, security filters</li>
 * </ul>
 *
 * <p><strong>Spring Boot annotations explained:</strong>
 * <table>
 *   <tr><th>Annotation</th><th>Purpose</th><th>Lecture</th></tr>
 *   <tr><td>{@code @SpringBootApplication}</td>
 *       <td>Combines {@code @Configuration} + {@code @EnableAutoConfiguration} + {@code @ComponentScan}.
 *           Auto-configures Spring Boot based on classpath dependencies (H2, JPA, RabbitMQ, Security).</td>
 *       <td>Lecture 1</td></tr>
 *   <tr><td>{@code @EnableRabbit}</td>
 *       <td>Activates {@code @RabbitListener} annotations on consumer classes, enabling them
 *           to receive messages from RabbitMQ queues.</td>
 *       <td>Lecture 8</td></tr>
 *   <tr><td>{@code @EnableAsync}</td>
 *       <td>Enables {@code @Async} on event listeners so they run on separate threads.
 *           The {@code RemoteOutboxListener} and local event listeners use this to avoid
 *           blocking the HTTP response while processing events.</td>
 *       <td>Lecture 7/8</td></tr>
 *   <tr><td>{@code @EnableRetry}</td>
 *       <td>Activates {@code @Retryable} and {@code @Recover} annotations on the
 *           {@code RemoteOutboxListener} for automatic retry with exponential backoff
 *           when RabbitMQ publishing fails.</td>
 *       <td>Lecture 8</td></tr>
 *   <tr><td>{@code @EnableScheduling}</td>
 *       <td>Activates {@code @Scheduled} annotations, enabling the
 *           {@code EventStoreCleanupJob} to run its daily purge at 02:00.</td>
 *       <td>—</td></tr>
 * </table>
 *
 * <p><strong>Running the application:</strong>
 * <ul>
 *   <li>{@code mvn spring-boot:run} — starts on port 8900 (configured in application.yaml)</li>
 *   <li>Requires: Docker RabbitMQ running on localhost:5672, Firebase serviceAccountKey.json in resources</li>
 *   <li>H2 Console: http://localhost:8900/h2-console (jdbc:h2:mem:leavebooking, sa, blank)</li>
 * </ul>
 */
@EnableRabbit       // Activates @RabbitListener annotations for consuming remote events from RabbitMQ
@EnableAsync        // Enables @Async on event listeners — they run on separate threads after commit
@EnableRetry        // Activates @Retryable/@Recover on the outbox publisher for retry with backoff
@EnableScheduling   // Activates @Scheduled on EventStoreCleanupJob for daily event purge
@SpringBootApplication // Combines @Configuration + @EnableAutoConfiguration + @ComponentScan
public class LeavebookingApplication {

    /**
     * Application entry point — boots the Spring application context.
     * Spring Boot auto-configures all components based on the classpath:
     * H2 (embedded database), JPA (Hibernate), RabbitMQ (Spring AMQP),
     * Spring Security (OAuth2 Resource Server), and all custom beans.
     *
     * @param args command-line arguments (not used in this application)
     */
    public static void main(String[] args) {
        SpringApplication.run(LeavebookingApplication.class, args);
    }
}
