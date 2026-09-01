# Issues & Fixes Log â€” Leave Booking System

**Module:** COMP60047 Enterprise Application Development
**Project:** Leave Booking System (Scenario 1)
**Date Started:** 2026-08-24

---

## 1. Compilation Fixes

### 1.1 `@EnableRabbit` import incorrect

| Item | Detail |
|------|--------|
| **Error** | `package org.springframework.rabbit.annotation does not exist` |
| **Cause** | Wrong import path for `@EnableRabbit` in `LeavebookingApplication.java` |
| **Fix** | Changed `import org.springframework.rabbit.annotation.EnableRabbit` to `import org.springframework.amqp.rabbit.annotation.EnableRabbit` |
| **Status** | âœ… FIXED |

### 1.2 `CustomMessageConverter` â€” `TypePrecedence` not found

| Item | Detail |
|------|--------|
| **Error** | `cannot find symbol: variable TypePrecedence` in `Jackson2JsonMessageConverter` |
| **Cause** | The `TypePrecedence` enum was removed/moved in Spring AMQP 3.2.x |
| **Fix** | Removed the `setTypePrecedence()` call â€” not needed for our use case |
| **Status** | âœ… FIXED |

### 1.3 `CustomMessageConverter` â€” `JsonMapper` bean not found

| Item | Detail |
|------|--------|
| **Error** | `No qualifying bean of type 'com.fasterxml.jackson.databind.json.JsonMapper' available` |
| **Cause** | Spring Boot auto-configures `ObjectMapper` but not `JsonMapper` (a subclass) |
| **Fix** | Changed parameter from `JsonMapper` to `ObjectMapper` |
| **Status** | âœ… FIXED |

### 1.4 H2 reserved word `current_role`

| Item | Detail |
|------|--------|
| **Error** | `Syntax error in SQL statement... expected "identifier"` on `CREATE TABLE staff_member` |
| **Cause** | `current_role` is a reserved word in H2 2.x |
| **Fix** | Quoted the column name in `schema.sql` and `data.sql`: `"current_role"` |
| **Status** | âœ… FIXED |

### 1.5 Lombok + JDK 25 incompatibility

| Item | Detail |
|------|--------|
| **Error** | `java.lang.ExceptionInInitializerError: com.sun.tools.javac.code.TypeTag :: UNKNOWN` |
| **Cause** | IntelliJ was using JDK 25 (Oracle OpenJDK 25.0.1) but the Lombok version bundled with Spring Boot 3.4.1 doesn't support JDK 25 |
| **Fix** | Upgraded Lombok to version `1.18.42` in `pom.xml` and added explicit `annotationProcessorPaths` in the maven-compiler-plugin configuration |
| **Status** | âœ… FIXED |

### 1.6 Spring Boot 3.4.1 incompatible with JDK 25

| Item | Detail |
|------|--------|
| **Error** | `Unsupported class file major version 69` â€” `ASM ClassReader failed to parse class file` |
| **Cause** | Spring Framework 6.2.1 (bundled with Spring Boot 3.4.1) doesn't support JDK 25 class files. Spring Boot 4.0+ is required for JDK 25. |
| **Fix** | Changed IntelliJ's Project SDK from JDK 25 to **JDK 21** (File â†’ Project Structure â†’ SDK). Reverted `pom.xml` java.version back to `21`. The lectures use JDK 21, Spring Boot 3.4.1 supports up to JDK 23. |
| **Status** | âœ… FIXED |

---

## 2. Runtime / Environment Issues

### 2.1 RabbitMQ â€” CloudAMQP connection timeout

| Item | Detail |
|------|--------|
| **Error** | `java.net.ConnectException: Connection timed out: getsockopt` when connecting to `seal.lmq.cloudamqp.com:5671` |
| **Cause** | Corporate network (BT/Zscaler) blocks outbound connections to external cloud services on both port 5671 (TLS) and 5672 (non-TLS) |
| **Status** | âœ… FIXED â€” confirmed: `Created new connection: rabbitConnectionFactory [delegate=amqp://guest@127.0.0.1:5672/]` on 2026-08-25 |

### 2.2 Firebase â€” "Unexpected error refreshing access token"

