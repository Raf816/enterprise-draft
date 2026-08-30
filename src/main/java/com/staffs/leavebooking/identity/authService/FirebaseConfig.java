package com.staffs.leavebooking.identity.authService;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.auth.http.HttpTransportFactory;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.util.List;

/**
 * Firebase initialization and JWT decoder configuration
 * (Lecture 9 — Identity, Firebase Admin SDK, JWT Validation).
 *
 * <p><strong>Three beans provided:</strong>
 * <ol>
 *   <li>{@code FirebaseApp} — the Firebase SDK entry point, initialized from
 *       {@code serviceAccountKey.json} with Windows-ROOT SSL trust store</li>
 *   <li>{@code FirebaseAuth} — the Firebase authentication service (for user management)</li>
 *   <li>{@code JwtDecoder} — Spring Security's JWT decoder (for token validation in
 *       the OAuth2 Resource Server chain)</li>
 * </ol>
 *
 * <p><strong>Windows-ROOT SSL workaround:</strong> On the corporate network (BT/Zscaler),
 * Google's SSL certificates are intercepted by a corporate proxy. The standard Java
 * trust store doesn't include the proxy's CA certificate. The Windows-ROOT KeyStore
 * contains all certificates trusted by Windows, including the corporate proxy's CA.
 * This workaround uses the Windows-ROOT store instead of Java's default.
 *
 * <p><strong>Profile: !test</strong> — Not loaded during tests. Tests use mocked
 * Firebase beans from {@code TestSecurityConfig}.
 *
 * @see FirebaseAuthService which uses the FirebaseAuth bean
 * @see com.staffs.leavebooking.identity.security.SecurityConfig which uses the JwtDecoder bean
 */
@Configuration // Spring configuration class — provides bean definitions
@Slf4j         // Lombok: generates a private static final Logger
@org.springframework.context.annotation.Profile("!test") // Don't load during tests
public class FirebaseConfig {

    // Error message constants
    public static final String FIREBASE_CREDENTIALS_FILE_MISSING = "Firebase credentials file missing";
    public static final String SERVICE_ACCOUNT_INVALID_PROJECT_ID = "Service account JSON does not contain a valid project_id";

    /** The service account JSON file path on the classpath (gitignored — contains secrets) */
    private static final String RESOURCE_FILE = "serviceAccountKey.json";

    /**
     * Creates and initialises the FirebaseApp singleton.
     * Reads the service account credentials from the classpath and configures
     * the Firebase SDK with Windows-ROOT SSL trust for corporate network compatibility.
     *
     * @return the initialized FirebaseApp instance
     * @throws IOException if the credentials file cannot be read
     */
    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        // Check if a FirebaseApp instance already exists (singleton pattern)
        // This prevents double-initialization if Spring calls this bean method twice
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        // Load the service account JSON file from src/main/resources/
        ClassPathResource resource = new ClassPathResource(RESOURCE_FILE);
        if (!resource.exists()) {
            throw new FileNotFoundException(FIREBASE_CREDENTIALS_FILE_MISSING);
        }

