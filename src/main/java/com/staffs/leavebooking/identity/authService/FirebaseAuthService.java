package com.staffs.leavebooking.identity.authService;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.auth.UserRecord.CreateRequest;
import com.staffs.leavebooking.identity.dto.LoginResponse;
import com.staffs.leavebooking.identity.security.Role;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Service handling Firebase user registration, login, role updates, and password changes
 * (Lecture 9 — Identity and Access Control, Firebase Admin SDK).
 *
 * <p><strong>Not domain-driven:</strong> This service belongs to the Generic context
 * (Identity & Access). It talks directly to Firebase — no DDD patterns (aggregates,
 * repositories, events) are used here. This follows the brief's architecture where
 * Identity is a generic supporting context, not a core business context.
 *
 * <p><strong>Two Firebase APIs used:</strong>
 * <ul>
 *   <li><strong>Firebase Admin SDK</strong> (server-side) — used for registration,
 *       user lookup, role management, password changes. Authenticated via
 *       {@code serviceAccountKey.json}.</li>
 *   <li><strong>Firebase Identity Toolkit REST API</strong> (client-facing) — used for
 *       login authentication. Returns an ID token (JWT) that the client uses for
 *       subsequent authenticated API requests.</li>
 * </ul>
 *
 * <p><strong>Custom claims:</strong> Firebase custom claims are key-value pairs stored
 * in the user record and embedded in every JWT the user receives. We store:
 * <ul>
 *   <li>{@code role} — "STAFF", "MANAGER", or "ADMIN" (used by Spring Security for RBAC)</li>
 *   <li>{@code admin} — boolean flag (true if role is ADMIN)</li>
 * </ul>
 *
 * @see FirebaseConfig for Firebase initialization and JwtDecoder setup
 * @see com.staffs.leavebooking.identity.AuthController which delegates to this service
 */
@Service // Spring-managed singleton — injected into AuthController and StaffController
@Slf4j   // Lombok: generates a private static final Logger
public class FirebaseAuthService {

    /** Firebase Admin SDK for server-side user management operations */
    private final FirebaseAuth firebaseAuth;

    /** Spring's declarative HTTP client for making REST calls to Firebase Identity Toolkit */
    private final RestClient restClient;

    /**
     * Firebase Web API key — read from application.yaml ({@code firebase.web-api-key}).
     * Required for the Identity Toolkit REST API login endpoint.
     * This is a public key (safe to use in client apps), NOT the service account private key.
     */
    @Value("${firebase.web-api-key}")
    private String firebaseApiKey;

    /**
     * Constructor — injects the FirebaseAuth bean (created by FirebaseConfig).
     * Also creates a RestClient instance for making HTTP calls to Firebase login API.
     *
     * @param firebaseAuth the Firebase Admin SDK auth instance
     */
    public FirebaseAuthService(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
        this.restClient = RestClient.create(); // Spring 6.1+ declarative HTTP client
    }

    /**
     * Registers a new user in Firebase and sets custom claims (role).
     *
     * <p><strong>Steps:</strong>
     * <ol>
     *   <li>Create a Firebase user with email, password, and display name</li>
     *   <li>Validate the requested role against the Role enum</li>
     *   <li>Set custom claims (role + admin flag) on the Firebase user record</li>
     * </ol>
     *
     * <p>Custom claims are embedded in every JWT the user receives after login,
     * enabling Spring Security's {@code @PreAuthorize} to check roles without
     * a database lookup on every request.
     *
     * @param username the display name for the user
     * @param email    the user's email address (must be unique in Firebase)
     * @param password the user's password (minimum 6 characters — Firebase requirement)
     * @param role     the role to assign ("STAFF", "MANAGER", or "ADMIN")
     * @return the created UserRecord from Firebase
     * @throws FirebaseAuthException if user creation fails (e.g., email already exists)
     */
    public UserRecord registerUser(String username, String email,
                                    String password, String role) throws FirebaseAuthException {
        // Build the Firebase user creation request
        CreateRequest createRequest = new CreateRequest()
                .setEmail(email)             // Must be unique across all Firebase users
                .setPassword(password)       // Firebase enforces min 6 characters
                .setDisplayName(username)    // Display name (stored in Firebase, returned in JWT)
                .setEmailVerified(false);    // Email not verified by default (would need verification flow)

        // Create the user in Firebase — returns a UserRecord with the assigned UID
        UserRecord userRecord = firebaseAuth.createUser(createRequest);

        // Validate and normalise the role using the Role enum
        // If role is null, default to STAFF
        String confirmedRole = (role != null)
                ? Role.fromString(role).name()  // Validates and converts to uppercase
                : Role.STAFF.name();            // Default: "STAFF"

        // Set custom claims on the Firebase user record
        // These claims will be embedded in every JWT the user receives on login
        Map<String, Object> customClaims = Map.of(
                "role", confirmedRole,                              // e.g., "ADMIN"
                "admin", confirmedRole.equals(Role.ADMIN.name())   // boolean: true if ADMIN
        );
        firebaseAuth.setCustomUserClaims(userRecord.getUid(), customClaims);

        log.info("Registered user {} with role {}", email, confirmedRole);
        return userRecord;
    }