| Item | Detail |
|------|--------|
| **Error** | `Registration failed: Unknown error while making a remote service call: Unexpected error refreshing access token` |
| **Cause** | `ClassNotFoundException: com.google.auth.CredentialTypeForMetrics` — version mismatch between Firebase Admin SDK 9.4.2 and a newer transitive `google-auth-library-oauth2-http` 1.29.0 dependency. The newer library expects a class that does not exist in the credentials JAR bundled with 9.4.2. Additionally, Zscaler SSL interception required using Windows-ROOT certificate store. |
| **Attempted fixes that did NOT work** | (1) `-Djava.net.useSystemProxies=true` (2) `-Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=9000` (Zscaler local proxy) (3) BT corporate proxy `cloudproxy.nat.bt.com:8080` via custom `HttpTransportFactory` (4) Importing Zscaler Root CA into JDK 21 cacerts (5) `-Djavax.net.ssl.trustStore` explicit truststore |
| **Fix** | (1) Upgraded Firebase Admin SDK from 9.4.2 to **9.7.0** in pom.xml (resolves ClassNotFoundException). (2) Configured `FirebaseConfig` to use **Windows-ROOT KeyStore** (`KeyStore.getInstance("Windows-ROOT")`) which includes the Zscaler root CA certificate. This allows Java to trust Zscaler's intercepted SSL certificates on port 443. Firebase now works on BT corporate network without hotspot. | â€” no code/proxy change can bypass it. Firebase registration/login only needs internet for the initial API call; all other features (H2, local RabbitMQ, domain logic, tests) work offline. |
| **Status** | âš ï¸ NETWORK LIMITATION â€” requires unrestricted internet access |

---

## 3. Configuration Summary (What Works)

| Component | Configuration | Status |
|-----------|---------------|--------|
| **H2 Database** | `jdbc:h2:mem:leavebooking`, user: `sa`, no password | âœ… Works on any network |
| **H2 Console** | `http://localhost:8900/h2-console` | âœ… Works on any network |
| **RabbitMQ** | Docker local: `localhost:5672`, user: `guest`/`guest` | âœ… Works on any network |
| **RabbitMQ Management UI** | `http://localhost:15672` | âœ… Works on any network |
| **Firebase Auth** | `serviceAccountKey.json` + web API key | âš ï¸ Requires unrestricted internet |
| **Unit Tests (423)** | `mvn test` | âœ… Works on any network (no external deps) |
| **Application Startup** | `mvn spring-boot:run` or IntelliJ Run | âœ… Starts on any network (RabbitMQ/Firebase errors are non-fatal) |

---

## 4. Environment Setup Checklist

### Prerequisites

| Requirement | Version | Location | Purpose |
|---|---|---|---|
| JDK | 21 | `C:\Program Files\Java\jdk-21` | Language runtime |
| Maven | 3.9+ (wrapper included) | `./mvnw` | Build tool |
| Docker Desktop | Any | System PATH | Local RabbitMQ container |
| IntelliJ IDEA | 2024+ | â€” | IDE |
| Firebase Project | â€” | `serviceAccountKey.json` in `src/main/resources/` | Authentication |
| Postman | Any | â€” | API testing |

### IntelliJ Configuration

| Setting | Value |
|---------|-------|
| Project SDK | JDK 21 (`C:\Program Files\Java\jdk-21`) |
| Language Level | SDK default (21) |
| Maven Java version | `<java.version>21</java.version>` in pom.xml |
| VM Options (Run Config) | `-Djavax.net.ssl.trustStore="C:\Program Files\Java\jdk-21\lib\security\cacerts" -Djavax.net.ssl.trustStorePassword=changeit` |
| Lombok plugin | Installed + annotation processing enabled |

### Docker Commands

```bash
# Start RabbitMQ (first time)
docker run -d --name leave-rabbitmq -p 5672:5672 -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=guest -e RABBITMQ_DEFAULT_PASS=guest \
  rabbitmq:management

# Stop RabbitMQ
docker stop leave-rabbitmq

# Start again (after stop)
docker start leave-rabbitmq

# Remove completely
docker rm -f leave-rabbitmq
```

### RabbitMQ Setup (via Management UI at localhost:15672)