        // Read the credentials file and initialize Firebase
        try (InputStream serviceAccount = resource.getInputStream()) {

            // Create an HttpTransportFactory that uses the Windows-ROOT certificate store
            // This allows Firebase SDK to make HTTPS calls through corporate proxies
            HttpTransportFactory httpTransportFactory = () -> {
                try {
                    // Load Windows' trusted certificates (includes corporate proxy CA)
                    KeyStore windowsRootStore = KeyStore.getInstance("Windows-ROOT");
                    windowsRootStore.load(null, null); // Windows-ROOT doesn't need a password

                    // Create a TrustManagerFactory using the Windows certificates
                    TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                    tmf.init(windowsRootStore);

                    // Create an SSL context with the Windows trust managers
                    SSLContext sslContext = SSLContext.getInstance("TLS");
                    sslContext.init(null, tmf.getTrustManagers(), null);

                    // Build an HTTP transport using the Windows-trusted SSL socket factory
                    return new NetHttpTransport.Builder().setSslSocketFactory(sslContext.getSocketFactory()).build();
                } catch (Exception e) {
                    // If Windows-ROOT fails (e.g., on Linux), fall back to default Java trust store
                    log.warn("Could not create Windows-ROOT SSL transport: {}. Falling back to default.", e.getMessage());
                    return new NetHttpTransport();
                }
            };

            // Load the service account credentials using our custom HTTP transport
            GoogleCredentials credentials = ServiceAccountCredentials.fromStream(serviceAccount, httpTransportFactory);

            // Extract the project ID from the service account JSON
            String projectId = null;
            if (credentials instanceof ServiceAccountCredentials sac) {
                projectId = sac.getProjectId(); // e.g., "leave-booking-system-12345"
            }

            // Create the HTTP transport for Firebase API calls
            HttpTransport transport = httpTransportFactory.create();

            // Build the FirebaseOptions with all configuration
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)      // Service account credentials
                    .setProjectId(projectId)          // Firebase project ID
                    .setHttpTransport(transport)       // Custom transport with Windows-ROOT SSL
                    .setConnectTimeout(15000)          // 15 second connection timeout
                    .setReadTimeout(15000)             // 15 second read timeout
                    .build();

            log.info("Firebase initialised for project: {} (with Windows-ROOT SSL trust)", projectId);
            return FirebaseApp.initializeApp(options);
        }
    }

    /**
     * Creates the FirebaseAuth bean from the FirebaseApp.
     * This is the main entry point for all Firebase user management operations
     * (create user, set claims, verify tokens, etc.).
     *
     * @param firebaseApp the initialized FirebaseApp
     * @return the FirebaseAuth instance
     */
    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }

    /**
     * Creates a JwtDecoder for Spring Security's OAuth2 Resource Server.
     * This decoder validates Firebase ID tokens (JWTs) by:
     * <ol>
     *   <li>Fetching Google's public keys from the JWK Set URI</li>
     *   <li>Verifying the JWT signature against those keys</li>
     *   <li>Validating the issuer claim ({@code https://securetoken.google.com/{projectId}})</li>
     *   <li>Validating the audience claim (must include the Firebase project ID)</li>
     * </ol>
     *
     * <p>This works alongside the {@link FirebaseTokenFilter} — the filter does
     * Firebase Admin SDK verification, and this decoder does JWK-based verification
     * for the OAuth2 Resource Server chain.
     *
     * @param firebaseApp the initialized FirebaseApp (used to get the project ID)
     * @return a configured NimbusJwtDecoder for Firebase JWT validation
     */
    @Bean
    public JwtDecoder jwtDecoder(FirebaseApp firebaseApp) {
        // Get the project ID — needed for issuer and audience validation
        String projectId = firebaseApp.getOptions().getProjectId();
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalStateException(SERVICE_ACCOUNT_INVALID_PROJECT_ID);
        }

        // Google's public key endpoint for Firebase token signatures
        // This is where Spring fetches the RSA public keys to verify JWT signatures
        String jwkSetUri = "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com";

        // Create a Nimbus JWT decoder that fetches keys from the JWK Set URI
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();

        // Create an issuer validator — ensures the JWT was issued by Firebase for our project
        String issuerUri = "https://securetoken.google.com/" + projectId;
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);

        // Create an audience validator — ensures the JWT is intended for our project
        OAuth2TokenValidator<Jwt> withAudience = new JwtClaimValidator<List<String>>(
                "aud",  // The audience claim
                audList -> audList != null && audList.contains(projectId) // Must contain our project ID
        );

        // Combine both validators — both must pass for the JWT to be accepted
        OAuth2TokenValidator<Jwt> combinedValidator = new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience);

        // Set the combined validator on the decoder
        jwtDecoder.setJwtValidator(combinedValidator);

        return jwtDecoder;
    }
}
