package com.staffs.leavebooking.integration;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.staffs.leavebooking.common.events.RabbitOutboxRouter;
import com.staffs.leavebooking.common.events.RemoteOutboxListener;
import com.staffs.leavebooking.identity.authService.FirebaseAuthService;
import com.staffs.leavebooking.identity.authService.FirebaseConfig;
import com.staffs.leavebooking.identity.authService.FirebaseTokenFilter;
import com.staffs.leavebooking.identity.security.SecurityConfig;
import com.staffs.leavebooking.identity.security.UnauthorisedAccessLogger;
import org.mockito.Mockito;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Test configuration that mocks all external dependencies (Firebase, RabbitMQ)
 * and disables security for integration tests.
 * This allows testing the application service layer with a real H2 database
 * without needing Firebase or RabbitMQ connections.
 */
@TestConfiguration
public class TestSecurityConfig {

    @Bean
    @Primary
    public SecurityFilterChain testFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }

    @Bean
    @Primary
    public FirebaseAuth firebaseAuth() {
        return Mockito.mock(FirebaseAuth.class);
    }

    @Bean
    @Primary
    public FirebaseApp firebaseApp() {
        return Mockito.mock(FirebaseApp.class);
    }

    @Bean
    @Primary
    public JwtDecoder jwtDecoder() {
        return Mockito.mock(JwtDecoder.class);
    }

    @Bean
    @Primary
    public FirebaseAuthService firebaseAuthService() {
        return Mockito.mock(FirebaseAuthService.class);
    }

    @Bean
    @Primary
    public FirebaseTokenFilter firebaseTokenFilter() {
        return Mockito.mock(FirebaseTokenFilter.class);
    }

    @Bean
    @Primary
    public UnauthorisedAccessLogger unauthorisedAccessLogger() {
        return Mockito.mock(UnauthorisedAccessLogger.class);
    }

    @Bean
    @Primary
    public RabbitTemplate rabbitTemplate() {
        return Mockito.mock(RabbitTemplate.class);
    }

    @Bean
    @Primary
    public RemoteOutboxListener remoteOutboxListener() {
        return Mockito.mock(RemoteOutboxListener.class);
    }

    @Bean
    @Primary
    public RabbitOutboxRouter rabbitOutboxRouter() {
        return Mockito.mock(RabbitOutboxRouter.class);
    }
}