1. **Exchange:** `staff-management` (type: topic, durable: yes)
2. **Queue 1:** `leave-management.staff-member-added` (durable: yes)
3. **Queue 2:** `leave-management.staff-member-updated` (durable: yes)
4. **Binding 1:** `staff-management` â†’ `leave-management.staff-member-added` with key `staff.member.added`
5. **Binding 2:** `staff-management` â†’ `leave-management.staff-member-updated` with key `staff.member.updated`

### Firebase Setup

1. Create Firebase project at https://console.firebase.google.com
2. Enable Authentication â†’ Email/Password
3. Project Settings â†’ Service Accounts â†’ Generate Private Key â†’ save as `serviceAccountKey.json`
4. Register a web app â†’ copy the `apiKey` value
5. Place `serviceAccountKey.json` in `src/main/resources/`
6. Set `firebase.web-api-key` in `application.yaml`

---

## 5. Testing Strategy

### Unit Tests (Task 12 â€” COMPLETE)

- **450 tests, 0 failures, 1 skipped** (the skipped test is the Spring Boot context loader which needs Firebase)
- Run: `mvn test`
- Runtime: ~2-3 seconds
- No external dependencies needed (pure Java domain tests + mapper tests)
- Patterns: AAA, Object Mother, @DisplayName, @Nested
- Coverage: All value objects, all aggregate invariants/state machines, all 9 mapper classes

### Manual Testing (requires phone hotspot for Firebase)

1. Start Docker RabbitMQ
2. Connect to phone hotspot
3. Run app from IntelliJ
4. Register users via Postman (ADMIN, MANAGER, STAFF)
5. Login to get JWT tokens
6. Test all CRUD endpoints with appropriate role tokens
7. Verify event flow: POST /staff â†’ check RabbitMQ â†’ check leave_allowance created
8. Verify state machine: submit â†’ approve/reject/cancel â†’ check allowance updates

---

## 6. Project Timeline

| Date | Milestone |
|------|-----------|
| 2026-08-21 | Lecture material loaded (Lectures 1-9) |
| 2026-08-24 | Tasks 1-11 completed (full implementation) |
| 2026-08-24 | Task 12: Unit tests created (450 tests, all passing) |
| 2026-08-25 | Design docs (01-05) enhanced to first-class standard |
| 2026-08-25 | Environment setup: Firebase, RabbitMQ (Docker), compilation fixes |
| 2026-08-25 | Identified corporate network limitation (Zscaler blocks Google OAuth2) |
| 2026-08-26 | Task 13: Integration tests (14 tests, @DataJpaTest) |
| TBD | Task 14: Postman collections |
| 2026-09-21 | **Submission deadline** |

---

## 7. Key Decisions Made During Development

| Decision | Reason |
|----------|--------|
| Use JDK 21 (not 25) | Spring Boot 3.4.1 only supports up to JDK 23. JDK 25 requires Spring Boot 4.0+. Lectures use JDK 21. |
| Run RabbitMQ locally via Docker | Corporate network blocks all external cloud services. Local Docker matches how the reference project (Keanu) runs RabbitMQ. |
| Keep Firebase (not switch to a mock) | The assignment requires real authentication per Lecture 9. Firebase works on unrestricted networks. The code is correct â€” only the BT network is blocking it. |
| Lombok 1.18.42 with explicit annotation processor | Required for JDK 21 compatibility with IntelliJ's compiler. Earlier versions caused `TypeTag :: UNKNOWN` errors. |
| Quote `"current_role"` in SQL | H2 2.x treats `current_role` as a reserved word. Standard SQL quoting resolves it without changing the column name. |

---

## 8. Integration Tests Fix (Task 13)

### 8.1 @SpringBootTest context loading failure

