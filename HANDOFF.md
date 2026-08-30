# Leave Booking System — Project Handoff

**Last Updated:** 2026-08-26
**Project:** COMP60047 Enterprise Application Development — Assignment 1 (Leave Booking System)
**Target Grade:** 95%+ (First Class)
**Scenario:** Scenario 1 — Leave Booking System
**Deadline:** 21 September 2026

---

## 1. Project Status

### Completed (13/14 tasks + design docs + environment setup)

| # | Task | Status |
|---|------|--------|
| 1 | Spring Boot project skeleton (pom.xml, modules) | ✅ |
| 2 | Common module (supertypes, events, outbox infrastructure) | ✅ |
| 3 | Leave Management domain layer (aggregates, VOs, enums, events) | ✅ |
| 4 | Leave Management infrastructure layer (JPA entities, repositories) | ✅ |
| 5 | Leave Management application layer (CQRS handlers, mappers, DTOs, commands) | ✅ |
| 6 | Leave Management UI layer (controllers, facade, @PreAuthorize) | ✅ |
| 7 | Staff Management supporting context (full CRUD + remote events) | ✅ |
| 8 | Identity module (Firebase auth + Spring Security RBAC) | ✅ |
| 9 | Local events (4 listeners: submitted/approved/rejected/cancelled → LeaveAllowance) | ✅ |
| 10 | Remote events via RabbitMQ (StaffMemberAdded/Updated → Leave Management) | ✅ |
| 11 | Security features (rate limiting, header obfuscation, access logging) | ✅ |
| 12 | Unit tests (321 tests, 0 failures — domain, VOs, aggregates, mappers) | ✅ |
| 13 | Integration tests (@DataJpaTest, 14 tests, 0 failures) | Done |
| 14 | Postman/API test collections | ⬜ |

### Additional completed work
- ✅ All 5 design docs (01-05) enhanced with PlantUML diagrams, JavaDoc code snippets, lecture references, how-to-run
- ✅ HOW-IT-WORKS.md — full system walkthrough
- ✅ docs/06-issues-and-fixes.md — all issues documented with fixes
- ✅ Firebase registration + login working (201 Created, 200 OK with JWT confirmed)
- ✅ RabbitMQ connected via local Docker
- ✅ Clean code refactoring (dispatchAndClear helper, extracted SubmitLeaveRequestBody, @Valid)
- ✅ All compilation issues resolved (Lombok, H2, JDK, Firebase SDK version)
- ✅ App starts and runs end-to-end (Postman tested: register + login working)

---

## 2. IMMEDIATE NEXT TASK — Postman Collections (Task 14)

### Problem encountered
`@SpringBootTest` loads the full context including Firebase and RabbitMQ which causes context loading failures because:
- `FirebaseConfig` tries to load `serviceAccountKey.json` and connect to Google OAuth2
- `CustomMessageConverter` depends on RabbitMQ beans
- `RemoteOutboxListener` depends on `RabbitTemplate`

### What was already tried
- Added `@Profile("!test")` to `FirebaseConfig` and `SecurityConfig`
- Created `TestSecurityConfig` with `@Primary` mocked beans
- Excluded `RabbitAutoConfiguration` via properties
- None of these fully resolved the context loading — other transitive beans still fail

### Recommended approach for next session
Use **`@DataJpaTest`** + **`@Import`** instead of `@SpringBootTest`:

```java
@DataJpaTest  // Only loads JPA layer (repos, entities, H2)
@Import({
    LeaveRequestApplicationService.class,
    LeaveAllowanceApplicationService.class,
    DomainEventManager.class,
    EventStoreService.class,
    // Listeners for local events:
    LeaveRequestSubmittedListener.class,
    LeaveRequestApprovedListener.class,
    LeaveRequestRejectedListener.class,
    LeaveRequestCancelledListener.class
})
@ActiveProfiles("test")
class LeaveRequestIntegrationTest { ... }
```

This avoids loading Firebase, Security, RabbitMQ, Controllers, or any web layer. It only tests:
- ApplicationService → Domain → Repository → H2
- Local events → Listeners → AllowanceService → Repository

### Files already created (need fixing)
- `src/test/java/com/staffs/leavebooking/integration/LeaveRequestIntegrationTest.java` — 9 test methods written, context won't load
- `src/test/java/com/staffs/leavebooking/integration/TestSecurityConfig.java` — mocked beans config
- `src/test/resources/application-test.yaml` — test-specific H2 config

### Changes made to main code for testing
- `@Profile("!test")` added to `FirebaseConfig.java` and `SecurityConfig.java`
- These DO NOT affect production — only excluded when running with `--spring.profiles.active=test`
- Verify with `mvn test` (unit tests use no profile, so they still pass)

---

## 3. After Integration Tests — Remaining Tasks

### Task 14: Postman Collections
- Export JSON files for all endpoint collections (Identity, Leave Requests, Leave Allowances, Staff)
- Include pre-request scripts for JWT token management
- Include valid + invalid test cases per endpoint
- Include per-role testing (STAFF/MANAGER/ADMIN)

### Final polish
- Re-render all PlantUML diagrams as PNG images for the report
- Verify all 335 tests still pass after integration test changes
- Update HANDOFF.md one final time

---