    /**
     * Authenticates a user via the Firebase Identity Toolkit REST API.
     * Returns a LoginResponse containing the ID token (JWT) and refresh token.
     *
     * <p><strong>Why REST API instead of Admin SDK?</strong> The Firebase Admin SDK
     * can create and manage users but cannot generate ID tokens for them.
     * Token generation requires the Identity Toolkit REST API, which simulates
     * a client-side login and returns the JWT.
     *
     * <p><strong>API endpoint:</strong>
     * {@code POST https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=API_KEY}
     *
     * @param email    the user's email address
     * @param password the user's password
     * @return LoginResponse containing the JWT, refresh token, UID, etc.
     * @throws IllegalArgumentException if authentication fails (wrong credentials, user disabled, etc.)
     */
    public LoginResponse loginUser(String email, String password) {
        // Guard clause: reject null inputs
        if (email == null || password == null) {
            throw new IllegalArgumentException("Email and password must not be null");
        }

        // Build the Firebase Identity Toolkit login URL with the API key
        String firebaseLoginUrl =
                "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + firebaseApiKey;

        // Build the request body for the Firebase login API
        Map<String, Object> requestBody = Map.of(
                "email", email,                  // User's email
                "password", password,            // User's password
                "returnSecureToken", true         // Tells Firebase to return the ID token
        );

        try {
            // Make the POST request to Firebase and deserialise the response
            // The LoginResponse record uses @JsonProperty to map Firebase's field names
            return restClient.post()
                    .uri(firebaseLoginUrl)
                    .body(requestBody)
                    .retrieve()
                    .body(LoginResponse.class);
        } catch (HttpClientErrorException e) {
            // Firebase returned an error (wrong password, user not found, etc.)
            log.error("Firebase login failed [{}]: {}", e.getStatusCode(), e.getResponseBodyAsString());
            // Extract a clean, user-friendly error message from the Firebase JSON error
            String errorMessage = extractFirebaseErrorMessage(e.getResponseBodyAsString());
            throw new IllegalArgumentException(errorMessage);
        }
    }


    /**
     * Looks up a Firebase user by their email address.
     * Used by the GET /auth/users/{email} admin endpoint.
     *
     * @param email the email address to search for
     * @return the UserRecord from Firebase
     * @throws FirebaseAuthException if no user exists with this email
     */
    public UserRecord findUserByEmail(String email) throws FirebaseAuthException {
        return firebaseAuth.getUserByEmail(email);
    }

    /**
     * Updates the role custom claim on an existing Firebase user.
     * Called when an admin changes a staff member's role via PATCH /staff/{id}.
     * The new role takes effect on the user's NEXT login (when they get a new JWT).
     *
     * @param uid     the Firebase UID of the user to update
     * @param newRole the new role string ("STAFF", "MANAGER", or "ADMIN")
     * @throws FirebaseAuthException if the update fails
     */
    public void updateUserRole(String uid, String newRole) throws FirebaseAuthException {
        // Validate the role using the Role enum (throws if invalid)
        String confirmedRole = Role.fromString(newRole).name();

        // Build the updated custom claims map
        Map<String, Object> customClaims = Map.of(
                "role", confirmedRole,
                "admin", confirmedRole.equals(Role.ADMIN.name())
        );
        // Update the claims in Firebase
        firebaseAuth.setCustomUserClaims(uid, customClaims);
        log.info("Updated role for user {} to {}", uid, confirmedRole);
    }

    /**
     * Changes a user's password in Firebase.
     * Called by the PATCH /auth/password endpoint for users to change their own password.
     *
     * @param uid         the Firebase UID of the user (extracted from their JWT)
     * @param newPassword the new password (minimum 6 characters)
     * @throws IllegalArgumentException if the password is too short
     * @throws FirebaseAuthException    if the password update fails in Firebase
     */
    public void changePassword(String uid, String newPassword) throws FirebaseAuthException {
        // Validate minimum password length (Firebase requirement)
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters");
        }
        // Build the Firebase user update request
        var updateRequest = new com.google.firebase.auth.UserRecord.UpdateRequest(uid)
                .setPassword(newPassword);
        // Execute the update in Firebase
        firebaseAuth.updateUser(updateRequest);
        log.info("Password changed for user {}", uid);
    }

    /**
     * Extracts a user-friendly error message from Firebase's JSON error response body.
     * Firebase returns verbose error messages with error codes — this method translates
     * them into clean messages for API consumers.
     *
     * @param responseBody the raw JSON error response from Firebase
     * @return a clean, user-friendly error message
     */
    private String extractFirebaseErrorMessage(String responseBody) {
        if (responseBody != null) {
            // Check for known Firebase error codes and return friendly messages
            if (responseBody.contains("INVALID_LOGIN_CREDENTIALS")) {
                return "Invalid email or password";
            } else if (responseBody.contains("EMAIL_NOT_FOUND")) {
                return "No account found with this email address";
            } else if (responseBody.contains("INVALID_PASSWORD")) {
                return "Incorrect password";
            } else if (responseBody.contains("USER_DISABLED")) {
                return "This account has been disabled";
            } else if (responseBody.contains("TOO_MANY_ATTEMPTS_TRY_LATER")) {
                return "Too many failed login attempts. Please try again later";
            }
        }
        // Default message when the error code is unrecognised
        return "Authentication failed. Please check your credentials and try again";
    }
}