| Item | Detail |
|------|--------|
| **Error** | Context fails to load: FirebaseConfig needs serviceAccountKey.json, CustomMessageConverter needs RabbitMQ beans, RemoteOutboxListener needs RabbitTemplate |
| **Cause** | @SpringBootTest loads the FULL application context including Firebase, RabbitMQ, and Security which require external connectivity |
| **Attempted fixes** | (1) @Profile(!test) on FirebaseConfig/SecurityConfig (2) TestSecurityConfig with mocked beans (3) Exclude RabbitAutoConfiguration via properties. None fully resolved transitive bean failures. |
| **Fix** | Switched to @DataJpaTest + @Import approach. Only loads the JPA slice (repos, entities, H2). Imports exactly the beans needed: LeaveRequestApplicationService, LeaveAllowanceApplicationService, DomainEventManager, EventStoreService, JacksonAutoConfiguration |
| **Result** | 14 integration tests, 0 failures. Context loads in ~6 seconds. No Firebase, RabbitMQ, or Security beans instantiated. |
| **Status** | FIXED |

### 8.2 @TransactionalEventListener not firing in tests

| Item | Detail |
|------|--------|
| **Observation** | Event listeners annotated with @TransactionalEventListener(AFTER_COMMIT) + @Async never fire during @DataJpaTest |
| **Cause** | @DataJpaTest wraps each test in a transaction that rolls back (never commits). AFTER_COMMIT listeners only fire after commit. |
| **Resolution** | Test the allowance update logic by calling LeaveAllowanceApplicationService directly (reserveDays, confirmDays, etc.) which is exactly what the listeners do. This proves the service logic is correct without needing the event wiring. |
| **Status** | BY DESIGN (not a bug) |

### 8.3 ObjectMapper bean missing in @DataJpaTest

| Item | Detail |
|------|--------|
| **Error** | No qualifying bean of type ObjectMapper available (needed by EventStoreService) |
| **Cause** | @DataJpaTest does not auto-configure Jackson (only JPA-related beans) |
| **Fix** | Added JacksonAutoConfiguration.class to the @Import list |
| **Status** | FIXED |
### 8.4 Why Firebase works on corporate network but CloudAMQP does not

| Item | Detail |
|------|--------|
| **Observation** | Firebase Auth connects successfully on BT/Zscaler corporate network (no hotspot needed). CloudAMQP always times out on the same network. |
| **Root cause** | Firebase uses **port 443 (HTTPS)** which Zscaler allows through (it intercepts and re-signs the SSL cert). CloudAMQP uses **port 5671 (AMQPS)** which Zscaler blocks entirely at the TCP level. |
| **Evidence** | `Test-NetConnection seal.lmq.cloudamqp.com -Port 443` returns **TcpTestSucceeded: True**. `Test-NetConnection seal.lmq.cloudamqp.com -Port 5671` times out. |
| **Why Firebase SSL trust fix worked** | Zscaler intercepts HTTPS (port 443) by injecting its own certificate. Java's default truststore doesn't trust Zscaler's cert, causing `SSLHandshakeException`. Using `Windows-ROOT` truststore (which includes Zscaler's root CA) resolves this. |
| **Why the same fix can't help CloudAMQP** | Port 5671 traffic is not intercepted — it is **dropped entirely**. The TCP connection never establishes, so no SSL handshake ever occurs. No truststore change can fix a connection that never reaches the server. |
| **Resolution** | Use local Docker RabbitMQ (localhost:5672) for development. CloudAMQP config kept as comment in application.yaml for use on unrestricted networks. Code is broker-agnostic — switching is a config change only. |
| **Status** | NETWORK LIMITATION (by design — not a code issue) |


---

## 9. Outstanding Gaps — Final Audit (2026-08-29)

These items were identified during an exhaustive line-by-line audit of both the formal mark scheme and the lecturer's guidance document against our implementation. They are recorded here so nothing is lost.

---

### GAP 1: Postman Collection — RESOLVED