## 4. Technology Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 21 | Language (IntelliJ SDK must be JDK 21, NOT 25) |
| Spring Boot | 3.4.1 | Application framework |
| Spring Modulith | 1.3.1 | Module boundary enforcement |
| Spring Data JPA | (via Boot) | Persistence |
| H2 Database | (via Boot) | In-memory DB for dev/testing |
| Spring Security | (via Boot) | Authentication & authorisation |
| Spring OAuth2 Resource Server | (via Boot) | JWT validation |
| Spring AMQP | (via Boot) | RabbitMQ integration |
| Firebase Admin SDK | 9.7.0 | Cloud user management (upgraded from 9.4.2) |
| Lombok | 1.18.42 | Boilerplate reduction (upgraded for JDK 21 compat) |
| Bucket4j | 8.10.1 | Rate limiting |
| Spring Retry | (via Boot) | Outbox retry mechanism |
| Docker | (local) | RabbitMQ container |
| Maven | (wrapper) | Build tool |

---

## 5. Environment Setup (REQUIRED before running)

### Docker RabbitMQ
```bash
docker start leave-rabbitmq
# OR if container doesn't exist:
docker run -d --name leave-rabbitmq -p 5672:5672 -p 15672:15672 \
  -e RABBITMQ_DEFAULT_USER=guest -e RABBITMQ_DEFAULT_PASS=guest rabbitmq:management
```

RabbitMQ Manager: http://localhost:15672 (guest/guest)

**No manual queue/exchange setup required.** `RabbitInfrastructureConfig.java` declares all infrastructure as Spring beans — Spring AMQP auto-creates them on the broker at startup.

Auto-provisioned infrastructure:
- Exchange `staff-management` (topic) with bindings to:
  - Queue `leave-management.staff-member-added` (key: `staff.member.added`)
  - Queue `leave-management.staff-member-updated` (key: `staff.member.updated`)
- Exchange `leave-notifications` (topic) with bindings to:
  - Queue `notifications.manager-pending-request` (key: `notification.manager.pending`)
  - Queue `notifications.staff-request-decided` (key: `notification.staff.decided`)

### Firebase
- `serviceAccountKey.json` in `src/main/resources/` (gitignored)
- Web API key in `application.yaml` under `firebase.web-api-key`
- `FirebaseConfig` uses `Windows-ROOT` KeyStore for SSL (corporate Zscaler compatibility)

### IntelliJ
- Project SDK: **JDK 21** (NOT 25 — Spring Boot 3.4.1 doesn't support JDK 25)
- VM Options: clear (no special options needed)
- Lombok plugin installed + annotation processing enabled

### Running
- `mvn spring-boot:run` or IntelliJ Run button
- Server: http://localhost:8900
- H2 Console: http://localhost:8900/h2-console (JDBC URL: `jdbc:h2:mem:leavebooking`, user: `sa`, no password)

### Testing
- Unit tests: `mvn test` (335 tests, ~3 seconds, no external deps needed)
- Manual testing: Postman with JWT from /auth/login

---

## 6. Key Files Reference

| File | Purpose |
|------|---------|
| `HANDOFF.md` | This file — project status for session continuity |
| `HOW-IT-WORKS.md` | Full system walkthrough with code explanations |
| `docs/01-domain-model-design.md` | Aggregates, VOs, state machines, invariants |
| `docs/02-event-architecture-design.md` | Local + remote events, outbox, listeners |
| `docs/03-database-schema-design.md` | ERD, data dictionary, schema.sql, JPA |
| `docs/04-api-endpoint-design.md` | All endpoints, RBAC, curl examples, Postman |
| `docs/05-folder-structure-design.md` | Architecture, layers, module visibility |
| `docs/06-issues-and-fixes.md` | Every issue and fix (encoding may need cleanup) |
| `src/main/resources/application.yaml` | All runtime config |
| `src/test/resources/application-test.yaml` | Test profile config |
| `pom.xml` | Dependencies (Firebase 9.7.0, Lombok 1.18.42) |

---

## 7. Known Issues

1. **docs/06-issues-and-fixes.md has encoding corruption** — the PowerShell write introduced garbled UTF-8. Needs to be rewritten cleanly in IntelliJ (content is correct, just rendering issues with special characters).

2. **Integration tests don't load context** — `@SpringBootTest` approach failed. Need to switch to `@DataJpaTest` + `@Import` approach (see Section 2 above).

3. **`@Profile("!test")` on FirebaseConfig and SecurityConfig** — verify this doesn't break the normal app run (it shouldn't — only activates when profile "test" is explicitly set).

---

## 8. Lecture Material Reference

The handoff document for all lecture content is:
`handoff_enterprise_app_dev_lectures1_2_3_4_5_6_7_8_9_2026-08-21 (1).md`

Key mappings:
- Lectures 1-2: Spring Boot, DDD, Entities, Value Objects, DomainAssertions
- Lecture 3: Aggregates, invariants
- Lecture 4: Records, DTOs, Data Mappers, Modulith, Facade, Shared Kernel
- Lecture 5: CQRS Queries
- Lecture 6: CQRS Commands
- Lecture 7: Local Domain Events
- Lecture 8: Remote Events (RabbitMQ, Outbox)
- Lecture 9: Identity (Firebase, Spring Security, JWT, @PreAuthorize)