| Item | Detail |
|------|--------|
| **Mark scheme criterion** | Testing Decisions (/30): "API (if used) is **comprehensively tested** using an appropriate tool. **Follows best practice.**" |
| **Guidance says** | "Coverage of **ALL** end points – considering both valid data (which might have different roles to take into consideration) as well as invalid data." |
| **Resolution** | Comprehensive Postman collections created: automated at `postman/Leave-Booking-System.postman_collection.json`, manual at `postman/Leave-Booking-System-MANUAL.postman_collection.json`, with environment file `Leave-Booking-System.postman_environment.json`. |
| **Coverage** | 8 domain-based folders with Edge Cases subfolders, 137 requests total covering all 26 endpoints: (1) Auth: Registration & Login — register 5 users + login all + edge cases (blank/invalid/duplicate/empty); (2) Auth: Role Check, Find User & Password — role-check, find-by-email, change password + edge cases (401/403/404); (3) Staff Management: Setup & Queries — PATCH 5 skeletons, GET all/by-id, POST search + edge cases (RBAC 403, 404 not-found, invalid status BANANA→400, no filters→400, POST /staff validation: missing fields, future hireDate, digits in name domain VO, >50 chars, empty body); (4) Staff Management: Updates & Transitions — terminate + edge cases (reactivate terminated→409, terminated submits leave→403); (5) Leave Requests: Submit — auto-resolve manager, explicit managerId, Staff2 submit + edge cases (missing fields, past dates, end<start, reason >500, non-existent manager, date overlap→409, no token, empty body); (6) Leave Requests: Approve, Reject & Cancel — approve/reject/cancel happy paths, admin override + edge cases (already-approved→409, already-rejected→409, already-cancelled→409, cancel-rejected→409, wrong manager→403, staff role→403, not-owner→403, reason >500, 404 not-found, no-token 401); (7) Leave Requests: Queries & Search — GET my/team/all/{id}, POST search by status/date-range/staffMemberId/managerId/combined + edge cases (RBAC 403, 404, no filters→400, invalid status→400, single date→400, from>to→400); (8) Leave Allowances — GET my/staff/team/all/dept-filter, PATCH amend/revert + edge cases (401, 403 staff/manager, 404, @Min 0→400, negative→400). |
| **Test scripts** | Every request in the automated collection has `pm.test()` assertions verifying status codes, response structure, and business rules. Login scripts auto-save JWT tokens via `pm.environment.set()` for use in subsequent requests. Manual collection uses PASTE_*_TOKEN placeholders with no scripts. |
| **Status** | ✅ IMPLEMENTED AND RESTRUCTURED (2026-08-31) — 137 requests, domain-based folders matching bounded contexts |

---

### GAP 2: Prior Learning Reflection (MEDIUM PRIORITY — REPORT TASK)

| Item | Detail |
|------|--------|
| **Mark scheme criterion** | Appears **5 times** across all 3 sections: "Where possible we will see reflection on how previous learning has influenced the decision making here." |
| **Guidance says** | "where possible make brief reference to previous patterns learned, OOP principles, SOLID, GRASP, etc." and "Hopefully this is self-explanatory." |
| **What's missing** | Our docs reference Phil's current module lectures (Lecture 2, 7, 8, 9 etc.) but do NOT explicitly reference earlier modules: COMP50051 (OOP — SOLID, GRASP, GoF, REST APIs in TypeScript, Jest testing, security headers, rate limiting), COMP60044 (Mobile Apps — Firebase Auth, MVVM, Hilt DI, Room, testing), COMP50045 (Agile — React, WCAG, web security, JWT). |
| **What's needed** | In the REPORT (not necessarily the code docs), add 1-2 sentences per section connecting decisions to prior learning. Examples: "In COMP50051 we implemented JWT authentication from scratch in TypeScript — this gave us confidence to choose Firebase's JWT-based auth here, knowing how tokens work under the hood." / "COMP50051's coverage of SOLID principles (specifically SRP) directly influenced our decision to separate query handlers from application services — each class has exactly one reason to change." / "The AAA test pattern and Object Mother fixture approach we first used in COMP50051's Jest testing naturally transferred to our JUnit 5 test suite here." |
| **Source material** | Full details of prior module content are in the handoff document section 12 ("Prior Module Learning — Full Detail"). Contains complete COMP50051, COMP60044, COMP50045 lecture summaries. |
| **Impact if not done** | Loses differentiation across all 3 sections. Won't drop below 70% band but prevents reaching 85-90%+ as it's clearly something Phil values (mentioned 5 times). |
| **Status** | DEFERRED — will address during report writing phase |

---

### GAP 3: Admin Filtering on /leave-requests/all Endpoint — RESOLVED (then REWORKED to POST Search)

| Item | Detail |
|------|--------|
| **Brief says** | Admin action 3: "View all outstanding leave requests **filtered by staff member, manager's team or across the company**". Manager action 1: "View outstanding requests... **(could be enhanced with start and end dates to reduce the reporting period)**" |
| **Original gap** | Only `GET /leave-requests/all` (company-wide) with optional `?status=` param. No way for admin to filter by staff member or manager's team. No date range filtering for managers. |
| **First fix (2026-08-29)** | Added sub-path endpoints: `GET /all/staff/{id}` and `GET /all/manager/{id}` with optional `?status=` query param. |
| **Rework (2026-08-29)** | Migrated ALL filtering from GET query params to POST search endpoints. Removed the sub-paths and `?status=` params. GET endpoints are now simple unfiltered reads. Three POST search endpoints handle all filtering with a structured JSON body: `POST /my/search` (status), `POST /team/search` (status + date range), `POST /all/search` (status + staffMemberId + managerId + date range). |
| **Implementation** | `LeaveRequestSearchCriteria` record (5 optional fields: status, staffMemberId, managerId, from, to). `LeaveRequestQueryHandler` has 3 search methods with dynamic filter combination logic. `LeaveManagementFacade` has 3 search methods with `@PreAuthorize`. 8 new Spring Data JPA repository query methods for date range combinations. |
| **Tests** | Controller: 18 tests (4 GET + 10 POST search + 4 commands). Query handler: 17 tests (5 basic + 12 search). Total: 423 tests, all passing. |
| **Design justification** | Documented in docs/04 section 13: "Why POST search endpoints instead of GET with query parameters?" — covers clean URL separation, structured filter body, extensibility, and enterprise convention (Elasticsearch, Stripe). |
| **Status** | ✅ IMPLEMENTED (2026-08-29) — both gap 3 (admin filtering) and gap 5 (date range) resolved in one rework |

---

### GAP 4: PUT and DELETE HTTP Verbs Not Demonstrated — RESOLVED (Justified in Documentation)

| Item | Detail |
|------|--------|
| **Brief says** | Under "General Points About the API Design": "Apply clear, semantic URL paths and HTTP verbs (GET, POST, **DELETE**, **PUT**, PATCH)" |
| **What we have** | GET ✅, POST ✅, PATCH ✅. No PUT endpoints. No DELETE API endpoints. Only physical deletion is the scheduled `EventStoreCleanupJob` (infrastructure, not an API endpoint). |
| **Resolution** | Fully justified in `docs/04` section 13: "Why PATCH (not PUT)" explains PUT would require sending entire resource for partial changes (RFC 7231 vs RFC 5789). "Why no DELETE API endpoints" explains soft-delete via state machine is the enterprise standard for HR data, supported by lecturer guidance ("deletion might not be a good idea"), UK Working Time Regulations, and industry practice (SAP, Workday, BambooHR). The REST conventions table at the top of docs/04 explicitly acknowledges the verb selection with cross-references. |
| **Status** | ✅ JUSTIFIED IN DOCS (2026-08-29) — no code change needed, fully documented for report |

---

### GAP 5: Manager Team Requests — Date Range Filter — RESOLVED (via POST Search rework)

| Item | Detail |
|------|--------|
| **Brief says** | Manager action 1: "View outstanding requests for annual leave for their assigned members of staff **(could be enhanced with start and end dates to reduce the reporting period)**" |
| **What we had** | `GET /leave-requests/team` — no date range filtering. |
| **Resolution** | `POST /leave-requests/team/search` accepts `{"from": "2026-09-01", "to": "2026-09-30"}` in the request body, optionally combined with `{"status": "PENDING"}`. Also available on `POST /leave-requests/all/search` for admins. Implemented as part of the POST search migration (see GAP 3 above). |
| **Status** | ✅ IMPLEMENTED (2026-08-29) |

---

### Summary: Priority Order for Remaining Work

| Priority | Gap | Effort | Impact |
|----------|-----|--------|--------|
| 1 (RESOLVED) | ~~Postman collection~~ | ~~2-3 hours~~ | ✅ Implemented — 8 folders, 137 requests, all 26 endpoints covered with edge cases |
| 2 (MEDIUM) | **Prior learning reflection** | 1 hour writing | +3-5 marks across all sections |
| 3 (RESOLVED) | ~~Admin filtering params~~ | ~~10 minutes code~~ | ✅ Implemented — POST `/all/search` with staffMemberId/managerId filters |
| 4 (RESOLVED) | ~~PUT/DELETE justification in report~~ | ~~Already documented~~ | ✅ Fully justified in docs/04 section 13 |
| 5 (RESOLVED) | ~~Date range filter on /team~~ | ~~20 minutes code~~ | ✅ Implemented — POST `/team/search` with from/to date range |
| 6 (RESOLVED) | ~~Rate limit 429 Postman test~~ | ~~30 min~~ | ✅ Increased to 20/min, 429 test added to both collections, unit tests updated |
| 7 (BY DESIGN) | Allowance eventual consistency | — | ⚠️ Follows Lecture 7 Simpler Subscriber pattern. Documented trade-off |
| 8 (JUSTIFIED) | No HR role / two-stage approval | — | ⚠️ Admin acts as HR. Brief scopes for single developer. Documented |
| 9 (NOTED) | No pagination on list endpoints | — | ⚠️ Acceptable for prototype. Would need Spring Data Pageable for production |
| 10 (KNOWN) | Combined staff+manager filters | — | ⚠️ staffMemberId takes precedence. Deterministic, documented in code |

---

### GAP 6: Rate Limit (429) Demonstrated in Postman Collection — RESOLVED

| Item | Detail |
|------|--------|
| **What we had** | `RateLimitFilter` used Bucket4j token-bucket algorithm with 5 POST requests per IP per minute on `/auth/login`. Code worked, unit tests passed, but the Postman collection did not include a test triggering the 429 response. The limit of 5 also conflicted with the Postman collection flow — the 5 happy-path logins exhausted the bucket, causing edge case login tests to get 429 instead of expected 400/401. |
| **Fix applied** | (1) Increased `MAX_REQUESTS` from 5 to 20 per minute — a realistic production value (most APIs use 10-30 for login endpoints). Gives headroom for the collection runner (5 happy logins + edge case logins = ~11 total, well within 20). (2) Added a rate limit test to both Postman collections in folder 1 Edge Cases. The automated version uses a pre-request script (`pm.sendRequest` fires 10 dummy login requests to reach 20 total consumed, then the actual request is the 21st → 429). Asserts status 429, `"Rate limit exceeded"` message, and `"Too Many Requests"` error. (3) Updated `RateLimitFilterTest` — all 4 tests updated from 5→20 (shouldAllowFirst20Requests, shouldReturn429On21stRequest, shouldRateLimitPerIp, shouldUseXForwardedForHeader). |
| **Files changed** | `RateLimitFilter.java` (MAX_REQUESTS 5→20 + all Javadoc), `RateLimitFilterTest.java` (all loops and assertions 5→20), both Postman collections (+1 request each, now 138 total). |
| **Status** | ✅ RESOLVED (2026-08-31) |

---

### GAP 7: Allowance Updates Use Eventual Consistency (Async AFTER_COMMIT) — BY DESIGN

| Item | Detail |
|------|--------|
| **Observation** | All four leave allowance listeners (`LeaveRequestSubmittedListener`, `LeaveRequestApprovedListener`, `LeaveRequestRejectedListener`, `LeaveRequestCancelledListener`) use `@Async` + `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`. This means the leave request commits first, then the allowance update runs asynchronously in a separate transaction. If the allowance update fails (e.g., insufficient balance on `reserveDays`), the request is already persisted as PENDING but the reservation never happened. |
| **Why this is by design** | This follows Phil James's **Simpler Subscriber** pattern from Lecture 7. The case study code uses the same `@Async` + `AFTER_COMMIT` pattern for the `DeliveryAddressAddedEvent` listener. Two aggregates within the same bounded context communicate through local domain events rather than direct method calls, keeping each aggregate focused on its own invariants. |
| **Alternative** | Making the allowance update synchronous (`BEFORE_COMMIT` or direct method call within the same `@Transactional` service method) would guarantee atomicity — the request and allowance update succeed or fail together. However, this would (a) contradict the lecture's event-driven architecture, (b) couple the two aggregates, and (c) violate the DDD principle that one transaction should only modify one aggregate. |
| **Risk** | In practice, the over-booking invariant check on `reserveDays()` would only fail if the staff member submits requests faster than the async listener can process them (a race condition requiring sub-second concurrent submissions). For a university prototype this is acceptable. |
| **Mitigation** | The controller's `verifyNoDateOverlap()` check catches the most common double-booking scenario synchronously (before the command is dispatched). The async listener is a secondary safety net. |
| **Status** | ⚠️ KNOWN TRADE-OFF — deliberate architectural decision following lecture pattern |

---

### GAP 8: No Explicit HR Role or Two-Stage Approval — JUSTIFIED SIMPLIFICATION

| Item | Detail |
|------|--------|
| **Brief says** | "Your employer has stated that at this stage several things are required to be included — HR approval of specific staff requests, manager alerts re pending requests or staff alerts for approved/cancelled requests." |
| **What we have** | Three roles: STAFF, MANAGER, ADMIN. Single-stage approval: PENDING → APPROVED/REJECTED by the assigned manager or admin. Manager alerts via `ManagerNotificationEvent` → RabbitMQ. Staff alerts via `StaffNotificationEvent` → RabbitMQ. No separate HR role or `PENDING_MANAGER → PENDING_HR → APPROVED` workflow. |
| **Justification** | (1) The brief says "at this stage" and scopes requirements deliberately: "The following scenario limits the scope to certain core state requirements in order for it to be achievable by a single developer in this time window." (2) The ADMIN role in our system functions as HR — admins can approve/reject any request (bypassing the assigned manager check), amend entitlements, view all requests company-wide, and manage staff records. (3) Adding a fourth role and two-stage state machine would add significant complexity (extra database columns, additional state transitions, new RBAC rules, extra Postman tests) with minimal benefit for a prototype that already demonstrates the core patterns. (4) The brief explicitly asks for manager alerts and staff alerts (both implemented via RabbitMQ), with HR approval listed alongside them — suggesting it's one of several requirements, not the primary focus. |
| **If time permitted** | Add an `HR` role, a `PENDING_HR` state between `PENDING` and `APPROVED`, and a configurable rule (e.g., requests > 5 days require HR approval). The state machine infrastructure already supports additional states. |
| **Status** | ⚠️ JUSTIFIED SIMPLIFICATION — admin acts as HR, documented for report |

---

### GAP 9: No Pagination on List Endpoints — FUTURE ENHANCEMENT

| Item | Detail |
|------|--------|
| **Observation** | All list endpoints (`GET /staff`, `GET /leave-requests/all`, `GET /leave-allowances/all`, etc.) return every record in a single response. With 5 test users this is fine. In production with 10,000+ staff, returning all records would be slow and memory-heavy. |
| **What would be needed** | Spring Data supports pagination natively via `Pageable`. Controller methods would accept `?page=0&size=20` query parameters, repositories would return `Page<T>` instead of `List<T>`, and responses would include pagination metadata (totalPages, totalElements, currentPage). |
| **Why not implemented** | The brief does not mention pagination. The prototype operates with a small dataset where pagination adds complexity without value. The POST search endpoints already narrow results via filters, reducing the risk of large unfiltered result sets. |
| **Status** | ⚠️ NOTED — acceptable for prototype, would be required for production |

---

### GAP 10: Combined Staff + Manager Search Filters Not Genuinely Combined — KNOWN LIMITATION

| Item | Detail |
|------|--------|
| **Observation** | In `POST /leave-requests/all/search`, if an admin provides both `staffMemberId` and `managerId`, the query handler uses a three-tier if/else: staffMemberId takes precedence and managerId is silently ignored. They do not combine to mean "show requests where this staff member's requests are managed by this manager." |
| **Why this is acceptable** | (1) This is an unlikely use case — an admin searching by staff member already sees the manager in each result. (2) The code is deterministic and the behaviour is documented in comments. (3) Genuinely combining them would require 4-8 additional repository methods for a scenario that barely occurs. (4) The controller's `validateSearchCriteria()` already validates that at least one filter is provided. |
| **Status** | ⚠️ KNOWN LIMITATION — documented, deterministic behaviour |