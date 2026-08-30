# Handoff Document
**Generated:** 2026-08-21
**Chat Topic:** Enterprise Application Development — Lectures 1, 2, 3, 4, 5, 6, 7, 8 & 9 (Spring Boot, Repository Pattern, DDD Overview, Entities & Value Objects, Aggregates, Records/DTOs/Modulith — CQRS preparation, CQRS: Queries, CQRS: Commands, Local Domain Events, Working with Remote Events, and Identity and Access Management)

---

## 1. Objective

Raf is a software engineering apprentice at BT on the DTS (Digital and Technology Solutions) standard working towards his End Point Assessment (EPA). He is studying a university module called **Enterprise Application Development**, delivered by lecturer **Phil James** at **Staffordshire University**. The goal of these chats is to load lecture material PDF by PDF across multiple chats so that Claude has full context of the module content — this context will be used to assist with understanding, assignments, and KSB evidence mapping. **No detail should ever be condensed or summarised** — full granularity must be preserved in every handoff.

---

## 2. Current Status

- **Phase:** Loading lecture material — Lectures 1, 2, 3, 4, 5, 6, 7, 8 & 9 complete
- **Progress:** Lecture 1 (Spring Boot, Repository Pattern), Lecture 2 (DDD Overview, Entities & Value Objects), Lecture 3 (Aggregates), Lecture 4 (Preparation and considerations before the discussion of CQRS — records vs classes, DTO pattern, Data Mapper pattern, Monolith vs Modulith, Façade, Open Host Service, Shared Kernel), Lecture 5 (CQRS: Queries — new Spring Modulith project setup, schema.sql/data.sql, custom exceptions, GlobalExceptionHandler, the CQRS pattern, three full query walkthroughs traced through every layer, Service Layer pattern, CQRS-vs-Service-Layer comparison, and PlantUML appendices), and Lecture 6 (CQRS: Commands — the restaurant module added, extended schema/data with the Restaurant context, OrderRestaurantNotFoundException, the CQRS command/write-model theory, the OrderRestaurant value object, OrderJpa, and one complete command flow — *Adding a new order* — traced Controller → ContextFacade → OrderApplicationService → repositories/mappers → domain aggregate → save, with sequence-diagram and Postman appendices) fully loaded. No tasks actioned yet — Raf is building up context across chats before working on anything specific. Lecture 6 is the CQRS **Commands** side that Lecture 5 deferred; it implements only **one** command (add-new-order) and signposts that the remaining Ordering-context write operations happen **via events**, to be covered in the **next handout**. Lecture 7 (**Local Domain Events**) is that next handout: it delivers the event-driven mechanism, building the *DeliveryAddressAddedEvent* end-to-end within the Ordering context (Order aggregate raises it → Spring `@TransactionalEventListener` → `BuyerApplicationService` → `Buyer` aggregate keeps a de-duplicated `Set` of delivery addresses). It is scoped to **local** events only and explicitly signposts a future **remote events** lecture (`RemoteEvent`, message brokers, eventual consistency). Lecture 8 (**Working with Remote Events**) is that remote-events lecture: it takes the cross-context `NewRestaurantAddedEvent` (a `RemoteEvent`) from the **Restaurant** context, saves it to a local `event_store` (status machine PENDING→PUBLISHED/FAILED/UNROUTABLE + retry_count), publishes it **after commit** on an `@Async` thread with retry via a **`RemoteOutboxListener`** to **CloudAMQP/RabbitMQ** (a `restaurant` direct exchange → `newRestaurantKey` → `newRestaurant` queue), and consumes it in the **Ordering** context via a **`@RabbitListener`** (`NewRestaurantAddedListener` → stub `OrderRestaurantApplicationService`), with a `CustomMessageConverter` (trusted-packages `"*"`) doing the JSON↔record mapping. It also reorganises the `common` module into `domain`/`dto`/`events` sub-packages via `@ApplicationModule(type = OPEN)`. This is the **Remote Subscriber** pattern (broker, network boundary, outbox, eventual consistency) that Lecture 7 previewed but did not implement. Lecture 9 (**Identity and Access Management**) delivers the module's **third learning outcome** — *"Implement an enterprise application that includes relevant security features"* — via a new **Identity** bounded context (explicitly **not** domain-driven; no domain folder/aggregates). It uses **Google Firebase** for cloud-based user authentication (email/password sign-in provider, Firebase Admin SDK 9.10.0) and **Spring Security** (+ OAuth2 Resource Server) for role-based authorisation. The implementation covers: JWT theory recap (RFC 7519, JWS signed tokens, Header/Payload/Signature, IETF best-practice warnings); Firebase project setup (service account key JSON, web API key); the full Identity module folder structure (`authService`/`dto`/`security` sub-packages + `AuthController`); `FirebaseConfig` (three `@Bean`s: `FirebaseApp`, `FirebaseAuth`, `JwtDecoder` with Nimbus JWKS + issuer/audience validation); `Role` enum with `ROLE_` prefix and `@JsonCreator fromString`; `FirebaseJwtAuthenticationConverter` (`@Component`, converts JWT custom claims to `GrantedAuthority`); `SecurityConfig` (`@EnableWebSecurity`, `@EnableMethodSecurity`, CSRF disabled, `/auth/**` permitted, all other endpoints authenticated, OAuth2 resource server with custom JWT converter); `FirebaseTokenFilter` (`OncePerRequestFilter`, extracts Bearer token, `FirebaseAuth.verifyIdToken`, sets `SecurityContextHolder`); registration flow (`RegisterRequest` record → `AuthController.register` → `FirebaseAuthService.registerUser` → Firebase `CreateRequest` + custom claims `{role, admin:false}` → `RegisterResponse` record); login flow (`LoginRequest` record → `AuthController.login` → `FirebaseAuthService.loginUser` via Firebase REST Identity Toolkit API → `LoginResponse` record with `@JsonProperty` mappings); `ErrorResponse` record; Postman testing (Scripts tab storing `jwt_admin_token` via `pm.globals.set`, collection-level and test-level Bearer Token setup); the `roleCheck` endpoint (`@PreAuthorize("isAuthenticated()")`, `@GetMapping("/role-check\`")`); and **role-based access control** applied to the `RestaurantContextFacade` and `ContextFacade` (ordering) via `@PreAuthorize("hasAnyRole(…)")` annotations at façade method level. The `GlobalExceptionHandler` gains a `FirebaseAuthException` handler.

---

## 3. What Was Completed

- Loaded and read **three PDF documents** from Lecture 1 of the Enterprise Application Development module:
  1. `Enterprise_Application_Development_-_Introduction.pdf` — covers what enterprise applications are, Spring Framework, Spring Boot, and Beans.
  2. `Enterprise_Application_Development_-_First_Application.pdf` (titled "Developing a Simple API") — step-by-step guide to building a REST API using Spring Boot with H2, Maven, JPA, Lombok, and a REST controller. Covers full CRUD.
  3. `Repository_Pattern.pdf` — standalone one-page handout defining the Repository Pattern from Fowler's Patterns of Enterprise Architecture.

- Loaded and read **two PDF documents** from Lecture 2 of the Enterprise Application Development module:
  1. `Enterprise_Application_Development_-_Domain_Driven_Design_part_1_Overview.pdf` — covers DDD introduction, domain models, ubiquitous language, bounded contexts, solution architectures (monolith/modulith/microservices), declarative vs reactive systems, problem space vs solution space, layered architecture, and the core domain model table.
  2. `Enterprise_Application_Development_-_Entities_and_Value_Objects.pdf` — covers entities, value objects, DomainAssertions utility class, precondition guards, Identity value object, ValueObject/Entity supertypes, FullName/Address value objects, Person entity, persistence vs domain models, unit testing principles (AAA pattern, properties/pillars of good unit tests), full test code for Identity, FullName, and Person.

- Loaded and read **one PDF document** from Lecture 3 of the Enterprise Application Development module:
  1. `Enterprise_Application_Development_-_Aggregates.pdf` — covers what an aggregate is, the aggregate root concept, the Buyer and Order aggregates in an Ordering bounded context, value objects (ExpirationDate, PaymentMethod, Money, OrderItem), enums (OrderStatus), invariants (business rules), domain rules (Strategy pattern injection), a PlantUML of the full Ordering context, and a fictional online restaurant ordering system example.

- Loaded and read **one PDF document** from Lecture 4 of the Enterprise Application Development module:
  1. `Enterprise_Application_Development_-_Preparation_and_considerations_before_the_discussion_of_CQRS_1_.pdf` (22 pages, authored by Phil James, created 26/06/2026) — covers Java records vs classes (including the refactor of `ValueObject` from a class to an interface, the record version of `FullName`, compact constructors, `@Embeddable`/`@Embedded` on JPA entities, the SQL naming-convention issue, and a summary of when to use each), the DTO pattern (Fowler and Esposito definitions, Figures 1 & 2, DTO vs Entity comparison table, points for/against, middle ground, `FullNameDTO` and `BuyerDTO` code), the Data Mapper pattern (`BuyerJpaToDTOMapper`), Monolith vs Modular Monolith (Modulith) with both full folder structures, the Façade pattern (and why the façade is public but controllers private), the Open Host Service pattern (`BuyerController` and `ContextFacade` code), and the Shared Kernel pattern.

- Loaded and read **one PDF document** from Lecture 5 of the Enterprise Application Development module:
  1. `Enterprise_Application_Development_-_CQRS_-_Queries.pdf` (42 pages, authored by Phil James) — the CQRS lecture that Lecture 4 prepared for, covering the **Queries** side only. Covers: the new-project setup in IntelliJ via start.spring.io (Maven, Java 26, Spring Modulith + DevTools + Lombok + Spring Web + Spring Data JPA + H2, `spring-boot-starter-validation` added to pom.xml), `application.yaml` (`ddl-auto: none`, port 8900), a reminder of the five bounded contexts (Order/Identity/Restaurant/Kitchen/Delivery) with Figure 1, the Spring Modulith folder structure (adding a `dto` sub-item vs Lecture 4), custom exceptions (`BuyerNotFoundException`, `OrderNotFoundException`) and the expanded `GlobalExceptionHandler`, the ordering schema (ERD Figure 2, Crow's Foot legend Figure 3, full `schema.sql` and `data.sql`), the H2 in-memory datasource config, the CQRS architectural pattern (Vaughn Vernon and Bertrand Meyer definitions, Figures 4/5/6), the `Buyer` aggregate constructor, three complete query walkthroughs each traced Controller → ContextFacade → QueryHandler → Repository → Mapper → Jpa with sequence diagrams and Postman outputs (Find All Buyers, Find Buyer By Id, Find Payment Details for Buyer Id), the Order-endpoints review exercise, the Service Layer pattern (Fowler), the CQRS-vs-Service-Layer comparison, and four PlantUML appendices (ERD + three sequence diagrams).

- Loaded and read **one PDF document** from Lecture 6 of the Enterprise Application Development module:
  1. `Enterprise_Application_Development_-_CQRS_-_Commands.pdf` (32 pages, authored by Phil James / "Philip James" in metadata, created 16/07/2026) — the **Commands** counterpart to Lecture 5's Queries. Covers: the reminder of the five bounded contexts (Figure 1) now featuring the **Restaurant** context; the Spring Modulith folder structure now with **three** modules (common, ordering, **restaurant**) and the full modulith reminder for both ordering and restaurant; the new custom exception `OrderRestaurantNotFoundException` and the further-expanded `GlobalExceptionHandler`; the **extended schema** (ERD Figure 2; `buyer`/`order_from_user` PKs now `VARCHAR(36)`; new `order_restaurant`, `restaurant`, `menu_item` tables) and **extended `data.sql`** (restaurant-context seed rows); the CQRS architectural pattern restated (Vaughn Vernon + Bertrand Meyer's Command-Query Separation, Figures 4/5/6) plus *More about CQRS* (the "full-fat" separated-DB version and why it's optional); why `order_restaurant` exists (no cross-context joins; populated via `NewRestaurantAddedEvent`); the `OrderRestaurant` value **record** (`@Embeddable`, compact constructor, `implements ValueObject`); `OrderJpa` with the two restaurant design choices (snapshot vs retrieve); the effect on last week's query (Find Order 1111 JSON now embeds the `restaurant` block); and **one complete command walkthrough — *Adding a new order*** — the `AddNewOrderCommand` record, the Postman `POST /orders` payload, and every class in the flow (`OrderController`, `ContextFacade`, `OrderApplicationService`, `OrderRestaurantJpaToDomainMapper`, `OrderRestaurantJpa`, `OrderRestaurantRepository`, `OrderDTOToDomain`, `OrderDomainToJpaMapper`) with full code, plus PlantUML appendices for the ERD and the new-order sequence diagram. Only **one** command is implemented; the rest are said to happen **via events** (next handout).

- Loaded and read **one PDF document** from Lecture 7 of the Enterprise Application Development module:
  1. `Enterprise_Application_Development_-_Working_with_local_events.pdf` (33 pages, title "Local Domain Events", authored by Phil James / "JAMES Phillip" in metadata, created 24/07/2026) — the **Local Domain Events** handout, the events lecture Lecture 6 deferred to. Covers: the declarative-vs-reactive reminder (from Lecture 2) and the Domain Model Operations table; the current OrderController/BuyerController endpoint inventory (and why there was **no `BuyerApplicationService`** until now); **what a domain event is** (Evans p.20 quote; local vs foreign/integration events; command *present-tense/may-be-rejected* vs event *past-tense/has-happened*; `AddNewOrderCommand` vs `DeliveryAddressAddedEvent`); domain vs **application** vs **infrastructure** events; the Buyer-aggregate amendment (keep a de-duplicated `Set<DeliveryAddress>`); Vaughn's *Aggregates create Events and publish them* diagram with the **Store-and-Forward (Simpler Subscriber)** / **Immediate Forwarding** / **Remote Subscriber** patterns (XA/2PC + eventual consistency previewed); the **`AggregateRoot` interface → abstract class** promotion (now holds `List<Event>`, with add/remove/list/clear/exists methods); the `Event` / `LocalEvent` (/ future `RemoteEvent`) marker interfaces; the `Order` split into `orderOf` (read, no event) vs **`OrderOfWithEvent`** (write, raises event) factories with a private constructor; the `DeliveryAddressAddedEvent` record; the amended `OrderApplicationService.addNewOrder` (now injects `DomainEventManager`, dispatches + clears events after save, same `@Transactional`); **`DomainEventManager`** (`ApplicationEventPublisher` + `EventStoreService`); `EventStoreService`/`EventStoreJpa`/`EventStoreRepository`; the appended **`schema.sql`** (`event_store` + Spring Modulith's manually-created **`event_publication`** registry table); **`DeliveryAddressAddedListener`** (`@Component`, `@Async`, `@TransactionalEventListener(AFTER_COMMIT)`); the new **`DeliveryAddress`** identifiable value **record** (`implements IdentifiedValueObject`, surrogate `Long id`, compact + overloaded constructors, custom id-omitting equals/hashCode); `DeliveryAddressAddedDomainEventMapper`; the first **`BuyerApplicationService.updateDeliveryAddresses`**; the amended **`Buyer`** aggregate (`extends AggregateRoot<Buyer>`, new `savedDeliveryAddresses` `HashSet`, `addSavedDeliveryAddress`, `retrieveAllSavedDeliveryAddresses`); **`BuyerJpa`** (new `@OneToMany Set<DeliveryAddressJpa>`); **`DeliveryAddressJpa`** (the `HashSet` + generated-id **hash-mutation pitfall** and the constant-`hashCode` fix, foojay source); **`BuyerToJpaMapper`** (persists only the null-id/new address); the **Testing That It Works** Postman walkthrough (GET one address → POST new order → GET two addresses; re-posting the same order does **not** duplicate); and the new read path (`BuyerController`/`ContextFacade`/`BuyerQueryHandler`/`BuyerJpaToDTOMapper` for `GET /buyers/{id}/deliveryAddress`). **No PlantUML appendices, no Academic Disclosure, and no Maths/English/Digital-Skills block** in this handout (unlike Lectures 5 & 6).

- Loaded and read **one PDF document** from Lecture 9 of the Enterprise Application Development module:
  1. `Enterprise_Application_Development_-_Identity_and_Access_Management.pdf` (38 pages, title "Identity and Access Management", authored by Phil James / "Philip James" in metadata, created 8 August 2026) — the **Identity and Access Management** handout, delivering the module's **third learning outcome** ("Implement an enterprise application that includes relevant security features"). Covers: **JWT theory recap** (RFC 7519, JWS signed tokens, Header/Payload/Signature structure, a verbatim example signed token from the case study, the three JWT use cases — Authentication/Authorisation/Information Exchange, the `Authorization: Bearer <token>` header, IETF RFC 8725 best-practice warning on weak symmetric keys, the Langkemper attack reference); **decoded payload figure** (jwt.io screenshot showing `role`, `surname`, `first_name`, `id`, `email`, `username`, `sub`, `iat`, `exp` claims); **creating an Identity context** (explicitly **not** DDD — no domain folder, no aggregates; identity is a *supporting* context; greenfield design using a cloud provider for authentication while the app handles RBAC); **Google Firebase setup walkthrough** (create project "case-study-example" in Firebase console, enable Google Analytics, add a Web App with nickname matching the Spring project's top-line package, navigate to Settings → Service Accounts → generate new private key JSON, rename to `serviceAcccountKey.json` [sic — triple 'c'] in the project's `resources` folder, retrieve the web API key from Settings → General → SDK setup and configuration → Config tab); **`application.yaml` amendment** (adds `firebase.web-api-key` property); **Spring Security** introduction (the de-facto standard for Spring authentication/authorization, features: authentication + authorization, protection against session fixation / clickjacking / CSRF, Servlet API integration, optional Spring MVC integration; links to `spring.io/projects/spring-security` and GeeksforGeeks tutorial); **`pom.xml` amendment** (add Spring Security + OAuth2 Authorization Server starters via IntelliJ "Add Starters", plus the `com.google.firebase:firebase-admin:9.10.0` dependency; note that adding Spring Security immediately mandates authentication on all endpoints — **least privilege** — returning 401 until `SecurityConfig` permits specific paths); **Firebase Authentication setup** (Security → Authentication → Sign-in method → enable Email/Password; note: better methods exist, this is illustrative); **Identity module folder structure** (verbatim tree: `identity/authService/{FirebaseAuthService, FirebaseConfig, FirebaseTokenFilter}`, `identity/dto/{ErrorResponse, LoginRequest, LoginResponse, RegisterRequest, RegisterResponse}`, `identity/security/{FirebaseJwtAuthenticationConverter, Role, SecurityConfig}`, `identity/AuthController`); **`FirebaseConfig`** (`@Configuration @Slf4j`, three `@Bean`s — `firebaseApp()` reads `serviceAcccountKey.json` via `ClassPathResource`, extracts `projectId` from `ServiceAccountCredentials`, builds `FirebaseOptions`, initialises the `FirebaseApp` singleton with idempotency check; `firebaseAuth(FirebaseApp)` via `FirebaseAuth.getInstance`; `jwtDecoder(FirebaseApp)` builds a `NimbusJwtDecoder` from Google's JWKS endpoint `https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com`, creates a combined `OAuth2TokenValidator` checking issuer `https://securetoken.google.com/{projectId}` + audience contains `projectId`, sets it on the decoder); **`Role`** enum (`USER`, `MANAGER`, `ADMIN`; `PREFIX = "ROLE_"`; `getAuthority()` returns `ROLE_ + name()`; `@JsonCreator fromString(String)` with null/blank/invalid guards; teaching note: Spring Security differentiates authorities starting with `ROLE_` as roles vs fine-grained authorities like `READ_PRIVILEGE`; `@PreAuthorize("hasRole('ADMIN')")` usage); **`FirebaseJwtAuthenticationConverter`** (`@Component`, implements `Converter<Jwt, AbstractAuthenticationToken>`, extracts the `"role"` custom claim via `jwt.getClaimAsString("role")`, wraps it in a `SimpleGrantedAuthority`, returns a `JwtAuthenticationToken` with the raw JWT, authorities, and `jwt.getSubject()` as principal); **`SecurityConfig`** (`@Configuration @EnableWebSecurity @EnableMethodSecurity @AllArgsConstructor`, injects the `Converter<Jwt, AbstractAuthenticationToken>`, defines `SecurityFilterChain filterChain(HttpSecurity)` — CSRF disabled (JWTs are stateless), `/auth/**` permitted, all other requests authenticated, OAuth2 resource server with custom JWT converter); **`FirebaseTokenFilter`** (`@Component @Slf4j`, extends `OncePerRequestFilter`, extracts `Authorization` header, strips `Bearer ` prefix, calls `FirebaseAuth.getInstance().verifyIdToken(token)` for cryptographic signature + expiration validation, extracts `"role"` claim, builds `UsernamePasswordAuthenticationToken(uid, null, authorities)` — three-arg constructor sets `authenticated = true` — stores in `SecurityContextHolder`; on `FirebaseAuthException` clears context and returns 401); **Registering a new admin account** (Postman screenshot: `POST http://localhost:8900/auth/register` with JSON body `{username: "admin1", email: "admin@email.com", password: "password123", role: "ADMIN"}`; figure caption: "clearly we need something more robust for admin"; note to add further tests for USER and MANAGER); **`RegisterRequest`** record (`String username, email, password, role`; `// add validation` comment); **`AuthController` — part 1: register** (`@RestController @RequestMapping("/auth") @AllArgsConstructor @Slf4j`; `USER_CREATED_CONFIRMATION` constant; injects `FirebaseAuthService`; `@PostMapping("/register")` takes `@RequestBody RegisterRequest`, delegates to `firebaseAuthService.registerUser(username, email, password, role)`, wraps the returned `UserRecord` in a `RegisterResponse(uid, email, displayName, message)`, returns `201 CREATED`; note: no façade used in this module, module should not follow DDD); **`FirebaseAuthService`** (`@Service @Slf4j`; injects `FirebaseAuth`; creates `RestClient`; reads `firebase.web-api-key` via `@Value`; `registerUser` builds a `CreateRequest` (Firebase SDK class) with email/password/displayName/emailVerified=false, calls `firebaseAuth.createUser`, validates role via `Role.fromString(role).getAuthority()` or defaults to `Role.USER.name()`, sets custom claims `Map.of("role", role, "admin", false)` via `firebaseAuth.setCustomUserClaims`, returns `UserRecord`; note: `CreateRequest` and `UserRecord` are Firebase SDK classes); **`RegisterResponse`** record (`String uid, email, username, message`; `// add validation`; Postman response: 201 Created with `{uid, email, username, message: "User created successfully"}`); **`ErrorResponse`** record (`String error, message`; compact constructor `ErrorResponse(String error) { this(error, null); }`; Postman response: 400 Bad Request with `{error: "Bad Request", message: "The user with the provided email already exists (EMAIL_EXISTS).", status: 400, timestamp: "2026-08-06T11:05:09.974780900Z"}`); **Testing the Code** (Postman screenshots of register request/response in Identity collection); **`AuthController` — part 2: login** (`@PostMapping("/login")` takes `@RequestBody LoginRequest`, delegates to `firebaseAuthService.loginUser(emailOrUsername, password)`, returns `200 OK` with `LoginResponse`); **`LoginRequest`** record (`String emailOrUsername, password`; `// add validation`; Postman: `POST http://localhost:8900/auth/login` with `{emailOrUsername: "admin@email.com", password: "password123"}`); **`FirebaseAuthService` — part 2: `loginUser`** (null check on email/password; calls Firebase REST Identity Toolkit API `https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key={firebaseApiKey}` with `Map.of("email", email, "password", password, "returnSecureToken", true)` via `RestClient.post()`, returns `LoginResponse.class`; catches `HttpClientErrorException`); **`LoginResponse`** record (`@JsonProperty("localId") String uid`, `String email`, `@JsonProperty("displayName") String username`, `@JsonProperty("idToken") String accessToken`, `String refreshToken`, `@JsonProperty("expiresIn") String expiresInSeconds`; `// add validation`; Postman sample: 200 OK, 1.68 KB, fields `localId`/`email`/`displayName`/`idToken` (long JWT)/`refreshToken`/`expiresIn: "3600"`); **Testing — Before We Go Any Further** (since Spring Security locks down all endpoints except `/auth/**`, JWT must be passed to Restaurant/Ordering requests; Postman Scripts tab on login: JavaScript extracts `response.idToken || response.token` and stores it as `pm.globals.set("jwt_admin_token", token)` — note: `pm.globals` not `pm.collections` for cross-collection visibility; collection-level auth: Restaurant collection → Authorization tab → Bearer Token → `{{jwt_admin_token}}`; per-test auth: individual test → Authorization tab → Bearer Token → `{{jwt_admin_token}}`; same approach for Ordering); **Applying Role Based Access to the end points** — `AuthController` (identity): new `@PreAuthorize("isAuthenticated()") @GetMapping("/role-check\`")` endpoint (the path contains a stray backtick), injects `Authentication`, streams `getAuthorities()` → `GrantedAuthority::getAuthority` → joins with comma → returns `"{roles} access granted"`; `RestaurantContextFacade` (restaurant): `findAllRestaurants` and `findRestaurantById` → `@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")`, `addNewRestaurant` and `updateMenu` → `@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")`; teaching note: could use `@PreAuthorize("isAuthenticated()")` instead for endpoints all roles share, and/or give admin users multiple authorities (admin = admin + manager + user) so they inherit lower-role access; `ContextFacade` (ordering): `findAllBuyers` → `@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")`, `findBuyerById`/`findDeliveryAddressesForBuyerById`/`findPaymentsForBuyerById`/`findOrdersByBuyerId`/`findOrderByOrderId` → `@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")`, `addNewOrder` → `@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")`; **`GlobalExceptionHandler` (modify)**: adds `@ExceptionHandler(FirebaseAuthException.class)` returning `HttpStatus.BAD_REQUEST` with `ex.getMessage()`. No PlantUML appendices, no Academic Disclosure, and no Maths/English/Digital-Skills block in this handout. Seventeen embedded hyperlinks confirmed via PyMuPDF (auth0.com JWT claims p.3; auth0.com ID tokens, OpenID Connect specs, jwt.io/introduction, auth0.com JSON Web Tokens, jwt.io ×2 all on p.4; Medium Badamchi article ×2, IETF RFC 8725 §2.2, RFC 8725 weak-symmetric-keys, Langkemper attack, RFC 8725 do-not-trust-received-claims, RFC 7518 §8.8 all on p.5; Firebase console p.7; Spring Security project p.14; GeeksforGeeks Spring Security tutorial p.14).

- Loaded and read **one PDF document** from Lecture 8 of the Enterprise Application Development module:
  1. `Enterprise_Application_Development_-_Working_with_remote_events.pdf` (36 pages, title "Working with Remote Events", authored by Phil James / "JAMES Phillip" in metadata, created 24 July 2026 — same date as Lecture 7) — the **Remote Events** handout, the remote-events lecture Lecture 7 repeatedly signposted. Covers: the **`common` module reorganisation** into `domain`/`dto`/`events` sub-packages enabled by `@ApplicationModule(type = OPEN)` on `common`'s `package-info.java` (with the full re-organised folder tree); a reminder of the **CQRS read/write diagram** (Figure 1, highlighting the *Event (all) Subscriber*); the verbatim **Commands vs Events** comparison table (Table 1); a reminder of the two bounded contexts (Restaurant, Ordering) with the `Restaurant`/`Buyer`/`Order` aggregate field snippets; **CloudAMQP/AMQP** theory (broker key terms — producer/consumer/exchange/queue; the four exchange routing types — direct/topic/fanout/headers; point-to-point vs publish/subscribe); a step-by-step **CloudAMQP + LavinMQ Manager GUI walkthrough** (create a free "Loyal Lemming" instance on AWS eu-west-1; read the AMQP details host `seal-01.lmq.cloudamqp.com` / user+vhost `kssfwiov`; create a `restaurant` **Direct** exchange, a `newRestaurant` queue, and bind them with routing key `newRestaurantKey`); the modified **`application.yaml`** (Spring `rabbitmq.*` connection config + the custom `rabbitmq.outbox.bindings` FQCN→{exchange,routing-key} map); the **Spring for RabbitMQ dependency**; `@EnableRabbit`/`@EnableAsync` on `DemoApplication`; the `Restaurant` **`RestaurantOf`/`RestaurantOfWithEvent`** factory split; the **extended `event_store` schema** (adds `status` + `retry_count`) and `EventStoreJpa`; the **`Event`** interface gaining `withId`; the **`RemoteEvent`** marker interface; the amended **`DeliveryAddressAddedEvent`** and the new **`NewRestaurantAddedEvent`** record; `RestaurantApplicationService`; `RestaurantJpaToDomainMapper`; the amended **`DomainEventManager`** (publishes `event.withId(savedId)`); the **`RabbitOutboxRouter`** (`@ConfigurationProperties`, `Destination` record, `resolve`); the **`RemoteOutboxListener`** (`@Async` + `@TransactionalEventListener(AFTER_COMMIT)` + `@Retryable`/`@Recover`, spring-retry dependency, PENDING→PUBLISHED/FAILED/UNROUTABLE status updates); the amended **`EventStoreService`** (`StatusOfMessageDelivery` enum, `append`, `updateStatus`); the **"Checking RabbitMQ" Postman demo** (`POST /restaurant` "Alessi" → message held on the `newRestaurant` queue → *Get message(s)* shows the JSON payload `{"id":1,"occurredOn":"2026-07-21","restaurantId":"019f8458-…","restaurantName":"Alessi"}` with `__TypeId__` header); the Ordering-context consumer **`NewRestaurantAddedListener`** (`@RabbitListener(queues="newRestaurant")`); the stub **`OrderRestaurantApplicationService`**; and the **`CustomMessageConverter`** (trusted-packages `"*"` fix for the "not in the trusted packages" error). Three embedded hyperlinks confirmed via PyMuPDF (rabbitmq.com/getstarted, cloudamqp.com, customer.cloudamqp.com/instance). No PlantUML appendices, no Academic Disclosure, and no Maths/English/Digital-Skills block (as with Lecture 7).

---

## 4. What's Pending / Next Steps

- [ ] Load any further lecture PDFs as they become available — the **Identity and Access Management handout (Lecture 9) has now arrived and been fully loaded**, delivering the module's **third learning outcome** ("relevant security features") via Firebase authentication + Spring Security RBAC. Threads still open from earlier lectures that a future handout may pick up: (a) the **Outbox poller** — `RemoteOutboxListener.@Recover` marks failed events `FAILED` and logs *"Assigning to Outbox poller"*, but the scheduled poller that re-attempts FAILED events is **not** implemented; (b) the consumer side is a **stub** (`OrderRestaurantApplicationService` only logs — it doesn't actually upsert the `order_restaurant` snapshot); (c) **event sourcing** is mentioned in Table 1 (an "Event Source" DB to replay/recreate an aggregate) but not built; (d) the `CQRS commands vs conventional request DTOs` objective from Lecture 9 (p.1, fifth objective) is mentioned but not explicitly addressed in the handout body — may be covered in a future lecture or implicitly via the DTOs already used. Also still open: the assessment/case-study threads below.
- [ ] Continue accumulating full module context before actioning any tasks
- [ ] Action the implicit exercise from Lecture 6 (pp.19–20): note the two design choices for restaurant data on `OrderJpa` (snapshot vs retrieve) and how the *Find Order 1111* query now embeds the `restaurant` block — blocked from hands-on inspection until the case-study code is uploaded (the handout shows the command path in full, but not the query-side `OrderJpaToDTOMapper`/`OrderDTO` that produce the p.20 JSON)
- [ ] Lecture 7 contains **no explicit numbered activity/task** for students (like Lecture 4 & 6, unlike Lectures 2 & 3). The implicit exercise is the **"Testing That It Works"** Postman walkthrough (GET `/buyers/0000/deliveryAddress` → POST `/orders` with a new delivery address → GET again to see the de-duplicated `Set` grow; re-post the identical order to confirm no duplicate) — blocked from hands-on execution until the **case-study code is uploaded** (the handout shows every class and the expected before/after JSON, but the app itself isn't runnable here). Also note for later: Lecture 7 mentions a `CancelOrderCommand` endpoint on `OrderController` that has never been implemented/traced in any handout.
- [ ] Lecture 8 also contains **no explicit numbered activity/task**. Its implicit exercise is the **CloudAMQP/LavinMQ setup + the "Checking RabbitMQ for Messages" Postman demo** (create a free CloudAMQP instance → build a `restaurant` direct exchange, a `newRestaurant` queue, and a `newRestaurantKey` binding → put the AMQP details in `application.yaml` → run the Restaurant context → `POST /restaurant` "Alessi" → in the RabbitMQ Manager, *Get message(s)* on the `newRestaurant` queue to see the message held → then wire up the Ordering-context `NewRestaurantAddedListener` to consume it). Blocked from hands-on execution until the **case-study code is uploaded** *and* a **CloudAMQP account** is created (the handout prints every class and the expected message JSON, but the app isn't runnable here and the broker is external). Note the handout's own screenshots show a captured message whose `__TypeId__` header is `com.example.demo.restaurant.domain.events.NewRestaurantAddedEvent` even though the class now lives in `common.events` — a build-drift artefact to be aware of when reproducing.
- [ ] Lecture 9 contains **no explicit numbered activity/task**. Its implicit exercises are: (a) the **Firebase setup walkthrough** (create a Firebase project, generate service account key JSON, enable Email/Password sign-in, retrieve the web API key — these are cloud-setup steps requiring a Google account and the case-study code running locally); (b) the **Postman testing** of register/login endpoints and JWT token forwarding to secured endpoints; (c) the **role-based access** setup and testing of different roles (ADMIN/MANAGER/USER) against the annotated façade methods. All blocked from hands-on execution until the **case-study code is uploaded** and a **Firebase project** is created. The handout prints every class and the expected Postman request/response screenshots, but the app isn't runnable here and Firebase is external. Note the handout's `@GetMapping("/role-check\`")` contains a **stray backtick** in the path — this would cause the endpoint to be mapped as `/role-check\`` rather than `/role-check`, likely a formatting artefact.
- [ ] Address the explicit task from Lecture 2: **"Please generate the tests for the Address class based on the examples above"** — this was left as an exercise in the PDF (Address Tests section, p.35); AddressTests.class is commented out in the TestSuite
- [ ] Action the explicit activity from Lecture 3 (p.28): **"For the assessment — we know what bounded contexts we need (see assessment) but compare to the example as that does help provide a comparison. What aggregates will be needed in each context? Within each aggregate what will form the aggregate root? What entities and value objects will be required?"** — this explicitly references the assessment brief, which has not yet been uploaded
- [ ] Action the implicit exercise from Lecture 4 (p.10): **"In the case study code, you should note (take a look) at the following Value Objects that have now been refactored to records (a useful exercise in the differences between the two forms)"** — Identity, FullName, Address, PaymentMethod, ExpirationDate. The case study code files themselves have **not** been uploaded, only the handouts.
- [ ] Action the implicit exercise from Lecture 5 (p.35): **review the Order query endpoints and test via Postman** — OrderController, ContextFacade, OrderQueryHandler, OrderJpaToDTOMapper, OrderJpa, OrderItemJpa, OrderRepository; and look at the Order aggregate and OrderItem value object. Blocked until the case-study code is uploaded (the Buyer path is fully documented in the handout; the Order path mirrors it but isn't shown).
- [ ] Eventually: assist with assignments, exam prep, practical coding tasks, and KSB evidence mapping once sufficient lecture material is loaded

---

## 5. Approach & Key Decisions

- **Decision:** Raf wants ALL lecture material loaded across multiple chats, with each handoff preserving every detail at full fidelity. → **Reason:** So that Claude always has complete module context regardless of which chat session is active.
- **Decision:** No summarising or condensing of lecture content in handoffs. → **Reason:** Raf explicitly stated everything must be covered in full detail.
- **Decision:** KSBs are tracked silently in the background during university and BT work conversations. → **Reason:** Raf's user preferences specify KSBs should not be mentioned unless he asks.
- **Decision:** Loading precedes doing — no tasks are actioned until sufficient lecture material context is built up.

---

## 6. Technical Context

### Module: Enterprise Application Development
**Lecturer:** Phil James, Staffordshire University
**Module Outcomes:**
- Critically evaluate development approaches to solutions to enterprise applications
- Design an enterprise application, critically evaluating alternatives and justifying selections
- Implement an enterprise application that includes relevant security features

**KSBs mapped to this module (as stated in lecture PDFs):**
- K21 — How to operate at all stages of the SDLC and how each stage is applied in a range of contexts
- K22 — Principles of a range of development techniques for each SDLC stage that produce artefacts (UML, unit testing, programming, debugging, frameworks, architectures)
- C18 — Fluent in written communications and able to articulate complex issues (referenced in Lecture 3 PDF)
- SE9 — How to operate at all stages of the software development lifecycle (referenced in Lecture 3 PDF)
- S18 — Use appropriate analysis methods, approaches and techniques in SE projects to deliver an outcome that meets requirements
- S19 — Implement SE projects using appropriate SE methods, approaches and techniques
- S21 — Determine, refine, adapt and use appropriate SE methods, approaches and techniques to evaluate SE project outcomes

---

### LECTURE 1 CONTENT — FULL DETAIL

---

#### Document 1: Enterprise Application Development — Introduction

**What are Enterprise Applications?**
- "Enterprise applications are about the display, manipulation and storage of (mainly) large amounts of often complex data and the support or automation of business processes with that data" — Fowler (2003)
- Also defined as 'information systems'
- Software platforms designed to operate in a corporate environment (including schools, hospitals, charities, government departments) — to solve enterprise-wide problems; to meet the needs of a large business or organisation
- Often scalable, multi-tier solutions
- Examples: accounting, insurance, e-commerce, billing systems, supply chain management, CRM, HRM, payroll, business intelligence, product catalogues, resource planning

**Advantages of Enterprise Applications:**
- Scalability
- Reliability
- Increased security
- Improved communications — management and sharing of data
- Better quality of data and integration for automated reporting
- Expected improvements in productivity and efficiency
- Better communication across the organisation
- Better customer relationships

**Limitations/Challenges:**
- Complexity of business needs — need to be captured accurately
- Reliance on a single system can lead to problems if that system becomes unavailable
- Cost of adoption and running — from hardware, software and expertise

**Java / Spring Framework Industry Context (Stack Overflow Survey 2025):**
- Java primary domains: Enterprise backend systems, Android app development, Large-scale financial and government systems
- 90% of Fortune 500 companies use Java
- Companies using Spring Framework include: Amazon, eBay, Google, JPMorgan Chase Bank, LinkedIn, Microsoft, Netflix, Uber
- Netflix quote (Taylor Wicksell, Senior Software Engineer): "Originally [Netflix's Java] libraries and frameworks were built in-house. I'm very proud to say, as of early 2019, we've moved our platform almost entirely over to Spring Boot."

---

**Spring Framework:**
- Widely used framework providing comprehensive support for developing Java applications (run-time and supporting infrastructure) — enterprise, internet, or cloud-based
- Unlike standard Java enterprise framework (Jakarta EE), no need to deploy a separate application server — Spring comes with its own **TomCat server** (so applications use **Jar** rather than **War**)
- Spring's **application context interface** serves as the **Inversion of Control (IoC) container** — provides a simple mechanism for **dependency injection**, making the system very loosely coupled and easier to test
- The container gets its instructions on what objects to instantiate, configure, and assemble by reading **configuration metadata** (including annotations)
- Configuration metadata can be represented in XML, Java annotations, or Java code
- Spring uses **Aspect Oriented Programming (AOP)** — breaks down program logic into distinct parts called "concerns"
- Functions that span multiple points of an application are called **cross-cutting concerns** (conceptually separate from the application's business logic)
- Examples of aspects: logging, auditing, declarative transactions, security, caching

---

**Spring Boot:**
- A micro framework built on top of Spring (uses many of Spring's dependencies — e.g. Spring Web Services and Spring Security)
- Makes developing and deploying a web-based application much simpler than Java/Jakarta EE
- Three key features:
  - **Autoconfiguration**
  - **Standalone**
  - **Opinionated**

---

**Bean (object):**
- A bean is a regular class that is instantiated, configured and managed by a Spring IoC container
- Conventions: no-args constructor, private fields supported by accessor and mutator methods, should be serializable
- Called "Beans" because they are small, modular (self-contained) reusable software components — ties in with the Java coffee brand
- Instead of a class constructing dependencies by itself, the object can retrieve and inject its dependencies from an IoC container
- Beans are defined for: service layer objects, data access objects, presentation objects, infrastructure objects — NOT domain specific objects used by business logic
- Container needs appropriate configuration metadata
- `@ComponentScan` annotation used to find beans; `@Autowired` annotation used to inject them — can inject via constructor or setter method

---

#### Document 2: Enterprise Application Development — Developing a Simple API

**What is being developed:**
A REST API that returns JSON representing objects in a `role_allocation` table. The API supports:
- GET all role allocations → `http://localhost:8900/api/roleallocations`
- GET role allocation by id → `http://localhost:8900/api/roleallocations/{id}`
- POST (create) role allocation → `http://localhost:8900/api/roleallocations`
- DELETE role allocation by id → `http://localhost:8900/api/roleallocations/{id}`
- PATCH (edit) role allocation by id → `http://localhost:8900/api/roleallocations/{id}`

---

**Creating a New Project with Spring Boot (IntelliJ Ultimate):**
- Server URL: start.spring.io
- Name: api
- Location: C:\Temp\EnterpriseIntro
- Language: Java
- Type: Maven
- Group: staffs
- Artifact: api
- Package name: staffs.api
- JDK: Oracle OpenJDK 26.0.1 (download required — causes delay)
- Java: 26
- Packaging: Jar
- Configuration: YAML

**Dependencies selected:**
- Developer Tools → Spring Boot DevTools (auto-restarts app when classpath files change)
- Developer Tools → Lombok (annotation library to reduce boilerplate — negates explicit constructor declaration, toString, getters/setters)
- Web → Spring Web (Spring Web Services including RESTful using Spring MVC; adds Tomcat as default embedded container)
- SQL → Spring Data JPA (Java Persistence API — intelligently generates DAO pattern implementation for entities; offers several repository interfaces; uses Hibernate as ORM)
- SQL → H2 Database (in-memory database, commonly used for testing/small applications — not production; uses SQL)

Note: Can also be done via https://start.spring.io/ which creates a project as a zip.

---

**Build Automation Tools:**
- A build automation tool streamlines and automates the process of compiling, testing, and deploying software applications
- Eliminates the need for manual and error-prone processes

**Maven:**
- Build automation tool (like Gradle) for building and managing Java-based projects
- Name is Yiddish for "accumulator of knowledge"
- Primary goal: allow a developer to comprehend the complete state of a development effort in the shortest period of time
- Uses an XML file called **Project Object Model (POM)** to define dependencies
- Downloads libraries and plugins from different repositories and caches them locally
- Areas of concern:
  - Making the build process simpler (shields many details)
  - Providing a uniform build system using POM and plugins
  - Providing quality project information (changelog from source control, cross-referenced sources, mailing lists, dependencies used, unit test reports including coverage)
  - Encouraging better development practices (unit testing is part of normal build cycle; suggests directory structure guidelines)

---

**pom.xml (Project Object Model):**
- XML file containing configuration details, used by Maven to build the project
- Parent section — how all Spring dependency management is obtained; allows specifying components without version numbers; guarantees compatibility between Spring components

```xml
<parent>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-parent</artifactId>
  <version>4.0.6</version>
  <relativePath/>
</parent>
```

- With starter parent, dependencies can be pulled just by declaring them — no version numbers needed:

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

- **spring-boot-starter-web** — transitively pulls in all web development dependencies; reduces build dependency count; uses Spring MVC, REST, and Tomcat (default embedded server; can use Jetty or Undertow instead)
- **spring-boot-maven-plugin** — when building, Maven clean package uses this plugin to create an executable JAR
- **spring-boot-starter-parent** — brings in all Spring dependencies needed to run a web-based application; provides default configurations and a complete dependency tree

---

**@SpringBootApplication:**
- Designates the file as a configuration file and the start of all component scanning
- Convenience annotation combining:
  - `@SpringBootConfiguration` — tags the class as a source of bean definitions for the application context
  - `@EnableAutoConfiguration` — tells Spring Boot to start adding beans based on classpath settings, other beans, and various property settings
  - `@ComponentScan` — tells Spring to look for other components (controllers, services and repositories) in the package and sub-packages

```java
@SpringBootApplication
public class ApiApplication {
  public static void main(String[] args) {
    SpringApplication.run(DemoApplication.class, args);
  }
}
```

**Bootstrap steps when the class launches:**
1. Creates an appropriate `ApplicationContext` instance — `ApplicationContext` is a sub-interface based on `BeanFactory` interface; represents the IoC container; needed to access and inject beans via annotations
2. Registers a `CommandLinePropertySource` — exposes command line arguments as Spring properties
3. Refreshes the application context, loading all singleton beans (one instance created for the whole application; does not terminate until the application is shut down)
4. Triggers any `CommandLineRunner` beans — calls the run methods of any beans implementing this interface

---

**application.yaml (resources folder):**
- Auto-detected by Spring Boot
- YAML uses spaces (2 each time), not tabs

Default content:
```yaml
spring:
  application:
    name: api
```

Amended to:
```yaml
spring:
  application:
    name: api
  jpa:
    hibernate:
      ddl-auto: none

server:
  port: 8900
```

- `application: name` — identity of the application; appears in logs, tracing systems and service discovery tools
- `jpa: hibernate: ddl-auto: none` — tells Hibernate how to handle the database schema; "none" assumes a schema.sql file exists rather than setting up a default
- `server: port: 8900` — opens the application on the embedded Tomcat server on port 8900

---

**H2 — Adding an In-Memory Database:**
- An in-memory database relies on main memory for storage
- H2 is the most popular in-memory database (including its web console)
- Alternatives: HSQLDB, Apache Derby — Spring Boot auto-configures if found in POM
- Every time the application restarts, the in-memory database is 'recreated' — useful for testing as code is effectively non-destructive
- Tables and records can be defined using SQL or directly via Java Entities via CommandLineRunner
- Populated using schema files (schema.sql and data.sql)
- Not a long-term production solution, but if SQL is used to create the database it can be migrated to a separate database; moving from in-memory to external database requires altering POM and application.properties files

---

**Creating Tables and Adding Data:**

schema.sql (right-click New File in resources):
```sql
CREATE TABLE role_allocation (
  id INT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(35) NOT NULL UNIQUE
);
```

data.sql (right-click New File in resources):
```sql
INSERT INTO role_allocation (name) VALUES
  ('manager'),
  ('admin'),
  ('staff');
```

- No `id` values set because the entity sets it to auto-generated type (GenerationType.IDENTITY)
- More sophisticated id generation possible via a separate ID class from the application

---

**RoleAllocation Entity:**
- Entities are simply POJOs representing data that can be persisted to the database

```java
@Entity
@Table(name = "role_allocation")
@Getter
@Setter
public class RoleAllocation {
  @Id
  @Column(name = "id")
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private long id;

  @Column(name = "name")
  private String name;
}
```

- `@Table(name = "role_allocation")` — maps entity to the role_allocation table
- `@Id` — marks the primary key field
- `@Column(name = "id")` — maps field to the id column
- `GenerationType.IDENTITY` — auto-increment strategy; database generates the id
- Getter/setter/toString/equals/hashcode auto-inserted by Lombok via `@Getter` `@Setter`
- It is possible to auto-generate a database schema from an entity (with settings in application.properties) — but this won't help when moving to an external database

---

**RoleAllocationRepository:**
- Implements `Repository<T, ID>` (T = entity class, ID = type of id field) — allows operations on a repository of a particular type; persists entity objects into a database

```java
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleAllocationRepository extends CrudRepository<RoleAllocation, Long> {
}
```

- All JPA repositories are interfaces instead of classes
- Extends `CrudRepository` with templated argument:
  - Entity: `RoleAllocation`
  - ID type: `Long` (entity uses primitive `long`, but CrudRepository requires the class wrapper)
- Each repository interface can only perform data access operations for that particular entity — only communicates with RoleAllocation, not other entities
- Spring Data provides:
  - A common set of interfaces for interacting with SQL (or NoSQL) data stores — based on the repository pattern
  - A common naming convention for data access methods — provides data access routines without the developer writing them
  - Repository and data mapping conventions for common ORM behaviour — developer works only with objects; mappers are dynamic and aspected
  - Option to write low-level queries and data mapping if full control over SQL statements and result set mappings is required

---

**Data Access Object (DAO) Pattern:**
- By supplying entity (RoleAllocation) and its primary key data type (Long), the CRUD repository knows exactly which database table and columns it can work with — all that information is inside the entity
- When the application starts up, Spring Data JPA recognises the CRUD repository and automatically generates an implementation for the DAO contract specified in that interface
- `CrudRepository` implements many common methods to interact with the persistence layer

---

**RoleAllocationController:**

**Servlet mapping concept:**
- When a request is received from a client (using a URI), our servlet container decides which application it should forward to, that is which servlet it has to invoke
- In regular Java/Jakarta EE this involves web.xml or navigation
- In Spring, the controller is a Spring bean (a POJO decorated via annotations to make it a controller)
- Both class-level and method-level annotations provide behaviour for servlet mapping
- These methods (once annotated) respond to incoming web requests and perform work

```java
@RequestMapping("/api/roleallocations")
@AllArgsConstructor
@RestController
public class RoleAllocationController {
  private RoleAllocationRepository roleAllocationRepository;

  @GetMapping("")
  public Iterable<RoleAllocation> getAllRoles() {
    return roleAllocationRepository.findAll();
  }

  @GetMapping("/{id}")
  public Optional<RoleAllocation> getToolById(@PathVariable Long id) {
    Optional<RoleAllocation> result = roleAllocationRepository.findById(id);
    if (result.isPresent())
      return result;
    else {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "id not found");
    }
  }
}
```

**Annotation explanations:**
- `@RequestMapping("/api/roleallocations")` — maps web requests onto classes or methods; looks for endpoint starting with /api/roleallocations
- `@AllArgsConstructor` (Lombok) — creates a constructor including all attributes; assigns value to each attribute; defines a constructor expecting a RoleAllocationRepository and assigns a value to it (constructor injection)
- `@RestController` — combines `@Controller` and `@ResponseBody` to simplify request handling; allows implementation classes to be autodetected through classpath scanning; every request handling method automatically serialises return objects into an HttpResponse. NOTE: Must use `@RestController` not `@Controller`, otherwise it will not work
- `@GetMapping` — maps GET web requests onto methods; shorthand for `@RequestMapping(method = RequestMethod.GET)`
- `@PathVariable` — name of the request variable supplied in the request; NOT necessary for param name and variable names to match
- `Optional` — wrapper class providing a type-level solution for representing optional values instead of null references

---

**GlobalExceptionHandler:**
- Central class for handling different exceptions the application can throw — Single Responsibility Principle; separation of concerns
- Removes the need for try/catch blocks in the classes that raise these exceptions
- Centralises logging for errors

```java
@ControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception ex) {
    HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
    String message = ex.getMessage();

    if (ex instanceof ResponseStatusException rse) {
      status = HttpStatus.valueOf(rse.getStatusCode().value());
      message = rse.getReason();
    }
    else if (ex instanceof DataIntegrityViolationException) {
      status = HttpStatus.BAD_REQUEST;
      message = "A duplicate record already exists.";
    }
    else if (ex instanceof IllegalArgumentException) {
      status = HttpStatus.BAD_REQUEST;
      message = ex.getMessage();
    }

    Map<String, Object> responseBody = Map.of(
      "status", status.value(),
      "error", status.getReasonPhrase(),
      "message", Objects.requireNonNullElse(message, "No message provided"),
      "timestamp", Instant.now().toString()
    );

    return ResponseEntity.status(status).body(responseBody);
  }
}
```

- `@ControllerAdvice` — AOP interceptor; captures exceptions thrown by any controller
- `@ExceptionHandler(Exception.class)` — since `Exception` is the parent of all other exceptions, all exceptions are handled by this file
- `HttpStatus.INTERNAL_SERVER_ERROR` — default/fallback status; changed if a specific exception type is found
- `ResponseStatusException` — developer throws an HTTP error
- `DataIntegrityViolationException` — violations of database constraints
- `IllegalArgumentException` — invalid input
- `responseBody` — immutable JSON response; `value` = status code; `getReasonPhrase` = human readable

---

**Testing:**
- Test using IntelliJ HTTP client: go to controller, click the globe icon next to the method, select "Generate Request in HTTP Client", click the generated endpoint, press the play button
- Or use Postman

```
### GET all
GET http://localhost:8900/api/roleallocations

### GET by id
@id = 1
GET http://localhost:8900/api/roleallocations/{{id}}
```

---

**Adding Constraints to the RoleAllocation Entity:**
- Add dependency to pom.xml:

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```

- Add `@NotBlank` to the `name` field in RoleAllocation entity
- Add `@Valid` to controller POST method parameter

---

**Creating a RoleAllocation (POST):**

```java
@PostMapping("")
@ResponseStatus(HttpStatus.CREATED)
public void addRoleAllocation(@Valid @RequestBody RoleAllocation newRoleAllocation) {
  roleAllocationRepository.save(newRoleAllocation);
}
```

- `@PostMapping("")` — POST request received (remember URI commences from controller's `@RequestMapping`)
- `@ResponseStatus(HttpStatus.CREATED)` — default response status for this method; method is void
- `@Valid` — triggers validation of `@NotBlank` on RoleAllocation's fields
- `@RequestBody` — JSON object that is received by the method (automatic deserialisation)

---

**Deleting a RoleAllocation (DELETE):**

```java
@DeleteMapping("/{id}")
@ResponseStatus(HttpStatus.OK)
public void deleteRoleAllocation(@PathVariable Long id) {
  if (!roleAllocationRepository.existsById(id)) {
    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "id not found");
  }
  roleAllocationRepository.deleteById(id);
}
```

- `@DeleteMapping("/{id}")` — DELETE request received (URI commences from controller's `@RequestMapping`)
- `@ResponseStatus(HttpStatus.OK)` — default response status; method is void

---

**Editing a RoleAllocation (PATCH):**

```java
@PatchMapping("/{id}")
@ResponseStatus(HttpStatus.OK)
public void updateRoleAllocation(
  @PathVariable Long id,
  @Valid @RequestBody RoleAllocation updateRoleAllocation) {

  Optional<RoleAllocation> result = roleAllocationRepository.findById(id);
  if (result.isEmpty()) {
    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "id not found");
  }

  var updatedRoleAllocation = existingRoleAllocation.get();
  updatedRoleAllocation.setName(updateRoleAllocation.getName());
  roleAllocationRepository.save(updatedRoleAllocation);
}
```

- `@PatchMapping("/{id}")` — PATCH request received
- `@ResponseStatus(HttpStatus.OK)` — default response status; method is void
- NOTE: Some code is the same as the POST method — this duplication should be removed and extracted into a centralised private method

---

#### Document 3: Repository Pattern (standalone handout)

**Definition (Hieatt and Mae, 2002 — Patterns of Enterprise Architecture):**
"Mediates between the domain and data mapping layers using a collection-like interface for accessing domain objects."

**Full explanation from the handout:**
- A system with a complex domain model often benefits from a layer, such as the one provided by **Data Mapper**, that isolates domain objects from details of the database access code. In such systems it can be worthwhile to build another layer of abstraction over the mapping layer where query construction code is concentrated. This becomes more important when there are a large number of domain classes or heavy querying. In these cases particularly, adding this layer helps minimize duplicate query logic.
- A **Repository** mediates between the domain and data mapping layers, acting like an in-memory domain object collection. Client objects construct query specifications declaratively and submit them to Repository for satisfaction. Objects can be added to and removed from the Repository, as they can from a simple collection of objects, and the mapping code encapsulated by the Repository will carry out the appropriate operations behind the scenes.
- Conceptually, a Repository encapsulates the set of objects persisted in a data store and the operations performed over them, providing a more object-oriented view of the persistence layer. Repository also supports the objective of achieving a clean separation and one-way dependency between the domain and data mapping layers.

---

### LECTURE 2 CONTENT — FULL DETAIL

---

#### Document 4: Enterprise Application Development — Domain Driven Design part 1 Overview

**What is a Domain Model?**
- A domain model is not a particular diagram — it is the idea that the diagram is intended to convey.
- It is not just the knowledge in a domain expert's head — it is a rigorously organised and selective abstraction of that knowledge.
- A diagram can represent and communicate a model, as can carefully written code, as can an English sentence.

**Why Domain Driven?**
- The challenge: how to capture business complexity accurately in software
- DDD is an approach to software development that helps manage this complexity

---

**DDD — General Introduction / Important Points:**

*Collaboration:*
- Agile Manifesto principle: "Business people and developers must work together daily throughout the project"
- Most significant complexity of many applications lies in the domain itself (activity of the business or user) rather than technical issues — so **developers and domain experts** need a close relationship
- Developers need to **work collaboratively** with domain experts (users of existing systems and those who have prior experience in the same domain or related systems) **to identify what is really important and present it simply as a set of abstract concepts** (relationships, terms, etc.)
- The model represents the **distilled knowledge** and **shared understanding** of domain experts and developers — this will take time to wrestle into submission and for developers to build up knowledge of the domain.
- **Some domain knowledge may never have been formalised before** — often helps domain experts to refine their own understanding by being forced to reduce what they know to essential business principles.
- Example — Booking cargo: associating Cargo with a Voyage. "There are always last-minute cancellations so it is standard practice to allow overbooking." How is this handled? — can be a percentage of capacity (10%), can be prioritising certain customers or types of cargo = we need a policy (strategy) in our design to reflect this.
- **We need a knowledge rich design** — some business rules are contradictory, but software cannot do this.

*Modelling:*
- Model changes need to be reflected in the code
- All team members need to understand and use the ubiquitous language

---

**Ubiquitous Language:**
- Domain experts use their jargon; technical team members have their own language tuned for discussing the domain in terms of design. E.g. in air traffic monitoring, domain experts know about planes, routes, altitudes, longitudes, latitudes, deviances from normal route, plane trajectories. (Avram and Marinescu, Domain-Driven-Design Quickly, page 14)
- Discussions/conversations/limited documentation should use the **ubiquitous language of the domain model** (both developers and domain experts should understand it — avoid linguistic divide that leads to translation muddles).
  - If the language changes, this should be reflected in the model (class name, prominent operations, business rules, behaviour, terms, patterns to be applied) — this change is then made to the code.
  - Using the language means that weaknesses in the model (ambiguity or inconsistency) should be highlighted.
  - If the domain experts do not understand the model it needs to be altered.
  - The model should be expressed by the code — naming of classes and methods = the code can be interpreted based on the model.
  - UML diagrams may be used (class or sequence) BUT these do not show the whole picture, so may be supported by additional notes.
- From Evans (pg 34): Ubiquitous Language is cultivated at the **intersection of jargons** — between technical aspects/terms/design patterns (one side) and business terms (the other), with domain model terms, names of bounded contexts, terminology of large-scale structure, and many pattern names from DDD in the middle.
- DDD provides a set of tools and techniques which helps break down business complexity while keeping the core business model as the centrepiece of the approach.

---

**Ingredients of Effective Modelling** (Source: Evans, page 12, 37):
- **Binding the model and the implementation** — may be crude to start with but the model and code will be refined/deepened through iterations.
- **Cultivate a language based on the model** — as project proceeds, the terminology/language of the model will be used to communicate with both developers and domain experts understanding each other clearly.
- **Knowledge rich model** — model captures knowledge of various kinds not just a data schema; complex business rules need to be identified.
- **Distilling the model** — over time the model will have important concepts added while others become less central or even unimportant.
- **Brainstorming and experimenting** — language, sketches and brainstorming help to transform discussions into improvements of the model based on reviewing scenarios.
- **The model is not the diagram** — diagrams purpose is to help communicate and explain the model. Code can serve as a repository of the details of the design. Carefully selected and constructed diagrams can serve to focus attention and aid navigation.

---

**What is a Bounded Context?**
- Recommended: Eric Evans talk at DDD Europe 2020 conference — https://www.youtube.com/watch?v=am-HXycfalo
- A bounded context is a **natural logical boundary/division within the business**: "an area where certain business-processes are implemented, the certain ubiquitous language is applied, and certain terms make clear sense, while the others don't" — so **align bounded contexts with business capabilities**. (Source: https://codeburst.io/ddd-strategic-patterns-how-to-define-bounded-contexts-2dc70927976e)
- Divides a large complex domain into **specific contexts (sub-domains)** — one of which will be considered the core domain.
- Example — **Book Store** has two distinct responsibilities:
  - **CORE/PRIMARY CONTEXT** — the Store (sales) itself — mainly for a sales responsibility
  - **SUPPORTING CONTEXT** — in the Warehouse (the store cannot operate without it) — mainly a shipping responsibility
  - Ubiquitous language differs per sub-domain re information about the book:
    - In the **Store** — information related to sales: author, length, genre, readability; we organise books.
    - In the **Warehouse** — information related to shipping: size, location in warehouse, weight; we pick books, we pack books.
- What do we need to know about Product in a Sales Context vs Support Context? What about concepts that are not in both/all contexts (e.g. no Ticket in the Sales Context)?

*Different Perspectives:*
- **Domain Expert** — sees a bounded context as an area where certain business-processes (and rules) are implemented and certain ubiquitous language is applied. This is **business reality**.
- **Developer** — sees a bounded context as a way to model boundaries, data consistency, communication between contexts (the **solution space**). For them a bounded context is a **boundary around a model**: what classes, aggregates, repositories and logic should reside together.

---

**Solution Architecture** (not specific to DDD but a consideration):

DDD is the **preferred approach** for complex monolithic projects (where it started) and fits well with microservices architecture (both have strengths and weaknesses).

- **Monolith** — Single tier application (unified model) — all parts from UI to data/persistence layer are interconnected in some way to form a single application (may have web and mobile front ends).
- **Modulith (Modular Monolith)** — Modular monolith featuring modules which are **self-contained (more loosely coupled)** but physically part of the same application — closer to microservices in design (modules are separate but not distributed).
  - Packages nested under main application packages are considered context ones.
  - Access components via APIs.
  - Integration testing only to include necessary packages.
  - Useful video: https://www.youtube.com/watch?v=5OjqD-ow8GE (Simon Brown)
- **Microservice** — Structures application as a collection of services — deployed independently, loosely coupled, organised around business capabilities, owned by a small team — exposing their services and schemas via an API. Requires additional overhead for service discovery and gateway management.

Key quotes:
- Martin Fowler: "Don't even consider microservices unless you have a system that's too complex to manage as a monolith. The majority of software systems should be built as a single monolithic application. Do pay attention to good modularity within that monolith, but don't try to separate it into separate services." (https://martinfowler.com/bliki/MicroservicePremium.html)
- Simon Brown: "If you can't build a well-structured monolith, what makes you think microservices is the answer?" (http://www.codingthearchitecture.com/presentations/devnexus2016-modular-monoliths)
- Lecture notes add: "but remember the skill of the team will outweigh any monolith/microservice choice"
- Graph (Fowler): For less-complex systems, the extra baggage required to manage microservices reduces productivity. As complexity kicks in, productivity starts falling rapidly for monolith. The decreased coupling of microservices reduces the attenuation of productivity at high complexity.

---

**A Suggested Separation by Layer (per bounded context)** (Source: Evans, pg 72):

Layered architecture (four layers, top to bottom):
1. **User Interface** — showing information and interpreting user commands. e.g. html page, controller
2. **Application** — Does NOT contain business rules or knowledge; coordinates tasks and delegates work to collaborations of domain objects in the next layer down. Does not have state reflecting the business situation (but can have state re progress of a task for the user/program). e.g. command and query (types of actions), command handler (service), events, DTOs
3. **Domain/Model** — represents the concepts of the business, information about the business situation and rules. e.g. aggregate, entity, value object, repo interface
4. **Infrastructure** — application and domain call on services of this layer — persistence. e.g. entity, repository, mapper, DTO interface

---

**Declarative vs Reactive Systems** (examples from Software Architecture: Domain-Driven Design, LinkedIn Learning):

*Declarative (orchestrated):*
- Service communicates directly with another service (works in a monolith via function calls). Doesn't work when these are network calls to other remote services (microservice).
- Requires shopping-cart service to know things about the other downstream services (**higher coupling**).
- Example diagram: Shopping-Cart Service → `issueInvoice()` → Billing Service; → `queueItemForShipping()` → Warehouse Service; → `emailCustomer()` → Email Service

*Reactive (choreographed = publish/subscribe model):*
- Shopping cart announces an event with a messaging system. Other services are registered with the broker and are waiting for that type of event to be generated and then they respond accordingly.
- We now have **lower coupling** (no knowledge of who wants to use this event).
- Example diagram: Shopping-Cart Service → `orderPlaced()` → (Billing Service, Warehouse Service, Email Service all receive it via broker)
- This type of approach can be done in monolith or microservice.

---

**Problem Space (Core Business Domain)** (Source: Chapter 2 of Practical Domain-Driven Design in Enterprise Java, Vijay Nair, 2019):

- **Core Domain** — is the sub-domain most important to the business' survival (what gives it competitive advantage over its competitors).

*What is the main business problem that we are trying to solve?* — Example: Auto Finance Services → Problem Space → Core Business Domain = Auto Loans/Lease Management → Business Problem

*Break Down Domain into Sub-Domains:*
We next need to identify the business capabilities that are used on a day-to-day basis by breaking down the various business capabilities of your main business domain into cohesive units of business functionality.
- **Originations** — business capability of issuing new auto loans/leases to customers.
- **Servicing** — business capability of servicing (e.g., monthly billing/payments) these auto loans/leases.
- **Collections** — business capability of managing these auto loans/leases if something goes wrong (e.g. customer defaults on payment).

*2nd Example (Retail Banking):*
- Retail Banking Services has multiple problem spaces: Checking Account Management, Savings Account Management, Credit Card Management.
- Credit Card Management sub-domains:
  - **Products** — business capability of managing all types of credit card products.
  - **Billing** — business capability of billing for a customer's credit card.
  - **Claims** — business capability of managing any kinds of claims for a customer's credit card.

---

**Bounded Context — Moving from the Problem Space to the Solution Space:**

We COULD choose to:
- Have a **single bounded solution** for the entire domain (containing all sub-domains)
- Or a **bounded context mapped to single sub-domain / multiple sub-domains**

Figures 5 & 6 (Auto-finance example):
- Figure 5: Auto-finance solution as a **single bounded context** — all three sub-domains (Originations, Servicing, Collections) inside one boundary in the Solution Space.
- Figure 6: Auto-finance solution as **separate bounded contexts** — each sub-domain maps to its own bounded context (Originations BC, Servicing BC, Collections BC).

There are no restrictions to the choice of deployment provided the Bounded Context is treated as a single cohesive unit:
- **Monolithic** deployment for the multiple bounded contexts approach
- **Microservices** deployment model with each bounded context as a separate container
- **Serverless** model with each bounded context deployed as a function

---

**Bounded Context Domain Model — Core Domain Model:**

Implementation of the core business logic within a specific Bounded Context.

| Business Language | DDD Technical Language |
|---|---|
| Business Entities | Aggregates, Entities, Value Objects |
| Business Rules | Domain Rules (Invariants) |
| Business Operations (Commands) | Commands — objects that wrap create, update, delete requests from a user — sent to aggregate |
| Queries | Queries — read data from persistence and display |
| Business Events | Events — record that commands were successful — generate other commands |
| Business Flows | Sagas |

---

**Appendix A — Creating the Ubiquitous Language** (Taken from Domain Driven Design Quickly pages 16 to 21):

How can we start building a language?
- Start by writing down all the terms used in the domain and their meanings
- A ubiquitous language requires creating a model of the domain — and the language should be consistent with this model

---

#### Document 5: Enterprise Application Development — Domain Driven Design part 2 — Entities and Value Objects

**Bounded Contexts — Review:**
- Bounded contexts represent a **logical conceptual model of the system using a ubiquitous language** understood by the domain expert and the developer.
- Recognises that we cannot have a single canonical model for the entire enterprise system but instead need to think in terms of the usage of language about the elements of the enterprise within smaller more specific areas (boundaries agreed upon by domain experts).
- In the Customer/Sales/Support context example: information we hold about Customer for the Sales context (Customer id, name, address, contact details, which territory the customer is in, etc.) is different to what we need about Customer in the Support context (Customer id, name, potentially other information).
- **Shared concepts** (Customer and Product) and **unrelated concepts** (anything other than Customer and Product).

---

**New Spring Boot Project for Lecture 2:**
- Name: day3
- Location: C:\Temp\Entities&ValueObjects
- Build system: Maven
- JDK: openjdk-26 Oracle OpenJDK 26.0.1
- Package: staffs
- Classes in src/main/java/staffs: Address, DomainAssertions, Entity, FullName, Identity, Main, Person, ValueObject

---

**What is a Value Object?**
- An object that contains attributes but has no conceptual identity (Evans, pg 97)
- Value objects are a cluster of one or more attributes which together represent an important business concept
- Example: 50,000 and dollars — separately meaningless. Put inside a Value Object called MonetaryValue with attributes amount and currency = makes sense
- E.g. FullName as a value object which includes first name, surname and potentially middle name(s) — these form a conceptual whole rather than individual attributes directly in our entity
- Regarding naming — whether one or more attributes, use of custom value types speaks the ubiquitous language better than String or Double or BigDecimal
- Source: http://fit.c2.com/wiki.cgi?WholeValue

*Other points to consider:*
- Attaching an identity to objects other than entities will impact on system performance, make the model more confusing and complex — **don't do it for value objects**
- A value object can be an assemblage of other objects including other value objects. It IS possible to include an entity in a value object — but as value objects must be immutable and entities are not immutable, this creates a contradiction. Resolution: hold a **reference to the entity id** in the value object rather than a reference to the entity itself.
- Often passed as parameters in messages (transient).
- When storing in a relational database may be better to **de-normalize** (put the attributes with the entity that uses it rather than hold in a separate table — especially if in a distributed context).

*Example 1:* If each electrical outlet is a separate value object, there might be a hundred in a single house plan. But if all outlets are considered interchangeable, we could share just one instance of an outlet and point to it a hundred times (an example of the **flyweight pattern**).

*Example 2 (Evans, pg 99):* Street, city and state should not be separate attributes of Customer but part of a single, whole Address (defined as a coherent value object). Diagram: Customer (customerID, name, street, city, state) → refactored to Customer (customerID, name, address) → Address (street, city, state). "A VALUE OBJECT can give information about an ENTITY. It should be conceptually whole."

---

**Summary of Value Object Characteristics:**
- No identity
- Immutable
- Comparable by value (all attributes)
- Self-validating

---

**What is an Entity?**
- Has a unique identity that runs through time and different states
- An object primarily defined by its identity is called an Entity (Evans, pg 89)
- Even if two entities have all the same attributes, they are still different entities if they have different identities
- Example: customerID is the only identifier of the Customer entity BUT phone and address are often used to find/match a Customer (so should stay in Customer as name does not define the customer's identity but is often used as part of the means of determining it)
- If a customer had many phone numbers for different purposes it would stay in Sales Contact (as its own entity)

---

**Entity vs Value Object — Full Comparison Table:**

| | Entity | Value Object |
|---|---|---|
| Attributes | Represent several attributes (including the id) | Represent one or more attributes that represent a conceptual whole (e.g. FullName, Address, Money) |
| Persistence | Will represent a table in the infrastructure | Will represent fields in a table in the infrastructure |
| Identity | Has an identity attribute (typically contrived as a UUID) — the id is a value object | No identity attribute defined (doesn't need one as it exists as a part of an identity) |
| Equals | Uses identity as its only criteria — can differentiate between one entity and another | Uses the entire state as its criteria |
| Belongs to | Belongs to an aggregate (which is an entity itself). An entity id might also be stored in a value object (not the entity itself) | Belongs to an entity |
| Mutability | Is mutable | Is immutable |
| Setters | Has setter methods defined in DDD language | No setters (as immutable) |
| Validation | Self-validating via pre guard conditions in the constructor or setter methods | Self-validating via pre guard conditions in the constructor |

---

**DomainAssertions (utility class for precondition guards):**

```java
public class DomainAssertions {
  public static void argumentNotEmpty(String argument, String message) {
    if (argument == null || argument.isBlank()) {
      throw new IllegalArgumentException(message);
    }
  }

  public static void argumentLength(String argument, int minLength, int maxLength, String message) {
    if (argument.length() < minLength || argument.length() > maxLength) {
      throw new IllegalArgumentException(message);
    }
  }

  public static void argumentNotEmpty(BigDecimal argument, String message) {
    if (argument == null) {
      throw new IllegalArgumentException(message);
    }
  }
}
```

- Static methods — no need to instantiate
- `argumentNotEmpty` — checks for null or blank strings; throws `IllegalArgumentException` with message
- `argumentLength` — checks min/max length; throws `IllegalArgumentException` with message
- Imported statically in value object/entity classes: `import static staffs.DomainAssertions.*;`

---

**ValueObject SuperType:**

```java
public abstract class ValueObject {}
```

- `ValueObject` is an empty class defined to **label** our classes as belonging to the family of ValueObjects.
- All ValueObject classes (objects without an id) will extend this class.
- Distinguishes those that inherit it AS a value object.
- Value objects are compared on their state (all attributes), not their ids.
- When persisted to storage they are saved as fields within a row that represents an entity — so they have no need of a separate identity of their own.

---

**FullName (Value Object):**
- Extends `ValueObject`
- Meaningful as a whole — not as separate values within another object (e.g. Person or Student) due to the naming of the value object and its attributes.
- For a value object the equals method compares the type then entire state — achieved using Lombok's `@EqualsAndHashCode`
- Setting of values is only possible via the constructor — any changes to this object within the entity that contains it require a new object.
- Guards for empty and length as examples.
- Constants defined for error messages to make unit testing cleaner.

```java
import static staffs.DomainAssertions.argumentLength;
import static staffs.DomainAssertions.argumentNotEmpty;

@EqualsAndHashCode(callSuper = false)
@ToString
public class FullName extends ValueObject {
  public static final int MAX_FIRST_NAME_LENGTH = 20;
  public static final int MAX_SURNAME_LENGTH = 20;
  public static final String FIRST_NAME_NOT_EMPTY = "First name cannot be empty";
  public static final String SURNAME_NOT_EMPTY = "Surname cannot be empty";
  public static final String FULL_NAME_CANNOT_BE_NULL = "Full name to copy cannot be null";
  public static final String FIRST_NAME_LENGTH = "First name must be between 1 and ${MAX_FIRST_NAME_LENGTH} characters";
  public static final String SURNAME_LENGTH = "Surname must be between 1 and ${MAX_SURNAME_LENGTH} characters";

  private final String surname;
  private final String firstName;

  public FullName(String firstName, String surname) {
    argumentNotEmpty(firstName, FIRST_NAME_NOT_EMPTY);
    argumentNotEmpty(surname, SURNAME_NOT_EMPTY);
    argumentLength(firstName, 1, MAX_FIRST_NAME_LENGTH, FIRST_NAME_LENGTH);
    argumentLength(surname, 1, MAX_SURNAME_LENGTH, SURNAME_LENGTH);
    this.firstName = firstName.trim();
    this.surname = surname.trim();
  }

  // Shallow copy constructor
  public FullName(FullName fullName) {
    if (fullName == null) {
      throw new IllegalArgumentException(FULL_NAME_CANNOT_BE_NULL);
    }
    this(fullName.firstName, fullName.surname);
  }

  public String firstName() { return firstName; }
  public String surname() { return surname; }
}
```

---

**Address (Value Object):**
- Extends `ValueObject`
- Meaningful as a whole — not as separate values within another class due to the naming of the value object and its attributes.
- As a value object the equals method (using Lombok) compares the type then entire state.
- Setting of values is only possible via the constructor — any changes to this object within the entity that contains it require a new object.

```java
import static staffs.DomainAssertions.argumentNotEmpty;

@EqualsAndHashCode(callSuper = false)
@ToString
public class Address extends ValueObject {
  public static final String HOUSE_NAME_NUMBER_NOT_EMPTY = "House name/number cannot be empty";
  public static final String STREET_NOT_EMPTY = "Street cannot be empty";
  public static final String TOWN_NOT_EMPTY = "Town cannot be empty";
  public static final String ADDRESS_NOT_NULL = "Address to copy cannot be null";

  private final String houseNameNumber;
  private final String street;
  private final String town;

  public Address(String houseNameNumber, String street, String town) {
    argumentNotEmpty(houseNameNumber, HOUSE_NAME_NUMBER_NOT_EMPTY);
    argumentNotEmpty(street, STREET_NOT_EMPTY);
    argumentNotEmpty(town, TOWN_NOT_EMPTY);
    // Add other guard rails here
    this.houseNameNumber = houseNameNumber.trim();
    this.street = street.trim();
    this.town = town.trim();
  }

  // Shallow copy constructor
  public Address(Address address) {
    if (address == null) {
      throw new IllegalArgumentException(ADDRESS_NOT_NULL);
    }
    this(address.houseNameNumber, address.street, address.town);
  }

  public String houseNameNumber() { return houseNameNumber; }
  public String street() { return street; }
  public String town() { return town; }
}
```

---

**How to define an ID in your entity:**
- Don't use a primitive int or long — use a UUID-based Identity value object
- Identity is a value object and not an entity — it doesn't need its own id

**Identity (Value Object):**

```java
@EqualsAndHashCode(callSuper = false)
public class Identity<T> extends ValueObject {
  public static final String IDENTITY_CANNOT_BE_NULL = "Identity cannot be null";
  public static final String IDENTITY_MUST_BE_UUID = "Identity must be a UUID";

  private final String id;

  private Identity(String id) {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException(IDENTITY_CANNOT_BE_NULL);
    }
    try {
      UUID.fromString(id);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(IDENTITY_MUST_BE_UUID);
    }
    this.id = id;
  }

  public static <T> Identity<T> of(String id) {
    return new Identity<>(id);
  }

  public static <T> Identity<T> generateId() {
    return new Identity<>(UUID.randomUUID().toString());
  }

  public String id() { return id; }
}
```

- Type parameter `<T>` — Identity knows which type it belongs to; helps avoid mixing up identities of different types at compile time (e.g. `Identity<Person>` vs `Identity<Order>`)
- Private constructor — can only be created via static factory methods `of()` or `generateId()`
- `of(String id)` — creates an Identity from an existing UUID string (used when loading from persistence)
- `generateId()` — generates a new random UUID (used when creating a new entity)
- UUID format: 8-4-4-4-12 digits (e.g. `00000000-0000-0000-0000-000000000001`)

*More on ID Creation Strategies:*
- Could be implemented by saving our object to the repository and retrieving the id.

*Identity Stability:*
- When we instantiate an entity via its constructor — we need to capture enough state to fully identify it.
- We typically **make setters private** (no use of `@Setter`) as their use is often ambiguous. Instead, we want an **intention revealing interface** = methods with names that clearly indicate their intent.
  - e.g. `changeFullName` over `setFullName`
  - `activate()` and `deactivate()` over `setActive()`
  - `isActive()` over `getActive()`
- Avoid calling more than one setter when fulfilling a request.
- Setters may only be called by methods from within the class including the constructor.
- Setter methods must not be available to clients of the entity.

---

**Entity SuperType:**

- Entity layer supertype (used by all entity classes) contains an `Identity` value object holding the id.
- Why do we need an identity in an entity? Why is the id a value object?
- Equals method should compare itself with another object based on the id only.
- As every entity needs an id we define it here — not a primitive int/long but an `Identity` class.
- Because `Identity` requires a Type — we need to pass this to `Entity`, in order for it to be applied to `Identity`.

```java
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public abstract class Entity<T> {
  public static final String IDENTITY_CANNOT_BE_NULL = "Identity cannot be null";

  @EqualsAndHashCode.Include
  protected final Identity<T> id;

  public Entity(Identity<T> id) {
    if (id == null) throw new IllegalArgumentException(IDENTITY_CANNOT_BE_NULL);
    this.id = id;
  }

  public Identity<T> id() { return id; }
}
```

- `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` — by default @EqualsAndHashCode calculates equality by comparing every field (fine for value objects — not for entities who should compare on identity only).
- `onlyExplicitlyIncluded = true` — ignore any attributes that do not feature `@EqualsAndHashCode.Include`.
- Compare the way **Entity** uses Equals (identity only) vs **ValueObject** uses Equals (entire state).

---

**Person (Entity):**

- Extends `Entity<Person>` — inherits an `Identity` from Entity, but must pass the specific class that `Identity` is using.
- Has two value objects: `FullName` and `Address`.
- `@ToString(callSuper = true)` — includes attributes from parent class as well as this class.
- No `@Setter` — changes are made via methods using ubiquitous language.
- `updateFullName` and `changeAddress` are domain-based setters (declared `final` to avoid Liskov Substitution issues when overriding).
- As we have domain-based setters that can be called directly as well as via the constructor, we can choose (as shown here) to call the 'setters' from the constructor — but we need to finalise the 'setter' methods to avoid any issues where this class is overwritten and Liskov Substitution might be compromised. **The alternative to this is to create helper methods that are called by the constructor and 'setters'.**
- Setters use the **copy constructor** methods of `FullName` and `Address` to ensure **defensive copying** of value object contents.
- DDD getters (`fullName()` and `address()`) use domain language — not prefixed with `get…`.
- Pre-condition assertions/guards are performed by `FullName` and `Address` — no need to add pre-guards here other than null checks.

```java
@ToString(callSuper = true)
public class Person extends Entity<Person> {
  public static final String FULL_NAME_CANNOT_BE_NULL = "Full name cannot be null";
  public static final String ADDRESS_CANNOT_BE_NULL = "Address cannot be null";

  private FullName fullName;
  private Address address;

  public Person(Identity<Person> id, FullName fullName, Address address) {
    super(id);
    updateFullName(fullName);
    changeAddress(address);
  }

  public final void updateFullName(FullName fullName) {
    if (fullName == null) throw new IllegalArgumentException(FULL_NAME_CANNOT_BE_NULL);
    this.fullName = new FullName(fullName);
  }

  public final void changeAddress(Address address) {
    if (address == null) throw new IllegalArgumentException(ADDRESS_CANNOT_BE_NULL);
    this.address = new Address(address);
  }

  public Identity<Person> id() { return id; }
  public FullName fullName() { return fullName; }
  public Address address() { return address; }
}
```

---

**Persistence vs Domain (why would we require both):**

Look at the Person entity above. If saving to a relational database, the Person class when saved would consist of (at least):
- `id` (from Identity)
- `firstName` (from FullName)
- `surname` (from FullName)
- `houseNameNumber` (from Address)
- `street` (from Address)
- `town` (from Address)

We would usually also have a **persistence id** for each entry in our table — to be distinguished from the Identity id.

As you can see, the Person in the infrastructure layer looks much different structurally from that of the domain layer.

Another example — **Order entity**:
- Could consist of: Order id, customer details, date/time, who placed the order, as well as `OrderItem(s)`.
- The Order in the persistence/infrastructure layer may look completely different from the domain layer entity.

---

**Unit Testing in DDD:**

*Why unit tests?*
- Fastest way to run tests (in-memory, no network/database calls needed)
- In DDD, domain objects are pure Java — no Spring context needed, fast feedback
- Tests validate both happy paths and guard conditions

*AAA Pattern (Arrange / Act / Assert):*
- **Arrange** — set up test data and objects
- **Act** — call the method under test
- **Assert** — verify the outcome

*Properties/Pillars of Good Unit Tests (lecture mentions these but full detail in PDF):*
- Fast
- Isolated/Independent
- Repeatable
- Self-validating
- Thorough

*Object Mother pattern:*
- A method that creates a valid test object, used to reuse test setup across multiple tests
- Example: `private FullName createValidFullName() { return new FullName("first1", "surname1"); }`
- Note: there is also a **Test Data Builder** pattern — using the builder pattern to create objects — but this is often more work

---

**Identity Tests (IdentityTests.java):**

```java
import static org.junit.jupiter.api.Assertions.*;

public class IdentityTests {
  private static class TestContext {}

  @Test
  @DisplayName("An identity cannot be null")
  void test01() {
    assertThrows(IllegalArgumentException.class, () -> Identity.of(null));
  }

  @Test
  @DisplayName("An identity cannot be blank")
  void test02() {
    assertThrows(IllegalArgumentException.class, () -> Identity.of(""));
  }

  @Test
  @DisplayName("An identity encapsulates its text value")
  void test03() {
    final String VALID_UUID_FORMAT = "00000000-0000-0000-0000-000000000001";
    Identity<TestContext> identity = Identity.of(VALID_UUID_FORMAT);
    assertDoesNotThrow(() -> UUID.fromString(identity.id()));
    assertEquals(VALID_UUID_FORMAT, identity.id());
  }

  @Test
  @DisplayName("New identities are generated by the system are UUID")
  void test04() {
    Identity<TestContext> generatedIdentity = Identity.generateId();
    assertDoesNotThrow(() -> UUID.fromString(generatedIdentity.id()));
  }
}
```

---

**FullName Tests (FullNameTests.java):**

Tests shown passing in PDF:
- Full names are considered the same when all parts are the same
- A full name requires a non-blank surname to be valid
- A full name requires a non-null surname to be valid
- A surname can be up to a specified maximum number of characters in length
- A surname exceeding the specified number of characters is rejected

```java
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import staffs.FullName;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

public class FullNameTests {
  private FullName createValidFullName() {
    return new FullName("first1", "surname1");
  }

  private String createTextOfLength(int length) {
    char[] chars = new char[length];
    Arrays.fill(chars, 'a');
    return new String(chars);
  }

  @Test
  @DisplayName("Full names are considered the same when all parts are the same")
  void test01() {
    FullName fullName1 = createValidFullName();
    FullName fullName2 = createValidFullName();
    assertEquals(fullName1, fullName2);
  }
  // ... additional tests (not all shown in handoff, but the PDF contains the full suite)
}
```

---

**Address Tests — EXERCISE (not yet written):**
- The Address Tests class was explicitly left incomplete in the PDF for students to write themselves.
- The section heading "Address Tests" exists on p.35 but the tests themselves are blank — this is the exercise.
- AddressTests.class is commented out in the TestSuite: `//AddressTests.class,`

---

**Person Tests (PersonTests.java — full code from PDF):**

```java
import static org.junit.jupiter.api.Assertions.*;

public class PersonTests {
  private Identity<Person> identity;
  private FullName fullName;
  private Address address;

  @BeforeEach
  void setUp() {
    identity = Identity.of("12345678-1234-1234-1234-123456789012");
    fullName = new FullName("first", "surname");
    address = new Address("houseNameNumber", "street", "town");
  }

  private Person createValidPerson() {
    return new Person(identity, fullName, address);
  }

  @Test
  @DisplayName("You can create a Person when all arguments are valid")
  void test01() {
    assertDoesNotThrow(() -> new Person(identity, fullName, address));
  }

  @Test
  @DisplayName("You cannot create a Person if the id is null")
  void test02() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () ->
      new Person(null, fullName, address)
    );
    assertEquals(Person.IDENTITY_CANNOT_BE_NULL, exception.getMessage());
  }

  @Test
  @DisplayName("You cannot create a Person if the full name is null")
  void test03() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () ->
      new Person(identity, null, address)
    );
    assertEquals(Person.FULL_NAME_CANNOT_BE_NULL, exception.getMessage());
  }

  @Test
  @DisplayName("You cannot create a Person if the address is null")
  void test04() {
    Throwable exception = assertThrows(IllegalArgumentException.class, () ->
      new Person(identity, fullName, null)
    );
    assertEquals(Person.ADDRESS_CANNOT_BE_NULL, exception.getMessage());
  }

  @Test
  @DisplayName("You can update a full name if that new full name is valid")
  void test05() {
    Person person = createValidPerson();
    FullName newFullName = new FullName("first2", "surname2");
    assertDoesNotThrow(() -> person.updateFullName(newFullName));
  }

  @Test
  @DisplayName("You cannot change the full name if that new full name is null")
  void test06() {
    Person person = createValidPerson();
    Throwable exception = assertThrows(IllegalArgumentException.class, () ->
      person.updateFullName(null)
    );
    assertEquals(Person.FULL_NAME_CANNOT_BE_NULL, exception.getMessage());
  }

  @Test
  @DisplayName("You can change an address if that new address is valid")
  void test07() {
    Person person = createValidPerson();
    Address newAddress = new Address("houseNameNumber2", "street2", "town2");
    assertDoesNotThrow(() -> person.changeAddress(newAddress));
  }

  @Test
  @DisplayName("You cannot change an address if that new address is null")
  void test08() {
    Person person = createValidPerson();
    Throwable exception = assertThrows(IllegalArgumentException.class, () ->
      person.changeAddress(null)
    );
    assertEquals(Person.ADDRESS_CANNOT_BE_NULL, exception.getMessage());
  }

  @Test
  @DisplayName("Two Persons with the same id are considered equal")
  void test09() {
    Person person1 = createValidPerson();
    FullName differentName = new FullName("first2", "surname2");
    Address differentAddress = new Address("houseNameNumber2", "street2", "town2");
    Person person2 = new Person(identity, differentName, differentAddress);
    assertEquals(person1, person2); // id's are the same so entities should be equal
  }
}
```

---

**Test Suite and Test Runner:**

```java
@Suite
@SelectClasses({
  IdentityTests.class,
  FullNameTests.class,
  //AddressTests.class,  // <-- commented out as tests not yet written
  PersonTests.class
})
public class TestSuite {}
```

```java
public class TestRunner {
  public static void main(String[] args) {
    Result result = JUnitCore.runClasses(TestSuite.class);
    for (Failure failure : result.getFailures()) {
      System.out.println(failure.toString());
    }
    System.out.println(result.wasSuccessful());
  }
}
```

Note: Can also run all tests by right-clicking on the domain test folder and selecting "Run Tests in domain".

---

### LECTURE 3 CONTENT — FULL DETAIL

---

#### Document 6: Enterprise Application Development — Domain Driven Design — Aggregates

**Relevant Learning Outcome:** DESIGN AN ENTERPRISE APPLICATION, CRITICALLY EVALUATING ALTERNATIVES AND JUSTIFYING SELECTIONS.
**Analysis | Communications | Problem Solving**

**Relevant KSBs:**
- C18 — Fluent in written communications and able to articulate complex issues.
- SE9 — How to operate at all stages of the software development lifecycle.

**Objectives:**
- Explain what is meant by an aggregate in domain driven design
- Explore how aggregates are used in a bounded context
- Identify suitable entities (including the aggregate root) and associated value objects
- Demonstrate how aggregates are implemented in domain model

---

**Recap Questions from Lecture 2 (included at start of Lecture 3 PDF):**
- What is the most significant difference between an entity and a value object?
- How does an entity distinguish itself from another entity?
- Give an example of a value object?
- How is a value object distinguished (equals method) from another value object?
- What is a pre-condition guard and how were these implemented in the examples last time using the DomainAssertions class? How does this relate to our entity and value object example shown in the lecture?
- What do we mean by an entity supertype — and what does it contain?
- What is the purpose of the Identity value object?
- How do the naming of getter methods differ in DDD to what you have seen previously?
- How do we write setter methods for entities?

---

**Bounded Context (reminder of its meaning):**
- Implementation of the core business logic within a specific Bounded Context. Contexts such as Ordering, Identity, Sales, Warehousing, etc.
- Objects can require and have different meanings in the different contexts — what we want to hold about a Book in a Sales context differs from what we want to know about a Book in a Warehousing context.
- Source: https://martinfowler.com/bliki/images/boundedContext/sketch.png

---

**Core Domain Model (reminder table):**

| Business Language | DDD Technical Language |
|---|---|
| Business Entities | **Aggregates**, Entities, Value Objects |
| Business Rules | Domain Rules |

---

**What is an Aggregate?**

Evans (pg 126) defines an aggregate as "a cluster [collection] of [entities and value objects] that we treat as a unit for the purpose of data changes."

Examples of data changes: adding a new buyer, updating the buyer's name, add/edit or remove a payment method etc.

Evans states that we should **favour small (cluster) aggregates** (objects that contain as few other objects as possible, denoting a small graph) when designing a system — as a large cluster could potentially hold lots of objects therefore performance would be an issue when we assign data to an aggregate, therefore several smaller aggregates in a [bounded] context would reduce the number of objects retrieved, or in memory at any one time.

The question is always **"what data is necessary to fulfil the business requirements?"** and the challenge is on deciding on what should be in each cluster! (More on this under the topic of Invariants).

An aggregate is a **special kind of entity**. It decomposes the domain model into chunks which are then easier to understand.

---

**Buyer (Aggregate Root) — the first example:**

When we make an order (in the ordering context) we need to store information about the buyer but clearly we also need to store the actual order information.

Buyer attributes:
```java
public class Buyer extends Entity<Buyer> implements AggregateRoot {
  private FullName fullName;
  private List<PaymentMethod> paymentMethods;
```

The above aggregate is a **small cluster aggregate** made up of a single entity featuring three value objects (FullName, PaymentMethod and Identity — inherited from Entity).

---

**Order Context — Class Diagram (full, both aggregates):**

The whole Order context contains two aggregates: **Buyer** and **Order**.

Buyer (aggregate root):
- `-id: Identity<Buyer>`
- `-fullName: FullName`
- `-paymentMethods: ArrayList<PaymentMethod>`
  - Business rule: can have from 1 to 3 payment methods

PaymentMethod (value object):
- `-cardNumber: String`
- `-cardHolderName: String`
- `-expirationDate: ExpirationDate`
- `-securityNumber: String`

FullName (value object):
- `-firstName: String`
- `-surname: String`

Identity<T> (value object):
- `-id: String`
- `-Identity(id: String)`

ExpirationDate (value object):
- `-expiryMonth: int`
- `-expiryYear: int`

Order (aggregate root):
- `-id: Identity<Order>`
- `-buyerId: String`
- `-restaurantId: String`
- `-fullNameOfBuyer: FullName`
- `-deliveryAddress: Address`
- `-orderedOn: LocalDate`
- `-orderStatus: OrderStatus`
- `-descriptionOfStatus: String`
- `-lineItems: List<OrderItem>`
- `-verifiedPaymentMethodID: String`

OrderItem (value object):
- `-orderID: String`
- `-productID: String`
- `-productName: String`
- `-unitPrice: Money`
- `-units: int`

OrderStatus (enum):
- `PENDING`
- `APPROVED`
- `SHIPPED`
- `REJECTED`

Address (value object):
- `-houseNameNumber: String`
- `-street: String`
- `-town: String`
- `-postalCode: String`

Money (value object):
- `-amount: BigDecimal`

---

**Aggregate Root — Key Points:**

- **Aggregate Root** represents the **main entry point** to our aggregate (cluster/graph of objects); it can be regarded as the **principle identifier** of a specific aggregate (it has an id that makes each instance of an aggregate unique).
- It **controls access [entry point] to the graph of objects** within it (entities and value objects) and their behaviour.
- The **root is also responsible for business rules**. e.g. Buyer cannot have more than 3 payment methods at any one time.
- As an aggregate root is an entity it **has an identity (known as its global identifier)**.
- As with other entities, it is not uncommon to have an aggregate with an attribute that is a reference to another aggregate (storing the id of the other aggregate as a value object).
- **Note:** storing the **id** of an aggregate (instead of storing the other aggregate as an object reference to the actual aggregate) is the **preferred way** of showing relationships and identifying specific aggregates.
  - This use of an identity rather than an object reference results in **looser coupling**.
  - It also results in a **smaller memory footprint** (as no eager loading of unwanted data).
- Other entity objects that are attributes of the aggregate will have a **local identity** — meaning that they can have an id that is unique only within the aggregate. We do not store this id 'outside' of the aggregate (that is, to store the entity id in another aggregate).
- We communicate with this aggregate via its root when wishing to access it or any objects that it holds.

**ADVANTAGES of this design approach:**
- Simplifies the design of the domain model — implementation is simplified as you require one class for the overall aggregate and **one repository per aggregate** — this also reduces the number of queries/transactions as a result — as only aggregate roots can be returned by database queries (from the repository directly or via a service) — other objects must be found via traversal of associations.

**CAUTION:**
- Single aggregates per bounded context can make the domain model difficult to maintain (this relates to having a large aggregate where it would be better served by splitting the existing aggregate into two or more smaller ones).
- This is because with a single larger aggregate, loading the entire aggregate to access a portion of its data may seem excessive.

**Invariants:**
- Conditions (that must be true at all times) associated within the Bounded Context by interacting with the aggregate root (once initialised), rather than the individual objects that it is made of.
- By doing this we **avoid consistency boundaries** as the design using aggregates enforces a consistency boundary.

---

**Buyer — Full Code (Aggregate Root):**

```java
import static staffs.FullName.FULL_NAME_CANNOT_BE_NULL;

public class Buyer<T> extends Entity<Buyer> implements AggregateRoot {
  public static final String PAYMENT_METHOD_CANNOT_BE_NULL = "Payment method cannot be null";
  public static final String PAYMENT_METHOD_ALREADY_EXISTS = "Payment method already exists";

  private FullName fullName;
  private List<PaymentMethod> paymentMethods;

  public Buyer(Identity<Buyer> id, FullName fullName) {
    super(id);
    updateFullName(fullName);
    this.paymentMethods = new ArrayList<>();
  }

  public Identity<Buyer> id() { return id; }

  public FullName fullName() { return fullName; }

  public void addVerifyPaymentDetails(PaymentMethod paymentMethod) {
    if (paymentMethod == null) {
      throw new IllegalArgumentException(PAYMENT_METHOD_CANNOT_BE_NULL);
    }
    if (paymentMethods.contains(paymentMethod)) {
      throw new IllegalArgumentException(PAYMENT_METHOD_ALREADY_EXISTS);
    }
    // Business Rule/Guard
    if (paymentMethods.size() == 3) {
      throw new IllegalArgumentException(NO_MORE_THAN_THREE_PAYMENT_METHODS_ACCEPTED);
    }
    this.paymentMethods.add(paymentMethod);
  }

  public void updateFullName(FullName fullName) {
    if (fullName == null) {
      throw new IllegalArgumentException(FULL_NAME_CANNOT_BE_NULL);
    }
    this.fullName = fullName;
  }

  public List<PaymentMethod> retrieveAllPaymentDetails() {
    return Collections.unmodifiableList(paymentMethods);
  }
}
```

---

**AggregateRoot Interface:**

```java
public interface AggregateRoot {}
```

- An empty marker interface — defines what entities are aggregates.

---

**ExpirationDate (Value Object associated with PaymentMethod — inside Buyer):**

```java
@EqualsAndHashCode(callSuper = false)
public class ExpirationDate extends ValueObject {
  public static final String CARD_HAS_ALREADY_EXPIRED = "Card has already expired";
  public static final String INVALID_MONTH_PROVIDED = "Invalid month provided";
  public static final String EXPIRATION_DATE_NOT_NULL = "Expiration date to copy cannot be null";

  private final int expiryMonth;
  private final int expiryYear;

  public ExpirationDate(int expiryMonth, int expiryYear) {
    if (expiryYear <= LocalDate.now().getYear() && expiryMonth < LocalDate.now().getMonthValue()) {
      throw new IllegalArgumentException(CARD_HAS_ALREADY_EXPIRED);
    }
    if (expiryMonth < 1 || expiryMonth > 12) {
      throw new IllegalArgumentException(INVALID_MONTH_PROVIDED);
    }
    this.expiryMonth = expiryMonth;
    this.expiryYear = expiryYear;
  }

  // Shallow copy constructor
  public ExpirationDate(ExpirationDate expirationDate) {
    if (expirationDate == null) {
      throw new IllegalArgumentException(EXPIRATION_DATE_NOT_NULL);
    }
    this(expirationDate.expiryMonth, expirationDate.expiryYear);
  }

  public String toString() {
    return String.format("expires %2d | %4d", expiryMonth, expiryYear);
  }

  public int expiryMonth() { return expiryMonth; }
  public int expiryYear() { return expiryYear; }
}
```

---

**PaymentMethod (Value Object associated with Buyer):**

Remember that a value object is just one or more fields that we want to group together that belong to an entity (in this case Buyer).

```java
@EqualsAndHashCode(callSuper = false)
@ToString
public class PaymentMethod extends ValueObject {
  public static final String CARD_HOLDER_CANNOT_BE_EMPTY = "Card holder name cannot be empty";
  public static final String CARD_NUMBER_CANNOT_BE_EMPTY = "Card number name cannot be empty";
  public static final String SECURITY_NUMBER_CANNOT_BE_EMPTY = "Security number cannot be empty";
  public static final String SECURITY_NUMBER_MUST_BE_THREE_DIGITS = "Security number must be 3 digits";
  public static final String PAYMENT_METHOD_CANNOT_BE_NULL = "Cannot copy a null PaymentMethod";

  // Omitting card type of payment
  private final String cardNumber;
  private final String cardHolderName;
  private ExpirationDate expirationDate;
  private final String securityNumber;

  public PaymentMethod(String cardHolderName,
                       String cardNumber,
                       ExpirationDate expirationDate,
                       String securityNumber) {
    argumentNotEmpty(cardHolderName, CARD_HOLDER_CANNOT_BE_EMPTY);
    argumentNotEmpty(cardNumber, CARD_NUMBER_CANNOT_BE_EMPTY);
    // Could add more validation here, e.g. card number format
    argumentNotEmpty(securityNumber, SECURITY_NUMBER_CANNOT_BE_EMPTY);
    argumentLength(securityNumber, 3, 3, SECURITY_NUMBER_MUST_BE_THREE_DIGITS);
    this.cardHolderName = cardHolderName.trim();
    this.cardNumber = cardNumber.trim();
    expirationDate = new ExpirationDate(expirationDate);
    this.securityNumber = securityNumber;
  }

  // Shallow copy constructor
  public PaymentMethod(PaymentMethod paymentMethod) {
    if (paymentMethod == null) {
      throw new IllegalArgumentException(PAYMENT_METHOD_CANNOT_BE_NULL);
    }
    this(paymentMethod.cardHolderName, paymentMethod.cardNumber,
        paymentMethod.expirationDate, paymentMethod.securityNumber);
  }

  public String cardHolderName() { return cardHolderName; }
  public String cardNumber() { return cardNumber; }
  public int monthOfExpiryAsInteger() { return expirationDate.expiryMonth(); }
  public String securityNumber() { return securityNumber; }
  public int yearOfExpiryAsInteger() { return expirationDate.expiryYear(); }
}
```

---

**Fictional Online Restaurant Ordering System:**

If we were to design a system for ordering meals online that are delivered to you, we might identify the following high-level domain areas:
- **Consumer (Identity)** — user details including name and address, payment details — these are essentially the buyer's details once authorised.
- **Restaurant** — name, menu items available
- **Order** — holds info about the Consumer's address, payment token, restaurant the order was taken at, menu items ordered (order lines), delivery information
- **Kitchen** — tickets for each order (and the items that make up the order), restaurant id
- **Delivery** — courier, time/date of delivery, location of delivery, order to be delivered

**Focus for this handout: the Ordering sub domain.**

A traditional domain model (not DDD based — Source: Microservice Patterns page 152) shows: Consumer (placed by) → Order (for) → Restaurant; Consumer (pays using) → PaymentInfo; Order (paid using) → DeliveryInfo, OrderLineItem → MenuItem → Address; Courier (assigned to) → Location. This model is missing explicit boundaries.

A DDD model (Source: Microservice Patterns page 156) shows three separate aggregates:
- **Order aggregate** (Order aggregate root + DeliveryInfo value object + PaymentInfo value object + OrderLineItem value object)
- **Consumer aggregate** (Consumer aggregate root + DeliveryInfo + PaymentInfo)
- **Restaurant aggregate** (Restaurant aggregate root)

Order references Consumer and Restaurant by **identity only**, not by object reference — this makes the boundaries explicit.

---

**Order Aggregate — Detailed:**

For an Ordering system we would have an **Order aggregate root** that contains:
- **id** — Identity of the Order
- **buyerId** — reference to the identity of the buyer (the person who ordered it — that is, the id of the user who has placed the order) — not a reference to the whole User
- **restaurantId** — reference to the Restaurant that the Order is for
- **fullNameOfBuyer** — FullName of the buyer
- **deliveryAddress** — Address where to send the delivery to
- **orderedOn** — Date/Time the order was placed
- **orderStatus** — Status of the order (starts in pending, then can be approved by the kitchen or rejected, then is shipped)
- **descriptionOfStatus** — Description of the status — text version of the status
- **lineItems** — one or more OrderLine objects representing what has been ordered
- **verifiedPaymentMethodId** — verified payment method id — used to identify if the order was made by Visa, Mastercard, etc.

We cannot access the OrderLines directly — we need to communicate with the Order to add, amend, remove or view order lines.

**You can have more than 1 aggregate within a bounded context** (which is what we have in Ordering as we have Order and Buyer).

---

**Money (Value Object in OrderItem):**

```java
import static staffs.DomainAssertions.*;

@ToString
@EqualsAndHashCode(callSuper = false)
public class Money extends ValueObject {
  public static final String AMOUNT_CANNOT_BE_EMPTY = "Amount cannot be empty";
  public static final String AMOUNT_MUST_BE_POSITIVE = "Amount must be greater than zero";
  public static final String MONEY_CANNOT_BE_NULL = "Money to copy cannot be null";

  private final BigDecimal amount;

  public Money(BigDecimal amount) {
    argumentNotEmpty(amount.toString(), AMOUNT_CANNOT_BE_EMPTY);
    if (amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException(AMOUNT_MUST_BE_POSITIVE);
    }
    this.amount = amount;
  }

  public Money(int i) {
    if (i <= 0) {
      throw new IllegalArgumentException(AMOUNT_MUST_BE_POSITIVE);
    }
    this.amount = new BigDecimal(i);
  }

  // Shallow copy constructor
  public Money(Money money) {
    if (money == null) {
      throw new IllegalArgumentException(MONEY_CANNOT_BE_NULL);
    }
    this(money.amount);
  }

  public Money add(Money delta) { return new Money(amount.add(delta.amount)); }
  public boolean isGreaterThanOrEqual(Money other) { return amount.compareTo(other.amount) >= 0; }
  public BigDecimal asBigDecimal() { return amount; }
  public String asString() { return amount.toPlainString(); }
  public Money multiply(int x) { return new Money(amount.multiply(new BigDecimal(x))); }
}
```

---

**OrderStatus (Enum used by Order):**

```java
public enum OrderStatus {
  PENDING("Awaiting approval"),
  APPROVED("Order approved"),
  SHIPPED("The order was shipped"),
  REJECTED("Order rejected");

  private final String description;

  OrderStatus(String description) {
    if (description == null) {
      throw new IllegalArgumentException("Description cannot be null");
    }
    this.description = description;
  }

  public String description() { return description; }
}
```

**Note:** String constants (which Phil typically uses for testing) cannot be defined in an enum class.

---

**OrderItem (Value Object used by Order):**

```java
import lombok.EqualsAndHashCode;
import staffs.ValueObject;
import static staffs.DomainAssertions.argumentNotEmpty;
import lombok.ToString;

@EqualsAndHashCode(callSuper = false)
@ToString
public class OrderItem extends ValueObject {
  public static final String ORDER_ID_CANNOT_BE_EMPTY = "OrderID cannot be empty";
  public static final String PRODUCT_ID_CANNOT_BE_EMPTY = "ProductID cannot be empty";
  public static final String PRODUCT_NAME_CANNOT_BE_EMPTY = "Product Name cannot be empty";
  public static final String UNIT_PRICE_CANNOT_BE_NULL = "Unit Price cannot be null";
  public static final String INVALID_PRICE = "Invalid price";
  public static final String INVALID_NUMBER_OF_UNITS = "Invalid number of units";

  private final String orderId;
  private final String productId;
  private final String productName;
  private final Money unitPrice;
  private int units;

  public OrderItem(String orderId,
                   String productId,
                   String productName,
                   Money unitPrice,
                   int units) {
    argumentNotEmpty(orderId, ORDER_ID_CANNOT_BE_EMPTY);
    argumentNotEmpty(productId, PRODUCT_ID_CANNOT_BE_EMPTY);
    argumentNotEmpty(productName, PRODUCT_NAME_CANNOT_BE_EMPTY);
    if (unitPrice == null) {
      throw new IllegalArgumentException(UNIT_PRICE_CANNOT_BE_NULL);
    }
    if (unitPrice.asBigDecimal().doubleValue() <= 0) {
      throw new IllegalArgumentException(INVALID_PRICE);
    }
    this.orderId = orderId;
    this.productId = productId;
    this.productName = productName;
    this.unitPrice = unitPrice;
    if (units <= 0) {
      throw new IllegalArgumentException(INVALID_NUMBER_OF_UNITS);
    }
    this.units += units;
  }

  public String orderId() { return orderId; }
  public String productID() { return productId; }
  public String productName() { return productName; }
  public int units() { return units; }
  public Money unitPrice() { return unitPrice; }
  public Money totalPrice() { return unitPrice().multiply(units()); }
}
```

---

**Order (Aggregate Root — Full Code):**

```java
import static staffs.DomainAssertions.argumentNotEmpty;
import static staffs.FullName.FULL_NAME_CANNOT_BE_NULL;

@ToString(callSuper = true)
public class Order extends Entity<Order> implements AggregateRoot {
  public static final String BUYER_ID_CANNOT_BE_EMPTY = "BuyerID cannot be empty";
  public static final String RESTAURANT_ID_CANNOT_BE_EMPTY = "RestaurantID cannot be empty";
  public static final String ORDER_ITEMS_CANNOT_BE_NULL_OR_EMPTY = "Order Items cannot be null or empty";
  public static final String PAYMENT_METHOD_DOES_NOT_BELONG_TO_BUYER = "Payment method does not belong to the buyer of this order";
  public static final String ADDRESS_CANNOT_BE_NULL = "Address cannot be null";

  private String buyerID;
  private final String restaurantID;
  private FullName fullNameOfBuyer;
  // Assume no ability to set a delivery time (otherwise Address and time would be in a separate value object and referred here)
  private Address deliveryAddress;
  private final LocalDate orderedOn;
  private OrderStatus orderStatus;
  private final String descriptionOfStatus;
  private final List<OrderItem> orderItems;
  private String verifiedPaymentMethodID;

  public Order(Identity<Order> id,
               String buyerID,
               String restaurantID,
               FullName fullNameOfBuyer,
               Address deliveryAddress,
               List<OrderItem> orderItems) {
    super(id);
    argumentNotEmpty(buyerID, BUYER_ID_CANNOT_BE_EMPTY);
    argumentNotEmpty(restaurantID, RESTAURANT_ID_CANNOT_BE_EMPTY);
    this.buyerID = buyerID;
    this.restaurantID = restaurantID;
    updateFullNameOfBuyer(fullNameOfBuyer);
    updateDeliveryAddress(deliveryAddress);
    if (orderItems == null || orderItems.isEmpty()) {
      throw new IllegalArgumentException(ORDER_ITEMS_CANNOT_BE_NULL_OR_EMPTY);
    }
    this.orderItems = new java.util.ArrayList<>(orderItems);
    orderedOn = LocalDate.now();
    orderStatus = OrderStatus.PENDING; // We will look at this in a later lecture
    descriptionOfStatus = orderStatus.description();
  }

  public Identity<Order> id() { return id; }
  public String buyerID() { return buyerID; }
  public Address deliveryAddress() { return deliveryAddress; }
  public String descriptionOfStatus() { return descriptionOfStatus; }
  public FullName fullNameOfBuyer() { return fullNameOfBuyer; }
  public LocalDate orderedOn() { return orderedOn; }
  public OrderStatus orderStatus() { return orderStatus; }

  public void approveOrder() {
    if (orderStatus == OrderStatus.PENDING) {
      orderStatus = OrderStatus.APPROVED;
    }
  }

  // reading data doesn't affect the validity of the aggregate
  public List<OrderItem> orderItems() {
    return Collections.unmodifiableList(orderItems);
  }

  public double orderTotal() {
    double total = 0;
    for (OrderItem item : orderItems) {
      total += item.totalPrice().asBigDecimal().doubleValue();
    }
    return total;
  }

  public void verifyPaymentMethod(String buyerID, String verifiedPaymentMethodID) {
    if (!this.buyerID.equals(buyerID)) {
      throw new IllegalArgumentException(PAYMENT_METHOD_DOES_NOT_BELONG_TO_BUYER);
    }
    this.buyerID = buyerID;
    this.verifiedPaymentMethodID = verifiedPaymentMethodID;
  }

  public void rejectOrder() {
    switch (orderStatus) {
      case OrderStatus.PENDING:
        orderStatus = OrderStatus.REJECTED;
        // Might need to add more logic here
        break;
      // Add more cases for other statuses if needed
      default:
    }
  }

  public void updateFullNameOfBuyer(FullName fullName) {
    if (fullName == null) {
      throw new IllegalArgumentException(FULL_NAME_CANNOT_BE_NULL);
    }
    this.fullNameOfBuyer = fullName;
  }

  public void updateDeliveryAddress(Address address) {
    if (address == null) {
      throw new IllegalArgumentException(ADDRESS_CANNOT_BE_NULL);
    }
    this.deliveryAddress = address;
  }
}
```

---

**Invariants (Business Rules):**

Invariants are rules that are needed for consistency (ensuring validity of state) within the aggregate — these must be maintained when data changes in the aggregate (involving relationships between members of the aggregate).

"When trying to discover the Aggregates in a Bounded Context, we must understand the model's true invariants. Only with that knowledge can we determine which objects should be clustered into a given Aggregate." (Vernon, 2013).

Our consistency boundary asserts that everything inside adheres to a specific set of business rules regardless of what operations we perform on our aggregate — and that it is consistent within a **single transaction**.

**Note (to come back to later in the module):** A properly designed Bounded Context modifies only **one aggregate instance per transaction** indicating that the UI should only request a single command on an Aggregate. e.g. for the Order Aggregate that might be: findOrderItem, orderTotal or cancelOrder (but this last one would be the result of an event which are additional methods in an aggregate — to be discussed in the events lecture).

**Evans Purchase Order example (pg 130, 131):**
- Every time a new item is added (via the appropriate method), the Purchase Order (method) checks to see if an item pushes the total over the approved limit.
- In terms of multiple users on the system, we need to lock the Purchase Order if it is being edited by a user to avoid violations of the invariant.
- Note: This invariant's effects could be compromised if the price of an item is changed….

At this point we can begin to improve the model by incorporating the following knowledge of the business:
1. Parts are used in many POs (high contention).
2. There are fewer changes to parts than there are to POs.
3. Changes to part prices do not necessarily propagate to existing POs. It depends on the time of a price change relative to the status of the PO.

**Other useful points about aggregates:**
- Objects within the aggregate may hold references to other aggregates (as value objects or simply Strings).
- Deletion must remove anything in the aggregate boundary.

---

**Domain Rules (can be injected via Strategy pattern rather than hard coded within):**

- These objects assist the Aggregate for any kind of business logic execution within the scope of a Bounded Context.
- Example 1: a "County Applicant Compliance Validation" Business Rule — depending upon the "county" of the Loan Application (e.g., Oxfordshire, Staffordshire) additional validation checks could be applicable to the loan applicant. This Domain Rule works with the Loan Aggregate to validate the Loan Application based on the county where the Loan Application is created.
- Example 2: For a doctor's booking system:
  - Allow scheduling of up to 60 ten-minute slots per day per doctor
  - Each slot can be booked only once
  - Patient can book a double visit if two adjacent slots are available
  - Patient can't book more than 10 slots in any calendar month

**Interesting reads:**
- Free Microsoft reads: https://dotnet.microsoft.com/en-us/learn/aspnet/microservices-architecture
- Interesting discussion of invariants: https://domaincentric.net/blog/modelling-business-rules-invariants-vs-corrective-policies

---

**Activity from Lecture 3 (p.28 — explicitly referenced as assessment-relevant):**

"For the assessment — we know what bounded contexts we need (see assessment) but compare to the example as that does help provide a comparison. What aggregates will be needed in each context? Within each aggregate what will form the aggregate root? What entities and value objects will be required?"

**→ This exercise is outstanding and requires the assessment brief to be uploaded before it can be actioned.**

---

**PlantUML of Ordering Context (for reference):**

```plantuml
@startuml
skinparam classAttributeIconSize 0
hide class circle

package "order" {
  class Buyer <<aggregate root>> {
    - id: Identity
    - fullName: FullName
    - paymentMethods: ArrayList<PaymentMethod>
  }
  class PaymentMethod <<value object>> {
    - cardNumber: String
    - cardHolderName: String
    - expirationDate: ExpirationDate
    - securityNumber: String
  }
  class FullName <<value object>> {
    - firstName: String
    - surname: String
  }
  class "Identity<T>" <<value Object>> {
    - id: String
    - Identity(id: String)
  }
  class ExpirationDate <<value object>> {
    - expiryMonth: int
    - expiryYear: int
  }
  class Order <<aggregate root>> {
    - id: Identity
    - buyerId: String
    - restaurantId: String
    - fullNameOfBuyer: FullName
    - deliveryAddress: Address
    - orderedOn: LocalDate
    - orderStatus: OrderStatus
    - descriptionOfStatus: String
    - lineItems: List<OrderItem>
    - verifiedPaymentMethodID: String
  }
  class Address <<value object>> {
    - houseNameNumber: String
    - street: String
    - town: String
    - postalCode: String
  }
  class OrderItem <<value object>> {
    - orderID: String
    - productID: String
    - productName: String
    - unitPrice: Money
    - units: int
  }
  class Money <<value object>> {
    - amount: BigDecimal
  }
  class OrderStatus <<enum>> {
    PENDING
    APPROVED
    SHIPPED
    REJECTED
  }

  Buyer --> PaymentMethod
  Buyer --> Identity
  Buyer --> FullName

  PaymentMethod --> ExpirationDate
  Order --> Identity
  Order --> FullName
  Order --> Address
  Order --> OrderItem
  Order --> OrderStatus
  OrderItem --> Money
}
@enduml
```

---

### LECTURE 4 CONTENT — FULL DETAIL

---

#### Document 7: Enterprise Application Development — Domain Driven Design — Preparation and considerations before the discussion of CQRS

**File:** `Enterprise_Application_Development_-_Preparation_and_considerations_before_the_discussion_of_CQRS_1_.pdf`
**Author:** Phil James
**Pages:** 22
**PDF created:** 26 June 2026

**Learning Outcomes (all three listed in this handout):**
- Critically evaluate development approaches to solutions to enterprise applications.
- Design an enterprise application, critically evaluating alternatives and justifying selections.
- Implement an enterprise application that includes relevant security features.

**Mapping to Knowledge, Skills and Behaviours (verbatim table from the PDF):**

| Key | Knowledge, Skills and Behaviour |
|---|---|
| K21 | How to operate at all stages of the software development life cycle and how each stage is applied in a range of contexts. For example, requirements analysis, design, development, testing, implementation. |
| K22 | Principles of a range of development techniques, for each stage of the software development cycle that produce artefacts and the contexts in which they can be applied. For example, UML, unit testing, programming, debugging, frameworks, architectures. |
| S18 | Use appropriate analysis methods, approaches and techniques in software engineering projects to deliver an outcome that meets requirements. |
| S19 | Implement software engineering projects using appropriate software engineering methods, approaches and techniques. |
| S21 | Determine, refine, adapt and use appropriate software engineering methods, approaches and techniques to evaluate software engineering project outcomes. |

**Maths, English and Digital Skills Development (verbatim):**
- **Maths**
  - Selecting appropriate values
  - Use of logic
  - Considering values for testing
- **English**
  - Reading a formal document and interpreting its content effectively
  - Use of specialised language
- **Digital Skills**
  - Communicating/Handling information and content/Problem solving – Install the required technologies and then utilise these to design and develop solutions.

**Contents (as printed in the PDF, with page numbers):**
- Records vs Classes — 3
  - Class version (using Lombok to reduce the boilerplate code) — 3
  - record version (no need for Lombok) — 3
  - FullNameDTO with validation — 4
  - FullName Value Object – Conversion from Class To record — 5
  - Impact of records on JPA Entities — 8
    - Buyer entity – how we could do it without the use of record (full name as separate strings – not an issue but slightly more work) — 8
    - Buyer entity – use of record for FullName — 9
    - SQL Issue vs Naming Convention for Value Objects in Table Schemas — 9
  - Summary of Classes vs Records — 10
  - Existing Case Study Value Object Classes Amended to Records — 10
  - Data Transfer Object (DTO) Pattern — 11
    - DTO Example BuyerDTO — 14
  - Data Mapper Pattern — 15
  - Monolith vs Modular Monolith (Modulith) — 16
    - Monolith Folder Structure (global package) — 17
    - Modulith Folder Structure — 18
  - Façade Pattern — 19
    - Why is the Façade public but the controllers private? — 19
  - Open Host Service Pattern — 19
    - BuyerController — 20
    - ContextFacade — 21
  - Shared Kernel Pattern — 22

---

**Records vs Classes (p.3):**

In 2021 Java introduced the **record** (lower case R) as a way of creating read-only classes. The inclusion of this type also provided something very similar to the **data class in Kotlin** — where there was less boilerplate code required (no need for Equals or HashCode, or getters/accessors).

- **Classes** are blueprints for behavioural objects (state plus methods to do things with that state) i.e. the data within them can change.
- **Records** are just data objects (what we might call **anaemic data classes**)
  - Fields/attributes are **immutable by default** (implicitly final).
  - They are also **final in terms of their design** — they cannot be extended by inheritance.
  - Interesting: the accessor naming style for records is the same as DDD — just the field name e.g. `firstName()` — rather than `getFirstName()`

By way of a comparison, consider the following DTO (data transfer object) — both as a class, then as a record.

> "I am deliberately not including any validation here…."

---

**Class version (using Lombok to reduce the boilerplate code) (p.3):**

```java
@ToString
@EqualsAndHashCode
@Getters
@AllArgsConstructor
public class FullNameDTO {
  private final String surname;
  private final String firstName;
}
```

*(Note: `@Getters` is written exactly as it appears in the PDF.)*

---

**record version (no need for Lombok) (p.3):**

```java
public record FullNameDTO (
   String firstName,
   String lastName) {
}
```

*(Note: this version uses `lastName`; later versions in the handout use `surname` — reproduced exactly as printed.)*

Not a huge difference between them when it is just something as simple as this.

If we wanted pre-guard checks in a constructor (and possibly other logic) then it would make a difference.

---

**FullNameDTO with validation (p.4):**

```java
public record FullNameDTO(
     String firstName,
     String surname
){
   public static final String FIRST_NAME_BLANK = "A first name is required";
   public static final String SURNAME_BLANK = "A surname is required";
   public static final String FIRST_NAME_TOO_LONG = "First name must be under 40 characters";
   public static final String SURNAME_TOO_LONG = "Surname must be under 40 characters";

    public FullNameDTO {
      if (firstName != null) firstName = firstName.trim();
      if (surname != null) surname = surname.trim();

        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException(FIRST_NAME_BLANK);
        }
        if (firstName.length() > 40) {
            throw new IllegalArgumentException(FIRST_NAME_TOO_LONG);
        }

        if (surname == null || surname.isBlank()) {
            throw new IllegalArgumentException(SURNAME_BLANK);
        }
        if (surname.length() > 40) {
            throw new IllegalArgumentException(SURNAME_TOO_LONG);
        }
    }
}
```

**Note (verbatim):** You could use Jakarta constraints for the validation (using annotations) but then you would have to consider that these occur **after the compact constructor**. Additionally you would need to explicitly call the Validator from the mapper class where the DTO was created.

---

**FullName Value Object – Conversion from Class To record (p.5–7):**

Do **NOT** confuse this with the `FullNameDTO` discussed previously — which is just there to map the relevant fields to return as a response to a client.

Let us look at the FullName Value object class which we introduced previously:

```java
import static staffs.DomainAssertions.argumentLength;
import static staffs.DomainAssertions.argumentNotEmpty;

@EqualsAndHashCode(callSuper = false)
@ToString
public class FullName extends ValueObject {
    public static final int MAX_FIRST_NAME_LENGTH = 20;
    public static final int MAX_SURNAME_LENGTH = 20;
    public static final String FIRST_NAME_NOT_EMPTY = "First name cannot be empty";
    public static final String SURNAME_NOT_EMPTY = "Surname cannot be empty";
    public static final String FULL_NAME_CANNOT_BE_NULL = "Full name to copy cannot be null";
    public static final String FIRST_NAME_LENGTH = "First name must be between 1 and ${MAX_FIRST_NAME_LENGTH} characters";
    public static final String SURNAME_LENGTH = "Surname must be between 1 and ${MAX_SURNAME_LENGTH} characters";

    private final String surname;
    private final String firstName;

    public FullName(String firstName, String surname){
        argumentNotEmpty(firstName, FIRST_NAME_NOT_EMPTY);
        argumentNotEmpty(surname, SURNAME_NOT_EMPTY);

        argumentLength(firstName, 1, MAX_FIRST_NAME_LENGTH, FIRST_NAME_LENGTH);
        argumentLength(surname, 1, MAX_SURNAME_LENGTH, SURNAME_LENGTH);

        this.firstName = firstName.trim();
        this.surname = surname.trim();
    }

    // Shallow copy constructor
    public FullName(FullName fullName) {
        if (fullName == null) {
            throw new IllegalArgumentException(FULL_NAME_CANNOT_BE_NULL);
        }
        this(fullName.firstName, fullName.surname);
    }

    public String firstName(){
        return firstName;
    }

    public String surname(){
        return surname;
    }
}
```

**What would this look like as a record?**

Before we explore this further, you are probably thinking that these would be great for Value Objects **BUT** from an implementation perspective, as value objects currently extend the `ValueObject` class, I am going to have to amend `ValueObject` to an **interface** rather than a class.

So amend ValueObject to this:

```java
public interface ValueObject {}
```

Now the record version of FullName looks like this:

```java
import static com.example.demo.common.DomainAssertions.argumentLength;
import static com.example.demo.common.DomainAssertions.argumentNotEmpty;

@Embeddable // used by JPA entity
public record FullName (
   String firstName,
   String surname
) implements ValueObject{
   public static final int MAX_FIRST_NAME_LENGTH = 20;
   public static final int MAX_SURNAME_LENGTH = 20;
   public static final String FIRST_NAME_NOT_EMPTY = "First name cannot be empty";
   public static final String SURNAME_NOT_EMPTY = "Surname cannot be empty";
   public static final String FULL_NAME_CANNOT_BE_NULL = "Full name to copy cannot be null";
   public static final String FIRST_NAME_LENGTH = "First name must be between 1 and ${MAX_FIRST_NAME_LENGTH} characters";
   public static final String SURNAME_LENGTH = "Surname must be between 1 and ${MAX_SURNAME_LENGTH} characters";

  public FullName {
    argumentNotEmpty(firstName, FIRST_NAME_NOT_EMPTY);
    argumentNotEmpty(surname, SURNAME_NOT_EMPTY);

    argumentLength(firstName, 1, MAX_FIRST_NAME_LENGTH, FIRST_NAME_LENGTH);
    argumentLength(surname, 1, MAX_SURNAME_LENGTH, SURNAME_LENGTH);
    // transform the parameters using trim (otherwise no need for the following lines)
    firstName = firstName.trim();
    surname = surname.trim();
  }
   // Shallow copy constructor
  public FullName(FullName fullName) {
     if (fullName == null) {
         throw new IllegalArgumentException(FULL_NAME_CANNOT_BE_NULL);
     }
     this(fullName.firstName, fullName.surname);
    }
}
```

*(Note: the import path changes from `staffs.DomainAssertions` in the class version to `com.example.demo.common.DomainAssertions` in the record version — reproduced exactly as printed.)*

Notice the opening part where the **interface is implemented** — this is different in format to a class. The **Lombok annotations are also absent** here.

Notice also the **compact constructor** — anything in a record constructor is carried out **BEFORE** the implicit assignment of the data passed to it — here we just so happen to want to trim the data (after the pre-guard checks have been conducted).

---

**Impact of records on JPA Entities (p.8):**

Whilst **the entities themselves must remain classes**, we can use records for attributes within the JPA entity, just as we did within the domain layer entities.

**Buyer entity – how we could do it without the use of record (full name as separate strings – not an issue but slightly more work) (p.8):**

```java
@Entity(name = "buyer")
@Table(name ="buyer")
@Getter
@Setter
@ToString(exclude = "paymentMethods") // prevent stack overflow loop
public class BuyerJpa {
  @Id
  @Column(name="id")
  private String id;

    @NotBlank(message = "First name is required")
    @Size(min = 5, message = "First name must be at least 5 characters long")
    @Pattern(regexp = "^[^0-9]*$", message = "First name must not contain numbers")
    @Column(name="first_name")
    private String firstname;

    @NotBlank(message = "Surname is required")
    @Size(min = 5, message = "Surname must be at least 5 characters long")
    @Pattern(regexp = "^[^0-9]*$", message = "Surname must not contain numbers")
    @Column(name="surname")
    private String surname;

    @OneToMany(mappedBy = "buyer", cascade = {CascadeType.ALL}, orphanRemoval = true)
    private List<PaymentMethodJpa> paymentMethods = new ArrayList<>();;
}
```

*(Note: the double semicolon `new ArrayList<>();;` appears in the PDF as printed.)*

---

**Buyer entity – use of record for FullName (p.9):**

Make sure that your record FullName has the following:

```java
@Embeddable
public record FullName (
```

`BuyerJpa` – note the change – using the FullName value object (record).

```java
@Entity(name = "buyer")
@Table(name ="buyer")
@Getter
@Setter
@ToString(exclude = "paymentMethods") // prevent stack overflow loop
public class BuyerJpa {
  @Id
  @Column(name="id")
  private String id;

    @Embedded
    @Valid
    private FullName fullName;

    @OneToMany(mappedBy = "buyer", cascade = {CascadeType.ALL}, orphanRemoval = true)
    private List<PaymentMethodJpa> paymentMethods = new ArrayList<>();;
}
```

---

**SQL Issue vs Naming Convention for Value Objects in Table Schemas (p.9) — full text:**

Embedding value objects in an entity brings with it a need to amend the naming conventions that were originally recommended for attributes belonging to a value object saved within a table. It is typical for the naming of the value object data fields to start with the value object followed by the specific field name. **This practice does not translate when you make use of records.**

*(Section contains no further text, code or figure in the PDF — it ends there.)*

---

**Summary of Classes vs Records (p.10):**

- Need data that is **immutable**? **records** (ideal for transferring between layers in an application)
- Need data that **changes**? **Classes**
- Need **inheritance and polymorphic behaviour**? **Classes**.
- Needs **interfaces**? **Classes or records**
- Need **complex business logic**? **Classes**

**Additional things:**

- unlike classes where you MUST have a standard getter method to allow Spring to implicitly serialise/deserialise data to and from a constructor, with a record it will **natively serialise/deserialise** with a record.
- records are **more secure and faster** than classes at serialisation (converting data into a byte stream for transportation), or de-serialising data (converting back into objects) - note that you have to implement the `Serializable` interface for this to happen.
  **Why is it faster?** Because Java ignores any custom `writeObject` and `readObject` methods that may have been written to avoid security exploits that standard classes can have.
- **Switch statements can deconstruct a record inside the condition**, this is not possible with classes.

**Link:** JEP 395: Records — https://openjdk.org/jeps/395

---

**Existing Case Study Value Object Classes Amended to Records (p.10):**

In the case study code, you should note (take a look) at the following Value Objects that have now been refactored to records (**a useful exercise in the differences between the two forms**):

- Identity
- FullName
- Address
- PaymentMethod
- ExpirationDate

Existing JPA Entities that can use record value objects in order to reduce the number of attributes/fields:

- **BuyerJpa** (discussed) – full name
- **OrderJpa** – full name, address,
- **PaymentMethodJpa** – expiration date

---

**Data Transfer Object (DTO) Pattern (p.11–13):**

> "An object that carries data between processes in order to reduce the number of method calls." …" DTOs are called Data Transfer Objects because their whole purpose is to shift data in expensive remote calls." (Fowler, 2002)

> "A DTO is nothing more than a container class that exposes properties but no methods. A DTO is helpful whenever you need to group values in ad hoc structures for passing data around." (Esposito, 2008, for Microsoft).

**Figure 1: Data Transfer Object Pattern (Fowler, 2002)** — reproduced UML class diagram from Fowler's *Patterns of Enterprise Application Architecture* showing the DTO pattern using the Album/Track example: an **Assembler** class that maps between the domain objects and the DTOs (`AlbumDTO`, `TrackDTO`), with the DTOs exposing only get/set accessors for their data fields and the assembler holding the `writeDTO`/`createAlbum`-style conversion methods. *(Image is embedded in the PDF at low resolution; the diagram is Fowler's standard DTO figure.)*

**Figure 2: Microsoft explanation of where a DTO is used** — layered diagram showing the presentation layer communicating with the service layer via DTOs, with an assembler/mapper converting domain entities to and from DTOs, and the domain and data access layers sitting below the service layer.

Source: https://docs.microsoft.com/en-us/archive/msdn-magazine/2009/brownfield/pros-and-cons-of-data-transfer-objects#id0080022

DTOs are typically plain objects (in Java we call them **POJO**) that have zero argument constructors (mainly due to the getters used by the serialiser), and are **serialisable** to allow communication remotely.

**Note (verbatim):** Some argue that DTOs should only be used at the controller layer and be converted to/from entities there - rather than in the service (application) layer (but this violates what a controller's responsibility is really about – namely to interact the UI and as you can see above many (including Microsoft) support the service layer (**our service layer is split up as query handlers and command handlers**) as the place that DTOs are converted to/from an entity.

In the **first session of this module, we built a simple API**; this API omitted certain best practices and patterns. For example, **we don't typically serialise JPA entities (as JSON) directly to a client** when we request some data, and **we don't usually have a controller communicate directly with a repository** (as it breaks SRP).

Instead we make a class that contains just the data we want to return to the user (typically as a JSON payload). Then we simply map (convert) that JPA entity data to a DTO using the **data mapper pattern** (discussed later on in this handout).

When a controller returns a DTO (or collection of DTOs) to the client, **Spring first of all implicitly converts them to JSON** (there are a number of considerations that could be made at this point re fields, annotations, etc which we would typically do if what we were returning was not a record and a DTO at that).

**Why is it useful to map the data in this way, rather than simply returning JPA entities?**

- **Avoids exposing sensitive fields** (we saw this in the OOP module where we could map fields for exclusion - but we had to be careful).
- **Avoids over-fetching data** – data required might be a few fields but the entity retrieves a lot more
- **Avoids coupling to the infrastructure layer**

More here: *Best Practices for Data Transfer Objects (DTOs) in Software Development | Medium* — https://medium.com/@samuelcatalano/best-practices-for-data-transfer-objects-dtos-d5007e3f2729 — there is some discussion here re use of the **Builder pattern**, or a **DTO factory** – we don't need a factory when we have **Spring IoC**.

---

**Comparing DTOs to Entities (p.13 table, verbatim):**

| Entity | Data Transfer Object |
|---|---|
| Represent all data for a single table/entity persisted to a database | Container for data transporting between layers |
| Every instance represents a row in a table. | Every instance contains ALL the data required by the UI or to send to lower layers in the application to be processed/stored. |
| Represents a single entity/table. | Can contain some or all data that relates to an entity – as required (no unnecessary data).<br><br>Can contain data from more than one entity (as required by the UI). |

**Points in favour of DTOs:**

- Potentially require multiple calls to move more than one object which is an expensive approach to implementation (when we could have a single call, passing a DTO, that passes all the data that we require).
- "use when you have a significant mismatch between the model in your presentation layer and the underlying domain model" (Fowler)
- We do not want to expose the entire entity – memory/redundancy or security reasons.
- Client is separated from the entities so any changes to entities are not impacting on the client code = **reduced coupling between layers**.
- Client is separated from the entities so any changes to the UI can be handled by DTOs rather than entities.
- **Avoids circular references when serialising** which would be a problem when working directly with entities.

**Points against DTOs:**

- Additional coding overhead to create/manage DTOs for each entity (if they were a large number of them) as well as convertor logic (although this can be done via Mapper classes which negates convertors in favour of 3rd party dependency code).
- "You can do without DTOs only if the presentation layer and the service layer are co-located in the same process. In this case, you can easily reference the entity assembly from within both layers without dealing with thorny issues such as remoting and data serialization".

**Middle Ground:**

- For large projects where presentation and service layers are in the same process (as discussed above) – use entities where possible (but it increases coupling).

---

**DTO Example BuyerDTO (p.14):**

Compare the following code with the **Buyer aggregate** (remember this is a data class and has to hold the fields required by a request plus guard conditions, but **not any behaviours** that buyer had).

```java
public record BuyerDTO(
     String id,
     String firstName,
     String surname
){
   public static final String ID_BLANK = "Buyer ID is required";
   public static final String FIRST_NAME_BLANK = "A first name is required";
   public static final String SURNAME_BLANK = "A surname is required";
   public static final String FIRST_NAME_TOO_LONG = "First name must be under 40 characters";
   public static final String SURNAME_TOO_LONG = "Surname must be under 40 characters";

    public BuyerDTO {
      if (id != null) id = id.trim();
      if (firstName != null) firstName = firstName.trim();
      if (surname != null) surname = surname.trim();

        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(ID_BLANK);
        }
        if (firstName == null || firstName.isBlank()) {
            throw new IllegalArgumentException(FIRST_NAME_BLANK);
        }
        if (firstName.length() > 40) {
            throw new IllegalArgumentException(FIRST_NAME_TOO_LONG);
        }
        if (surname == null ||surname.isBlank()) {
            throw new IllegalArgumentException(SURNAME_BLANK);
        }
        if (surname.length() > 40) {
            throw new IllegalArgumentException(SURNAME_TOO_LONG);
        }
    }
}
```

---

**Data Mapper Pattern (p.15):**

This pattern acts as an **isolation barrier** between the different parts of the application. It **translates data from one object to another**.

It is most likely to be seen in the following two contexts:

- JPA entities to DTOs (retrieving data before sending it to the client)
- JPA entities to domain objects (retrieving data before assigning it to an aggregate)
- Domain objects to JPAs (converting the aggregate into one or more JPA instances)

*(Note: the PDF says "two contexts" but then lists three — reproduced as printed.)*

The following is part of the `BuyerJpaToDTOMapper` class – included just to illustrate a point.

```java
public class BuyerJpaToDTOMapper {
  public static BuyerDTO toBuyerDTO(BuyerJpa buyer) {
    Objects.requireNonNull(buyer, "Buyer JPA entity cannot be null");

      return new BuyerDTO(
         buyer.getId(),
         buyer.getFullName().firstName(),
         buyer.getFullName().surname()
      );
  }


// see code file for the rest of this class
```

More here: *Data Mapper* — https://martinfowler.com/eaaCatalog/dataMapper.html

`BuyerJPA` has `@Getter` implemented as it is **not a domain model class** so does not need to follow the DDD naming conventions for accessor methods (though as `FullName` within the `BuyerJPA` is a record, it does follow this naming convention as you can see above).

**Entities are not good candidates for records** (for a number of reasons that include mutability and the requirement for a no-args constructor) – but they can be used as fields within JPA entities as we have already demonstrated.

---

**Monolith vs Modular Monolith (Modulith) (p.16–18):**

On day one we created a simple API as a **Spring Boot monolith**.

The sessions on **Entities and Value Objects**, as well as the one on **Aggregates** just focused on classes so did not consider the question of **where to put this domain layer code** in our application.

Here we are going to start to create an application that includes **all of the layers described by Evans** (included as a reminder below):

> **Figure (p.16):** Evans' layered architecture diagram — the four stacked layers (User Interface, Application, Domain, Infrastructure) with each layer depending on the one below.
> **Source: Evans, pg 72**
> *(Cross-reference: the full four-layer breakdown is recorded in the Lecture 2 section of this handoff — User Interface, Application, Domain/Model, Infrastructure.)*

We are going to implement this via the **Modular Monolith pattern, also known as a Modulith** (rather than the existing Monolith pattern).

The idea behind the modular monolith is a **better separation of code**, particularly for Domain Driven Design projects with their architectural separation around **bounded contexts**.

---

**Monolith Folder Structure (global package) (p.17) — verbatim:**

- `com.example.demo` (or whatever your package is called)
  - application class
  - global exception handler
  - **ui**
    - controllers e.g. OrderController, BuyerController
    - DTOs e.g. BuyerDTO, FullNameDTO, AddressDTO, PaymentMethodDTO, OrderDTO, OrderLineDTO,
  - **application**
    - services OR CQRS (command and queries handlers)
      e.g. OrderService
      e.g. OrderQueryHandler, and OrderCommandHandler
    - read/query mappers
      e.g. BuyerJpaToDTOMapper, OrderJpaToDTOMapper
  - **domain**
    - aggregates (and entities/value objects)
      e.g. Aggregates: Order, Buyer
      Entities: OrderLine
      Value Objects: PaymentMethod, Money, ExpirationDate, Identity, FullName, Address
      DomainAssertions, any interfaces required (e.g. Aggregate, ValueObject)
  - **infrastructure**
    - repositories
      e.g. OrderRepository, BuyerRepository
    - entities
      e.g. OrderJpa, BuyerJpa

More here: *Service Layer* — https://martinfowler.com/eaaCatalog/serviceLayer.html

More here: *CQRS* — https://martinfowler.com/bliki/CQRS.html

---

**Modulith Folder Structure (p.18) — verbatim:**

There is just a **common** package, as well as **ordering** bounded context here… and there will be others added to the case study in future handouts.

- `com.example.demo`
  - application class
  - global exception handler
  - **common (shared kernel)**
    - Value Objects: PaymentMethod, Money, Identity, FullName, Address
    - DomainAssertions
    - Entity
    - Any interfaces required (e.g. Aggregate, ValueObject)
  - **ordering**
    - context façade
    - **ui (hidden)**
      - controllers e.g. OrderController
      - exceptions that relate to this layer
    - **application (hidden)**
      - services
        e.g. OrderService
      - or CQRS – command and queries handlers
        e.g. OrderQueryHandler, and OrderCommandHandler
      - read/query mappers
        e.g. BuyerJpaToDTOMapper, OrderJpaToDTOMapper
      - exceptions that relate this application layer
    - **domain (hidden)**
      - aggregates (and entities/value objects)
        e.g. Aggregates: Order, Buyer
        Entities: OrderLine
        Value Objects: PaymentMethod, ExpirationDate
      - exceptions that relate to this layer
    - **infrastructure (hidden)**
      - repositories
      - e.g. OrderRepository, BuyerRepository
      - entities
      - e.g. OrderJpa, BuyerJpa
      - exceptions that relate to this layer

**Note (verbatim):** Each of these top level packages is a **module**. Only code in the root of this folder is visible (unless we add a `package-info` file), the rest is hidden, so we cannot accidently import classes that should be internal.

**Note (verbatim):** This approach makes **converting to a microservice architecture more straightforward** (if that becomes necessary).

---

**Façade Pattern (p.19):**

In this case study I am also choosing to implement a **façade pattern**. *(Hyperlinked in the PDF to: https://www.tutorialspoint.com/design_pattern/facade_pattern.htm)*

In the **UI folder** the controller will expose API endpoints, then **communicate with the facade** as needed.

If any change are required to the layers below then **we do not need to change the controller** as it retains a **stable contract with the façade**.

This separation enables the controller to focus on the **single responsibility of HTTP requests**, validating those requests, extracting any data passed in those requests and returning data and status codes.

**Why is the Façade public but the controllers private? (p.19)**

If the point of entry for web requests is the controller, then why are they in the (private) ui folder but the façade is not? **We need the façade to be accessible to other code in our Modulith, but our controllers to be inaccessible for the same reason.**

What we see here demonstrated is the difference between the **internal visibility**, the **architectural boundaries** and the **external, network boundaries**.

`@SpringBootApplication` will scan the project for any `@RestController` classes which handle external HTTP requests, **regardless of whether the classes are private (sub folder of our module) or public (at the root level of our module)**.

The internal visibility (or lack of) is **purely about coupling**, the dependency on something (or avoiding it where possible).

---

**Open Host Service Pattern (p.19):**

The visibility of the façade **enables communication between this module and others**. The façade is the **API for this module (service) for the rest of the system**. If any other modules need something from this module/context then **they must communicate with the façade only**.

**Note (verbatim):** That is not to say that we cannot communicate in other ways, such as **via events**.

Read more: *Open Host Service – Domain-driven Design: A Practitioner's Guide* — https://ddd-practitioners.com/home/glossary/bounded-context/bounded-context-relationship/open-host-service/

To illustrate the façade and OHS patterns we will consider some of the code from the case study.

---

**BuyerController (p.20):**

The controller receives HTTP requests and **collaborates with the context façade** in order to return a suitable response.

Any exceptional behaviour will be **picked up by the global exception handler**.

```java
@RequestMapping("/buyers")
@RestController
@AllArgsConstructor
public class BuyerController {
  private final ContextFacade facade;

  @GetMapping
  @ResponseStatus(HttpStatus.OK)
  public Iterable<BuyerDTO> getAllBuyerDetails() {
    return facade.findAllBuyers();
  }

  @GetMapping("/{buyer_id}")
  @ResponseStatus(HttpStatus.OK)
  public BuyerDTO getBuyerById(@PathVariable String buyer_id){
    return facade.findBuyerById(buyer_id);
  }

  @GetMapping("/{buyer_id}/paymentDetails")
  @ResponseStatus(HttpStatus.OK)
  public List<PaymentMethodDTO> getPaymentDetailsForBuyer(@PathVariable String buyer_id){
    return facade.findPaymentsForBuyerById(buyer_id);
  }
}
```

---

**ContextFacade (p.21):**

The context façade **interacts with dependencies downstream** – in this case (at least for this week) they are **query handlers** (services that just handle read requests – more on this later on).

This façade **collaborates with all aggregates in the bounded context**, not just Buyer (or Order).

It is **public facing** so other modules can access this file if they want to make use of the services that it offers.

```java
@Component
@AllArgsConstructor
public class ContextFacade {
  private final BuyerQueryHandler buyerQueryHandler;
  private final OrderQueryHandler orderQueryHandler;

    // Not recommended to have this method (too much data)
    //e.g. http://localhost:8900/buyers/findAll
    public Iterable<BuyerDTO> findAllBuyers() {
       return buyerQueryHandler.findAllBuyers();
    }

    // e.g. http://localhost:8900/buyers/0000
    public BuyerDTO findBuyerById(String buyerId) {
       return buyerQueryHandler.findBuyerById(buyerId);
    }

    // e.g. http://localhost:8900/buyers/paymentDetails/0000
    public List<PaymentMethodDTO> findPaymentsForBuyerById(String buyerId) {
       return buyerQueryHandler.findPaymentsForBuyerById(buyerId);
    }

  // e.g. http://localhost:8900/orders/findAll - but not recommended as can result in a large response
  public Iterable<OrderDTO> findAllOrders() {
     return orderQueryHandler.findAllOrders();
  }

    // e.g. http://localhost:8900/orders/buyer/0000
    public Iterable<OrderDTO> findOrdersByBuyerId(String buyerId) {
       return orderQueryHandler.findOrdersByBuyerId(buyerId);
    }

    // e.g. http://localhost:8900/orders/1111
    public OrderDTO findOrderByOrderId(String orderId) {
       return orderQueryHandler.findOrderByOrderId(orderId);
    }
}
```

---

**Shared Kernel Pattern (p.22):**

**Common's inclusion is an example of the Shared Kernel pattern.**

This will contain code that is **shared between two or more bounded contexts** (I appreciate that presently there is only one bounded context: ordering).

This sharing of classes should, of course, be **carefully managed** but its intent is a **reduction of code** where one or more classes are used by different contexts.

Presently, we would add the following files to this folder (as they will be shared across bounded contexts (packages in our modulith):

- DomainAssertions
- AggregateRoot
- Entity
- ValueObject
- Identity
- FullName
- Address
- Money

Read more: https://ddd-practitioners.com/home/glossary/bounded-context/bounded-context-relationship/shared-kernel/

---

### LECTURE 5 CONTENT — FULL DETAIL

---

#### Document 8: Enterprise Application Development — CQRS: Queries

**File:** `Enterprise_Application_Development_-_CQRS_-_Queries.pdf`
**Author:** Phil James
**Pages:** 42

**Learning Outcomes (all three listed in this handout):**
- Critically evaluate development approaches to solutions to enterprise applications.
- Design an enterprise application, critically evaluating alternatives and justifying selections.
- Implement an enterprise application that includes relevant security features.

**Mapping to Knowledge, Skills and Behaviours (verbatim table from the PDF):**

| Key | Knowledge, Skills and Behaviour |
|---|---|
| K21 | How to operate at all stages of the software development life cycle and how each stage is applied in a range of contexts. For example, requirements analysis, design, development, testing, implementation. |
| K22 | Principles of a range of development techniques, for each stage of the software development cycle that produce artefacts and the contexts in which they can be applied. For example, UML, unit testing, programming, debugging, frameworks, architectures. |
| S18 | Use appropriate analysis methods, approaches and techniques in software engineering projects to deliver an outcome that meets requirements. |
| S19 | Implement software engineering projects using appropriate software engineering methods, approaches and techniques. |
| S21 | Determine, refine, adapt and use appropriate software engineering methods, approaches and techniques to evaluate software engineering project outcomes. |

**Academic Disclosure (verbatim):**
In an attempt to provide additional visual imagery to help aid learning, the versions of the ERD and sequence diagram used in this handout were **initially generated (due to time constraints) by AI from my own source code, then amended/adapted/corrected**.

**Maths, English and Digital Skills Development (verbatim):**
- **Maths**
  - Selecting appropriate values
  - Use of logic
  - Considering values for testing
- **English**
  - Reading a formal document and interpreting its content effectively
  - Use of specialised language
- **Digital Skills**
  - Communicating/Handling information and content/Problem solving – Install the required technologies and then utilise these to design and develop solutions.

**Contents (as printed in the PDF, with page numbers):**
- New Project Set Up — 5
  - application.yaml — 7
- Boundaries of Context for a Fictional Ordering System (reminder) — 8
- Folder Structure for Spring Modulith — 9
  - Modulith Folder Structure (reminder) — 10
- Custom Exceptions — 11
  - BuyerNotFoundException — 11
  - OrderNotFoundException — 11
- GlobalExceptionHandler — 12
- Order Context: Creating our Tables and Adding Data (in the resources folder) — 14
  - schema.sql — 15
  - data.sql — 16
- Domain Model Operations — 18
  - Command-Query Responsibility Segregation (CQRS) Architectural Pattern — 18
- Buyer Aggregate — 20
- Find All Buyers — 21
  - Find All Buyers - BuyerController — 22
  - Find All Buyers - ContextFacade — 23
  - Find All Buyers - BuyerQueryHandler (application folder) — 24
  - BuyerRepository (persistence/repositories) — 24
  - Find All Buyers - BuyerJpaToDTOMapper (application/mappers) — 25
  - BuyerJpa (persistence/entities) — 25
- Find Buyer By Id — 26
  - Find Buyer By Id - BuyerController — 27
  - Find Buyer By Id - ContextFacade — 27
  - Find Buyer By Id - BuyerQueryHandler (application folder) — 28
  - Find Buyer By Id - BuyerRepository (persistence/repositories) - reminder — 28
  - Find Buyer By Id - BuyerJpaToDTOMapper (application/mappers) - reminder — 29
  - Find Buyer By Id - BuyerJpa (persistence/entities) - reminder — 29
- Find Payment Details for Buyer Id — 30
  - Find Buyer Payment Details for Buyer Id - BuyerController — 31
  - Find Buyer Payment Details for Buyer Id - ContextFacade — 31
  - Find Buyer Payment Details for Buyer Id - BuyerQueryHandler (application folder) — 32
  - Find Buyer Payment Details for Buyer Id - BuyerRepository (persistence/repositories) — 32
  - Find Buyer Payment Details for Buyer Id - BuyerJpaToDTOMapper (application/mappers) — 33
  - Find Buyer Payment Details for Buyer Id - PaymentMethodJpa (persistence/entities) — 34
- Order — 35
- Service Layer Pattern — 36
  - Comparing CQRS with the Service Layer — 37
- Appendix A – PlantUML for ERD — 38
- Appendix B - PlantUML for Sequence diagram findAll — 40
- Appendix C - PlantUML for Sequence diagram findBuyerById — 40
- Appendix D - PlantUML for Sequence diagram findPaymentsForBuyerById — 42

---

**New Project Set Up (p.5):**

This lecture builds the case-study application from scratch as a Spring Boot project via `start.spring.io` (the New Project dialog in IntelliJ). The screenshot on p.5 shows the following selections (with the important ones underlined in red in the handout):

- **Server URL:** start.spring.io
- **Name:** `demo`
- **Location:** `C:\Temp\CaseStudy` (project created in `C:\Temp\CaseStudy\demo`)
- **Create Git repository:** ticked
- **Language:** Java
- **Type:** **Maven** (not Gradle-Groovy or Gradle-Kotlin)
- **Group:** `staffs`
- **Artifact:** `demo`
- **Package name:** `com.example.demo`
- **JDK:** openjdk-26 (Oracle OpenJDK 26.0.1)
- **Java:** **26**
- **Packaging:** Jar
- **Configuration:** **YAML** (not Properties)

Dependencies selected:

- **Developers Tools:** Spring Boot DevTools, Lombok, **Spring Modulith**.
- **Web:** Spring Web
- **SQL:** Spring Data JPA, H2 Database

**Link:** Spring Modulith :: Spring Modulith — https://docs.spring.io/spring-modulith/reference/

After the project is created, the screenshot shows `main → java → com.example.demo → DemoApplication`. Instruction: **Right click – New Module**.

**Adding a module (New Module dialog screenshot, p.6):** the `common` module is created as a Spring Boot module with **Name:** `common`, **Location:** `C:\Temp\CaseStudy\demo` (module created in `C:\Temp\CaseStudy\demo\common`), **Language:** Java, **Type:** Maven, **Group:** `staffs`, **Artifact:** `common`, **Package name:** `staffs.common`, **JDK:** Project JDK openjdk-26, **Java:** 26, **Configuration:** YAML.

---

**Adding constraints to our JPA entities (p.6):**

We will be applying appropriate validation constraints.

**Add the following dependency to pom.xml:**

```xml
<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
  </dependency>
```

Then click the icon in the top right of the code window – to download this dependency. *(The handout shows the small Maven "Load Maven Changes" reload icon.)*

---

**application.yaml (p.7):**

```yaml
spring:
  application:
    name: api
  jpa:
    hibernate:
      ddl-auto: none
server:
  port: 8900
```

*(Note: `ddl-auto: none` — Hibernate does not generate the schema; the schema is created manually via `schema.sql`. The server runs on port **8900**, which matches the endpoint URLs used throughout the handout, e.g. `http://localhost:8900/buyers`.)*

---

**Boundaries of Context for a Fictional Ordering System (reminder) (p.8):**

Previously (see aggregates handout), we discussed a system for ordering meals online where the following high level domain areas are needed in order for the Consumer to create (place) an order (and the Restaurant to delivery it) – leading to the following sub domains:

- **Consumer (Identity)** – user details including name and address, payment details – these are essentially the buyer's details once authorised.
- **Restaurant** – name, menu items available
- **Order** – holds the buyer id, their name, delivery address, restaurant id and name that the order was taken at, menu items ordered (order lines), as well as the status of the order
- **Kitchen** – tickets for each order (and the items that make up the order) including which restaurant it is and what menu items make up the order
- **Delivery** – courier, time/date of delivery, location of delivery, order to be delivered

A subdomain in the problem space is mapped to a bounded context in the solution space. A bounded context is an area of the application that requires its own ubiquitous language and its own architecture. Or, put another way, **a bounded context is a boundary within which the ubiquitous language is consistent**. A bounded context **can have relationships to other bounded contexts.**

Source: https://www.microsoftpressstore.com/articles/article.aspx?p=2248811&seqNum=3

**Order is the core domain here**. Restaurant, Delivery and Kitchen support Order along with Identity to authenticate and assign role.

> **Figure 1: Simplification of the case study system showing the contexts and aggregates** — five bounded contexts drawn as coloured ellipses: **Order** (dark teal, containing two orange aggregate boxes: **Order** and **Buyer**), **Identity** (magenta/pink, no aggregate boxes shown — it authenticates and assigns role), **Restaurant** (dark teal, containing one orange box: **Restaurant**), **Kitchen** (dark teal, containing one orange box: **Ticket**), and **Delivery** (dark teal, containing two orange boxes: **Courier** and **Delivery**). The Order context is visually the core; the others surround/support it.

---

**Folder Structure for Spring Modulith (p.9):**

You will notice that **two modules have been added to this project: common and ordering**.

> **Screenshot (p.9):** the project tree shows `com.example.demo` containing `common` and `ordering` (both flagged as modules), plus `DemoApplication` and `GlobalExceptionHandler` at the root; and a `resources` folder containing `static`, `templates`, `application.yaml`, `data.sql`, and `schema.sql`.

To add a module such as common (or ordering), **right click on com.example.demo and select New Package**.

---

**Modulith Folder Structure (reminder) (p.10) — verbatim:**

There is just a **common** package, as well as **ordering** bounded context here… and there will be others added to the case study in future handouts.

- `com.example.demo`
  - application class
  - global exception handler
  - **common (shared kernel)**
    - Value Objects: PaymentMethod, Money, Identity, FullName, Address
    - DomainAssertions
    - Entity
    - Any interfaces required (e.g. **AggregateRoot**, ValueObject)
  - **ordering**
    - context façade
    - **ui (hidden)**
      - controllers e.g. OrderController
      - exceptions that relate to this layer
    - **application (hidden)**
      - **dto**
      - services
        e.g. OrderService
      - or CQRS – command and queries handlers
        e.g. OrderQueryHandler, and OrderCommandHandler
      - read/query mappers
        e.g. BuyerJpaToDTOMapper, OrderJpaToDTOMapper
      - exceptions that relate this application layer
    - **domain (hidden)**
      - aggregates (and entities/value objects)
        e.g. Aggregates: Order, Buyer
        Entities: OrderLine
        Value Objects: PaymentMethod, ExpirationDate
      - exceptions that relate to this layer
    - **infrastructure (hidden)**
      - repositories
      - e.g. OrderRepository, BuyerRepository
      - entities
      - e.g. OrderJpa, BuyerJpa
      - exceptions that relate to this layer

**Note (verbatim):** Each of these top level packages is a **module**. Only code in the root of this folder is visible (unless we add a `package-info` file), the rest is hidden, so we cannot accidently import classes that should be internal.

**Note (verbatim):** This approach makes **converting to a microservice architecture more straightforward** (if that becomes necessary).

> **Difference vs the Lecture 4 modulith structure:** this reminder adds a **`dto`** sub-item under the application layer, and the interface example is written as **`AggregateRoot`** (Lecture 4 wrote `Aggregate`). Otherwise identical.

---

**Custom Exceptions (p.11):**

As the Modulith folder diagram identified earlier, it is typical to include **custom exceptions**. These typically are created for **each level of the bounded context (ui, application, domain, infrastructure)**.

There is not usually much in the way of specialisation within them other than their name – which should clearly define their purpose.

We will use these by generating an exception of the appropriate type and passing it an **id** – this will then indicate which aggregate (by id) threw the exception. The **global exception handler** can then capture this and we can decide what it needs to do with this information!

**BuyerNotFoundException (p.11):**

```java
public class BuyerNotFoundException extends RuntimeException {
  public BuyerNotFoundException(String buyer_id) {
    super(buyer_id);
  }
}
```

Example from the `BuyerQueryHandler`:

```java
public BuyerDTO findBuyerById(String buyer_id) {
  return buyerRepository.findById(buyer_id)
    .map(BuyerJpaToDTOMapper::toBuyerDTO)
    .orElseThrow(() -> new BuyerNotFoundException(buyer_id));
}
```

**OrderNotFoundException (p.11):**

```java
public class OrderNotFoundException extends RuntimeException {
  public OrderNotFoundException(String order_id) {
    super(order_id);
  }
}
```

---

**GlobalExceptionHandler (p.12–13):**

This class represents a starting point for handling different exceptions that your application can throw. By implementing this we **no longer need try/catch blocks** in the classes that raise these exceptions. It also helps by **centralising any logging** that we have for errors.

This **expands on the version included in the first lecture** by considering a greater number of exceptions – in particular, the custom expections just discussed, as well as ones related to validation of entities. *(sic — "expections" is a typo in the source handout, preserved verbatim.)*

```java
@ControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception ex) {
    HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
    String message = ex.getMessage();
    Map<String, String> validationErrors = null;

    if (ex instanceof ResponseStatusException rse) {
      status = HttpStatus.valueOf(rse.getStatusCode().value());
      message = rse.getReason();
    }
    else if (ex instanceof MethodArgumentNotValidException manve) {
      status = HttpStatus.BAD_REQUEST;
      message = "Validation failed for one or more fields.";
      validationErrors = manve.getBindingResult().getFieldErrors().stream()
        .collect(Collectors.toMap(
          FieldError::getField,
          error -> Objects.requireNonNullElse(error.getDefaultMessage(), "Invalid value"),
          (existing, replacement) -> existing
        ));
    }
    else if (ex instanceof ConstraintViolationException cve) {
      status = HttpStatus.BAD_REQUEST;
      message = "Database constraint validation failed.";
      validationErrors = cve.getConstraintViolations().stream()
        .collect(Collectors.toMap(
          violation -> violation.getPropertyPath().toString(),
          ConstraintViolation::getMessage
        ));
    }
    else if (ex instanceof DataIntegrityViolationException) {
      status = HttpStatus.BAD_REQUEST;
      message = "A duplicate record already exists";
    }
    else if (ex instanceof IllegalArgumentException) {
      status = HttpStatus.BAD_REQUEST;
      message = ex.getMessage();
    }

    Map<String, Object> responseBody = new java.util.HashMap<>(Map.of(
      "status", status.value(),
      "error", status.getReasonPhrase(),
      "message", Objects.requireNonNullElse(message,
        "No message provided"),
      "timestamp", Instant.now().toString()
    ));

    if (validationErrors != null) {
      responseBody.put("errors", validationErrors);
    }

    return ResponseEntity.status(status).body(responseBody);
  }
}
```

*(Exceptions handled, in order: `ResponseStatusException` → uses its own status/reason; `MethodArgumentNotValidException` → 400 with per-field validation errors from the binding result; `ConstraintViolationException` → 400 with per-property constraint messages; `DataIntegrityViolationException` → 400 "A duplicate record already exists"; `IllegalArgumentException` → 400 with the exception message; anything else → 500 INTERNAL_SERVER_ERROR. The response body always contains `status`, `error`, `message`, `timestamp`, and optionally an `errors` map.)*

---

**Order Context: Creating our Tables and Adding Data (in the resources folder) (p.14):**

> **Figure 2: ERD of ordering schema (ERD initially generated by AI from human SQL)** — an entity-relationship diagram (Crow's Foot notation) of four tables:
> - **buyer**: `id : VARCHAR «PK»`, `first_name: VARCHAR`, `surname : VARCHAR`
> - **payment_method**: `id : int «generated» «PK»`, `buyer_id : VARCHAR «FK»`, `card_number : VARCHAR`, `cardholder_name : VARCHAR`, `expiry_month : int`, `expiry_year : int`, `security_number : VARCHAR`
> - **order_from_user**: `id : VARCHAR «PK»`, `buyer_id : VARCHAR «FK»`, `restaurant_id : VARCHAR`, `first_name : VARCHAR`, `surname : VARCHAR`, `house_name_number : VARCHAR`, `street : VARCHAR`, `town : VARCHAR`, `post_code : VARCHAR`, `ordered_on : DATE`, `order_status : int`, `description_of_status : VARCHAR`
> - **order_item**: `id : int «generated» «PK»`, `order_id : VARCHAR «FK»`, `product_id : VARCHAR`, `product_name : VARCHAR`, `unit_price : DECIMAL`, `units : int`
>
> Relationships shown on the diagram: buyer **"has saved a"** payment_method (one-to-many, mandatory), buyer **"places a"** order_from_user (one-to-many, optional/zero-or-more), order_from_user **"contains"** order_item (one-to-many, mandatory/one-or-more).

Business rules annotated alongside the ERD (verbatim bullets):

- A Buyer WILL have one or more payment methods (defined in the aggregate as a max of 3).
- Each Payment Method MUST belong to a specific Buyer.
- A Buyer CAN place one or more Orders.
- Orders from a user ARE made by a Buyer.
- An Order MUST contain one or more Order Items.
- Each Order Item MUST belong to an Order.

> **Figure 3: Crow's Foot Notation** — legend of cardinality symbols: **One to One**, **One to Many (Mandatory)**, **Many**, **One and Only One (Mandatory)**, **One or More (Mandatory)**, **Zero or one (Optional)**, **Zero or Many (Optional)**.
> Source: Cardinality Symbols in ER Diagrams: Types & Notations + Free Templates | Creately — https://creately.com/guides/cardinality-symbols/

---

**schema.sql (p.15):**

```sql
CREATE TABLE buyer(
  id VARCHAR PRIMARY KEY,
  first_name VARCHAR NOT NULL,
  surname VARCHAR NOT NULL
);

CREATE TABLE payment_method(
  id int AUTO_INCREMENT PRIMARY KEY,
  card_number VARCHAR NOT NULL,
  cardholder_name VARCHAR NOT NULL,
  expiry_month int NOT NULL,
  expiry_year int NOT NULL,
  security_number VARCHAR NOT NULL,
  buyer_id VARCHAR NOT NULL,
  FOREIGN KEY(buyer_id) REFERENCES buyer(id)
);

CREATE TABLE order_from_user(
  id VARCHAR PRIMARY KEY,
  buyer_id VARCHAR NOT NULL,
  restaurant_id VARCHAR NOT NULL,
  first_name VARCHAR NOT NULL,
  surname VARCHAR NOT NULL,
  house_name_number VARCHAR NOT NULL,
  street VARCHAR NOT NULL,
  town VARCHAR NOT NULL,
  post_code VARCHAR NOT NULL,
  ordered_on DATE NOT NULL,
  order_status int NOT NULL,
  description_of_status VARCHAR NOT NULL
);

CREATE TABLE order_item(
  id int AUTO_INCREMENT PRIMARY KEY,
  product_id VARCHAR NOT NULL,
  product_name VARCHAR NOT NULL,
  unit_price DECIMAL NOT NULL,
  units int NOT NULL,
  order_id VARCHAR NOT NULL,
  FOREIGN KEY(order_id) REFERENCES order_from_user(id)
);
```

---

**data.sql (p.16):**

```sql
INSERT INTO buyer(id, first_name, surname)
VALUES('0000', 'Ivor', 'Menu');

INSERT INTO payment_method(card_number, cardholder_name, expiry_month, expiry_year,
security_number, buyer_id)
VALUES ('1111-1111-1111-1111','I Menu',12,2027,'123','0000'),
  ('2111-1111-1111-1111','Ivor Menu',12,2028,'234','0000');

create sequence payment_method_sequence_id start with (select max(id) + 1 from
payment_method);

INSERT INTO order_from_user(id, buyer_id, restaurant_id,
  first_name, surname,
  house_name_number, street, town, post_code,
  ordered_on, order_status, description_of_status)
VALUES ('1111','0000','2222',
  'I','Menu',
  'house1','street1','town1', 'post_code',
  '2025-06-23',2, 'The order was shipped');

INSERT INTO order_from_user(id, buyer_id, restaurant_id,
  first_name, surname,
  house_name_number, street, town, post_code,
  ordered_on, order_status, description_of_status)
VALUES ('1112','0000', '2222',
  'I','Menu',
  'house1','street1','town1',
  'post_code','2025-07-01',2, 'The order was shipped');

INSERT INTO order_item(product_id, product_name, unit_price, units, order_id)
VALUES ('1', 'chicken korma',10.5,1,'1111');

INSERT INTO order_item(product_id, product_name, unit_price, units, order_id)
VALUES ('1', 'chicken korma',10.5,1,'1112');

INSERT INTO order_item(product_id, product_name, unit_price, units, order_id)
VALUES ('2', 'garlic naan',3.5,1,'1112');

create sequence order_item_sequence_id start with (select max(id) + 1 from order_item);
```

*(Seed data: one buyer "Ivor Menu" id `0000` with two payment methods; two orders `1111` and `1112`, both to restaurant `2222`, status `2` "The order was shipped"; order `1111` has one item (chicken korma), order `1112` has two items (chicken korma + garlic naan). Sequences for `payment_method` and `order_item` ids are created starting from `max(id)+1`.)*

---

**H2 Data Source configuration (p.17):**

> **Screenshot (p.17):** IntelliJ "Data Sources and Drivers" dialog — **Name:** default, **Driver:** H2, **Connection type:** In-memory, **Authentication:** No auth, **Database:** default, **URL:** `jdbc:h2:mem:default`.

When we click Test Connection we are asked to download the driver – do so (connection test should then be successful) - then click OK to close this page.

---

**Domain Model Operations — Command-Query Responsibility Segregation (CQRS) Architectural Pattern (p.18–19):**

**CQRS** - Represent any kind of operations **within the Bounded Context** which either affect:

- **Change the state** of the aggregate/entity (**command**)
- or, **query (view) the state** of the aggregate/ entity (**query**)

> **Figure 4: Commands/Queries within the Originations Bounded Context (to show the separation)** — a diagram with an "Originations Bounded Context" ellipse on the left, arrows fanning out to two groups on the right: **Commands** (yellow boxes: "Open a Loan Account", "Modify Loan Applicant Details") and **Queries** (green boxes: "View Loan Account Details", "View Loan Applicant Details"). Illustrates that within one bounded context, state-changing commands are visually separated from state-viewing queries.

**Why?**

Focusing on the query side…. It can be difficult to query from repositories all the data that users need to view, especially when we need to **create views of data that cuts across a number of aggregate types and instances**.

**Solution = CQRS – how does it work?**

> "Separate the query responsibilities from all the responsibilities that execute pure commands on the same model" (**Vaughn Vernon, pg 139**).

> "EVERY method should be either a **command that performs an action**, or a **query that returns data** to the caller, but not both.
> In other words, asking a question [query] should not change the answer. More formally, methods should return a value only if they are referentially transparent and hence possess **no side effects**." (**Bertrand Meyer**)

**CQRS Principles elaborating on what was written above (p.19):**

**Do you simply want to view some data? = Query (or read model)**

- Request/retrieve data from the system in a form that is **readable by the viewer**.
- The data is retrieved via our repository into appropriate entities – **it is NOT transformed into the domain as it just needs to be returned in a form that can be manipulated by the viewer e.g. as a JSON object** (any object with Getter methods – nice and easy for record's).
- It will **not modify the data** from the domain (as stored in the DB) as a result.
- We **could generate a number of views** that provide the data needed for a specific request
- this is better than returning data that is not needed i.e. all the fields from an entity and associated entities, or assigning a specific interface to an entity that hides access to certain getters but essentially has the same data underneath). Some argue for the system to return the data – 'as is' (entity); others propose the use of a DTO to hold just the data requested in a form that is JSON compatible.

> **Figure 5: Commands from Clients travel 'one way' (to the command model then store)** — a diagram showing a client/monitor on the left connected to a **Query Processor** and **Command Processors (Application Services)**. The Query Processor reads from a **Query Model** (database cylinder). The Command Processors write to a **Command Model**, which feeds a **Command Model Store** (database cylinder). An **Event (all) Subscriber** sits between them, propagating events from the command side to the query model. Illustrates the classic CQRS read/write split with eventual consistency between the command store and the query model.

> **Figure 6: Diagram that illustrates CQRS in the sample system and the form of the data passed to layer** — a diagram of the case-study wiring using colour-coded arrows and node types (rectangles = classes, ellipses = the data type passed). **Rest Controller** ↔ **Context Facade** (Command passed between them). Context Facade → **Query Handler** (up the query/read path, blue) and Context Facade → **ApplicationService** (down the command path, Command). Query Handler receives **Entity** from **Repository**, and passes **DTO** up via **EntityToDTOMapper** (top). ApplicationService receives **Aggregate** from **Domain** and **Entity** from Repository. Shows that on the **query side** the data flows Repository → Entity → Query Handler → mapper → **DTO** → controller; on the **command side** it involves the ApplicationService, Domain and Aggregate.
> **For more information consider Chapter 4 of Implementing Domain Driven Design by Vaughn Vernon.**

---

**Buyer Aggregate (p.20):**

As we are choosing to **discuss Queries separately from Commands** - we do **not need to focus on the domain layer** where aggregates, entities and value objects reside as **these are not involved in query retrieval**.

Data that is saved to the infrastructure has already been checked by the aggregates to ensure that it is in a **valid state** – both in terms of **invariant violations and business rules**.

```java
public class Buyer extends Entity<Buyer> implements AggregateRoot {
  public static final String PAYMENT_METHOD_NOT_NULL = "Payment method cannot be null";
  public static final String PAYMENT_METHOD_ALREADY_EXISTS = "Payment method already exists";
  public static final String NO_MORE_THAN_THREE_PAYMENT_METHODS_ACCEPTED = "No more than 3 payment methods accepted";

  private FullName fullName;
  private final List<PaymentMethod> paymentMethods;

  public Buyer(Identity<Buyer> id, FullName fullName) {
    super(id);
    this.fullName = new FullName(fullName);
    this.paymentMethods = new ArrayList<>();
  }

  … rest of the class omitted here
```

---

**Find All Buyers (p.21):**

**Let us follow the request from controller to repository and discuss what happens at each layer.**

> **Figure 7: Sequence Diagram of Find All Buyers** — participants: Client, BuyerController, ContextFacade, BuyerQueryHandler, BuyerRepository, BuyerJpaToDTOMapper. Flow: Client → BuyerController `GET /buyers/findAll`; BuyerController → ContextFacade `findAllBuyers()`; ContextFacade → BuyerQueryHandler `findAllBuyers()`; BuyerQueryHandler → BuyerRepository `findAll()`; BuyerRepository → BuyerQueryHandler `Iterable<BuyerJpa>`; note over BuyerQueryHandler: `StreamSupport.stream`; **loop [for each BuyerJpa in collection]**: BuyerQueryHandler → BuyerJpaToDTOMapper `toBuyerDTO(buyerJpa)` → `BuyerDTO`; note over BuyerQueryHandler: `.collect(toList())`; BuyerQueryHandler → ContextFacade `Iterable<BuyerDTO>`; ContextFacade → BuyerController `Iterable<BuyerDTO>`; BuyerController → Client `HTTP Response + data`.

> **Figure 8: Postman API output for all buyers** — `GET http://localhost:8900/buyers` returning a JSON array with one object: `{ "id": "0000", "firstName": "Ivor", "surname": "Menu" }`.

**Find All Buyers - BuyerController (p.22):**

The controller receives HTTP requests and collaborates with the context façade in order to return a suitable response. Any exceptional behaviour will be picked up by the global exception handler.

```java
@RequestMapping("/buyers")
@RestController
@AllArgsConstructor
public class BuyerController {
  private final ContextFacade facade;

  @GetMapping
  @ResponseStatus(HttpStatus.OK)
  public Iterable<BuyerDTO> getAllBuyerDetails() {
    return facade.findAllBuyers();
  }

  … rest of the class omitted here
```

**Find All Buyers - ContextFacade (p.23):**

The context façade interacts with dependencies downstream – in this case (at least for this week) they are query handlers (services that just handle read requests – more on this later on). This façade collaborates with all aggregates in the bounded context, not just Buyer (or Order). It is public facing so other modules can access this file if they want to make use of the services that it offers.

```java
@Component
@AllArgsConstructor
public class ContextFacade {
  private final BuyerQueryHandler buyerQueryHandler;
  private final OrderQueryHandler orderQueryHandler;

  // Not recommended to have this method (too much data)
  //e.g. http://localhost:8900/buyers/findAll
  public Iterable<BuyerDTO> findAllBuyers() {
    return buyerQueryHandler.findAllBuyers();
  }

  … rest of the class omitted here
```

**Find All Buyers - BuyerQueryHandler (application folder) (p.24):**

At the application (service) layer – using a mapper - we convert JPA entities to DTOs

```java
@AllArgsConstructor
@Service // Will not find this class without this annotation when trying to inject
public class BuyerQueryHandler {
  private BuyerRepository buyerRepository;

  // Not recommended to have this method as we could have a VERY big response
  public Iterable<BuyerDTO> findAllBuyers(){
    return StreamSupport.stream(buyerRepository.findAll().spliterator(), false)
      .map(BuyerJpaToDTOMapper::toBuyerDTO)
      .collect(toList());
  }

  … rest of the class omitted here
```

**BuyerRepository (persistence/repositories) (p.24):**

```java
@Repository
public interface BuyerRepository extends CrudRepository<BuyerJpa, String> {
}
```

**Find All Buyers - BuyerJpaToDTOMapper (application/mappers) (p.25):**

```java
public class BuyerJpaToDTOMapper {
  public static BuyerDTO toBuyerDTO(BuyerJpa buyer) {
    Objects.requireNonNull(buyer, "Buyer JPA entity cannot be null");

    return new BuyerDTO(
      buyer.getId(),
      buyer.getFullName().firstName(),
      buyer.getFullName().surname()
    );
  }

  … rest of the class omitted here
```

Initial guard to check if the buyer reference is null. Then we convert the `BuyerJpa` to `BuyerDTO` by calling the respective methods.

You will note that the `BuyerJpa` (buyer) has getter methods as it's not a domain object, but as this class contains a **record type value object** (`FullName` – see commons folder) by default its getter method is DDD style (as discussed last week), hence the `firstName()` rather than `getFirstName`.

**BuyerJpa (persistence/entities) (p.25):**

```java
@Entity(name = "buyer")
@Table(name ="buyer")
@Getter
@Setter
@ToString(exclude = "paymentMethods") // prevent stack overflow loop
public class BuyerJpa {
  @Id
  @Column(name="id")
  private String id;

  @Embedded
  @Valid
  private FullName fullName;

  @OneToMany(mappedBy = "buyer", cascade = {CascadeType.ALL}, orphanRemoval = true)
  private List<PaymentMethodJpa> paymentMethods = new ArrayList<>();;
}
```

---

**Find Buyer By Id (p.26):**

> **Figure 9 (first, p.26): Sequence Diagram of Find Buyer by Id (0000)** — participants: Client (actor), BuyerController, ContextFacade, BuyerQueryHandler, BuyerRepository, BuyerJpaToDTOMapper. Flow: Client → BuyerController `GET /buyers/{buyer_id}`; → ContextFacade `findBuyerById(buyer_id)`; → BuyerQueryHandler `findBuyerById(buyer_id)`; → BuyerRepository `findById(buyer_id)`; → `Optional<BuyerJpa>`. **alt [BuyerJpa present]**: BuyerQueryHandler → BuyerJpaToDTOMapper `toBuyerDTO(buyerJpa)` → `BuyerDTO`; up to ContextFacade → BuyerController → Client `200 OK + BuyerDTO`. **else [BuyerJpa empty (.orElseThrow)]**: BuyerQueryHandler → ContextFacade `throw BuyerNotFoundException` → BuyerController; note over BuyerController: `ExceptionHandler intercepts and maps to 404`; BuyerController → Client `404 Not Found`.

> **Figure 9 (second, p.26 — the handout labels two figures "Figure 9"): Postman API output for buyer by id (0000)** — `GET http://localhost:8900/buyers/0000` returning `{ "id": "0000", "firstName": "Ivor", "surname": "Menu" }`.

**Find Buyer By Id - BuyerController (p.27):**

```java
@RequestMapping("/buyers")
@RestController
@AllArgsConstructor
public class BuyerController {
  private final ContextFacade facade;

  @GetMapping("/{buyer_id}")
  @ResponseStatus(HttpStatus.OK)
  public BuyerDTO getBuyerById(@PathVariable String buyer_id){
    return facade.findBuyerById(buyer_id);
  }

  … rest of the class omitted here
```

**Find Buyer By Id - ContextFacade (p.27):**

```java
@Component
@AllArgsConstructor
public class ContextFacade {
  private final BuyerQueryHandler buyerQueryHandler;
  private final OrderQueryHandler orderQueryHandler;

  // e.g. http://localhost:8900/buyers/0000
  public BuyerDTO findBuyerById(String buyerId) {
    return buyerQueryHandler.findBuyerById(buyerId);
  }

  … rest of the class omitted here
```

**Find Buyer By Id - BuyerQueryHandler (application folder) (p.28):**

At the application (service) layer – using a mapper - we convert JPA entities to DTOs

```java
@AllArgsConstructor
@Service // Will not find this class without this annotation when trying to inject
public class BuyerQueryHandler {
  private BuyerRepository buyerRepository;

  public BuyerDTO findBuyerById(String buyer_id) {
    return buyerRepository.findById(buyer_id)
      .map(BuyerJpaToDTOMapper::toBuyerDTO)
      .orElseThrow(() -> new BuyerNotFoundException(buyer_id));
  }

  … rest of the class omitted here
```

**Find Buyer By Id - BuyerRepository (persistence/repositories) - reminder (p.28):**

```java
@Repository
public interface BuyerRepository extends CrudRepository<BuyerJpa, String> {
}
```

**Find Buyer By Id - BuyerJpaToDTOMapper (application/mappers) – here it is again! (p.29):**

```java
public class BuyerJpaToDTOMapper {
  public static BuyerDTO toBuyerDTO(BuyerJpa buyer) {
    Objects.requireNonNull(buyer, "Buyer JPA entity cannot be null");

    return new BuyerDTO(
      buyer.getId(),
      buyer.getFullName().firstName(),
      buyer.getFullName().surname()
    );
  }

  … rest of the class omitted here
```

Initial guard to check if the buyer reference is null. Then we convert the `BuyerJpa` to `BuyerDTO` by calling the respective methods.

You will note that the `BuyerJpa` (buyer) has getter methods as it's not a domain object, but as this class contains a record type value object (`FullName` – see commons folder) by default its getter method is DDD style (as discussed last week), hence the `firstName()` rather than `getFirstName`.

**Find Buyer By Id - BuyerJpa (persistence/entities) - reminder (p.29):**

```java
@Entity(name = "buyer")
@Table(name ="buyer")
@Getter
@Setter
@ToString(exclude = "paymentMethods") // prevent stack overflow loop
public class BuyerJpa {
  @Id
  @Column(name="id")
  private String id;

  @Embedded
  @Valid
  private FullName fullName;

  @OneToMany(mappedBy = "buyer", cascade = {CascadeType.ALL}, orphanRemoval = true)
  private List<PaymentMethodJpa> paymentMethods = new ArrayList<>();;
}
```

---

**Find Payment Details for Buyer Id (p.30):**

> **Figure 11: Sequence Diagram of Payment Details for Buyer Id 0000** — participants: Client (actor), BuyerController, ContextFacade, BuyerQueryHandler, BuyerRepository, BuyerToDTOMapper. Flow: Client → BuyerController `GET /paymentDetails/{buyer_id}`; → ContextFacade `findPaymentsForBuyerById(buyer_id)`; → BuyerQueryHandler `findPaymentsForBuyerById(buyer_id)`; → BuyerRepository `findById(buyer_id)`; → `Optional<BuyerJpa>`. **alt [buyer found (Optional.isPresent())]**: BuyerQueryHandler → BuyerToDTOMapper `convertToPaymentMethodDTOs(BuyerJpa)`; self-message `convertToPaymentMethodDTO for each payment method`; → `List<PaymentMethodDTO>`; up to ContextFacade → BuyerController → Client `200 OK + List<PaymentMethodDTO>`. **else [buyer not found (Optional.isEmpty())]**: BuyerQueryHandler self-message `throw BuyerNotFoundException`; propagated up; BuyerController → Client `404 Not Found (BuyerNotFoundException handled)`.

> **Figure 12: Postman API output for payment methods for buyer by id (0000)** — `GET http://localhost:8900/buyers/0000/paymentDetails` returning a JSON array of two payment methods:
> `[ { "cardNumber": "1111111111111111", "cardHolderName": "I Menu", "expirationDate": { "expiryMonth": 12, "expiryYear": 2027 }, "securityNumber": "123" }, { "cardNumber": "2111111111111111", "cardHolderName": "Ivor Menu", "expirationDate": { "expiryMonth": 12, "expiryYear": 2028 }, "securityNumber": "234" } ]`
> *(Note: the JSON nests `expirationDate` as an object with `expiryMonth`/`expiryYear` — reflecting the `ExpirationDateDTO` record — and the card numbers appear without dashes in the response.)*

**Find Buyer Payment Details for Buyer Id - BuyerController (p.31):**

```java
@RequestMapping("/buyers")
@RestController
@AllArgsConstructor
public class BuyerController {
  private final ContextFacade facade;

  @GetMapping("/{buyer_id}/paymentDetails")
  @ResponseStatus(HttpStatus.OK)
  public List<PaymentMethodDTO> getPaymentDetailsForBuyer(@PathVariable String buyer_id){
    return facade.findPaymentsForBuyerById(buyer_id);
  }
}

  … rest of the class omitted here
```

**Find Buyer Payment Details for Buyer Id - ContextFacade (p.31):**

```java
@Component
@AllArgsConstructor
public class ContextFacade {
  private final BuyerQueryHandler buyerQueryHandler;
  private final OrderQueryHandler orderQueryHandler;

  // e.g. http://localhost:8900/buyers/paymentDetails/0000
  public List<PaymentMethodDTO> findPaymentsForBuyerById(String buyerId) {
    return buyerQueryHandler.findPaymentsForBuyerById(buyerId);
  }

  … rest of the class omitted here
```

**Find Buyer Payment Details for Buyer Id - BuyerQueryHandler (application folder) (p.32):**

At the application (service) layer – using a mapper - we convert JPA entities to DTOs

```java
@AllArgsConstructor
@Service // Will not find this class without this annotation when trying to inject
public class BuyerQueryHandler {
  private BuyerRepository buyerRepository;

  public List<PaymentMethodDTO> findPaymentsForBuyerById(String buyer_id) {
    return buyerRepository.findById(buyer_id)
      .map(BuyerJpaToDTOMapper::convertToPaymentMethodDTOs)
      .orElseThrow(() -> new BuyerNotFoundException(buyer_id));
  }
}

  … rest of the class omitted here
```

**Find Buyer Payment Details for Buyer Id - BuyerRepository (persistence/repositories) (p.32):**

```java
@Repository
public interface BuyerRepository extends CrudRepository<BuyerJpa, String> {
}
```

**Find Buyer Payment Details for Buyer Id - BuyerJpaToDTOMapper (application/mappers) (p.33):**

```java
public class BuyerJpaToDTOMapper {
  public static PaymentMethodDTO convertToPaymentMethodDTO(PaymentMethodJpa paymentMethod) {
    Objects.requireNonNull(paymentMethod, "Payment method JPA entity cannot be null");
    Objects.requireNonNull(paymentMethod.getExpirationDate(), "Expiration date cannot be null");

    ExpirationDateDTO expirationDate = new ExpirationDateDTO(
      paymentMethod.getExpirationDate().expiryMonth(),
      paymentMethod.getExpirationDate().expiryYear()
    );

    return new PaymentMethodDTO(
      paymentMethod.getCardNumber(),
      paymentMethod.getCardholderName(),
      expirationDate,
      paymentMethod.getSecurityNumber()
    );
  }

  … rest of the class omitted here
```

Initial guard to check if the payment method reference is null. Then we convert the `PaymentMethodJpa` to `PaymentMethodDTO` by calling the respective methods.

You will note that the `PaymentMethodJpa` has getter methods as it's not a domain object, but as this class contains a record type value object (`FullName` – see commons folder) by default its getter method is DDD in style (as discussed last week), hence the `firstName()` rather than `getFirstName`.

*(Note: the handler calls `convertToPaymentMethodDTOs` (plural) via method reference, but the mapper code shown defines `convertToPaymentMethodDTO` (singular) for a single payment method — the plural version that maps over the buyer's list is in the "rest of the class omitted here". The narrative note references `FullName` even though this mapper handles `PaymentMethodJpa` — reproduced as printed.)*

**Find Buyer Payment Details for Buyer Id - PaymentMethodJpa (persistence/entities) (p.34):**

```java
@Entity(name="payment_method")
@Table(name="payment_method")
@Setter
@Getter
@ToString(exclude = "buyer") // prevent stack overflow loop
public class PaymentMethodJpa {
  @Id
  @Column(name="id")
  @SequenceGenerator(name= "payment_method_sequence",
    sequenceName = "payment_method_sequence_id",
    allocationSize = 1)
  @GeneratedValue(strategy= GenerationType.SEQUENCE,
    generator="payment_method_sequence")
  private long id;

  @NotBlank(message = "Card number is required")
  @Size(min = 16, max = 16, message = "Card number must be 16 digits")
  @Column(name = "card_number", length = 16)
  private String cardNumber;

  @Column(name="cardholder_name")
  private String cardholderName;

  @Embedded
  @Valid
  private ExpirationDate expirationDate;

  @NotBlank(message = "Security number is required")
  @Size(min = 3, max = 3, message = "CVV must be 3 digits")
  @Column(name = "security_number", length = 3)
  private String securityNumber;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "buyer_id", nullable = false)
  private BuyerJpa buyer;
}
```

*(Note: `id` uses a `@SequenceGenerator`/`@GeneratedValue(SEQUENCE)`; `cardNumber` constrained to exactly 16 chars, `securityNumber` (CVV) to exactly 3; `expirationDate` is an `@Embedded @Valid` value object; `buyer` is a lazy `@ManyToOne` back-reference joined on `buyer_id`.)*

---

**Order (p.35):**

Having discussed the Buyer end points, you will also find that there are **Order end points**.

**Please review the following code and test via Postman (or similar):**

- OrderController, ContextFacade
- OrderQueryHandler, OrderJpaToDTOMapper
- OrderJPa, OrderItemJpa, OrderRepository

**You might also want to look at the Order aggregate and OrderItem value object.**

> **This is the implicit exercise from Lecture 5** — review and Postman-test the Order query endpoints (the Buyer ones are fully walked through; the Order ones follow the same CQRS structure and are left for the student to inspect/test). Blocked in the same way as before if the case-study code isn't uploaded.

---

**Service Layer Pattern (p.36):**

> "Defines an application's boundary with a **layer of services that establishes a set of available operations and coordinates the application's response in each operation**."

> "A Service Layer defines an application's boundary and its set of available operations from the perspective of interfacing client layers. It encapsulates the application's business logic, controlling transactions and coordinating responses in the implementation of its operations."
> **(Martin Fowler)**

> **Figure (p.36):** Fowler's Service Layer diagram — concentric rings: outermost clients (**Data Loaders**, **User Interfaces**, **Integration Gateway**) sit outside the **Service Layer** ring, which surrounds the **Domain Model**, which surrounds the innermost **Data Source Layer** (drawn with a database cylinder). A **RecognitionService** is shown spanning the service-layer boundary at the bottom.

Read more: Service Layer — https://martinfowler.com/eaaCatalog/serviceLayer.html

**Comparing CQRS with the Service Layer (p.37) — full text:**

The **Service Layer** is a class that sits between the controllers and the data access layer (repository). It acts as a **single point of truth for business rules and domain logic** (it is not a DDD pattern per se, but can be used as the layer in which the system interacts with aggregates). The controller remains a **thin layer** using the service to fulfil any requests. The service layer uses the data access layer to interact with any database or other external systems, in order to handle data storage and retrieval. **The focus for the service is a single repository – consisting of one entity.**

As the application grows this can become overblown (i.e. **God class code smell**), as it conducts paginated and mapped DTO read operations, as well as potentially complex transactional write operations. The code itself is a single class which, of itself, might be viewed as simpler than the additional work required for CQRS.

The **CQRS pattern effectively splits the service layer up** so that the read operations can be handled separately from write operations. Simplifying what was (potentially) a large single service class into two classes with dedicated operations. The resulting classes are **coupled to fewer dependencies**. The query class has no need to communicate with the domain layer, only the mapper(s) and repository – before returning the requested data. It is also worth noting that the types of query requests in a domain model require the context to return all of the relevant data for a specific request – so that means ensuring that the repository has this data available (it is not going to look it up from another context); this means that the entity (or entities if you have a list of value objects associated with a particular aggregate – e.g. OrderLines with Order, PaymentMethods with Buyer) will contain data that originates in other aggregates e.g. user data beyond the id, full name, address, etc that are specific required for reporting purposes. This is essentially **de-normalised for reporting purposes**.

The **command class must communicate with the domain to ensure the data is valid** – in terms of business logic as well as being in a valid state; command operations have **no need to return anything other than potentially, a status code**. It is worth noting that with CQRS **we could have separate databases for read and write requests** – relying on **eventual consistency** to sync the data (we might also see the use of **caching data**).

---

**Appendix A – PlantUML for ERD (p.38–39):**

```plantuml
@startuml
' Layout adjustments
skinparam rhinestone true
hide circle

entity buyer {
  id : VARCHAR <<PK>>
  first_name: VARCHAR
  surname : VARCHAR
}

entity payment_method {
  id : int <<generated>> <<PK>>
  buyer_id : VARCHAR <<FK>>
  card_number : VARCHAR
  cardholder_name : VARCHAR
  expiry_month : int
  expiry_year : int
  security_number : VARCHAR
}

entity order_from_user {
  id : VARCHAR <<PK>>
  buyer_id : VARCHAR <<FK>>
  restaurant_id : VARCHAR
  first_name : VARCHAR
  surname : VARCHAR
  house_name_number : VARCHAR
  street : VARCHAR
  town : VARCHAR
  post_code : VARCHAR
  ordered_on : DATE
  order_status : int
  description_of_status : VARCHAR
}

entity "order_item" as order_item {
  id : int <<generated>> <<PK>>
  order_id : VARCHAR <<FK>>
  product_id : VARCHAR
  product_name : VARCHAR
  unit_price : DECIMAL
  units : int
}

' Relationships
buyer ||--{ payment_method : "has saved a"
buyer ||--o{ order_from_user : "places a"
order_from_user ||--|{ order_item : "contains"
@enduml
```

---

**Appendix B - PlantUML for Sequence diagram findAllBuyers (p.40):**

```plantuml
@startuml
hide footbox
Client -> BuyerController: GET /buyers/findAll
activate BuyerController
BuyerController -> ContextFacade: findAllBuyers()
activate ContextFacade
ContextFacade -> BuyerQueryHandler: findAllBuyers()
activate BuyerQueryHandler
BuyerQueryHandler -> BuyerRepository: findAll()
activate BuyerRepository
BuyerRepository --> BuyerQueryHandler: Iterable<BuyerJpa>
deactivate BuyerRepository
note over BuyerQueryHandler
  StreamSupport.stream
end note
loop for each BuyerJpa in collection
  BuyerQueryHandler -> BuyerJpaToDTOMapper: toBuyerDTO(buyerJpa)
  activate BuyerJpaToDTOMapper
  BuyerJpaToDTOMapper --> BuyerQueryHandler: BuyerDTO
  deactivate BuyerJpaToDTOMapper
end
note over BuyerQueryHandler
  .collect(toList())
end note
BuyerQueryHandler --> ContextFacade: Iterable<BuyerDTO>
deactivate BuyerQueryHandler
ContextFacade --> BuyerController: Iterable<BuyerDTO>
deactivate ContextFacade
BuyerController --> Client: HTTP Response + data
deactivate BuyerController
@enduml
```

---

**Appendix C - PlantUML for Sequence diagram findBuyerById (p.40–41):**

```plantuml
@startuml
hide footbox
actor Client
Client -> BuyerController: GET /buyers/{buyer_id}
activate BuyerController
BuyerController -> ContextFacade: findBuyerById(buyer_id)
activate ContextFacade
ContextFacade -> BuyerQueryHandler: findBuyerById(buyer_id)
activate BuyerQueryHandler
BuyerQueryHandler -> BuyerRepository: findById(buyer_id)
activate BuyerRepository
BuyerRepository --> BuyerQueryHandler: Optional<BuyerJpa>
deactivate BuyerRepository
alt BuyerJpa present
  BuyerQueryHandler -> BuyerJpaToDTOMapper: toBuyerDTO(buyerJpa)
  activate BuyerJpaToDTOMapper
  BuyerJpaToDTOMapper --> BuyerQueryHandler: BuyerDTO
  deactivate BuyerJpaToDTOMapper

  BuyerQueryHandler --> ContextFacade: BuyerDTO
  ContextFacade --> BuyerController: BuyerDTO
  BuyerController --> Client: 200 OK + BuyerDTO
else BuyerJpa empty (.orElseThrow)
  BuyerQueryHandler --> ContextFacade: throw BuyerNotFoundException
  deactivate BuyerQueryHandler

  ContextFacade --> BuyerController: throw BuyerNotFoundException
  deactivate ContextFacade

  note over BuyerController: ExceptionHandler intercepts\nand maps to 404

  BuyerController --> Client: 404 Not Found
  deactivate BuyerController
end
@enduml
```

---

**Appendix D - PlantUML for Sequence diagram findPaymentsForBuyerById (p.42):**

```plantuml
@startuml
hide footbox
actor Client
Client -> BuyerController: GET /paymentDetails/{buyer_id}
activate BuyerController
BuyerController -> ContextFacade: findPaymentsForBuyerById(buyer_id)
activate ContextFacade
ContextFacade -> BuyerQueryHandler: findPaymentsForBuyerById(buyer_id)
activate BuyerQueryHandler
' Move the DB call here, before branching
BuyerQueryHandler -> BuyerRepository: findById(buyer_id)
activate BuyerRepository
BuyerRepository --> BuyerQueryHandler: Optional<BuyerJpa>
deactivate BuyerRepository
alt buyer found (Optional.isPresent())
  BuyerQueryHandler -> BuyerToDTOMapper: convertToPaymentMethodDTOs(BuyerJpa)
  activate BuyerToDTOMapper
  BuyerToDTOMapper -> BuyerToDTOMapper: convertToPaymentMethodDTO for each payment method
  BuyerToDTOMapper --> BuyerQueryHandler: List<PaymentMethodDTO>
  deactivate BuyerToDTOMapper
  BuyerQueryHandler --> ContextFacade: List<PaymentMethodDTO>
  ContextFacade --> BuyerController: List<PaymentMethodDTO>
  BuyerController --> Client: 200 OK + List<PaymentMethodDTO>
else buyer not found (Optional.isEmpty())
  ' Represent throwing the exception as a self-message or a dotted return arrow
  BuyerQueryHandler -> BuyerQueryHandler: throw BuyerNotFoundException

  ' Propagate the exception back up the stack
  BuyerQueryHandler --> ContextFacade: throws BuyerNotFoundException
  deactivate BuyerQueryHandler

  ContextFacade --> BuyerController: throws BuyerNotFoundException
  deactivate ContextFacade
  BuyerController --> Client: 404 Not Found (BuyerNotFoundException handled)
  deactivate BuyerController
end
@enduml
```

---


### LECTURE 6 CONTENT — FULL DETAIL

---

#### Document 9: Enterprise Application Development — CQRS: Commands

**File:** `Enterprise_Application_Development_-_CQRS_-_Commands.pdf`
**Author:** Phil James (PDF metadata records the author as "Philip James"; every page footer reads "Author: Phil James")
**Pages:** 32
**Created:** 16 July 2026 (from PDF metadata)

This is the **CQRS: Commands** handout — the companion to Lecture 5 (CQRS: Queries), which had explicitly deferred the command side ("we are choosing to discuss Queries separately from Commands"). Where Queries never touch the domain/aggregate layer, **Commands go through the domain model so that validation and invariants can decide whether the write is valid**. This handout implements exactly one command end-to-end — *Adding a new order* — and notes that the other write operations (for the Ordering context) happen via events, to be covered in the next handout.

**Learning Outcomes (all three listed in this handout):**
- Critically evaluate development approaches to solutions to enterprise applications.
- Design an enterprise application, critically evaluating alternatives and justifying selections.
- Implement an enterprise application that includes relevant security features.

**Mapping to Knowledge, Skills and Behaviours (verbatim table from the PDF):**

| Key | Knowledge, Skills and Behaviour |
|---|---|
| K21 | How to operate at all stages of the software development life cycle and how each stage is applied in a range of contexts. For example, requirements analysis, design, development, testing, implementation. |
| K22 | Principles of a range of development techniques, for each stage of the software development cycle that produce artefacts and the contexts in which they can be applied. For example, UML, unit testing, programming, debugging, frameworks, architectures. |
| S18 | Use appropriate analysis methods, approaches and techniques in software engineering projects to deliver an outcome that meets requirements. |
| S19 | Implement software engineering projects using appropriate software engineering methods, approaches and techniques. |
| S21 | Determine, refine, adapt and use appropriate software engineering methods, approaches and techniques to evaluate software engineering project outcomes. |

**Academic Disclosure (verbatim):**
In an attempt to provide additional visual imagery to help aid learning, the versions of the ERD and sequence diagram used in this handout were **initially generated (due to time constraints) by AI from my own source code, then amended/adapted/corrected**.

**Maths, English and Digital Skills Development (verbatim):**
- **Maths**
  - Selecting appropriate values
  - Use of logic
  - Considering values for testing
- **English**
  - Reading a formal document and interpreting its content effectively
  - Use of specialised language
- **Digital Skills**
  - Communicating/Handling information and content/Problem solving – Install the required technologies and then utilise these to design and develop solutions.

**Contents (as printed in the PDF, with page numbers):**
- Boundaries of Context for a Fictional Ordering System (reminder) — 4
- Folder Structure for Spring Modulith — 5
  - Modulith Folder Structure (reminder) — 6
- Custom Exceptions — 8
  - Ordering Context (application/exceptions) — 8
- GlobalExceptionHandler — 8
- Creating our Tables and Adding Data (in the resources folder) — 9
  - schema.sql — 10
  - data.sql — 12
- Domain Model Operations — 14
  - Command-Query Responsibility Segregation (CQRS) Architectural Pattern — 14
  - More about CQRS — 16
- Ordering Context — 17
  - OrderRestaurant (domain) — 18
- OrderJpa (persistence/entities) — 19
- Adding a new order — 21
- OrderController — 23
- ContextFacade — 23
- OrderApplicationService (application) — 24
- OrderRestaurantJpaToDomainMapper (application/mapper) — 25
- OrderRestaurantJpa (persistence/entities) — 25
- OrderRestaurantRepository (persistence/repositories) — 25
- OrderDTOToDomain (application/mapper) — 26
- OrderDomainToJpaMapper (application/mapper) — 27
- Appendix A – PlantUML for ERD — 29
- Appendix B – New Order Sequence Diagram — 31

---

**Boundaries of Context for a Fictional Ordering System (reminder) (p.4):**

A reminder of the five bounded contexts from the case study (identical wording to earlier handouts):

- **Consumer (Identity)** – user details including name and address, payment details – these are essentially the buyer's details once authorised.
- **Restaurant** – name, menu items available
- **Order** – holds the buyer id, their name, delivery address, restaurant id and name that the order was taken at, menu items ordered (order lines), as well as the status of the order
- **Kitchen** – tickets for each order (and the items that make up the order) including which restaurant it is and what menu items make up the order
- **Delivery** – courier, time/date of delivery, location of delivery, order to be delivered

**Order is the core domain here.** Restaurant, Delivery and Kitchen support Order along with Identity to authenticate and assign role.

**Figure 1: Simplification of the case study system showing the contexts and aggregates.** Five ovals. A teal oval labelled **Order** containing two orange aggregate boxes: **Order** and **Buyer**. A magenta/pink oval labelled **Identity** (no aggregate box shown — it is the supporting authentication context). A teal oval labelled **Restaurant** containing an orange **Restaurant** box (the raster shows a stray "nt" fragment beneath it, an artefact of the label). A teal oval labelled **Kitchen** containing an orange **Ticket** box. A teal oval labelled **Delivery** containing two orange boxes: **Courier** and **Delivery**.

**Note (verbatim):** Our focus here is going to continue with the Ordering context, but also to feature the Restaurant context.

---

**Folder Structure for Spring Modulith (p.5):**

You will notice that three modules are now present for this project: **common, ordering** and **restaurant**.

**Screenshot (IntelliJ project tree):** the package `com.example.demo` expanded, containing three module packages — `common`, `ordering`, `restaurant` (each highlighted with a red pill/marker in the handout to draw attention to the three modules) — plus two top-level classes: `DemoApplication` and `GlobalExceptionHandler`. (This is the same modulith arrangement as Lecture 5 but with the **restaurant** module now added alongside common and ordering.)

---

**Modulith Folder Structure (reminder) (pp.6–7):**

There is just a common package, as well as ordering bounded context here… and there will be others added to the case study in future handouts.

- **com.example.demo**
  - application class
  - global exception handler
  - **common (shared kernel)**
    - Value Objects: PaymentMethod, Money, Identity, FullName, Address
    - DomainAssertions
    - Entity
    - Any interfaces required (e.g. Aggregate, ValueObject)
  - **ordering**
    - context façade
    - **ui (hidden)**
      - controllers e.g. OrderController
      - commands e.g. AddNewOrderCommand
      - exceptions that relate to this layer
    - **application (hidden)**
      - services e.g. OrderService
      - or CQRS – command and queries handlers e.g. OrderQueryHandler, and OrderCommandHandler
      - mappers e.g. BuyerJpaToDTOMapper, OrderJpaToDTOMappers
      - DTOs
      - exceptions that relate this application layer
    - **domain (hidden)**
      - aggregates (and entities/value objects) e.g. Aggregates: Order, Buyer; Entities: OrderLine; Value Objects: PaymentMethod, ExpirationDate
      - exceptions that relate to this layer
    - **infrastructure (hidden)**
      - repositories e.g. OrderRepository, BuyerRepository
      - entities e.g. OrderJpa, BuyerJpa
      - exceptions that relate to this layer
    - *(continues over the page…)*
  - **restaurant**
    - context façade
    - **ui (hidden)**
      - controllers e.g. RestaurantController
      - commands e.g. AddNewRestaurant
      - exceptions that relate to this layer
    - **application (hidden)**
      - RestaurantQueryHandler and RestaurantCommandHandler
      - mappers
      - DTOs
      - exceptions that relate this application layer
    - **domain (hidden)**
      - aggregates (and entities/value objects)
      - exceptions that relate to this layer
    - **infrastructure (hidden)**
      - repositories
      - entities
      - exceptions that relate to this layer

> **Delta vs Lecture 5's reminder:** the `restaurant` module is now fully fleshed out (previously only common + ordering existed). The `ordering.application` layer now explicitly lists **both** a `services` route (`OrderService`) **and** a CQRS route (`OrderQueryHandler` *and* `OrderCommandHandler`) — Lecture 5 only showed query handlers. Note this reminder writes `Any interfaces required (e.g. Aggregate, ValueObject)` — i.e. `Aggregate`, matching Lecture 4's wording (Lecture 5's reminder had written `AggregateRoot`).

---

**Custom Exceptions (p.8):**

Just to note the inclusion of the following custom exceptions in this handout.

**Ordering Context (application/exceptions):**

```java
public class OrderRestaurantNotFoundException extends RuntimeException {
    public OrderRestaurantNotFoundException(String buyer_id) {
        super(buyer_id);
    }
}
```

> **Note (verbatim quirk):** the constructor parameter is named `buyer_id` even though this exception is thrown for a missing *restaurant* (it is later thrown as `new OrderRestaurantNotFoundException(command.restaurantId())`). A copy-paste artefact carried through verbatim.

**GlobalExceptionHandler (p.8):**

Expanded the `handleNotFoundExceptions` method as follows (adding `OrderRestaurantNotFoundException.class` to the set — Lecture 5 handled only `BuyerNotFoundException` and `OrderNotFoundException`):

```java
@ExceptionHandler({BuyerNotFoundException.class, OrderNotFoundException.class,
        OrderRestaurantNotFoundException.class})
public ResponseEntity<Map<String, Object>> handleNotFoundExceptions(RuntimeException ex) {
    … rest of the code is the same
```

---

**Creating our Tables and Adding Data (in the resources folder) (p.9):**

**Note (verbatim):** For clarity I am partitioning the tables used by the Restaurant context off in the diagram.

**Figure 2: ERD of ordering schema (ERD initially generated by AI from human SQL).** A Crow's-Foot ERD. Entities and their columns:
- **buyer**: `id : VARCHAR «PK»`, `first_name : VARCHAR`, `surname : VARCHAR`
- **payment_method**: `id : int «generated» «PK»`, `buyer_id : VARCHAR «FK»`, `card_number : VARCHAR`, `cardholder_name : VARCHAR`, `expiry_month : int`, `expiry_year : int`, `security_number : VARCHAR`
- **order_restaurant**: `id : VARCHAR «PK»`, `name : VARCHAR`
- **order_from_user**: `id : VARCHAR «PK»`, `buyer_id : VARCHAR «FK»`, `restaurant_id : VARCHAR «FK»`, `first_name : VARCHAR`, `surname : VARCHAR`, `house_name_number : VARCHAR`, `street : VARCHAR`, `town : VARCHAR`, `post_code : VARCHAR`, `ordered_on : DATE`, `order_status : int`, `description_of_status : VARCHAR`
- **order_item**: `id : int «generated» «PK»`, `order_id : VARCHAR «FK»`, `product_id : VARCHAR`, `product_name : VARCHAR`, `unit_price : DECIMAL`, `units : int`
- **restaurant**: `id : VARCHAR «PK»`, `restaurant_name : VARCHAR`, `house_name_number : VARCHAR`, `street : VARCHAR`, `town : VARCHAR`, `post_code : VARCHAR`
- **menu_item**: `id : int «generated» «PK»`, `restaurant_id : VARCHAR «FK»`, `name : VARCHAR`, `price : DECIMAL(10,2)`

Relationships shown on the diagram (with the Restaurant-context tables — `restaurant`, `menu_item` — separated off behind a hand-drawn **orange partition line**): `buyer` **has saved a** `payment_method` (one-to-many); `buyer` **places a** `order_from_user` (one-to-many, optional); `order_restaurant` **snapshot restaurant details for** `order_from_user`; `restaurant` **receives** `order_from_user`; `restaurant` **offers** `menu_item` (one-to-many); `order_from_user` **contains** `order_item` (one-to-many, mandatory).

Business rules (verbatim bullets beneath Figure 2):
- A Buyer WILL have one or more payment methods (defined in the aggregate as a max of 3).
- Each Payment Method MUST belong to a specific Buyer.
- A Buyer CAN place one or more Orders.
- Orders from a user ARE made by a Buyer.
- OrderRestaurant holds the details of the restaurant id that might be referred to by a specific Order (these details are passed to the context via an event so contains a list of restaurants regardless of whether a specific order exists yet for them).
- Order WILL hold the Restaurant id contained in OrderRestaurant
- An Order MUST contain one or more Order Items.
- Each Order Item MUST belong to an Order.
- Restaurant MUST have one or more menu items
- Each menu item MUST be belong to a particular Restaurant.

---

**schema.sql (pp.10–11):**

> **Delta vs Lecture 5:** the `buyer.id` and `order_from_user.id` primary keys are now typed `VARCHAR(36)` (Lecture 5 used bare `VARCHAR`), and three **new** tables for the Restaurant context are added — `order_restaurant`, `restaurant`, and `menu_item`. This schema supersedes Lecture 5's.

```sql
-- The following tables are for the ordering context
CREATE TABLE buyer(
    id VARCHAR(36) PRIMARY KEY,
    first_name VARCHAR NOT NULL,
    surname VARCHAR NOT NULL
);
CREATE TABLE payment_method(
    id int AUTO_INCREMENT PRIMARY KEY,
    card_number VARCHAR NOT NULL,
    cardholder_name VARCHAR NOT NULL,
    expiry_month int NOT NULL,
    expiry_year int NOT NULL,
    security_number VARCHAR NOT NULL,
    buyer_id VARCHAR NOT NULL,
    FOREIGN KEY(buyer_id) REFERENCES buyer(id)
);
CREATE TABLE order_from_user(
    id VARCHAR(36) PRIMARY KEY,
    buyer_id VARCHAR NOT NULL,
    restaurant_id VARCHAR NOT NULL,
    first_name VARCHAR NOT NULL,
    surname VARCHAR NOT NULL,
    house_name_number VARCHAR NOT NULL,
    street VARCHAR NOT NULL,
    town VARCHAR NOT NULL,
    post_code VARCHAR NOT NULL,
    ordered_on DATE NOT NULL,
    order_status int NOT NULL,
    description_of_status VARCHAR NOT NULL
);
CREATE TABLE order_item(
    id int AUTO_INCREMENT PRIMARY KEY,
    product_id VARCHAR NOT NULL,
    product_name VARCHAR NOT NULL,
    unit_price DECIMAL NOT NULL,
    units int NOT NULL,
    order_id VARCHAR NOT NULL,
    FOREIGN KEY(order_id) REFERENCES order_from_user(id)
);
CREATE TABLE order_restaurant(
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR NOT NULL
);
-- The following tables are for the restaurant context
CREATE TABLE restaurant(
    id VARCHAR(36) PRIMARY KEY,
    restaurant_name VARCHAR NOT NULL,
    house_name_number VARCHAR NOT NULL,
    street VARCHAR NOT NULL,
    town VARCHAR NOT NULL,
    post_code VARCHAR NOT NULL
);
CREATE TABLE menu_item(
    id int AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    restaurant_id VARCHAR NOT NULL,
    FOREIGN KEY(restaurant_id) REFERENCES restaurant(id)
);
```

---

**data.sql (pp.12–13):**

> **Delta vs Lecture 5:** the ordering-context inserts are unchanged **except** that Lecture 5's `create sequence payment_method_sequence_id start with (select max(id) + 1 from payment_method);` line is **not** present in this handout's `data.sql`. New restaurant-context seed rows (`order_restaurant`, `restaurant`, `menu_item`) are added.

```sql
-- The following are for the ordering context
INSERT INTO buyer(id, first_name, surname)
VALUES('0000', 'Ivor', 'Menu');
INSERT INTO payment_method(card_number, cardholder_name, expiry_month, expiry_year,
security_number, buyer_id)
VALUES ('1111-1111-1111-1111','I Menu',12,2027,'123','0000'),
    ('2111-1111-1111-1111','Ivor Menu',12,2028,'234','0000');
INSERT INTO order_from_user(id, buyer_id, restaurant_id,
    first_name, surname,
    house_name_number, street, town, post_code,
    ordered_on, order_status, description_of_status)
VALUES ('1111','0000','2222',
    'I','Menu',
    'house1','street1','town1', 'post_code',
    '2025-06-23',2, 'The order was shipped');
INSERT INTO order_from_user(id, buyer_id, restaurant_id,
    first_name, surname,
    house_name_number, street, town, post_code,
    ordered_on, order_status, description_of_status)
VALUES ('1112','0000', '2222',
    'I','Menu',
    'house1','street1','town1',
    'post_code','2025-07-01',2, 'The order was shipped');
INSERT INTO order_item(product_id, product_name, unit_price, units, order_id)
VALUES ('1', 'chicken korma',10.5,1,'1111');
INSERT INTO order_item(product_id, product_name, unit_price, units, order_id)
VALUES ('1', 'chicken korma',10.5,1,'1112');
INSERT INTO order_item(product_id, product_name, unit_price, units, order_id)
VALUES ('2', 'garlic naan',3.5,1,'1112');

-- The following are for the restaurant context
INSERT INTO order_restaurant(id, name)
VALUES ('2222', 'Royal Balti');
INSERT INTO order_restaurant(id, name)
VALUES ('3333', 'Star of India');
INSERT INTO restaurant(id, restaurant_name,
    house_name_number, street, town, post_code)
VALUES ('2222', 'Royal Balti',
    'house1', 'street1','town1', 'ST2 1NG');
INSERT INTO restaurant(id, restaurant_name,
    house_name_number, street, town, post_code)
VALUES ('3333', 'Star of India',
    'house2','street2','town2', 'ST3 1FF');
INSERT INTO menu_item(restaurant_id, name, price)
VALUES('2222', 'chicken korma', 10.5);
INSERT INTO menu_item(restaurant_id, name, price)
VALUES( '2222', 'garlic naan', 3.5);
INSERT INTO menu_item(restaurant_id, name, price)
VALUES('3333', 'chicken korma', 12);
```

> **Seed-data notes:** `menu_item` is seeded with a `restaurant_id`, `name`, and `price` (the `INSERT` column list omits `id`, which auto-increments). Restaurant `2222` (Royal Balti) offers *chicken korma* (10.5) and *garlic naan* (3.5); restaurant `3333` (Star of India) offers *chicken korma* (12). The two seeded orders (`1111`, `1112`) both belong to buyer `0000` and restaurant `2222`, both with `order_status` = `2` and description "The order was shipped".

---

**Domain Model Operations — Command-Query Responsibility Segregation (CQRS) Architectural Pattern (p.14):**

CQRS - Represent any kind of operations **within the Bounded Context** which either affect:
- **Change the state** of the aggregate/entity (**command**)
- or, **query (view) the state** of the aggregate/entity (**query**)

**Figure 4: Commands/Queries within the Originations Bounded Context (to show the separation).** An ellipse labelled **Originations Bounded Context** on the left, with two arrows fanning out to the right. The top group, **Commands**, contains two orange/yellow boxes: **Open a Loan Account** and **Modify Loan Applicant Details**. The bottom group, **Queries**, contains two green boxes: **View Loan Account Details** and **View Loan Applicant Details**. (This is the same illustrative loan/originations example used in Lecture 5's CQRS section.)

**Quote (Vaughn Vernon, highlighted):** "Separate the query responsibilities from all the responsibilities that execute pure commands on the same model" (Vaughn Vernon, pg 139).

**Quote (Bertrand Meyer, highlighted — Command-Query Separation principle):** "EVERY method should be either a **command that performs an action**, or a **query that returns data** to the caller, but not both. In other words, asking a question [query] should not change the answer. More formally, methods should return a value only if they are referentially transparent and hence possess **no side effects**." (Bertrand Meyer)

**Are you modifying the state of the object? = Command (or write model) (p.15):**
- Commands do not return a value (void) back to the caller.
- **Commands are requests that have parameters to modify an aggregate** – so we pass any fields the system requires to create, edit or update an aggregate as part of the request – using JSON.
- The **UI layer (via a REST controller) transforms these end point requests and parameters into a suitably named Command object** that contain information (attributes) to modify the domain e.g. an end point to add a new Restaurant would be a POST request with parameters via a JSON payload that contains the Restaurant details to be created. This information is then assigned to an object called **AddNewRestaurantCommand** (or similar) which is in passed to the Application Service (or single Command Handler) for actioning. We then try to create a new Restaurant aggregate instance using these command details – **this command may or may not be successful as the data given may be invalid.** If unsuccessful no event would be generated. **It is up to the domain model via validation and domain invariants to determine if it is valid or not.**

**Figure 5: Commands from Clients travel 'one way' (to the command model then store).** The "full-fat" CQRS reference diagram. A client/monitor on the left issues two arrows. A **Query Processor** box (top) reads from a **Query Model** datastore (cylinder, top-right). A **Command Processors (Application Services)** box (bottom) writes to a **Command Model** (ellipse) which persists to a **Command Model Store** datastore (cylinder, bottom-right). An **Event (all) Subscriber** box sits in the middle: it observes events raised by command processing and updates the **Query Model** so the read side stays in line with the write side. Commands flow one way (client → command model → store); queries read from the separate query model.

**Figure 6: Diagram that illustrates CQRS in the sample system and the form of the data passed to layer.** Shows this project's classes (orange/peach boxes) and, on the arrows between them, the *form of data* passed (blue ellipses: **Command**, **DTO**, **Entity**, **Aggregate**):
- **Rest Controller** ↔ **Context Facade**: the controller passes a **Command** into the façade (and the façade returns to the controller).
- **Context Facade → Query Handler** (query path) and **Context Facade → ApplicationService** passing a **Command** (command path).
- **Query Handler → EntityToDTOMapper → DTO**: the query handler maps an entity to a **DTO** to return.
- **Repository → Query Handler**: passes an **Entity**.
- **Repository → ApplicationService**: passes an **Entity**.
- **Domain → ApplicationService**: passes an **Aggregate**.
- i.e. queries return DTOs mapped from entities; commands carry a Command into the ApplicationService, which loads Entities from the Repository and builds/validates Aggregates from the Domain before persisting.

**For more information consider Chapter 4 of *Implementing Domain Driven Design* by Vaughn Vernon.**

---

**More about CQRS (p.16):**

This diagram illustrates the **'full-fat' version of CQRS** – with separated databases, one for write actions on our persistence store (command model store) and one for read actions (query model). The idea behind this is that once command objects are actioned, each command generates an event object that can then be observed by a class that then initiates these actions on the query model so that it is then in line with the command model store.

All of what this diagram identifies is **NOT essential for simple CQRS systems** – here are a number of reasons why:
- We COULD do away with command objects and simply passed parameters from the controller to the service (or command handler) we would still have the segregation/separation of Command Handler and Query classes.
- Two physically separated database instances for the same bounded context – OK to have separate schemas on the same DB. Potentially our databases would differ in the methods they provided e.g. Command would have `save()` and `findById` (as we sometimes have a series of actions leading up to a save requiring us to read other aggregates). query would have a range of methods to retrieve data and it would be optimised for indexing - and the query database would be de-normalised to deliver just the data required for a specific view.
- We could even have a single DB as long as we have separate Commands and Queries at the application level.
- Some systems handle ALL commands asynchronously to handle larger demand.

---

**Ordering Context (p.17):**

You will have noticed earlier that the schema now includes an `order_restaurant` table – and that table (aside from an id which contains a restaurant_id), it also includes a name field (it could contain much more including the menu items available for a particular restaurant).

Why are we doing this? Remember that **in DDD we don't talk to other contexts (or have joins on tables)**, so we need to have all the available data for reporting purposes available 'within' the context. This means that whilst we do not require our order information to contain restaurant data (beyond the id of the restaurant that the order is for), here we will need other restaurant info for reporting purposes.

So, whilst we won't discuss it in this handout, **when a restaurant is created (or updated) in the Restaurant context (and saved to the restaurant table), an event is raised (e.g. `NewRestaurantAddedEvent`) that is subscribed to by our Ordering context** which then creates a restaurant with the sub set of info in `order_restaurant`.

Therefore, despite the Modulith having a single schema, **we are treating tables that are part of another schema as out of scope. This will make any transition to a microservice much easier as well.**

**Figure: Section of the ERD for our case study.** A cropped section of Figure 2 focusing on the Ordering/Restaurant boundary: `order_restaurant` and `restaurant` at the top, `order_from_user` and `menu_item` below, with the hand-drawn **orange partition line** separating the Restaurant-context tables (`restaurant`, `menu_item`) from the Ordering context. Relationship labels visible: "places a", "snapshot restaurant details for", "receives", "offers", "contains".

---

**OrderRestaurant (domain) (p.18):**

Prior to this lecture, we had a restaurant id (for simplicity and because new order info passed to the context needs to be as light weight as possible), but here we have a **value object created to hold restaurant info in Order** (as we need to be able to retrieve the relevant data for reporting purposes).

**Why a value record and not an aggregate, or an Entity?**
- It is **not an aggregate** because it is supporting information for another aggregate.
- It is **not an entity** because it is read only information, we do not need to identify it separately within the aggregate.

**Note (verbatim):** This version uses a modified version of `DomainAssertions` that returns a **trimmed** version of a string from `argumentNotEmpty`.

```java
@Embeddable // Needed for entity
public record OrderRestaurant (
    String id,
    String name
) implements ValueObject{
    public static final String RESTAURANT_ID_CANNOT_BE_EMPTY = "Restaurant name cannot be empty";
    public static final String RESTAURANT_NAME_CANNOT_BE_EMPTY = "Restaurant name cannot be empty";
    public OrderRestaurant{
        id = argumentNotEmpty(id, RESTAURANT_ID_CANNOT_BE_EMPTY);
        name = argumentNotEmpty(name, RESTAURANT_NAME_CANNOT_BE_EMPTY);
    }
}
```

> **Note (verbatim quirk):** both message constants read `"Restaurant name cannot be empty"` — the `RESTAURANT_ID_CANNOT_BE_EMPTY` constant carries the "name" message rather than an "id" one. Reproduced verbatim. Also note this is a **record** implementing `ValueObject` with a **compact constructor** (`public OrderRestaurant{ … }`) that reassigns the trimmed, validated values — consistent with the records refactor from Lecture 4.

---

**OrderJpa (persistence/entities) (p.19):**

```java
@Entity(name = "order_from_user")
@Table(name = "order_from_user")
@Getter
@Setter
@ToString(exclude = "orderItems") // prevent stack overflow loop
public class OrderJpa {
    @Id
    @Column(name = "id")
    private String id; // Created in the application service
    @NotBlank(message = "Buyer id is required")
    @Column(name = "buyer_id")
    private String buyerId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id")
    private OrderRestaurantJpa restaurant;
    @Embedded
    private FullName fullName;
    @Embedded
    @Valid
    private Address address;
    @Column(name = "ordered_on")
    private LocalDate orderedOn;
    @Column(name = "order_status")
    private int orderStatus;
    @Column(name = "description_of_status")
    private String descriptionOfStatus;
    @OneToMany(mappedBy = "order", cascade = {CascadeType.ALL}, orphanRemoval = true)
    private List<OrderItemJpa> orderItems = new ArrayList<>();
}
```

We have effectively got a couple of **design choices** here for assigning the restaurant details to this entity:
1. Do we **'snapshot'** the restaurant data when we write the order data to file, effectively writing the restaurant info as part of the order? Not shown here but achievable either by saving the name (or other fields) separately to the join here, or by including the `RestaurantOrder` value object instead of the JPA.
2. Do we **retrieve** the restaurant data (based on the id) when we read data from file and create a DTO? Keeping just the restaurant id here (as shown).

> **Note:** `OrderJpa` holds the restaurant as a `@ManyToOne`/`@JoinColumn(name = "restaurant_id")` relationship to `OrderRestaurantJpa` (option 2 — keep the id / relationship). `fullName` and `address` are `@Embedded` value objects; `address` also carries `@Valid`. `orderItems` is a `@OneToMany` with `cascade = ALL` and `orphanRemoval = true`.

---

**How does this affect the queries that we focused on last week? (p.20):**

**Figure: Find Order by id 1111 – but including the additional restaurant information.** A Postman capture of `GET http://localhost:8900/orders/1111` returning the JSON below. The **`restaurant`** block is highlighted (orange arrow) as the new addition compared with the Lecture 5 query output:

```json
{
    "id": "1111",
    "buyerID": "0000",
    "fullName": {
        "firstName": "I",
        "surname": "Menu"
    },
    "deliveryAddress": {
        "houseNameNumber": "house1",
        "street": "street1",
        "town": "town1",
        "postCode": "ST1 1NG"
    },
    "restaurant": {
        "id": "2222",
        "name": "Royal Balti"
    },
    "orderedOn": "2025-06-23",
    "orderStatus": "APPROVED",
    "descriptionOfStatus": "The order was shipped",
    "orderLines": [
        {
            "productId": "1",
            "productName": "chicken korma",
            "unitPrice": 11.0,
            "units": 1
        }
    ]
}
```

> **Notes:** the response now embeds the full `restaurant` object (`id` + `name`), not just a restaurant id. The JSON key is `buyerID` (capital "ID"). `orderStatus` serialises as the **enum name** `"APPROVED"` even though `data.sql` stored the raw `order_status` as the int `2` — the int is mapped through the `OrderStatus` enum (persisted via `order.orderStatus().ordinal()`; see `OrderDomainToJpaMapper`). `unitPrice` shows as `11.0` in this capture.

We are now going to focus on **commands** for our ordering context. **I am only considering one here (because the others happen via events – discussed in the next handout).**

---

**Adding a new order (p.21):**

**Figure: Sequence diagram for Adding a new order.** Renders the PlantUML in **Appendix B** (see below) — the full `POST /orders (AddNewOrderCommand)` flow: `OrderController → ContextFacade → OrderApplicationService`, which calls `OrderRestaurantRepository.findById`, maps via `OrderRestaurantJpaToDomainMapper`, generates an `Identity`, builds `OrderRestaurant`, maps order lines via `OrderDTOToDomain`, constructs the `Order` aggregate, maps via `OrderDomainToJpaMapper`, and finally `OrderRepository.save(OrderJpa)`, returning `HTTP 201 Created`.

**The command record:**

```java
public record AddNewOrderCommand(
    String consumerId,
    FullName fullName,
    String restaurantId,
    String paymentMethodId,
    Address deliveryAddress,
    List<OrderLineDTO> orderItems
){
    // add validation
}
```

What does this look like as a request..

**HTTP verb and end point (p.22):**
**POST** `http://localhost:8900/orders`  *(embedded hyperlink in the PDF: http://localhost:8900/orders)*

**Figure: Postman request to add a new order.** A `POST http://localhost:8900/orders` with a raw JSON body. The handout places a callout box beside it — **"Compare the payload with the command:"** — repeating the `AddNewOrderCommand` record signature (`String consumerId, FullName fullName, String restaurantId, String paymentMethodId, Address deliveryAddress, List<OrderLineDTO> orderItems`) so the reader can line the JSON fields up against the record components.

```json
{
    "consumerId": "0000",
    "fullName": {
        "firstName": "Bob",
        "surname": "Patel"
    },
    "restaurantId": "2222",
    "paymentMethodId": "visa",
    "deliveryAddress": {
        "houseNameNumber": "10",
        "street": "High Street",
        "town": "Stoke",
        "postCode": "ST1 12G"
    },
    "orderItems": [
        {"productId": "1",
         "productName": "Chicken Korma",
         "unitPrice": 3.5,
         "units": 1}
    ]
}
```

**Note (verbatim):** It is **VITAL** that the JSON payload structure and naming matches your command record.

> **Notes:** the payload's top-level keys map one-to-one onto the `AddNewOrderCommand` components — `consumerId`, `fullName` (a `FullName`), `restaurantId`, `paymentMethodId`, `deliveryAddress` (an `Address`), `orderItems` (a `List<OrderLineDTO>`). In this example `paymentMethodId` is set to the string `"visa"` (a scheme name rather than a numeric id — reproduced as printed). The order-line JSON uses `productId`/`productName`/`unitPrice`/`units`, matching `OrderLineDTO`.

---

**OrderController (p.23):**

`@RequestBody` - bind the incoming HTTP request body directly to command. Spring then reserialises this automatically.

If you don't have validation it is possible to omit fields in your request and then these are simply empty in the command when passed to the façade, thus the problem is picked up further downstream (hopefully).

```java
@PostMapping()
@ResponseStatus(HttpStatus.CREATED)
public void addNewOrder(@RequestBody AddNewOrderCommand command) {
    facade.addNewOrder(command);
}
```

> **Note:** the controller method returns `void` (commands do not return a value) and is annotated `@ResponseStatus(HttpStatus.CREATED)` — hence the `201 Created` seen in Postman.

**ContextFacade (p.23):**

We are adding our `OrderApplicationService` dependency (discussed over the page) that is required to handle **command** requests separately to **query** requests.

```java
@Component
@AllArgsConstructor
public class ContextFacade {
    private final BuyerQueryHandler buyerQueryHandler;
    private final OrderQueryHandler orderQueryHandler;
    private final OrderApplicationService orderApplicationService;
    … removing the other methods from here to reduce the volume of code shown
    public void addNewOrder(AddNewOrderCommand addNewOrderCommand) {
        orderApplicationService.addNewOrder(addNewOrderCommand);
    }
}
```

> **Note:** the façade now depends on **three** collaborators — two query handlers (`BuyerQueryHandler`, `OrderQueryHandler`) and the new **`OrderApplicationService`** (the command side). In Lecture 5 the façade held query handlers only; this is where the command path is wired in.

---

**OrderApplicationService (application) (p.24):**

This class is called by the `contextFacade` when requests are **commands** rather than **queries**.

`@Service` ensures that the IoC container can inject this dependency when using `@AllArgsConstructor`.

**Note (verbatim):** The various calls to the `OrderRestaurantRepository` (and `OrderRestaurantJpa`) plus mappers for this method are included over the following pages.

```java
@Service
@AllArgsConstructor
public class OrderApplicationService {
    private final OrderRepository orderRepository;
    private final OrderRestaurantRepository restaurantRepository;
    private final Logger LOG = LoggerFactory.getLogger(getClass());

    public void addNewOrder(AddNewOrderCommand command){
        // Retrieve the local restaurant info
        OrderRestaurant restaurant = restaurantRepository.findById(command.restaurantId())
            .map(OrderRestaurantJpaToDomainMapper::map)
            .orElseThrow(() -> new OrderRestaurantNotFoundException(command.restaurantId()));
        // We could validate that the menu items in the command are available at that restaurant (if we synced menu items as well)
        Identity<Order> newOrderId = Identity.generateId();
        // Pass info to aggregate to validate (including other fields from command required by aggregate)
        Order newOrder = new Order(newOrderId,
            command.consumerId(),
            // convert from separate fields to entity using factory method
            new OrderRestaurant(restaurant.id(),
                restaurant.name()),
            command.fullName(),
            command.deliveryAddress(),
            // Retrieve each order line from the command object and map to domain
            OrderDTOToDomain.map(command.orderItems(), newOrderId))
        );
        orderRepository.save(OrderDomainToJpaMapper.map(newOrder));
    }
}
```

> **Walkthrough of `addNewOrder`:**
> 1. Look up the local `order_restaurant` row by `command.restaurantId()` via `OrderRestaurantRepository.findById`, map the JPA to the `OrderRestaurant` value object, or throw `OrderRestaurantNotFoundException` if absent.
> 2. Generate a new order id via `Identity.generateId()` (typed `Identity<Order>`).
> 3. Construct the **`Order` aggregate** — this is where domain **validation/invariants** run. It is passed the new id, the consumer id, a fresh `OrderRestaurant(id, name)`, the command's `fullName` and `deliveryAddress`, and the order lines mapped to domain `OrderItem`s via `OrderDTOToDomain.map(command.orderItems(), newOrderId)`.
> 4. Persist via `orderRepository.save(OrderDomainToJpaMapper.map(newOrder))` — the domain aggregate is mapped back to `OrderJpa` before saving.
>
> **Note (verbatim quirk):** the comment says "convert from separate fields to entity using **factory method**" but the code uses the `new OrderRestaurant(...)` **constructor** directly (not a static factory). Reproduced as printed. Also note the query-side repo (`OrderRestaurantRepository`) is reused inside a command handler — the "series of actions leading up to a save requiring us to read other aggregates" scenario mentioned in *More about CQRS*.

---

**OrderRestaurantJpaToDomainMapper (application/mapper) (p.25):**

The following code is used by the first line of `addNewOrder`.

```java
public class OrderRestaurantJpaToDomainMapper {
    public static OrderRestaurant map(OrderRestaurantJpa jpaEntity) {
        return new OrderRestaurant(jpaEntity.getId(), jpaEntity.getName());
    }
}
```

**OrderRestaurantJpa (persistence/entities) (p.25):**

```java
@Entity(name="order_restaurant")
@Table(name="order_restaurant")
@ToString
@Getter
@Setter
public class OrderRestaurantJpa {
    @Id
    @Column(name="id")
    private String id;
    @Column(name="name")
    private String name;
}
```

We know that there is a precedent for using value objects in Jpas from previous examples but **if the following entity exists then why not use this entity as an attribute of `OrderJpa`?** *(rhetorical prompt in the handout — `OrderJpa` does in fact hold `OrderRestaurantJpa` via the `@ManyToOne` join rather than embedding the `OrderRestaurant` value object directly.)*

**OrderRestaurantRepository (persistence/repositories) (p.25):**

```java
@Repository
public interface OrderRestaurantRepository extends CrudRepository<OrderRestaurantJpa, String> {
}
```

---

**OrderDTOToDomain (application/mapper) (p.26):**

```java
public class OrderDTOToDomain {
    public static List<OrderItem> map(List<OrderLineDTO> dtoList, Identity<Order> orderId) {
        return dtoList.stream()
            .map(dto -> new OrderItem(
                orderId.id(),
                dto.productId(),
                dto.productName(),
                new Money(dto.unitPrice()),
                dto.units()
            ))
            .toList();
    }
}
```

> **Note:** maps each `OrderLineDTO` from the command into a domain `OrderItem`, wrapping the raw `unitPrice` in the `Money` value object and stamping each item with the parent `orderId.id()`.

---

**OrderDomainToJpaMapper (application/mapper) (pp.27–28):**

```java
public class OrderDomainToJpaMapper {
    public static OrderJpa map(Order order){
        OrderJpa orderJpa = new OrderJpa();
        orderJpa.setId(order.id().id());
        orderJpa.setBuyerId(order.buyerID());
        OrderRestaurantJpa orderRestaurantJpa = new OrderRestaurantJpa();
        orderRestaurantJpa.setId(order.restaurant().id());
        orderRestaurantJpa.setName(order.restaurant().name());
        orderJpa.setRestaurant(orderRestaurantJpa);
        FullName fullName = new FullName(order.fullNameOfBuyer().firstName(),
            order.fullNameOfBuyer().surname());
        orderJpa.setFullName(fullName);
        Address address = new Address(order.deliveryAddress().houseNameNumber(),
            order.deliveryAddress().street(),
            order.deliveryAddress().town(),
            order.deliveryAddress().postCode());
        orderJpa.setAddress(address);
        orderJpa.setOrderedOn(order.orderedOn());
        orderJpa.setOrderStatus(order.orderStatus().ordinal());
        orderJpa.setDescriptionOfStatus(order.descriptionOfStatus());
        orderJpa.setOrderItems(toListOrderItemJpa(order.orderItems(), orderJpa));
        return orderJpa;
    }
    // Helper method - OrderItem (menu item) domain to Jpa
    private static List<OrderItemJpa> toListOrderItemJpa(List<OrderItem> orderItems, OrderJpa order){
        List<OrderItemJpa> orderItemJpaList = new ArrayList<>();
        for (OrderItem orderItem : orderItems){
            OrderItemJpa orderItemJpa = new OrderItemJpa();
            // order item id missing here as it will be set by Spring
            orderItemJpa.setProductId(orderItem.productId());
            orderItemJpa.setProductName(orderItem.productName());
            orderItemJpa.setUnitPrice(orderItem.unitPrice().asBigDecimal().doubleValue());
            orderItemJpa.setUnits(orderItem.units());
            // Add order reference for this order item
            orderItemJpa.setOrder(order);
            // add new item to the list
            orderItemJpaList.add(orderItemJpa);
        }
        return orderItemJpaList;
    }
}
```

> **Notes:** the mapper flattens the `Order` aggregate back to `OrderJpa`: it sets a fresh `OrderRestaurantJpa` (id + name) as the `restaurant` relationship; rebuilds the `FullName` and `Address` embeddables from the domain value objects; stores `order.orderStatus().ordinal()` (the enum ordinal → the `int order_status` column); and converts each domain `OrderItem` to an `OrderItemJpa` via the helper `toListOrderItemJpa`, wiring the back-reference (`orderItemJpa.setOrder(order)`) and leaving the item `id` unset (Spring/JPA assigns it). `unitPrice` is unwrapped from `Money` via `asBigDecimal().doubleValue()`.

---

**Appendix A – PlantUML for ERD (pp.29–30):**

```plantuml
@startuml
' Layout adjustments
hide circle
entity buyer {
    id : VARCHAR <<PK>>
    first_name: VARCHAR
    surname : VARCHAR
}
entity payment_method {
    id : int <<generated>> <<PK>>
    buyer_id : VARCHAR <<FK>>
    card_number : VARCHAR
    cardholder_name : VARCHAR
    expiry_month : int
    expiry_year : int
    security_number : VARCHAR
}
entity order_from_user {
    id : VARCHAR <<PK>>
    buyer_id : VARCHAR <<FK>>
    restaurant_id : VARCHAR <<FK>>
    first_name : VARCHAR
    surname : VARCHAR
    house_name_number : VARCHAR
    street : VARCHAR
    town : VARCHAR
    post_code : VARCHAR
    ordered_on : DATE
    order_status : int
    description_of_status : VARCHAR
}
entity order_item {
    id : int <<generated>> <<PK>>
    order_id : VARCHAR <<FK>>
    product_id : VARCHAR
    product_name : VARCHAR
    unit_price : DECIMAL
    units : int
}
entity order_restaurant {
    id : VARCHAR <<PK>>
    name : VARCHAR
}
entity restaurant {
    id : VARCHAR <<PK>>
    restaurant_name : VARCHAR
    house_name_number : VARCHAR
    street : VARCHAR
    town : VARCHAR
    post_code : VARCHAR
}
entity menu_item {
    id : int <<generated>> <<PK>>
    restaurant_id : VARCHAR <<FK>>
    name : VARCHAR
    price : DECIMAL(10,2)
}
' Relationships
buyer ||--{ payment_method : "has saved a"
buyer ||--o{ order_from_user : "places a"
order_from_user ||--|{ order_item : "contains"
' New Relationships
restaurant ||--|{ menu_item : "offers"
restaurant ||--o{ order_from_user : "receives"
order_restaurant ||--{ order_from_user : "snapshot restaurant \ndetails for"
@enduml
```

---

**Appendix B – New Order Sequence Diagram (pp.31–32):**

```plantuml
@startuml
hide footbox
actor "External System / Client" as Client
Client -> OrderController: POST /orders (AddNewOrderCommand)
activate OrderController
OrderController-> ContextFacade: addNewOrder(command)
activate ContextFacade
ContextFacade-> OrderApplicationService: addNewOrder(command)
activate OrderApplicationService
OrderApplicationService -> OrderRestaurantRepository: findById(restaurantId)
activate OrderRestaurantRepository
OrderRestaurantRepository --> OrderApplicationService: OrderRestaurantJpa
deactivate OrderRestaurantRepository
OrderApplicationService-> OrderRestaurantJpaToDomainMapper: map(OrderRestaurantJpa)
activate OrderRestaurantJpaToDomainMapper
OrderRestaurantJpaToDomainMapper --> OrderApplicationService: OrderRestaurant (Domain)
deactivate OrderRestaurantJpaToDomainMapper
OrderApplicationService-> Identity: generateId()
activate Identity
Identity--> OrderApplicationService: orderId
deactivate Identity
OrderApplicationService-> OrderRestaurant: new OrderRestaurant(id, name)
activate OrderRestaurant
OrderRestaurant--> OrderApplicationService: newOrderRestaurant (Domain Aggregate)
deactivate OrderRestaurant
OrderApplicationService-> OrderDTOToDomain: map(orderItems, orderId)
activate OrderDTOToDomain
OrderDTOToDomain--> OrderApplicationService: List<OrderItem> (Domain)
deactivate OrderDTOToDomain

OrderApplicationService-> Order: new Order(orderId, buyerId, restaurant, ...)
activate Order
Order--> OrderApplicationService: newOrder (Domain Aggregate)
deactivate Order

OrderApplicationService-> OrderDomainToJpaMapper: map(newOrder)
activate OrderDomainToJpaMapper
OrderDomainToJpaMapper --> OrderApplicationService: OrderJpa
deactivate OrderDomainToJpaMapper
OrderApplicationService-> OrderRepository: save(OrderJpa)
activate OrderRepository
OrderRepository--> OrderApplicationService: saved OrderJpa
deactivate OrderRepository
OrderApplicationService --> ContextFacade
deactivate OrderApplicationService
ContextFacade --> OrderController
deactivate ContextFacade
OrderController--> Client : HTTP 201 Created
deactivate OrderController
@enduml
```

> **Note (verbatim quirk):** the sequence diagram labels the reply from `new OrderRestaurant(id, name)` as `newOrderRestaurant (Domain Aggregate)` and the reply from `new Order(...)` as `newOrder (Domain Aggregate)`. `OrderRestaurant` is explicitly **not** an aggregate (p.18 states it is a value object supporting the `Order` aggregate) — the "(Domain Aggregate)" label on the `OrderRestaurant` line is an inconsistency, reproduced as printed.

### LECTURE 7 CONTENT — FULL DETAIL

---

#### Document 10: Enterprise Application Development — Local Domain Events

**File:** `Enterprise_Application_Development_-_Working_with_local_events.pdf` (title on every page reads "Enterprise Application Development – Local Domain Events")
**Author:** Phil James (PDF metadata records the author as "JAMES Phillip"; every page footer reads "Author: Phil James")
**Pages:** 33
**Created:** 24 July 2026 (from PDF metadata)

This is the **Local Domain Events** handout — the lecture that Lecture 6 (CQRS: Commands) explicitly deferred to ("the remaining Ordering-context write operations happen **via events** … discussed in the next handout"). It is the **first events handout** and is scoped to **local** domain events only — events raised and consumed **within a single bounded context** (here, the Ordering context, between the `Order` and `Buyer` aggregates). It explicitly signposts a future **remote events** lecture (`RemoteEvent`, message brokers, cross-context/eventual consistency) that is out of scope here.

The worked example: when a new order is added, the `Order` aggregate raises a `DeliveryAddressAddedEvent`; a listener in the same context passes the delivery address to the `Buyer` aggregate, which keeps a de-duplicated `Set` of every delivery address it has ever used. This is the concrete continuation of the Lecture 6 *add-new-order* command.

**Learning Outcomes (all three listed in this handout — identical to Lectures 5 & 6):**
- Critically evaluate development approaches to solutions to enterprise applications.
- Design an enterprise application, critically evaluating alternatives and justifying selections.
- Implement an enterprise application that includes relevant security features.

**Mapping to Knowledge, Skills and Behaviours (verbatim table from the PDF — identical KSB set to Lecture 6):**

| Key | Knowledge, Skills and Behaviour |
|---|---|
| K21 | How to operate at all stages of the software development life cycle and how each stage is applied in a range of contexts. For example, requirements analysis, design, development, testing, implementation. |
| K22 | Principles of a range of development techniques, for each stage of the software development cycle that produce artefacts and the contexts in which they can be applied. For example, UML, unit testing, programming, debugging, frameworks, architectures. |
| S18 | Use appropriate analysis methods, approaches and techniques in software engineering projects to deliver an outcome that meets requirements. |
| S19 | Implement software engineering projects using appropriate software engineering methods, approaches and techniques. |
| S21 | Determine, refine, adapt and use appropriate software engineering methods, approaches and techniques to evaluate software engineering project outcomes. |

**Objectives (verbatim bullets from p.1):**
- Explain what is meant by a domain event
- Differentiate between a domain event and an application event
- Differentiate between an event and a command
- Using events to communicate between aggregates in a bounded context

**Contents (as printed in the PDF, with page numbers):**
- Objectives — 1
- Declarative vs Reactive Systems – reminder (from day 2 Intro to DDD) — 5
- Domain Model Operations (reminder) — 6
- Ordering Context – reminder of what functionality we currently have — 7
  - OrderController — 7
  - BuyerController — 7
- What is a Domain Event? — 8
  - Amendments to the Buyer Aggregate — 9
- Amending our Aggregate Super Type — 11
  - AggregateRoot (common) — 11
  - Event.java (common/events) — 12
  - LocalEvent.java (common/events) — 12
  - Order (domain) — 13
  - DeliveryAddressAddedEvent (domain/events) — 14
  - OrderApplicationService (application) — 15
  - DomainEventManager (common) — 16
  - EventStoreService (common) — 17
  - EventStoreJpa (common) — 17
  - EventStoreRepository (common) — 17
  - Schema.sql — 18
- Considering the subscriber/listener of our event — 19
  - DeliveryAddressAddedListener — 19
  - DeliveryAddress (domain) — 20
  - DeliveryAddressAddedDomainEventMapper (application/mapper) — 22
  - BuyerApplicationService — 23
  - Buyer (domain) – amendments — 24
  - BuyerJpa — 26
  - DeliveryAddressJpa — 27
  - BuyerToJpaMapper — 29
- Testing That It Works — 30
  - BuyerController (ui) - modify — 32
  - ContextFacade - modify — 32
  - BuyerQueryHandler - modify — 32
  - BuyerJpaToDTOMapper — 33

> **Note on this handout's structure:** unlike Lectures 5 & 6, there is **no Academic Disclosure block, no "Maths, English and Digital Skills Development" block, and no PlantUML appendices** in this PDF. The Contents list on p.2 stops at `BuyerJpaToDTOMapper — 33` (p.3 in the PDF just carries the overflow of that one line).

---

**Boundaries of Context for a Fictional Ordering System (reminder) — p.4**

> **Figure: Boundaries of Context — Ordering.** A single blue oval labelled **Order** (the context name is repeated — the oval's own centred label reads "Order", and a smaller **Order** aggregate box sits at the top) containing two orange aggregate boxes: **Order** (top) and **Buyer** (bottom).

Verbatim text: *"**Order is the core domain here.** We are going to focus on how we can use events to communicate between Order and Buyer within the Ordering context."*

---

**Declarative vs Reactive Systems – reminder (from day 2 Intro to DDD) — p.5**

This is a verbatim reminder of the declarative-vs-reactive material first introduced in the Lecture 2 DDD Overview.

> **Figure: Declarative (orchestrated).** The **Shopping-Cart Service** (basket icon) on the left issues three direct labelled calls to three services on the right: `issueInvoice()` → **Billing Service**, `queueItemForShipping()` → **Warehouse Service**, and `emailCustomer()` → **Email Service**.

*Declarative (orchestrated):* Central service **communicates directly** with another service (or services); this works in a monolith via function/method calls – we would see this approach used in a modular monolith via communication with the context façade of a context. It does not work when these are network calls to other remote services (microservice).

With the above example, this approach requires the shopping-cart service (to orchestrate) to know things about the other downstream services (higher coupling) – **to call these services directly (commands), to know what to expect in response, and in do this in a particular order (sequence).**

> **Figure: Reactive (choreographed = publish/subscribe model).** The **Shopping-Cart Service** on the left raises a single event `orderPlaced()` in the centre; that event fans out to the same three services on the right (**Billing Service**, **Warehouse Service**, **Email Service**), which are each subscribed to it.

*Reactive (choreographed = publish/subscribe model):* Shopping cart **announces an event using a messaging system**. **Other services** are registered with the message system (can be in-memory event broker for local events, or message broker for remote events), and **are waiting (listening) for that type of event to be generated and then they can act accordingly**. Compared to the declarative approach we now have lower coupling (no knowledge of who wants to use this event). If a consumer of these events is 'down' the message broker holds the event until they recover.

This type of approach can be done in monolith/modulith or microservice.

---

**Domain Model Operations (reminder) — p.6**

Reminder table (verbatim):

| Business Operations | Commands – objects that wrap create, update, delete requests from a user – sent to aggregate |
| | Queries – read data from persistence and display |
| Business Events | Events – record that commands were successful – generate other commands. |
| Business Flows | Sagas |

> **Note:** this is the same "Domain Model Operations" table used in Lectures 5 & 6. This handout is concerned with the **Business Events** row — specifically **Events** ("record that commands were successful – generate other commands"). **Sagas** (Business Flows) are named but not covered.

---

**Ordering Context – reminder of what functionality we currently have — p.7**

**OrderController** — endpoints defined (presently):
1. findAll
2. find Orders by Buyer id
3. find Order by Order id
4. add a new Order (details passed as a JSON payload using `AddNewOrderCommand`)
5. cancel an Order (using order id via a JSON payload using `CancelOrderCommand`)

> **Note (verbatim):** *"In reality Ordering relies upon other bounded contexts so things like adding a new order might be the result of an event from another context, so really would not have a controller end point (would be handled by the `OrderApplicationService` via an event listener) – but for now, as we have not covered this yet, we are leaving it 'as is'."*
>
> (Note the mention of `CancelOrderCommand` at endpoint 5 — a cancel-order command exists in the controller but is not implemented/traced in any handout so far.)

**BuyerController** — endpoints defined (presently):
1. findAll
2. find Buyer by id
3. find all the payment details for a buyer

> **Note (verbatim):** *"You will notice that there are no end points for adding buyers or payment methods, etc – and as a result **no `BuyerApplicationService`** – there is a reason for this – which I am about to explain!"* (This handout is where `BuyerApplicationService` is finally introduced — its first mutating use is driven by an event, not a controller endpoint.)

---

**What is a Domain Event? — p.8**

Evans definition (verbatim quote): *"Model information about activity in the domain as a series of discrete events. Represent each event as a domain object. . . . A domain event is a full-fledged part of the domain model, a representation of something that happened in the domain".* [Evans, p. 20]

Events are part of the **Ubiquitous language** – that is, understood by domain experts to describe an action of the system. These events may be **local** (to the bounded context) or **foreign** (to other bounded contexts or other systems entirely – some mistakenly call these **integration events**).

**Domain events are generated by the aggregates themselves in response to a command.**

The language used to define the naming of a Command Vs an Event is as follows (note Phil has the words "Command" and "Event" in his naming – "you might not see this as necessary"). Some commands are not accepted possibly due to invalid data or a violation of a business rule enforced by an aggregate.
- `AddNewOrderCommand` – intent to add a new order (**present tense**) – <u>may</u> be rejected
- `DeliveryAddressAddedEvent` – delivery address was added (**past tense**) – <u>has</u> been accepted

Our `Order` is going to raise a **local domain event** (an event raised and listened to within a single bounded context) which notifies any subscribing classes listening to that event. Our order receives a command to add a new order; after accepting the order, it raises an event to notify any subscribers that, as part of the order, a delivery address is received and has been added to the order. **Now the Order does not know if that delivery address is a new one or an existing one — that is the responsibility of the Buyer (hence the local event).**

> **Figure (highlighted yellow caption): "Order context showing the events (at present) that will be discussed in this handout and generated."** A large teal oval labelled **Ordering** contains two orange aggregate boxes — **Buyer** (left) and **Order** (right) — with a small green box labelled **DeliveryAddressAdded** between them. A dashed/dot-dashed arrow flows **from Order → (DeliveryAddressAdded) → Buyer**, i.e. the Order raises the event and the Buyer consumes it.

---

**Amendments to the Buyer Aggregate — p.9**

A buyer will keep a **set** (a collection that cannot have duplicates) of delivery addresses that have been used by that buyer for their orders. Thus over time, we build a list of all the delivery addresses used.

To do this, the buyer needs the delivery address from each order, so the **Order aggregate raises an event when a new order is received** containing specific information about that order to identify the buyer (id) and the delivery address information received as part of that new order command. The Buyer is then notified of this event (via a **listener** — a subscriber class created to respond to a particular event — so that when this event is received, it passes the delivery address and buyer id to `BuyerApplicationService`). We can then identify the particular buyer (via the event buyer id) and pass the delivery address to it. **If that delivery address is new, it is added, and we can then map the Buyer to its persistence version for saving.**

> **Figure (caption: "Aggregates create Events and publish them, Source: Vaughn").** A reference diagram from Vaughn Vernon showing the anatomy of event publishing. On the left, an **Aggregate** box: a **create** arrow points up to an **Event** box, and a **publish** arrow points to an **Event Publisher** (labelled *Lightweight Publisher*). From the Event Publisher, a **handle Event** arrow feeds three subscribers:
> - **Event Storing Subscriber** (labelled *Lightweight Subscriber storing all events*) — has a **store** arrow to a data-store cylinder at the top, captioned *"Data store is same used by domain model (simple single transaction)"*.
> - **Simple Subscriber** (bottom).
> - **Immediate Forwarding Subscriber** (middle) — with a note *"XA/2PC required here"* — which feeds a **Message Queue** (top right, backed by its own cylinder and captioned *Messaging Infrastructure (MoM)*); a **Forwarder** box also feeds the Message Queue.
> - **Remote Subscriber** (stacked boxes, right) consumes from the Message Queue.
>
> The three left-hand subscribers are bracketed as **Lightweight Subscribers**.

Two patterns are then defined beneath the figure (verbatim):
- **Simpler Subscriber (Store and Forward Pattern)** – resides within the same bounded context, local event is processed **typically within the same transaction boundary** as the aggregate that triggered it. We may also store events for a variety of reasons including publishing them asynchronously later.
  There is also another pattern (**not used by the case study** but shown above) called the **Immediate Forwarding Pattern** where a locally received event is published to a remote message broker (where a remote subscriber is listening in) **before committing** the local database transaction. The challenge with this is that we need the database write and the message publishing to **both succeed or fail**.
- **Remote Subscriber** – resides in another bounded context (and typically consumes an event across a **network boundary**) via a message broker; it therefore cannot/should not participate in the local database transaction, thus operates in its **own transactional event** (due to the temporal nature of this and the chance of failure to receive this event for some reason, we have **eventual consistency**). Remote subscribers **must not directly consume the publisher's internal class types**; instead they should consume a **serialised message (e.g. JSON)** that is mapped to a **published language** (to avoid conforming to an internal model).

> **Scope note:** the case study uses the **Store and Forward** (Simpler Subscriber) pattern for this local event. Immediate Forwarding, remote subscribers, message brokers, XA/2PC and eventual consistency are all previewed here but deferred to the **remote events** lecture.

---

**Amending our Aggregate Super Type — p.11**

When we interact with an aggregate it will create events. It will **not** communicate events with the outside world directly (as it has no knowledge of who is communicating with it — **uni-directional pattern**), so we need to **store these events in our aggregate**. We can then extract them via our `ApplicationService` class (e.g. `OrderApplicationService`) **after** we have actioned any commands received from our controller (as events are responses to commands).

In order for an Aggregate to store events (and for our `ApplicationService` to retrieve them) we need to **change our `AggregateRoot` interface** (used to identify a particular entity as an aggregate) **to a class**. This enables it to store any events raised by that aggregate, as well as provide methods/operations related to that list of events.

> **Delta vs Lecture 6:** in Lecture 6, aggregate roots were marked with an `AggregateRoot` **interface**. It is now promoted to an **abstract class** so it can hold the list of raised events. This supersedes the earlier form. (`Order` and `Buyer` change from `implements`/marker usage to `extends AggregateRoot<T>`.)

**AggregateRoot (common)** — because this class is still representing an entity (albeit a very important one), it needs to **extend the `Entity` class** (with its id field), passing the Type on to `Entity`:

```java
public abstract class AggregateRoot<T> extends Entity<T> {
    public List<Event> domainEvents = new ArrayList<>();

    public AggregateRoot(Identity<T> id) {
        super(id);
    }

    protected void addDomainEvent(Event event){
        domainEvents.add(event);
    }

    protected void removeDomainEvent(Event event){
        domainEvents.remove(event);
    }

    public List<Event> listOfDomainEvents(){
        return domainEvents;
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }

    public boolean domainEventsExist(){
        return ! domainEvents.isEmpty();
    }
}
```

In the `common` folder Phil has defined the following interfaces.

**Event.java (common/events)**

```java
public interface Event {
}
```

`Event` – interface defined to represent events published via Spring. We are using this class to store **DomainEvents**, but we also have something called an **Application Event** — what is the difference? **Domain events** are events generated by the domain (aggregates) and **for** the domain, whereas **application events** are things such as sending an email which might also be triggered when a new user is created for example (these types of event are stored in the **application layer**).

> **Note (verbatim):** *"you may also come across the term **Infrastructure Events** – which as the name suggests relate to actions at the infrastructure layer – in DDD we don't have these as separate events, these would relate to application events."*

**LocalEvent.java (common/events)**

```java
public interface LocalEvent extends Event {
}
```

The creation of `LocalEvent` (and `RemoteEvent`) allows us to differentiate different `Event` **sub types** (we will see how this works in the **remote events lecture**). This helps when determining **where to send an event** after it has been raised by the aggregate.

> **Note (forward reference):** `RemoteEvent` is named here but not defined in this handout — it belongs to the remote-events lecture. Also note that although `LocalEvent` is defined, the `DeliveryAddressAddedEvent` in this handout is written `implements Event` (not `implements LocalEvent`) — see the quirk flagged under `DeliveryAddressAddedEvent` below.

---

**Order (domain) — p.13**

Amend the opening line so `Order` extends the new `AggregateRoot` class — this gives the facility to store events:

```java
public class Order extends AggregateRoot<Order> {
```

When a new `Order` is created, the constructor checks that the data provided is acceptable. We might think this is the place to add our new event — **but we would be wrong.** If every time we called the constructor it raised an event, then when we **read data back from our repository** and assigned it to a domain instance of `Order`, we would be **unintentionally raising events we did not need to**. So while we need to call the constructor, we must **separate the event-raising element** to avoid this issue.

Start by making the constructor **private**:

```java
private Order(Identity<Order> id,
              String buyerID,
              OrderRestaurant orderRestaurant,
              FullName fullNameOfBuyer,
              Address deliveryAddress,
              List<OrderItem> orderItems) {
… remaining code is omitted
```

We already have a **factory method** in our class, which in turn calls the constructor. This is called when we **read data from the repository** (the **no-event** read path):

```java
public static Order orderOf(Identity<Order> id,
                            String buyerID,
                            OrderRestaurant orderRestaurant,
                            FullName fullNameOfBuyer,
                            Address deliveryAddress,
                            List<OrderItem> orderItems,
                            LocalDate orderedOn,
                            OrderStatus orderStatus,
                            String descriptionOfStatus){
    Order order = new Order(id, buyerID, orderRestaurant,
                            fullNameOfBuyer,
                            deliveryAddress,
                            orderItems);
… remaining code is omitted
```

What we need is something similar to our factory method but that **creates/raises** our event — in this case `DeliveryAddressAddedEvent`. We then explicitly call `addDomainEvent` (inherited from `AggregateRoot`) and pass it our Event record to record that something has happened (the **write / event-raising** path):

```java
// Used for event generation
public static Order OrderOfWithEvent(Identity<Order> id,
                                     String buyerID,
                                     OrderRestaurant orderRestaurant,
                                     FullName fullNameOfBuyer,
                                     Address deliveryAddress,
                                     List<OrderItem> orderItems) {
    Order newOrder = new Order(id, buyerID, orderRestaurant, fullNameOfBuyer, deliveryAddress, orderItems);
    // add event
    newOrder.addDomainEvent(new DeliveryAddressAddedEvent(LocalDate.now(),
                                                          id.toString(),
                                                          buyerID,
                                                          deliveryAddress));
    return newOrder;
}
```

The method (inherited from `AggregateRoot`) that our `Order` object calls, reprinted in the handout:

```java
protected void addDomainEvent(Event event){
    listOfEvents.add(event);
}
```

> **Note (verbatim quirks):**
> 1. The reprinted `addDomainEvent` body writes `listOfEvents.add(event)`, but the actual `AggregateRoot` class (p.11) declares the backing field as `domainEvents` and its `addDomainEvent` uses `domainEvents.add(event)`. The `listOfEvents` name in this reprint is an inconsistency (there is no `listOfEvents` field on `AggregateRoot`).
> 2. The read-path factory is lowercase-`o` `orderOf(...)`; the event-raising factory is uppercase-`O` `OrderOfWithEvent(...)` — the capitalisation differs from Java method-naming convention, reproduced as printed.

**Key design point:** two factory methods deliberately split the two paths — **`orderOf` (read from repository, no event)** vs **`OrderOfWithEvent` (new command, raises `DeliveryAddressAddedEvent`)** — so that reconstituting an order from persistence never re-fires events.

---

**DeliveryAddressAddedEvent (domain/events) — p.14**

This event needs everything our subscriber (Buyer) needs to complete its purpose. "We need to think about what our event needs to contain."

```java
public record DeliveryAddressAddedEvent(
        LocalDate occurredOn, // date of the event
        String orderId,
        String buyerId,
        Address deliveryAddress
) implements Event {
    // add validation
}
```

> **Note (verbatim quirk):** the event is written `implements Event` even though `LocalEvent` was just introduced (p.12) as the marker for local events. A stricter reading would have it `implements LocalEvent`; reproduced as printed. Also note the event carries an ordinary `Address` (not the new `DeliveryAddress` value object introduced later) — the mapping from `Address` to `DeliveryAddress` happens in the listener's mapper.

---

**OrderApplicationService (application) — p.15**

Already discussed before, but now with changes (highlighted in the handout):

```java
@Service
@Slf4j
@AllArgsConstructor
public class OrderApplicationService {
    private final OrderRepository orderRepository;
    private final OrderRestaurantRepository restaurantRepository;
    private DomainEventManager domainEventManager;

    @Transactional // Database save and event dispatching occur in same transaction
    public void addNewOrder(AddNewOrderCommand command){
        // Retrieve the local restaurant info
        OrderRestaurant restaurant = restaurantRepository.findById(command.restaurantId())
                .map(OrderRestaurantJpaToDomainMapper::map)
                .orElseThrow(() -> new
                        OrderRestaurantNotFoundException(command.restaurantId()));
        // We could validate that the menu items in the command are available at that restaurant (if we synced menu items as well)
        Identity<Order> newOrderId = Identity.generateId();
        // Pass info to aggregate to validate (including other fields from command required by aggregate)
        // note the method used to create this is not the constructor
        Order newOrder = Order.OrderOfWithEvent(newOrderId,
                command.consumerId(),
                // convert from separate fields to entity
                restaurant,
                command.fullName(),
                command.deliveryAddress(),
                // Retrieve each order line from the command object and map to domain
                OrderDTOToDomain.map(command.orderItems(), newOrderId)
        );
        orderRepository.save(OrderDomainToJpaMapper.map(newOrder));
        // Notify any subscribers
        if (newOrder.domainEventsExist()) {
            domainEventManager.manageDomainEvents(this.getClass().getSimpleName(),
                    newOrder.listOfDomainEvents());
            newOrder.clearDomainEvents();
        }
    }
}
```

**What changed vs Lecture 6's `OrderApplicationService.addNewOrder`:**
- A new dependency `DomainEventManager domainEventManager` is injected.
- The order is now built with `Order.OrderOfWithEvent(...)` (raises the event) rather than the plain factory.
- After `orderRepository.save(...)`, the service checks `newOrder.domainEventsExist()`, and if so **hands the events to `domainEventManager.manageDomainEvents(...)`** (passing the source-context name via `this.getClass().getSimpleName()`) and then **clears** the events from the aggregate.
- `@Transactional` note (verbatim): *"Database save and event dispatching occur in same transaction"* — the DB write and the event dispatch are in the **same** transaction (the Store-and-Forward pattern).

> **Note (verbatim quirk):** the two repository fields are `private final`, but `domainEventManager` is declared **`private DomainEventManager domainEventManager;` (no `final`)**, reproduced as printed.

---

**DomainEventManager (common) — p.16**

Dependencies listed in the handout as bullets:
- `ApplicationEventPublisher`
- `EventStoreService`

```java
@Service
@Slf4j
@AllArgsConstructor
public class DomainEventManager {
    private final ApplicationEventPublisher eventPublisher;
    private final EventStoreService eventStoreService;

    @Transactional // Storing events matches the caller's transactional state
    public void manageDomainEvents(String sourceContext, List<Event> events) {
        Objects.requireNonNull(sourceContext, "Context cannot be null");
        Objects.requireNonNull(events, "Events cannot be null");
        for (Event event : events){
            log.info("{}->{}", sourceContext, event);
            // Save to our local events DB
            eventStoreService.append(event);
            // Publish in Spring
            eventPublisher.publishEvent(event);
        }
    }
}
```

For each event it (1) logs `sourceContext -> event`, (2) **appends it to the local event store** via `EventStoreService`, and (3) **publishes it in Spring** via Spring's `ApplicationEventPublisher` (this is what the `@TransactionalEventListener` subscribers pick up). `@Transactional` note (verbatim): *"Storing events matches the caller's transactional state."*

---

**EventStoreService (common) — p.17**

Dependency: `EventStoreRepository`.

```java
@Service
@Slf4j
@AllArgsConstructor
public class EventStoreService {
    private final EventStoreRepository eventsStore;

    public void append(Event event){
        EventStoreJpa newEventJpa = new EventStoreJpa();
        newEventJpa.setId(null);
        newEventJpa.setEventType(event.getClass().getName());
        newEventJpa.setOccurredOn(LocalDate.now());
        newEventJpa.setEventBody(event.toString());
        eventsStore.save(newEventJpa);
        log.info("Added to event store: {}", newEventJpa);
    }
}
```

> **Note (verbatim quirk):** `setEventType(event.getClass().getName())` stores the **fully-qualified** class name (e.g. `com.example.demo.…DeliveryAddressAddedEvent`), but the `event_type` column in `schema.sql` is `VARCHAR(50)` — a fully-qualified name can exceed 50 characters. Reproduced as printed.

**EventStoreJpa (common) — p.17**

```java
@Entity(name="event_store")
@Table(name="event_store")
@ToString
@Getter
@Setter
public class EventStoreJpa{
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name="occurred_on")
    private LocalDate occurredOn;
    @Column(name="event_body")
    private String eventBody;
    @Column(name="event_type")
    private String eventType;
}
```

**EventStoreRepository (common) — p.17**

```java
@Repository
public interface EventStoreRepository extends CrudRepository<EventStoreJpa, Long> {
}
```

---

**Schema.sql — p.18**

Add the following to the **end** of the file. Two tables are added:
- `event_store` – local serialised store of all events – **optional** but implemented here to demonstrate how it might be done.
- `event_publication` – **required** for the raising of local events by Spring (used to be handled implicitly but not any more – see the comments below).

```sql
-- Common table for local events (could be one per context)
CREATE TABLE event_store(
    id int AUTO_INCREMENT PRIMARY KEY,
    occurred_on DATE NOT NULL,
    event_body VARCHAR(65000) NOT NULL,
    event_type VARCHAR(50) NOT NULL
);
-- Spring modulith event publication registry causes an issue that requires 1 of 2 solutions
-- This solution is a manual creation of the event_publication table normally implicitly created for local events
CREATE TABLE IF NOT EXISTS event_publication (
    id UUID NOT NULL PRIMARY KEY,
    listener_id VARCHAR(512) NOT NULL,
    event_type VARCHAR(512) NOT NULL,
    serialized_event VARCHAR(4000) NOT NULL,
    publication_date TIMESTAMP WITH TIME ZONE NOT NULL,
    completion_date TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) DEFAULT 'PUBLISHED' NOT NULL,
    completion_attempts INT DEFAULT 0 NOT NULL,
    last_resubmission_date TIMESTAMP WITH TIME ZONE
);
```

> **Delta vs Lecture 6 schema:** these two tables are **appended** to the existing `schema.sql` (buyer / payment_method / order_from_user / order_item / order_restaurant / restaurant / menu_item from Lecture 6) — they do not replace it.
>
> **Note (verbatim quirks):** (1) In the source PDF the `status` default is rendered with **curly quotes** — `DEFAULT ‘PUBLISHED’` — a Word smart-quote artefact; straight quotes (`'PUBLISHED'`) are shown above as they would need to be to run. (2) The `event_publication` table is Spring Modulith's **event publication registry**. The comment states the registry *"causes an issue that requires 1 of 2 solutions"*, and that this handout uses solution #1 — **manually creating** the `event_publication` table that "used to be handled implicitly but not any more". (The second solution is alluded to but not shown.)

---

**Considering the subscriber/listener of our event — p.19**

**DeliveryAddressAddedListener**

```java
@Component // infrastructure to the service – not a service so not labelled as such
@Slf4j
@AllArgsConstructor
public class DeliveryAddressAddedListener {
    private BuyerApplicationService buyerApplicationService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) // Execute AFTER transaction that called this is committed
    public void handle(DeliveryAddressAddedEvent event){ // Listen for DeliveryAddressAddedEvent
        log.info("Delivery Address Added event received for buyer ID: {}", event.buyerId());
        DeliveryAddress receivedDeliveryAddress =
                DeliveryAddressAddedDomainEventMapper.map(event);
        buyerApplicationService.updateDeliveryAddresses(event.buyerId(), receivedDeliveryAddress);
    }
}
```

**Key annotations:** `@Component` (deliberately not `@Service` — *"infrastructure to the service – not a service so not labelled as such"*); `@Async` (runs on a separate thread); `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` — the handler runs **after** the transaction that raised the event has committed. On receipt it logs the buyer id, maps the event's `Address` into a `DeliveryAddress` via `DeliveryAddressAddedDomainEventMapper`, and calls `buyerApplicationService.updateDeliveryAddresses(buyerId, deliveryAddress)`.

> **Note (verbatim typo):** the handout's footnote reads *"DeliveryAddress, DeliveryAddressAddedDomainEventMapper **amd** BuyerApplicationService are discussed over the next few pages"* — "amd" for "and", reproduced as printed.

---

**DeliveryAddress (domain) — p.20**

Using a **specific new value object `DeliveryAddress`** rather than recycling `Address`, due to it being an **identifiable value object** (due to its repeating nature it requires an **ORM id** when saving to a separate table).

```java
import static com.example.demo.common.DomainAssertions.argumentNotEmpty;

public record DeliveryAddress(
        Long id, // Surrogate id for ORM + Wrapper for JSON mapping (often hidden by using a class for a IVO)
        String houseNameNumber,
        String street,
        String town,
        String postCode
) implements IdentifiedValueObject {

    public static final String HOUSE_NAME_NUMBER_NOT_EMPTY = "House name/number cannot be empty";
    public static final String STREET_NOT_EMPTY = "Street cannot be empty";
    public static final String TOWN_NOT_EMPTY = "Town cannot be empty";
    public static final String POST_CODE_NOT_NULL = "Post code to copy cannot be null";

    public DeliveryAddress {
        houseNameNumber = argumentNotEmpty(houseNameNumber, HOUSE_NAME_NUMBER_NOT_EMPTY);
        street = argumentNotEmpty(street, STREET_NOT_EMPTY);
        town = argumentNotEmpty(town, TOWN_NOT_EMPTY);
        postCode = argumentNotEmpty(postCode, POST_CODE_NOT_NULL);
        // Add other guard rails here
    }

    // Overloaded constructor (used when mapping new address event data to this class
    public DeliveryAddress(String houseNameNumber, String street, String town, String postCode) {
        this(null, houseNameNumber, street, town, postCode);
    }

    // Cannot use Lombok - but want to omit id
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeliveryAddress that)) return false;
        return Objects.equals(houseNameNumber, that.houseNameNumber) &&
                Objects.equals(street, that.street) &&
                Objects.equals(town, that.town) &&
                Objects.equals(postCode, that.postCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(houseNameNumber, street, town, postCode);
    }
}
```

**Design points:**
- It is a **record** with a **surrogate `Long id`** as its first component — needed for the ORM because the same delivery address can repeat across orders and is stored in its own table. The comment calls this an **IVO** (Identifiable/Identified Value Object) and notes the id is *"often hidden by using a class for a IVO"* (i.e. you'd normally use a class to hide the surrogate id, but a record is used here).
- Implements a **new supertype `IdentifiedValueObject`** — its **first appearance** in the module. (Earlier value objects implemented the plain `ValueObject` interface; identifiable value objects that need a persistence id implement `IdentifiedValueObject`.)
- **Compact constructor** validates all four business fields via `argumentNotEmpty` (from `DomainAssertions`).
- **Overloaded constructor** `(houseNameNumber, street, town, postCode)` delegates to the canonical one with `id = null` — used when mapping **new** address data from an event (no id yet).
- **Custom `equals`/`hashCode`** deliberately **exclude the `id`** (identity is by value, not by surrogate id) — the comment explains Lombok can't be used because we want to omit the id.

> **Note (verbatim quirks):** (1) the constant `POST_CODE_NOT_NULL` carries the message text *"Post code to copy cannot be null"* (the phrase "to copy" is a leftover), reproduced as printed. (2) "a IVO" (should be "an IVO") and the un-closed comment `// Overloaded constructor (used when mapping new address event data to this class` are reproduced as printed.

---

**DeliveryAddressAddedDomainEventMapper (application/mapper) — p.22**

```java
public class DeliveryAddressAddedDomainEventMapper {
    public static DeliveryAddress map(DeliveryAddressAddedEvent event){
        return new DeliveryAddress( // this calls the overloaded constructor in record - that omits the id
                event.deliveryAddress().houseNameNumber(),
                event.deliveryAddress().street(),
                event.deliveryAddress().town(),
                event.deliveryAddress().postCode()
        );
    }
}
```

Maps the event's `Address` into a **new** `DeliveryAddress` using the **overloaded (id-omitting) constructor**, so the new address arrives with `id == null` (which is how the persistence mapper later recognises it as new).

---

**BuyerApplicationService — p.23**

```java
@AllArgsConstructor
@Slf4j
@Service
public class BuyerApplicationService{
    private final BuyerRepository buyerRepository;

    @Transactional
    public void updateDeliveryAddresses(String buyerId,
                                        DeliveryAddress receivedDeliveryAddress){
        // Verify if buyer already exists
        Optional<BuyerJpa> buyerJpa = buyerRepository.findById(buyerId);
        if (buyerJpa.isEmpty()) {
            throw new BuyerNotFoundException(buyerId);
        }
        Buyer buyer = BuyerJpaToDomainMapper.map(buyerJpa.get());
        // This will do the necessary validation checks + business rules
        buyer.addSavedDeliveryAddress(receivedDeliveryAddress);
        BuyerToJpaMapper.map(buyer, buyerJpa.get());
        // Save after converting
        buyerRepository.save(buyerJpa.get());
    }
}
```

This is the **first `BuyerApplicationService`** in the module (foreshadowed on p.7). Flow: find the `BuyerJpa` by id (throw `BuyerNotFoundException` if missing) → map to the `Buyer` **domain aggregate** → call `buyer.addSavedDeliveryAddress(receivedDeliveryAddress)` (**the domain enforces the validation + business rules, including the de-dup via the `Set`**) → map the (possibly-updated) domain back onto the existing `BuyerJpa` via `BuyerToJpaMapper` → save.

> **Design point:** the write goes **through the domain aggregate** (the command-side pattern from Lecture 6), even though it was triggered by an **event** rather than a controller. The event → listener → application service → aggregate → save chain is the whole point of the local-events mechanism.

---

**Buyer (domain) – amendments — p.24**

*Handout note: "Amendments in yellow, new code in green highlights or in red boxes."*

```java
public class Buyer extends AggregateRoot<Buyer> {
    public static final String PAYMENT_METHOD_NOT_NULL = "Payment method cannot be null";
    public static final String PAYMENT_METHOD_ALREADY_EXISTS = "Payment method already exists";
    public static final String NO_MORE_THAN_THREE_PAYMENT_METHODS_ACCEPTED = "No more than 3 payment methods accepted";
    public static final String DELIVERY_ADDRESS_NOT_NULL = "Delivery address cannot be null";

    private FullName fullName;
    private final HashSet<PaymentMethod> paymentMethods;
    private final HashSet<DeliveryAddress> savedDeliveryAddresses;

    public Buyer(Identity<Buyer> id, FullName fullName) {
        super(id);
        this.fullName = new FullName(fullName);
        this.paymentMethods = new HashSet<>();
        this.savedDeliveryAddresses = new HashSet<>();
    }

    public Identity<Buyer> id(){
        return id;
    }

    public FullName fullName(){
        return fullName;
    }

    public void addPaymentDetails(PaymentMethod paymentMethod){
        if (paymentMethod == null) {
            throw new IllegalArgumentException(PAYMENT_METHOD_NOT_NULL);
        }
        if(paymentMethods.contains(paymentMethod)){
            throw new IllegalArgumentException(PAYMENT_METHOD_ALREADY_EXISTS);
        }
        // Business Rule/Guard
        if (paymentMethods.size() == 3){
            throw new IllegalArgumentException(NO_MORE_THAN_THREE_PAYMENT_METHODS_ACCEPTED);
        }
        this.paymentMethods.add(paymentMethod);
    }

    public void addSavedDeliveryAddress(DeliveryAddress deliveryAddress){
        if (deliveryAddress == null) {
            throw new IllegalArgumentException(DELIVERY_ADDRESS_NOT_NULL);
        }
        this.savedDeliveryAddresses.add(deliveryAddress);
    }

    public void updateFullName(FullName fullName) {
        if (fullName == null) {
            throw new IllegalArgumentException(FULL_NAME_CANNOT_BE_NULL);
        }
        this.fullName = fullName;
    }

    public Set<PaymentMethod> retrieveAllPaymentDetails(){
        return Set.copyOf(paymentMethods);
    }

    public Set<DeliveryAddress> retrieveAllSavedDeliveryAddresses(){
        return Set.copyOf(savedDeliveryAddresses);
    }
}
```

**New/changed in `Buyer` vs earlier lectures:**
- Now `extends AggregateRoot<Buyer>` (per the super-type change).
- New constant `DELIVERY_ADDRESS_NOT_NULL = "Delivery address cannot be null"`.
- New field `private final HashSet<DeliveryAddress> savedDeliveryAddresses;` (initialised in the constructor) — a **`HashSet`**, so duplicate delivery addresses are silently ignored (the de-dup requirement).
- New method `addSavedDeliveryAddress(DeliveryAddress)` — null-guards then adds to the set.
- New read method `retrieveAllSavedDeliveryAddresses()` returns an immutable `Set.copyOf(...)` (mirrors `retrieveAllPaymentDetails()`).
- `updateFullName(...)` references `FULL_NAME_CANNOT_BE_NULL` (a constant defined elsewhere on `Buyer`/`FullName`, not redeclared in this excerpt).

> **Business-rule reminder:** payment methods are capped at 3 (`if (paymentMethods.size() == 3) throw …`); there is **no cap** on saved delivery addresses — the `Set` just de-duplicates them.

---

**BuyerJpa — p.26**

This class now includes a **`Set` of `DeliveryAddressJpa`s** to store the delivery addresses for a particular buyer. (Phil notes he *could* use the `DeliveryAddress` domain value object and embed it; and *could* also change `paymentMethods` to a `Set` given there should be no duplicates.)

```java
@Entity(name = "buyer")
@Table(name ="buyer")
@Getter
@Setter
@ToString(exclude = {"paymentMethods", "savedDeliveryAddress"}) // prevent stack overflow loop
public class BuyerJpa {
    @Id
    @Column(name="id")
    private String id;

    @Embedded
    @Valid
    private FullName fullName;

    @OneToMany(mappedBy = "buyer", cascade = {CascadeType.ALL}, orphanRemoval = true)
    private List<PaymentMethodJpa> paymentMethods = new ArrayList<>();

    @OneToMany(mappedBy = "buyer", cascade = {CascadeType.ALL}, orphanRemoval = true)
    private Set<DeliveryAddressJpa> savedDeliveryAddress = new HashSet<>();

    public void addDeliveryAddress(DeliveryAddressJpa newAddress) {
        this.savedDeliveryAddress.add(newAddress);
    }
}
```

New vs earlier `BuyerJpa`: the `@OneToMany` `Set<DeliveryAddressJpa> savedDeliveryAddress` (mappedBy `"buyer"`, `CascadeType.ALL`, `orphanRemoval = true`), the `addDeliveryAddress(...)` helper, and the `@ToString(exclude = {...})` now also excludes `savedDeliveryAddress` to prevent the bidirectional `toString()` stack-overflow loop.

> **Note (verbatim quirk):** the field is singular `savedDeliveryAddress` (holding a `Set`), so the generated getter is `getSavedDeliveryAddress()` — used later by `BuyerJpaToDTOMapper`. Reproduced as printed.

---

**DeliveryAddressJpa — p.27**

Using a **`Set`** instead of a `List` in `BuyerJpa` creates a **'problem'** with hash-based collections and generated ids. The handout explains it (verbatim summary):

When you create a new JPA entity its `@Id` field is **null** (the DB hasn't generated an id yet — that happens on save). If you add that new entity to a `HashSet` **before** saving, Java computes its hash code based on `null` (which hashes to 0); then `.save()` makes Hibernate generate an id, so the entity's id is **no longer null**. If your `.hashCode()` relies on the id, the hash code **changes while the entity is sitting inside the `Set`**. The next `mySet.contains(entity)` / `mySet.remove(entity)` then looks in the **wrong hash bucket** and fails to find it, even though it's there. One solution is shown below (see `equals`/`hashCode`).

Source (embedded hyperlink, p.27): `https://foojay.io/today/equals-and-hashcode-implementation-considerations/`

```java
@Entity(name="delivery_address")
@Table(name="delivery_address")
@Setter
@Getter
@ToString(exclude = "buyer") // prevent stack overflow loop
public class DeliveryAddressJpa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id")
    private Long id;

    @Embedded
    @Valid
    @EqualsAndHashCode.Include
    private Address deliveryAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private BuyerJpa buyer;

    //https://foojay.io/today/equals-and-hashcode-implementation-considerations/
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeliveryAddressJpa)) return false;
        DeliveryAddressJpa other = (DeliveryAddressJpa) o;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        // Constant value so hashcode never mutates post-save
        return getClass().hashCode();
    }
}
```

**The solution:** `hashCode()` returns a **constant** (`getClass().hashCode()`) so the hash code **never mutates** after save (all instances land in the same bucket, and `equals` disambiguates by id). `equals` compares by **id** (`id != null && id.equals(other.getId())`) — two entities are equal only once both have a persisted id.

> **Note (verbatim quirks):** (1) the `@Embedded` field is the **original `Address`** value object (`private Address deliveryAddress;`), **not** the domain `DeliveryAddress` record — so the JPA table embeds `Address`'s columns. (2) `@EqualsAndHashCode.Include` is placed on the `deliveryAddress` field, but there is **no class-level `@EqualsAndHashCode`** (only `@Setter @Getter @ToString`), and `equals`/`hashCode` are hand-written — so the Lombok `.Include` annotation has **no effect** here. Reproduced as printed.

---

**BuyerToJpaMapper — p.29**

The handout first re-prints the relevant lines from `BuyerApplicationService` for reference:

```java
Optional<BuyerJpa> buyerJpa = buyerRepository.findById(buyerId);
if (buyerJpa.isEmpty()) {
    throw new BuyerNotFoundException(buyerId);
}
Buyer buyer = BuyerJpaToDomainMapper.map(buyerJpa.get());
// This will do the necessary validation checks + business rules
buyer.addSavedDeliveryAddress(receivedDeliveryAddress);
BuyerToJpaMapper.map(buyer, buyerJpa.get());
// Save after converting
buyerRepository.save(buyerJpa.get());
```

Then the mapper itself:

```java
public class BuyerToJpaMapper {
    public static void map(Buyer buyer, BuyerJpa jpa) {
        Objects.requireNonNull(buyer, "Buyer entity cannot be null");
        Objects.requireNonNull(jpa, "Jpa entity cannot be null");
        // Ignoring full name
        // Ignoring the payment methods as these won't have changed
        for (DeliveryAddress savedDeliveryAddress : buyer.retrieveAllSavedDeliveryAddresses()){
            if(savedDeliveryAddress.id() == null){// If null this is the new item
                DeliveryAddressJpa newDeliveryAddress = new DeliveryAddressJpa();
                newDeliveryAddress.setId(null);
                newDeliveryAddress.setBuyer(jpa);
                newDeliveryAddress.setDeliveryAddress(new Address(
                        savedDeliveryAddress.houseNameNumber(),
                        savedDeliveryAddress.street(),
                        savedDeliveryAddress.town(),
                        savedDeliveryAddress.postCode()
                ));
                jpa.addDeliveryAddress(newDeliveryAddress);
                return;
            }
        }
    }
}
```

**How the "only save the new one" logic works:** the mapper iterates the domain buyer's saved delivery addresses and finds the one with **`id() == null`** (the newly-mapped address from the event, which came through the overloaded constructor and so has no id). It builds a fresh `DeliveryAddressJpa`, wraps the four fields in an `Address` value object, links it back to the parent `BuyerJpa` (`setBuyer(jpa)`), adds it via `jpa.addDeliveryAddress(...)`, and **`return`s immediately**. Full name and payment methods are deliberately **ignored** (they haven't changed on this path).

> **Note (verbatim quirk):** the `return;` sits **inside the loop**, so the method exits after handling the **first** null-id address. This works because the local-events flow only ever adds **one** new delivery address at a time; it would silently skip additional new addresses if more than one existed. Reproduced as printed.

---

**Testing That It Works — p.30–31**

A new endpoint is added to retrieve the delivery addresses for Buyer 0000.

> **Figure (p.30, top): "Delivery Addresses for buyer 0000" — before.** A Postman `GET http://localhost:8900/buyers/0000/deliveryAddress` returning a JSON array with **one** address:
> ```json
> [
>     {
>         "houseNameNumber": "house1",
>         "street": "street1",
>         "town": "town1",
>         "postCode": "ST1 1NG"
>     }
> ]
> ```

> **Figure (p.30, bottom): "Add new Order" (from last week).** A Postman `POST http://localhost:8900/orders` with a raw JSON body:
> ```json
> {
>     "consumerId": "0000",
>     "fullName":{
>         "firstName":"Bob",
>         "surname":"Patel"
>     },
>     "restaurantId":"2222",
>     "paymentMethodId":"visa",
>     "deliveryAddress":{
>         "houseNameNumber":"10",
>         "street":"High Street",
>         "town":"Stoke",
>         "postCode":"ST1 12G"
>     },
>     "orderItems":[
>         {"productId":"1",
>          "productName": "Chicken Korma",
>          "unitPrice": 3.5,
>          "units":1}
>     ]
> }
> ```

> **Figure (p.31): "Delivery Addresses for buyer 0000" — after.** Re-running the same `GET .../buyers/0000/deliveryAddress` now returns **two** addresses — the original plus the one carried on the new order:
> ```json
> [
>     {
>         "houseNameNumber": "house1",
>         "street": "street1",
>         "town": "town1",
>         "postCode": "ST1 1NG"
>     },
>     {
>         "houseNameNumber": "10",
>         "street": "High Street",
>         "town": "Stoke",
>         "postCode": "ST1 12G"
>     }
> ]
> ```

Verbatim narration: *"We should find that the list of delivery addresses in Buyer 0000 has changed."* and *"Attempting to add the same order does not result in an additional version of the new address being saved."* — the second sentence demonstrates the **`Set` de-duplication**: re-posting the identical order does **not** add a duplicate delivery address.

> **Note:** the POST payload uses `consumerId: "0000"` and `deliveryAddress` `10 High Street, Stoke, ST1 12G`, which is exactly the second entry that appears in the "after" GET — the end-to-end event flow is demonstrated with matching data.

---

**BuyerController (ui) - modify — p.32**

```java
// e.g. http://localhost:8900/buyers/0000/deliveryAddress
@GetMapping("/{buyer_id}/deliveryAddress")
@ResponseStatus(HttpStatus.OK)
public Iterable<AddressDTO> getDeliveryAddressForBuyer(@PathVariable String buyer_id){
    return facade.findDeliveryAddressesForBuyerById(buyer_id);
}
```

New **query** endpoint to read back the buyer's saved delivery addresses (returns `Iterable<AddressDTO>`, HTTP 200). This is the read side that lets us verify the event flow worked.

**ContextFacade - modify — p.32**

```java
public Iterable<AddressDTO> findDeliveryAddressesForBuyerById(String buyerId) {
    return buyerQueryHandler.findSavedDeliveryAddressesForBuyerById(buyerId);
}
```

**BuyerQueryHandler - modify — p.32**

```java
public List<AddressDTO> findSavedDeliveryAddressesForBuyerById(String buyer_id) {
    return buyerRepository.findById(buyer_id)
            .map(BuyerJpaToDTOMapper::convertToAddressDTOs)
            .orElseThrow(() -> new BuyerNotFoundException(buyer_id));
}
```

> **Note (verbatim quirk):** the façade returns `Iterable<AddressDTO>` while the query handler returns `List<AddressDTO>` (a `List` is an `Iterable`, so this compiles). The path parameter name toggles between `buyer_id` (controller/handler) and `buyerId` (façade), reproduced as printed.

**BuyerJpaToDTOMapper — p.33**

Add the following methods to this class:

```java
public static List<AddressDTO> convertToAddressDTOs(BuyerJpa buyer) {
    if (buyer == null || buyer.getSavedDeliveryAddress() == null) {
        return Collections.emptyList();
    }
    return buyer.getSavedDeliveryAddress().stream()
            .map(BuyerJpaToDTOMapper::convertToDeliveryAddressDTO)
            .toList();
}

// can use AddressDTO rather than making a DeliveryAddressDTO here
public static AddressDTO convertToDeliveryAddressDTO(DeliveryAddressJpa deliveryAddress) {
    Objects.requireNonNull(deliveryAddress, "Delivery address JPA entity cannot be null");
    return new AddressDTO(
            deliveryAddress.getDeliveryAddress().houseNameNumber(),
            deliveryAddress.getDeliveryAddress().street(),
            deliveryAddress.getDeliveryAddress().town(),
            deliveryAddress.getDeliveryAddress().postCode()
    );
}
```

`convertToAddressDTOs` null-guards then streams each `DeliveryAddressJpa` through `convertToDeliveryAddressDTO`. The handout explicitly notes *"can use `AddressDTO` rather than making a `DeliveryAddressDTO` here"* — the existing `AddressDTO` is reused for the delivery-address read model rather than introducing a new DTO. `convertToDeliveryAddressDTO` reads the embedded `Address` (`deliveryAddress.getDeliveryAddress()`) and builds an `AddressDTO` from its four fields.

> **Note (verbatim quirk):** in the source PDF, the `Objects.requireNonNull` message is rendered with **curly quotes** — `“Delivery address JPA entity cannot be null”` — a Word smart-quote artefact; straight quotes are shown above as they'd need to be to compile.

---

**LECTURE 7 — SUMMARY OF THE END-TO-END LOCAL EVENT FLOW (add-new-order → save delivery address on buyer)**

1. `POST /orders` → `OrderController` → `ContextFacade` → `OrderApplicationService.addNewOrder(AddNewOrderCommand)`.
2. The service looks up the local restaurant, generates an `Identity<Order>`, and builds the order via **`Order.OrderOfWithEvent(...)`**, which **raises a `DeliveryAddressAddedEvent`** and stores it on the aggregate (inherited from `AggregateRoot`).
3. The order is saved (`orderRepository.save(OrderDomainToJpaMapper.map(newOrder))`).
4. If events exist, the service hands them to **`DomainEventManager.manageDomainEvents(...)`**, which for each event **appends it to `event_store`** (`EventStoreService`) and **publishes it in Spring** (`ApplicationEventPublisher`), then the aggregate's events are cleared. All of this is in **one `@Transactional`** boundary (Store-and-Forward).
5. **`DeliveryAddressAddedListener`** (`@Component`, `@Async`, `@TransactionalEventListener(AFTER_COMMIT)`) picks up the published event **after commit**, maps the event's `Address` → domain `DeliveryAddress` (via `DeliveryAddressAddedDomainEventMapper`, id = null), and calls `BuyerApplicationService.updateDeliveryAddresses(buyerId, deliveryAddress)`.
6. `BuyerApplicationService` loads the `BuyerJpa`, maps to the `Buyer` **aggregate**, calls `buyer.addSavedDeliveryAddress(...)` (**domain enforces rules + the `Set` de-duplicates**), maps back via **`BuyerToJpaMapper`** (which only persists the address whose `id == null`, i.e. the new one), and saves.
7. Verify with `GET /buyers/{id}/deliveryAddress` — the new address now appears; re-posting the same order does **not** add a duplicate.

**Key concepts introduced in Lecture 7:**
- **Domain event** (Evans) vs **application event** vs **infrastructure event**; **local** vs **foreign/integration** events; **command (present tense, may be rejected)** vs **event (past tense, has happened)**.
- **Uni-directional aggregate** pattern: aggregates raise/store events but never dispatch them; the application service extracts and dispatches after actioning the command.
- **`AggregateRoot` interface → abstract class** promotion (to hold `List<Event>`); the `Event` / `LocalEvent` (/ future `RemoteEvent`) marker hierarchy.
- **Store-and-Forward (Simpler Subscriber)** vs Immediate Forwarding vs Remote Subscriber patterns (Vaughn); XA/2PC and eventual consistency previewed for remote events.
- **Spring plumbing:** `ApplicationEventPublisher`, `@TransactionalEventListener(AFTER_COMMIT)`, `@Async`, and Spring Modulith's **`event_publication` registry** (now requires the manual table creation shown).
- **Identifiable Value Object** (`IdentifiedValueObject`, `DeliveryAddress` with surrogate id) and the **`HashSet` + generated-id hash-mutation pitfall** with the constant-`hashCode` fix (foojay source).

---

### LECTURE 8 CONTENT — FULL DETAIL

---

#### Document 11: Enterprise Application Development — Working with Remote Events

**File:** `Enterprise_Application_Development_-_Working_with_remote_events.pdf` (title on every page reads "Enterprise Application Development - Working with Remote Events")
**Author:** Phil James (PDF metadata records the author as "JAMES Phillip"; every page footer reads "Author: Phil James")
**Pages:** 36
**Created:** 24 July 2026 (from PDF metadata — same creation date as the Lecture 7 local-events handout)

This is the **Working with Remote Events** handout — the **remote events** lecture that Lecture 7 (Local Domain Events) repeatedly signposted ("we will see how this works in the **remote events lecture**"; the **Remote Subscriber** pattern; `RemoteEvent`; message brokers; network boundaries; eventual consistency). Where Lecture 7 was scoped strictly to **local** events (raised and consumed **inside one bounded context**, in-memory, via Spring's `ApplicationEventPublisher`), Lecture 8 crosses the **context boundary**: an event raised by the **`Restaurant`** aggregate in the **Restaurant** bounded context is published to a **message broker** (CloudAMQP / RabbitMQ) and consumed by a listener in the **Ordering** context. This is the concrete delivery of the still-outstanding cross-context `NewRestaurantAddedEvent` that Lecture 6 flagged (the event that keeps the Ordering context's `order_restaurant` snapshot in sync with the Restaurant context) — although in this handout the consumer side is deliberately left as a stub that only logs.

The worked example end-to-end: when a new restaurant is added, the `Restaurant` aggregate raises a **`NewRestaurantAddedEvent`** (a `RemoteEvent`); the event is saved to a local `event_store` (status `PENDING`); a **`RemoteOutboxListener`** (fired **after commit**, on a separate async thread, with retry) resolves the event's **exchange + routing key** from `application.yaml` and publishes it to **RabbitMQ** via `RabbitTemplate.convertAndSend`, marking the local record `PUBLISHED`/`FAILED`/`UNROUTABLE`; a **`NewRestaurantAddedListener`** in the Ordering context (a `@RabbitListener` on the `newRestaurant` queue) consumes the JSON message, deserialises it back into the record via a **`CustomMessageConverter`**, and forwards it to a stub `OrderRestaurantApplicationService`.

**Learning Outcomes (all three listed in this handout — identical to Lectures 5, 6 & 7):**
- Critically evaluate development approaches to solutions to enterprise applications.
- Design an enterprise application, critically evaluating alternatives and justifying selections.
- Implement an enterprise application that includes relevant security features.

**Mapping to Knowledge, Skills and Behaviours (verbatim table from the PDF — identical KSB set to Lectures 6 & 7):**

| Key | Knowledge, Skills and Behaviour |
|---|---|
| K21 | How to operate at all stages of the software development life cycle and how each stage is applied in a range of contexts. For example, requirements analysis, design, development, testing, implementation. |
| K22 | Principles of a range of development techniques, for each stage of the software development cycle that produce artefacts and the contexts in which they can be applied. For example, UML, unit testing, programming, debugging, frameworks, architectures. |
| S18 | Use appropriate analysis methods, approaches and techniques in software engineering projects to deliver an outcome that meets requirements. |
| S19 | Implement software engineering projects using appropriate software engineering methods, approaches and techniques. |
| S21 | Determine, refine, adapt and use appropriate software engineering methods, approaches and techniques to evaluate software engineering project outcomes. |

**Objectives (verbatim bullets from p.1):**
- Explain what is meant by a remote event
- Differentiate between a remote event and a local event
- Configure a message broker to handle remote events
- Create listeners to respond to remote events

**Contents (as printed in the PDF, with page numbers):**
- Objectives — 1
- Tidying Up The Code In the Common Folder — 4
- Events — 5
  - Table 1 Commands vs Events — 6
  - Reminder of the Bounded Contexts created So Far — 8
  - CloudAMQP — 10
    - Broker Architecture (Key Terms) — 10
    - Exchange Routing Types — 10
    - Messaging Patterns Can Therefore Be Defined As — 10
  - Create an account — 11
  - Console Window — 13
  - LavinMQ Manager — 14
  - Add a new Exchange (name restaurant) — 14
  - Add a new Queue (newRestaurant) — 15
  - Route Messages from the exchange to the queue — 16
- Application.yaml (modify) — 17
- How to add the Rabbit/Lavin MQ Dependency — 18
- DemoApplication (modify) — 19
  - Restaurant (domain) – modify — 20
  - schema.sql (modify) — 21
  - EventStoreJpa (common/events) - modify — 21
  - Event (common/events) – amend — 22
  - Remote Event (common/events) — 22
  - DeliveryAddressAddedEvent (ordering/domain/events) – amend — 23
  - NewRestaurantAddedEvent (common/events) — 24
  - RestaurantApplicationService (application) - modify — 25
  - RestaurantJpaToDomainMapper (application/mapper) – modify — 26
  - DomainEventManager (common/events) – modify — 27
  - RabbitOutboxRouter (common/events) — 28
  - RemoteOutboxListener (common/events) — 29
  - EventStoreService (common/events) - amend — 31
- Back to RabbitMQ — 32
  - Checking RabbitMQ for Messages Sent from our context — 32
- Modifying the order context to listen/consume our NewRestaurantAddedEvent — 34
  - NewRestaurantAddedListener (application/events) — 34
  - OrderRestaurantApplicationService — 35
  - CustomMessageConverter (common/events) — 36

> **Note on this handout's structure:** as with Lecture 7 (Local Domain Events), there is **no Academic Disclosure block, no "Maths, English and Digital Skills Development" block, and no PlantUML appendices** in this PDF. The Contents list stops at `CustomMessageConverter (common/events) — 36`. The bulk of the middle of the handout (pp.11–16) is a **step-by-step CloudAMQP / LavinMQ Manager GUI walkthrough** made of annotated screenshots (creating an account, an instance, an exchange, a queue, and a binding).

---

**Tidying Up The Code In the Common Folder — p.4**

Verbatim intro: *"Using Spring Modulith we have the challenge that by default only the root folder is visible to other modules – which is a good thing! Given that we have a module called common that holds shared classes, records and interfaces to be used across bounded contexts, we can find that after a short time putting all of the shared code in this single folder becomes disorganised."*

*"To that end from this week we are going to amend that structure to reflect the evolution of the code base as it develops."*

*"To enable us to create sub folders that are visible across other modules (bounded contexts) we add the following to the root folder of common:"*

```java
@org.springframework.modulith.ApplicationModule(
    type = org.springframework.modulith.ApplicationModule.Type.OPEN
)
package com.example.demo.common;
```

Key teaching point: by default a Spring Modulith module exposes **only its root package** to other modules; sub-packages are internal. Marking the `common` module `ApplicationModule.Type.OPEN` makes **all** of `common`'s sub-packages (`domain`, `dto`, `events`, …) visible across the other bounded contexts, so shared code can be organised into sub-folders instead of one flat folder while still being cross-context-visible.

*"The case study code is now arranged as follows (note this includes the new files about to be discussed in this handout). We could arrange it differently, of course:"*

> **Figure (IntelliJ project tree): the re-organised `common` module.** The `common` module is now split into three sub-packages plus a `package-info.java`:
> - **`common/domain`** — `Address` (record), `AggregateRoot` (class — note: promoted to an abstract class back in Lecture 7), `DomainAssertions`, `Entity` (class), `FullName` (record), `IdentifiedValueObject` (interface), `Identity` (record), `Money` (record), `Person` (class), `ValueObject` (interface)
> - **`common/dto`** — `AddressDTO` (record)
> - **`common/events`** — `CustomMessageConverter` (class), `DomainEventManager` (class), `Event` (interface), `EventStoreJpa` (class), `EventStoreRepository` (interface), `EventStoreService` (class), `LocalEvent` (interface), `NewRestaurantAddedEvent` (record), `RabbitOutboxRouter` (class), `RemoteEvent` (interface), `RemoteOutboxListener` (class)
> - **`package-info.java`** (this is where the `@ApplicationModule(type = OPEN)` annotation shown above lives)
>
> All the events plumbing (`Event`, `LocalEvent`, `RemoteEvent`, `DomainEventManager`, `EventStoreJpa`, `EventStoreRepository`, `EventStoreService`, `NewRestaurantAddedEvent`, `RabbitOutboxRouter`, `RemoteOutboxListener`, `CustomMessageConverter`) now lives under `common/events`. `NewRestaurantAddedEvent` is a **remote** cross-context event, so it lives in `common` (visible to both Restaurant and Ordering); `DeliveryAddressAddedEvent` (a **local** Ordering-only event) stays in the **Ordering** context (`ordering/domain/events`), not in `common`.

---

**Events — p.5**

Verbatim: *"When discussing CQRS we also encountered this diagram (note the event subscriber featured here):"*

> **Figure 1 (hand-drawn CQRS read/write diagram — the same figure from the CQRS handouts, here with the event subscriber highlighted).** A client **monitor** (far left) has a two-way arrow to the **Query Processor** (top box) and a one-way arrow into the **Command Processors (Application Services)** (lower box). The Query Processor reads from the **Query Model** store (top-right cylinder). The Command Processors feed the **Command Model** (centre oval), which writes to the **Command Model Store** (bottom-right cylinder). An **Event (all) Subscriber** box sits in the centre: the Command Model raises an arrow **up** into it, and it emits an arrow into the **Query Model** (i.e. events update the read model). An arrow runs from the Query Model back to the Query Processor.
>
> **Figure 1 caption (verbatim):** *"Figure 1: Commands from Clients travel 'one way' to the command"*

The teaching point: this is the CQRS diagram seen previously; the **Event (all) Subscriber** is the piece this handout is about — the mechanism by which a write (command) side raises events that other parts of the system (or other services) subscribe to.

---

**Table 1 Commands vs Events — pp.6–7 (verbatim two-column table)**

Spanning-both-columns intro (verbatim): *"Both are messages/requests of some kind – they are DTOs – and sometimes they can look very similar (in terms of attributes) as a command to create a new Order may be very similar to an event reporting that a new Order has been created (which is required by other aggregates that need this information)."*

| Command | Event |
|---|---|
| Commands are dispatched to a single handler (in the case study via CQRS this is to an application service which is also known as a command handler) but if you didn't think it warranted CQRS then it would be a service layer. | Events are dispatched to multiple listeners. |
| Command is only a request, and thus may be refused | Event is a fact from the past |
| A command should be processed just once by a single receiver (command handler or service). This is because a command is a single action or transaction you want to perform in the application. For example, the same order creation command should not be processed more than once. | Events may be processed multiple times because many systems or microservices might be interested in the event. An event handler may be able to trigger a command as well after receiving notification that an event occurred. Events can be of two types: domain and application (discussed in the local events handout). Initialising an account after a `CustomerCreatedEvent` has occurred is a Domain event. Sending an email notification to the Customer is an Application Event. You might want to create an Event Source (a DB of all events to allow you to replay or recreate a DB from the events of an aggregate). |
| The originator of the command will typically have access to the intended executor of the command. This could be, for example, in the form of a message queue to the executor. Thus a command is intended for a single entity. | Events are typically processed in a background loop that needs to poll the event queues. Any party interested in acting on the event may, usually, register a callback that is called as a result of the event queue processing, so an event may be one to many. |
| Commands are directed towards a specific processor. Thus there is some level of dependence/coupling with the Command initiator and the processor. For example, a UserService upon creating a new user sends a "Send Email" Command to the EmailService. The fact that the UserService knows that it needs the EmailService, that is already coupling. If EmailService changes its API schema or goes down, it directly affects the UserService function. | Events are not directed towards any specific event handler/listener, thus the event publisher becomes loosely coupled. It does not care what service consumes its event. It's even valid to have 0 consumers of an Event. For example, a UserService upon creating a new user publishes a "User Created Event". Potentially an EmailService can consume that event and sends an email to the user. Here the UserService is not aware of the EmailService. They are totally decoupled. If the EmailService goes down, or changes business rules, we only need to edit the EmailService |
| Commands are functions with side-effects. | Events record the outcome of a particular execution of a command. |

---

**Reminder of the Bounded Contexts created So Far — pp.8–9**

Verbatim: *"We have two contexts featured so far in our case study: Ordering and Restaurant."*

*"The Restaurant context contains the following aggregate:"*

```java
public class Restaurant extends AggregateRoot<Restaurant> {
    public final String NAME_CANNOT_BE_EMPTY = "Name cannot be empty";
    private String restaurantName;
    public final String ADDRESS_CANNOT_BE_NULL = "Address cannot be null";
    public final String MENU_ITEM_CANNOT_BE_NULL = "Menu Items cannot be null";

    private final Address address;
    private final List<MenuItem> menuItems = new ArrayList<>();

… remaining code omitted
```

Verbatim: *"The Restaurant aggregate's responsibility is to hold a list of all restaurants that our system will be responsible for, along with a list of menu items for each restaurant."*

*"The Ordering context contains Buyer and Order aggregates"*

*"Buyer contains the following aggregate:"*

```java
public class Buyer extends AggregateRoot<Buyer> {
    public static final String PAYMENT_METHOD_NOT_NULL = "Payment method cannot be null";
    public static final String PAYMENT_METHOD_ALREADY_EXISTS = "Payment method already exists";
    public static final String NO_MORE_THAN_THREE_PAYMENT_METHODS_ACCEPTED = "No more than 3 payment methods accepted";
    public static final String DELIVERY_ADDRESS_NOT_NULL = "Delivery address cannot be null";

    private FullName fullName;
    private final HashSet<PaymentMethod> paymentMethods;
    private final HashSet<DeliveryAddress> savedDeliveryAddresses;

… remaining code omitted
```

*"Order contains the following aggregate:"*

```java
public class Order extends AggregateRoot<Order> {
    public static final String BUYER_ID_CANNOT_BE_EMPTY = "BuyerID cannot be empty";
    public static final String RESTAURANT_CANNOT_BE_NULL = "RestaurantID cannot be null";
    public static final String ORDER_ITEMS_CANNOT_BE_NULL_OR_EMPTY = "Order Items cannot be null or empty";
    public static final String ADDRESS_CANNOT_BE_NULL = "Address cannot be null";
    public static final String ORDER_CANNOT_BE_REJECTED = "Order cannot be rejected";

    private final String buyerID;
    private final OrderRestaurant orderRestaurant;
    private FullName fullNameOfBuyer;
    private Address deliveryAddress;
    private LocalDate orderedOn;
    private OrderStatus orderStatus;
    private String descriptionOfStatus;
    private final List<OrderItem> orderItems;

… remaining code omitted
```

Verbatim closing paragraphs (the motivation for this whole lecture): *"We have been focusing largely on the ordering context – presently our ordering context contains two aggregates: order (with order items) and restaurant (with menu items)."*

*"We also have a restaurant context – containing the restaurant info and the associated menu items. When we add a new restaurant to the restaurant context we need to synchronise that info with the restaurant information found in the order context (using events). As we have already seen in the CQRS handout, the only restaurant information that we are (currently) syncing with the order context is the id of the restaurant, along with its name."*

> Note: the ordering-context "restaurant (with menu items)" wording here refers to the **`order_restaurant`** local snapshot inside the Ordering context (introduced in Lecture 6), distinct from the full **`Restaurant`** aggregate that lives in the **Restaurant** context. Cross-context sync of that snapshot is exactly what `NewRestaurantAddedEvent` does — currently only `restaurantId` + `restaurantName`.

---

**CloudAMQP — p.10**

Verbatim: *"Advanced Message Queueing Protocol is an open standard networking protocol that allows applications, or systems, to communicate by passing asynchronous messages through queues."*

**Broker Architecture (Key Terms) — verbatim bullets:**
- **Producer:** App/aggregate that publishes event messages.
- **Consumer:** Receiving app that pulls/reads, processes, and acknowledges (Ack) messages so the broker can delete them.
- **Exchange:** Router that filters and assigns messages to queues based on exchange type and rules.
- **Queue:** In-memory or disk-backed buffer where messages wait safely for a consumer.

**Exchange Routing Types — verbatim bullets:**
- **Direct Exchange** – route messages to queues based on an exact match between the routing key on the message and the binding key on the queue.
- **Topic Exchange** – route messages based on pattern matching using wildcard symbols in routing keys.
- **Fanout Exchange** – ignores the routing key and duplicates the message to every queue bound to it.
- **Headers Exchange** - ignores the routing key and uses the attributes in the message headers instead.

**Messaging Patterns Can Therefore Be Defined As — verbatim bullets:**
- **Point-to-point** – ONE producer sends the message to ONE queue, and ONE consumer processes it.
- **Publish/Subscribe** – ONE producer sends the message to ONE exchange, which duplicates and routes it to MULTIPLE queues so that MULTIPLE services can consume it simultaneously (think e-commerce notifying a inventory update, billing and shipping services).

Verbatim: *"We will be using CloudAMQP, which runs two message brokers: Lavin and RabbitMQ. CloudAMQP (using RabbitMQ), will be our cloud messaging service to handle remote events that are produced by aggregates in one bounded context that need to be consumed by another."*

Embedded hyperlink (confirmed via PyMuPDF, p.10): *"Learn more about RabbitMQ here:"* → **https://www.rabbitmq.com/getstarted.html**

---

**Create an account — pp.11–12 (annotated screenshots)**

Embedded hyperlink (confirmed via PyMuPDF, p.11): **https://www.cloudamqp.com/** — *"Click Get Started to create an account."*

The screenshot walkthrough (verbatim captions + on-image detail):
- CloudAMQP landing page banner: *"Queue starts here. Managing the largest fleet of LavinMQ and RabbitMQ clusters worldwide"* — top-right **Get Started** button.
- *"You will now see the following screen:"* — an empty **Instances** list ("You don't have any instances yet, do you want to create one?") with a **+ Create New Instance** button.
- *"Click **Create New Instance**"* → **Create new instance** wizard, **Step 1 of 4 — Select a plan and name**: Name = **case study** (highlighted), Plan = **Loyal Lemming (Free)** (highlighted), Tags field (empty). (The wizard steps are Plan → Region → Configure (Dedicated plans only) → Confirm.)
- *"Select **Region**"* → **Step 2 of 4 — Select a region and data center**: Data center = **Amazon Web Services** (selected; other options Azure, DigitalOcean, Google Compute Engine, Scaleway), Region = **EU-West-1 (Ireland)**. Sidebar shows the **Loyal Lemming (LavinMQ)** plan mascot.
- *"Then click **Review**"*
- *"Then click **Create Instance** – I have called it **case study** – but you could be more imaginative."* → the Instances list now shows one row: Name **case study**, Host **seal**, Plan **Loyal Lemming**, Datacenter **Amazon Web Services EU-West-1 (Ireland)**, with **Edit** and **LavinMQ Manager** actions.

---

**Console Window — p.13 (screenshots)**

Verbatim: *"Click on your instance (case study)"* — then *"And you will notice these important pieces of information – we will be adding these to our **application.yaml** file:"*

> **Figure (CloudAMQP instance console — General + AMQP details panels):**
> - **General:** Region = `amazon-web-services::eu-west-1`; Cluster = `seal.lmq.cloudamqp.com` (DNS load balanced); **Hosts** = `seal-01.lmq.cloudamqp.com` (Availability Zone euw1-az1) *(highlighted)*; Created at = `2026-07-21 08:14 UTC+00:00`.
> - **AMQP details:** **User & Vhost** = `kssfwiov` *(highlighted)*; **Password** = `***` (masked, with reveal/copy icons); **Ports** = `5672 (5671 for TLS)`; **URL** = `amqps://kssfwiov:***@seal.lmq.cloudamqp.com/kssfwiov`.

Verbatim: *"**Click on LavinMQ Manager (top left)** – this will now open a new tab."*

> Note on credentials: this handout is a live tutorial, so the real broker connection values are shown in screenshots — host `seal-01.lmq.cloudamqp.com`, user/vhost `kssfwiov`, and (on p.17) the password `pAEROs5Anj2JXW24q2LsIz9HIzA4r58y`. These are Phil's throwaway free-tier instance credentials printed in the teaching material.

---

**LavinMQ Manager — p.14**

Verbatim: *"On the **left-hand menu** under **MESSAGING,** select **Exchanges** then scroll down and click the **Add exchange** button (see below):"*

**Add a new Exchange (name restaurant) — p.14**

Verbatim: *"I am calling the exchange restaurant as it will be used to handle any events raised by the restaurant aggregate."*

*"You can see that the type of exchange is 'Direct'. This is also known as 'point to point', where ONE producer sends the message to ONE queue, and ONE consumer processes it. Here we route messages to queues based on an exact match between the routing key on the message and the binding key on the queue."*

> **Figure (Add exchange form):** Virtual host = `kssfwiov`; **Name** = `restaurant` (highlighted); **Type** = `Direct`; **Durable** = ✔ (ticked); Auto-delete / Internal / Delayed = unticked; Arguments = `{ "key": value }` (placeholder); green **Add exchange** button.
>
> **Figure (Manage exchanges list — 8 of 8):** the built-in default exchanges plus the new one — `amq.default` (direct), `amq.direct` (direct), `amq.fanout` (fanout), `amq.topic` (topic), `amq.headers` (headers), `amq.match` (headers), `mqtt.default` (mqtt), and **`restaurant` (direct)** *(highlighted)*, all under virtual host `kssfwiov`.

**Add a new Queue (newRestaurant) — p.15**

Verbatim: *"On the **left-hand menu** under **MESSAGING** select **Queues** … Enter the name **newRestaurant** then click **Add Queue**. The name reflects what the message contains – namely, information about the new restaurant event, produced by the restaurant aggregate."*

> **Figure (Add queue form):** Virtual host = `kssfwiov`; **Name** = `newRestaurant` (highlighted); **Durable** = ✔; Auto-delete = unticked; **Add queue** button. Manage queues list then shows one row (1–1 of 1): virtual host `kssfwiov`, Name **newRestaurant**, Features **D** (durable), Consumers 0, State ● (green/running).

**Route Messages from the exchange to the queue — p.16**

Verbatim: *"On the left-hand menu click **Exchanges**, then under **Manage exchanges** select **restaurant**. We are going to route messages from the **restaurant** exchange to the **newRestaurant** queue if the routing key is set to **newRestaurantKey**. Enter the following and click Bind."*

> **Figure (Add a binding from this exchange):** **To queue** = `newRestaurant` (highlighted); **Binding key** = `newRestaurantKey` (highlighted); Arguments = `{ "key": value }` (placeholder); green **Bind** button. The resulting **Bindings 1** row: Type `queue`, To `newRestaurant`, Binding key `newRestaurantKey`, Arguments `{}`, with an **Unbind** action.

Verbatim: *"Now that we have set up one exchange (restaurant) and one queue (newRestaurant) – we will store this information in our application.yaml file as well:"*

---

**Application.yaml (modify) — p.17**

Verbatim: *"We will now return to the console test tab in your browser in order to retrieve the **hostname, username** and **password** (if you have closed it just click on the following link: **Instances - CloudAMQP** then select the name of your instance – then **add the following to your application.yaml file**; make sure there are no leading or trailing spaces!!!!)"*

Embedded hyperlink (confirmed via PyMuPDF, p.17): "Instances - CloudAMQP" → **https://customer.cloudamqp.com/instance**

Verbatim: *"Below I have included the AMQP details (above), as well as the bindings for our new event **NewRestaurantAddedEvent** – it is VITAL that you get the YAML indents correct and that you check carefully the path of your event file."*

```yaml
server:
  port: 8900
spring:
  application:
    name: api
  rabbitmq:
    host: seal-01.lmq.cloudamqp.com
    username: kssfwiov
    password: pAEROs5Anj2JXW24q2LsIz9HIzA4r58y
    virtual-host: kssfwiov
    ssl:
      enabled: true
# exchange and queue
rabbitmq:
  outbox:
    bindings:
      "[com.example.demo.common.events.NewRestaurantAddedEvent]":
        exchange: "restaurant"
        routing-key: "newRestaurantKey"
```

Key points about this YAML:
- The **`spring.rabbitmq.*`** block is Spring Boot's standard RabbitMQ connection config (host, username, password, virtual-host, `ssl.enabled: true` — CloudAMQP uses TLS on port 5671).
- The **second, top-level `rabbitmq.outbox.bindings`** block is a **custom** property (not a Spring standard) — it maps a fully-qualified **event class name** to an `{ exchange, routing-key }` pair. This is what `RabbitOutboxRouter` binds to via `@ConfigurationProperties(prefix = "rabbitmq.outbox")`. The map key is the FQCN `com.example.demo.common.events.NewRestaurantAddedEvent` wrapped in `"[ … ]"` (the bracket syntax lets YAML use a key containing dots without treating them as nesting).
- Port remains **8900** (the case-study app port from Lecture 5 onward).

---

**How to add the Rabbit/Lavin MQ Dependency — p.18**

Verbatim: *"Adding this will create a new Spring project within our top level folder that handles events:"*
- Developers Tools -> Lombok
- Web -> Spring Web
- SQL -> Spring Data JPA, H2 Database
- **Messaging -> Spring for Rabbit MQ**
- ➔ Finish

Verbatim: *"If you already have a project **go into pom.xml and click on**:"* — a screenshot shows the `<dependencies>` line with an **"Add Starters…"** affordance (circled). *"Then select the following dependency:"* — a **Messaging** group where **Spring for RabbitMQ** is ticked (and **Spring Integration** is left unticked).

---

**DemoApplication (modify) — p.19**

Verbatim: *"Add the following annotation to your restaurant application:"*

```java
@EnableRabbit
@EnableAsync // Async handling for post-commit tasks (event publishing happens in a background thread)
@SpringBootApplication
public class DemoApplication{
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
```

- `@EnableRabbit` — turns on Spring AMQP's `@RabbitListener`/`@RabbitHandler` infrastructure.
- `@EnableAsync` — enables `@Async` so the `RemoteOutboxListener.handleRemoteEvent` runs on a **background thread** (separate from the request thread), after the transaction commits.

---

**Restaurant (domain) – modify — p.20**

Verbatim: *"As discussed in the local events handout we will follow the same pattern re the use of constructors to include two methods:"*
- a factory method (**RestaurantOf**), used to recreate a Restaurant
- and a factory method used to produce an event (**RestaurantOfWithEvent**).

This mirrors the Lecture 7 `orderOf` (read, no event) vs `OrderOfWithEvent` (write, raises event) split, now applied to `Restaurant`.

```java
private Restaurant(Identity<Restaurant> id,
                String restaurantName,
                Address address,
                List<MenuItem> menuItems) {
    super(id);
    setRestaurantName(restaurantName);
    this.address = address;
    this.menuItems.addAll(menuItems);
}

// Factory method
public static Restaurant RestaurantOf(Identity<Restaurant> id,
                                        String restaurantName,
                                        Address address,
                                        List<MenuItem> menuItems){
    return new Restaurant(id, restaurantName, address, menuItems);
}

// Used for event generation
public static Restaurant RestaurantOfWithEvent(Identity id,
                            String restaurantName,
                            Address address,
                            List<MenuItem> menuItems) {
    Restaurant newRestaurant = new Restaurant(id, restaurantName, address, menuItems);

    newRestaurant.addDomainEvent(new NewRestaurantAddedEvent(LocalDate.now(),
                                                            id.id(),
                                                            restaurantName));
    return newRestaurant;
}
```

- `RestaurantOf` — pure reconstitution, **no** event (used by the JPA→domain mapper).
- `RestaurantOfWithEvent` — builds the aggregate **and** stages a `NewRestaurantAddedEvent` via the inherited `addDomainEvent(...)` (from the `AggregateRoot` abstract class promoted in Lecture 7). The event carries `LocalDate.now()`, the new restaurant's id (`id.id()`), and the name.
- **Verbatim quirk:** `RestaurantOfWithEvent` takes a **raw** `Identity id` (no `<Restaurant>` generic) while `RestaurantOf` and the private constructor take `Identity<Restaurant>`. Both factory names also break Java convention with a leading capital (same as `OrderOfWithEvent` in Lecture 7).

---

**schema.sql (modify) — p.21**

Verbatim: *"With remote events we are going to expand the amount of data we store about each event."*
- **status** – when we save our event to our local store we are going to save it with a status of **PENDING**, then either change it to **PUBLISHED, FAILED** or **UNROUTABLE** (see EventStoreService later on in this handout – where the enum for this is now defined).
- **retry_count** – how many goes we have to resend the event to our message broker before we decide that is enough.

```sql
CREATE TABLE event_store(
    id int AUTO_INCREMENT PRIMARY KEY,
    occurred_on DATE NOT NULL,
    event_body VARCHAR(65000) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    retry_count INT DEFAULT 0 NOT NULL
);
```

> This **supersedes/extends** the Lecture 7 `event_store` definition, which had only `id, occurred_on, event_body, event_type`. Two columns are added: `status VARCHAR(20)` and `retry_count INT DEFAULT 0`. (The Lecture 7 Spring Modulith `event_publication` registry table is **not** re-shown in this handout — only the extended `event_store` is.)

**EventStoreJpa (common/events) - modify — p.21**

```java
@Entity(name="event_store")
@Table(name="event_store")
@ToString
@Getter
@Setter
public class EventStoreJpa{
    @Id
    @Column(name="id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="occurred_on")
    private LocalDate occurredOn;

    @Column(name="event_body")
    private String eventBody;

    @Column(name="event_type")
    private String eventType;

    @Column(name="status")
    private String status = "PENDING";

    @Column(name="retry_count")
    private int retryCount = 0;
}
```

- New fields vs Lecture 7: `status` (defaulting to `"PENDING"` in Java as well as `NOT NULL` in SQL) and `retryCount` (defaulting to `0`).

---

**Event (common/events) – amend — p.22**

Verbatim: *"Event withId – factory method (implemented in our record) so as to return a version of the record event with an id associated. When we first produce/raise an event there is no id (as that is what the surrogate id provided by the ORM when we save it). As the record is immutable we cannot call a setter method, instead we create a new version here using the id returned by the ORM, using this method."*

*"Long id – Once we have a version with an id, we are going to need to retrieve that id for other purposes (namely, to retrieve a specific event from the repository and update it)."*

```java
public interface Event {
    Long Id();
    Event withId(Long id);
}
```

- The `withId(Long id)` "wither" pattern lets an **immutable record** event get an id after the ORM assigns one on save: `DomainEventManager` saves the event (ORM sets the id), reads that id back, then republishes `event.withId(savedId)` so the downstream listener has the surrogate id to update the record's status.
- **Verbatim quirk (carried through the whole handout):** the interface declares the accessor as **`Long Id();`** (capital `I`), but both event records (`DeliveryAddressAddedEvent`, `NewRestaurantAddedEvent`) `@Override public Long getId()` — a **different** signature — and, being records with a `Long id` component, also expose the record accessor **`id()`** (lowercase). The `RemoteOutboxListener`/`EventStoreService` calls use `event.id()`. So three id-accessor spellings coexist (`Id()`, `getId()`, `id()`); the records do not actually implement the interface's `Id()` method as written.

**Remote Event (common/events) — p.22**

Verbatim: *"We have two interfaces defined to identify whether an event is local or remote – we will use this to let Spring determine if it needs to let our Rabbit broker know about it. Later on we are going to implement an Outbox Listener that intercepts events that are defined as RemoteEvents."*

*"As each event is associated with an exchange and a routing key (to enable it to be sent to a queue), we could, for example, add exchange() and routingKey() methods here to be assigned by each event but, as we have that information in our application.yaml file we can keep this class simple."*

```java
public interface RemoteEvent extends Event {
}
```

- A **marker interface** (like `LocalEvent`) — its only purpose is to let Spring's `@TransactionalEventListener` on `RemoteOutboxListener.handleRemoteEvent(RemoteEvent event)` fire **only** for remote events. Local events (`LocalEvent`) do not match that listener's parameter type, so they are never forwarded to Rabbit.

---

**DeliveryAddressAddedEvent (ordering/domain/events) – amend — p.23**

Verbatim: *"Whilst this is not part of the event code for the new restaurant, our changes to the Event interface have a knock-on effect to this record, as it implements the LocalEvent interface (which in turn implements Event - so we need to assign an id). So, for completeness, I am including the amendments to this code here. We will see how this is used in DomainEventManager."*

```java
public record DeliveryAddressAddedEvent(
    Long id, // Surrogate id for ORM + Wrapper for JSON mapping (often hidden by using a class for a IVO)
    LocalDate occurredOn,
    String orderId,
    String buyerId,
    Address deliveryAddress
) implements LocalEvent {
    // Overloaded constructor - used when event initially raised in aggregate
    public DeliveryAddressAddedEvent(LocalDate occurredOn,
                                        String orderId,
                                        String buyerId,
                                        Address deliveryAddress) {
        this(null, occurredOn, orderId, buyerId, deliveryAddress);
    }

    @Override
    public Long getId() {
        return id;
    }
    @Override
    public DeliveryAddressAddedEvent withId(Long newId) {
        return new DeliveryAddressAddedEvent(newId, this.occurredOn, this.orderId, this.buyerId, this.deliveryAddress);
    }
    // add validation
}
```

- Now `implements LocalEvent` (which extends `Event`), so it must provide `withId`; the overloaded constructor injects `null` for the id when the event is first raised, and `withId` produces a copy carrying the ORM-assigned id.

---

**NewRestaurantAddedEvent (common/events) — p.24 (the star of this lecture)**

Verbatim: *"This record represents the information that we need to share between the Restaurant aggregate, in the Restaurant bounded context, and any other aggregates located elsewhere (remember when this event is raised, and it is published with Rabbit, the producer knows not where, or when, it will be consumed)."*

```java
public record NewRestaurantAddedEvent(
    Long id, // Surrogate id for ORM + Wrapper for JSON mapping
    LocalDate occurredOn,
    String restaurantId,
    String restaurantName
) implements RemoteEvent{

    // Overloaded constructor - used when event initially raised in aggregate
    public NewRestaurantAddedEvent(LocalDate occurredOn, String restaurantId, String restaurantName) {
        this(null, occurredOn, restaurantId, restaurantName);
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public NewRestaurantAddedEvent withId(Long newId) {
        return new NewRestaurantAddedEvent(newId, this.occurredOn, this.restaurantId, this.restaurantName);
    }
    // add validation
}
```

- `implements RemoteEvent` — so publishing it will trip the `RemoteOutboxListener`.
- Carries only the **minimal published language**: `restaurantId` + `restaurantName` (plus the `occurredOn` date and surrogate `id`). This is the deliberately-small cross-context contract flagged since Lecture 6.

Verbatim: *"Remember that in our application.yaml file we have (in part):"*

```yaml
# exchange and queue
rabbitmq:
  outbox:
    bindings:
      "[com.example.demo.common.events.NewRestaurantAddedEvent]":
        exchange: "restaurant"
        routing-key: "newRestaurantKey"
```

Verbatim: *"We will see how this works when we consider the RabbitOutboxRouter and RabbitOutboxListener classes."*

---

**RestaurantApplicationService (application) - modify — p.25**

Verbatim: *"We are going to make use of the aggregate's factory method to generate a new restaurant and raise the appropriate event. Next, we will notify the domain event manage to process any events. This, so far, is the same approach as discussed in the local events handout."*

```java
@Service
@Slf4j
@AllArgsConstructor
public class RestaurantApplicationService {
    private RestaurantRepository restaurantRepository;
    private DomainEventManager domainEventManager;

    @Transactional // must be transactional when publishing event
    public void addNewRestaurant(AddNewRestaurantCommand command) {
        Identity<Restaurant> newRestaurantId = Identity.generateId();

        Restaurant restaurant = Restaurant.RestaurantOfWithEvent(newRestaurantId,
                                    command.restaurantName(),
                                    command.address(),
                                    command.menuItems());

        // Map then save
        restaurantRepository.save(RestaurantDomainToJpaMapper.map(restaurant));

        // Dispatch event within the transaction
        domainEventManager.manageDomainEvents(this.getClass().getSimpleName(),
                                                restaurant.listOfDomainEvents());
    }

… remaining code omitted
```

- Same command→aggregate→save→dispatch shape as Lecture 7's `OrderApplicationService.addNewOrder`: generate id → `RestaurantOfWithEvent` (stages the event) → map to JPA and `save` → `domainEventManager.manageDomainEvents(...)` with the aggregate's staged events, all inside one `@Transactional`.
- Takes an `AddNewRestaurantCommand` (record with `restaurantName()`, `address()`, `menuItems()`).
- **Verbatim quirk:** the save uses `RestaurantDomainToJpaMapper.map(restaurant)` (the domain→JPA mapper), but the mapper shown on the next page is `RestaurantJpaToDomainMapper` (the reverse). The `RestaurantDomainToJpaMapper` is referenced but not printed. Also the aggregate method is `listOfDomainEvents()` here (compare Lecture 7's `listOfDomainEvents()` / `listOfEvents()` naming wobble).

**RestaurantJpaToDomainMapper (application/mapper) – modify — p.26**

Verbatim: *"Following the implementation of the factory method RestaurantOf (and the privatisation of the constructor) we now have the following minor amendment."*

```java
public class RestaurantJpaToDomainMapper {
    public static Restaurant map(RestaurantJpa jpa) {
        Identity<Restaurant> id = new Identity<Restaurant>(jpa.getId());

        Address address = new Address(jpa.getAddress().houseNameNumber(),
            jpa.getAddress().street(),
            jpa.getAddress().town(),
            jpa.getAddress().postCode());

        List<MenuItem> menuItems = new ArrayList<>();
        for (MenuItemJpa m : jpa.getRestaurantMenuItems()){
            menuItems.add(new MenuItem(m.getId(),
                m.getName(),
                new Money(m.getPrice())
            ));
        }

        return Restaurant.RestaurantOf(id,
            jpa.getRestaurantName(),
            address,
            menuItems);
    }
}
```

- The only change: it now calls the **factory** `Restaurant.RestaurantOf(...)` (no event) instead of a public constructor, because the constructor is now private. Reconstituting from persistence must **not** re-fire the `NewRestaurantAddedEvent`.

---

**DomainEventManager (common/events) – modify — p.27**

Verbatim: *"Minor change here as we are going to require the id of the event so we save the returned event JPA and then use the id from this to create a new version using the event.withId factory method discussed earlier in this handout."*

```java
@Service
@Slf4j
@AllArgsConstructor
public class DomainEventManager {
    private final ApplicationEventPublisher eventPublisher;
    private final EventStoreService eventStoreService;

    @Transactional // Storing events matches the caller's transactional state
    public void manageDomainEvents(String sourceContext, List<Event> events) {
        Objects.requireNonNull(sourceContext, "Context cannot be null");
        Objects.requireNonNull(events, "Events cannot be null");

        for (Event event : events){
            log.info("{} -> {}", sourceContext, event);

            // Save to our local events DB and retrieve the DB event id (allows for event status changes via id)
            EventStoreJpa savedEvent = eventStoreService.append(event);

            eventPublisher.publishEvent(event.withId(savedEvent.getId()));
        }
    }
}
```

- The change vs Lecture 7: it now captures the `EventStoreJpa savedEvent` returned by `eventStoreService.append(event)` and publishes **`event.withId(savedEvent.getId())`** — so the in-memory Spring event now carries the DB surrogate id. That id is what lets `RemoteOutboxListener` later update the same `event_store` row's status.
- Still publishes via Spring's `ApplicationEventPublisher` (in-process). For a **remote** event, that in-process publish lands on `RemoteOutboxListener` (which then goes out to Rabbit **after commit**); for a **local** event it lands on the local `@TransactionalEventListener` (e.g. `DeliveryAddressAddedListener`).

---

**RabbitOutboxRouter (common/events) — p.28**

Verbatim: *"This file retrieves the events with their associated exchange and routing keys from application.yaml, and then stores them in a Map called bindings. This is then used by the RemoteOutboxListener class."*
- `@ConfigurationProperties(prefix = "rabbitmq.outbox")` – populate using the application.yaml file.
- **Destination** – a locally defined record that features two properties – exchange and routingKey.
- **bindings** – key, value pair with a String based on the name of the record that represents an event, and a value (Destination that holds the exchange and routingKey for that event).
- *"It is vital that the name of our Map matches the name we use in the application.yaml file."*

```java
@Component
@ConfigurationProperties(prefix = "rabbitmq.outbox")
@Getter
public class RabbitOutboxRouter {
    public record Destination(String exchange, String routingKey) {}

    private final Map<String, Destination> bindings = new HashMap<>(); // populate from yaml

    public Destination resolve(Event event) {
        String className = event.getClass().getName(); // Get the naming of the incoming event
        Destination dest = bindings.get(className); // look up the exchange and destination of that event

        if (dest == null) {
            throw new IllegalArgumentException("No RabbitMQ destination configured for " + className);
        }
        return dest;
    }
}
```

- `bindings` is populated from `rabbitmq.outbox.bindings` in `application.yaml`; the map field name **`bindings`** must match the YAML key. Keys are **fully-qualified class names** (`event.getClass().getName()`), which is why the YAML key uses the FQCN in bracket syntax.
- `resolve(event)` returns the `Destination(exchange, routingKey)` for an event, or throws `IllegalArgumentException` if the event has no configured binding (→ the caller treats it as **UNROUTABLE**).

Verbatim reminder printed again on p.28:

```yaml
# exchange and queue
rabbitmq:
  outbox:
    bindings:
      "[com.example.demo.common.events.NewRestaurantAddedEvent]":
        exchange: "restaurant"
        routing-key: "newRestaurantKey"
```

---

**RemoteOutboxListener (common/events) — pp.29–30 (the outbox publisher)**

Verbatim: *"First, add this to pom.xml"*

```xml
<!-- Source: https://mvnrepository.com/artifact/org.springframework.retry/spring-retry -->
<dependency>
    <groupId>org.springframework.retry</groupId>
    <artifactId>spring-retry</artifactId>
    <version>2.0.13</version>
    <scope>compile</scope>
</dependency>
```

```java
@Component
@Slf4j
@AllArgsConstructor
public class RemoteOutboxListener { // Only invoked when a remote event is published
    private final EventStoreService eventStoreService;
    private final RabbitTemplate rabbitTemplate;
    private final RabbitOutboxRouter rabbitOutboxRouter;

    @Async // separate thread to HTTP request
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) )
    @Retryable(
        includes = { AmqpException.class },
        maxRetries = 2,
        delay = 500,
        multiplier = 2.0
    )
    public void handleRemoteEvent(RemoteEvent event) {
        RabbitOutboxRouter.Destination destination;

        try { // Ensure destination exists - if not, no point trying to send, if present exchange+routerKey
            destination = rabbitOutboxRouter.resolve(event);

        } catch (IllegalArgumentException e) {
            log.error("Unroutable event [{}]. Check RabbitOutboxRouter configuration",
                    event.getClass().getSimpleName(),
                    e);

            eventStoreService.updateStatus(event.id(),
                        EventStoreService.StatusOfMessageDelivery.UNROUTABLE,
                        false);
            return;
        }

        rabbitTemplate.convertAndSend(destination.exchange(),
                                    destination.routingKey(),
                                    event);

        // if successful – mark as PUBLISHED
        eventStoreService.updateStatus(event.id(),
                                        EventStoreService.StatusOfMessageDelivery.PUBLISHED,
                                        false);
    }

    @Recover
    public void recover(AmqpException e,
                        RemoteEvent event) {
        log.error("Failed to publish {} to RabbitMQ after retries. Assigning to Outbox poller",
            event.id(), e);

        eventStoreService.updateStatus(event.id(),
                                    EventStoreService.StatusOfMessageDelivery.FAILED,
                                    true);
    }
}
```

**Margin annotations printed against the `@Retryable(...)` block (verbatim):**
- *"Retries 3 times – initial + 2 retries"*
- *"Initial retry after 500 ms"*
- *"2nd retry after 1s"*
- *"If none are successful, it calls the @Recover annotated method."*

**Margin annotation printed against `rabbitTemplate.convertAndSend(...)` (verbatim):**
- *"Attempt to send the message to Rabbit – this will either be successful – or not, if it is unsuccessful then the Retry is followed."*

Verbatim footnote (bottom of p.30): *"Note: While we don't need the retry_count attribute in our table to determine if we have had sufficient attempts before stopping, it is useful to save this information for other purposes."*

How the outbox publisher works:
- `@TransactionalEventListener(phase = AFTER_COMMIT)` — fires **only after the surrounding transaction commits**, so the `event_store` row (and the restaurant) are already durably saved before we try to hit the broker. If the transaction rolls back, no message is sent.
- `@Async` — runs on a **background thread** so the network round-trip to Rabbit doesn't block the original request thread (hence `@EnableAsync` on `DemoApplication`).
- `@Retryable(includes = { AmqpException.class }, maxRetries = 2, delay = 500, multiplier = 2.0)` — retry on `AmqpException` with exponential backoff (initial 500 ms, then ×2 → 1 s), i.e. up to **3 attempts** total.
- Flow: `resolve` the destination (→ **UNROUTABLE** + `return` if no binding) → `convertAndSend(exchange, routingKey, event)` → on success mark **PUBLISHED**.
- `@Recover recover(AmqpException e, RemoteEvent event)` — called when all retries are exhausted; marks the event **FAILED** and increments `retry_count` (`incrementRetryCount = true`), *"Assigning to Outbox poller"* (i.e. a future scheduled poller would re-attempt FAILED events — the poller itself is not implemented in this handout).
- **Verbatim quirks:** (1) `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) )` has a **stray extra closing parenthesis**. (2) `@Retryable`'s attribute names as written (`includes`, `maxRetries`, `delay`, `multiplier`) are **not** the real Spring Retry `@Retryable` API (which uses `retryFor`/`include`, `maxAttempts`, and a nested `@Backoff(delay=…, multiplier=…)`) — the code as printed would not compile against `spring-retry` 2.0.13. (3) `eventStoreService.updateStatus(event.id(), …)` uses the record accessor `id()` (not `getId()`/`Id()`).

---

**EventStoreService (common/events) - amend — p.31**

```java
@Service
@Slf4j
@AllArgsConstructor
public class EventStoreService {
    public enum StatusOfMessageDelivery {
        PENDING, PUBLISHED, FAILED, UNROUTABLE
    }

    private final EventStoreRepository eventsStore;
    private final ObjectMapper objectMapper;

    // Create event in our event store (status PENDING)
    @Transactional
    public EventStoreJpa append(Event event){ // initial save of status to PENDING
        try {
            EventStoreJpa newEventJpa = new EventStoreJpa();
            newEventJpa.setId(null);
            newEventJpa.setEventType(event.getClass().getSimpleName());
            newEventJpa.setOccurredOn(LocalDate.now());

            // Use this as might not be able to deserialise toString + might change toString at some point
            newEventJpa.setEventBody(objectMapper.writeValueAsString(event));
            newEventJpa.setStatus(StatusOfMessageDelivery.PENDING.name());
            newEventJpa.setRetryCount(0);

            return eventsStore.save(newEventJpa);
        } catch (JacksonException je) {
            throw new IllegalArgumentException("Failed to serialise event payload", je);
        }
    }

    @Transactional // called from the RemoteOutboxRouter when a message status changes
    public void updateStatus(Long eventId,
                            StatusOfMessageDelivery statusOfMessageDelivery,
                            boolean incrementRetryCount){

        eventsStore.findById(eventId).ifPresent(event -> {
            event.setStatus(statusOfMessageDelivery.name());

            if (incrementRetryCount) event.setRetryCount(event.getRetryCount() + 1);
            eventsStore.save(event);

            log.error("Event {} marked as {}", eventId, event.getStatus());
        });
    }
}
```

**Margin annotations (verbatim):**
- against the enum: *"Define an enum of the possible statuses of message delivery. Make it public so we can refer to it from outside the class."*

Notes:
- `append(event)` — builds an `EventStoreJpa`, serialises the event to JSON via Jackson `ObjectMapper.writeValueAsString(event)` (stored in `event_body`), sets `event_type` to the **simple** class name, status **PENDING**, `retry_count` 0, and **returns** the saved JPA (so `DomainEventManager` can read the generated id). Wraps `JacksonException` as `IllegalArgumentException("Failed to serialise event payload")`.
- `updateStatus(eventId, status, incrementRetryCount)` — finds the row by id (via `EventStoreRepository.findById`), sets the new status, optionally bumps `retry_count`, saves, and logs (`log.error` used even for the success path). 
- **Change from Lecture 7:** `event_type` is now populated with `getSimpleName()` (fits the `VARCHAR(50)` column comfortably). Lecture 7's version was flagged for storing a longer/qualified name into the same column.
- **Verbatim quirk:** the `updateStatus` comment says it is *"called from the RemoteOutboxRouter when a message status changes"*, but it is actually called from `RemoteOutboxListener` (the `Router` only resolves destinations).

---

**Back to RabbitMQ — p.32**

Embedded link text again: "Instances - CloudAMQP" (→ https://customer.cloudamqp.com/instance).

Verbatim: *"When our program is running – we can see any current connections in our RabbitMQManager console – when you stop you app this the connection below will disappear."*

**Checking RabbitMQ for Messages Sent from our context — pp.32–33**

Verbatim: *"Before we add a listener let us demonstrate that the messaging is working. If we run our restaurant context and post a new restaurant via Postman e.g. this:"*

```java
/**e.g. POST http://localhost:8900/restaurant
  {
     "restaurantName":"Alessi",
     "address": {
        "houseNameNumber": "house3",
        "street": "street3",
        "town": "town3",
        "postCode": "ST2 1NG"
     },
     "menuItems":[
          {"name":"something", "price":2.4},
          {"name":"something2", "price":2.5}
     ]
  }
  **/
```

Verbatim: *"We will find that RabbitMQ is holding our message as we have no consumer/listener set up for it yet. Click on queues and streams in RabbitMQ then select our queue."*

> **Figure (RabbitMQ Management — Queues):** one queue row — Virtual host `pcefeoux`, Name **`newRestaurant`**, Consumers `0`, State ● (running), **Ready `4`**, Unacked `0`, **Total `4`**, Publish rate `0`. (Four messages are sitting Ready because they've been published but nothing is consuming yet.)
>
> **Figure (Bindings 2):** `(Default exchange binding)`, and **`restaurant`** → Routing key `newRestaurantKey` → Arguments `{}`.
>
> **Figure (Get messages panel):** Mode `Reject and Requeue`, Encoding `Auto string/base64`, Messages `1`, **Get message(s)** button.

Verbatim: *"Compare the Properties and Payload above with:"* — and the record is reprinted:

```java
public record NewRestaurantAddedEvent(
    Long id,
    LocalDate occurredOn,
    String restaurantId,
    String restaurantName
) implements RemoteEvent{
```

> **Figure (Message 1 — the actual message pulled off the queue):** *"The server reported 0 messages remaining."*
> - **Exchange:** `restaurant`
> - **Routing key:** `newRestaurantKey`
> - **Redelivered:** `false`
> - **Properties:** `{"content_type":"application/json","content_encoding":"utf-8","headers":{"__TypeId__":"com.example.demo.restaurant.domain.events.NewRestaurantAddedEvent"},"delivery_mode":2,"priority":0}`
> - **Payload (114 bytes, Encoding: string):** `{"id":1,"occurredOn":"2026-07-21","restaurantId":"019f8458-d2cb-7fec-9698-c5d155c887f3","restaurantName":"Alessi"}`

Observations on the captured message:
- `content_type` is `application/json`, `delivery_mode: 2` (persistent). Jackson stamps a **`__TypeId__`** header so the consumer knows which class to deserialise into.
- The payload maps 1:1 to the record: `id: 1` (the ORM surrogate assigned via `withId`), `occurredOn: "2026-07-21"`, `restaurantId` a **UUIDv7-style** id (`019f8458-…`, from `Identity.generateId()`), `restaurantName: "Alessi"`.
- **Verbatim quirk (important):** the message's `__TypeId__` header reads `com.example.demo.restaurant.domain.events.NewRestaurantAddedEvent`, but this handout's class (per the folder tree, the section heading, and the YAML binding key) lives at `com.example.demo.common.events.NewRestaurantAddedEvent`. The screenshot was evidently captured from an earlier build (event still in the restaurant context's `domain/events`) before the class was moved into `common/events`. This exact package-mismatch on `__TypeId__` is the kind of thing that then requires the `CustomMessageConverter` trusted-package `"*"` workaround (below).
- **Verbatim quirk (minor):** the queue screenshot's virtual host is `pcefeoux`, whereas the AMQP-details/YAML screenshots use `kssfwiov` — the screenshots were taken across different free-tier instances/sessions.

---

**Modifying the order context to listen/consume our NewRestaurantAddedEvent — p.34**

Verbatim: *"As NewRestaurantAddedEvent is in common/events our ordering context has visibility of this. Our application.yaml file has all the configuration to connect to RabbitMQ."*

**NewRestaurantAddedListener (application/events) — p.34**

Verbatim: *"Define a listener so that we can respond to specific events at our application layer – here we are forwarding the event DTO to the restaurant application service in the order context."*
- We can see that this RabbitListener is subscribed to the **newRestaurant** queue.
- As NewRestaurantAddedEvent is in the **common/events** folder any code in our Ordering context has sight of it.
- As per the local events handout the listeners role is to retrieve the event and to forward it on to the relevant Application Service.

```java
@Component
@AllArgsConstructor
@Slf4j
@RabbitListener(queues = "newRestaurant")
public class NewRestaurantAddedListener {
    private final OrderRestaurantApplicationService orderRestaurantApplicationService;

    @RabbitHandler
    public void receiver(NewRestaurantAddedEvent event) {
        try{
            log.info("NewRestaurantAddedListener received NewRestaurantCreatedEvent \n{}", event);
            orderRestaurantApplicationService.addNewRestaurant(event);
        }
        catch (Exception e){
            log.error(e.getMessage());
        }
    }
}
```

- `@RabbitListener(queues = "newRestaurant")` at class level + `@RabbitHandler` on `receiver` — Spring AMQP consumes from the **`newRestaurant`** queue and dispatches the deserialised `NewRestaurantAddedEvent` to `receiver`. The listener just forwards to the application service (same "listener forwards to application service" role as the local `DeliveryAddressAddedListener` in Lecture 7).
- **Verbatim quirk:** the log line says *"received **NewRestaurantCreatedEvent**"* though the type is `NewRestaurantAddedEvent`.

**OrderRestaurantApplicationService — p.35**

Verbatim: *"This code is simply to enable our listener to pass the event to an application service. In reality we would save our new restaurant to the repository."*

```java
@Service
@Slf4j
@AllArgsConstructor
public class OrderRestaurantApplicationService {
    private final OrderRestaurantRepository orderRestaurantRepository;

    public void addNewRestaurant(NewRestaurantAddedEvent event) {
        log.info("Save the restaurant to the repository");
    }
}
```

- A deliberate **stub**: it injects `OrderRestaurantRepository` but only logs; the handout says *"In reality we would save our new restaurant to the repository."* This is where the Ordering context's `order_restaurant` snapshot (from Lecture 6) would actually be populated/updated from the event.

Verbatim: *"Of course, some events will require us to retrieve a specific aggregate and then pass the event information to that aggregate."*

**CustomMessageConverter (common/events) — p.36**

Verbatim: *"When we retrieve an event from Rabbit there is a need to map the event from JSON to our event record. This class is going to be used across the different contexts so it is appropriate to save it to the commons package however, we will encounter an issue related to what classes/records we can map."*

*"The error will be:"*

> `The class 'com.example.demo.common.events.NewRestaurantAddedEvent' is not in the trusted packages`

Verbatim: *"We can avoid this through the use of "*" – which means to trust everything in the project (we can be more targeted)."*

```java
@Configuration
public class CustomMessageConverter {
    @Bean
    public MessageConverter jsonMessageConverter(JsonMapper jsonMapper) {
        return new JacksonJsonMessageConverter(
            jsonMapper,
            "*" // Avoid events not being in a trusted package causing an IllegalArgumentException
        );
    }
}
```

- Spring AMQP's Jackson converter refuses to deserialise a class whose package isn't on its **trusted packages** allow-list (a **security** guard against deserialising arbitrary types from an untrusted broker — ties to the module's "relevant security features" learning outcome). Passing **`"*"`** trusts everything; the handout notes *"we can be more targeted"* (i.e. list specific packages in production). This is the final piece that lets the Ordering context turn the inbound JSON back into a `NewRestaurantAddedEvent` record.
- Uses the newer `JacksonJsonMessageConverter` + `JsonMapper` (Spring's current Jackson 3 API) rather than the older `Jackson2JsonMessageConverter`/`ObjectMapper`.

---

**Lecture 8 — end-to-end summary of the remote-event round trip:**

1. **Producer side (Restaurant context):** `POST /restaurant` → `RestaurantApplicationService.addNewRestaurant` (in `@Transactional`) → `Restaurant.RestaurantOfWithEvent(...)` stages a `NewRestaurantAddedEvent` → save the restaurant → `DomainEventManager.manageDomainEvents(...)` → `EventStoreService.append` writes an `event_store` row (`PENDING`) and returns its id → Spring `ApplicationEventPublisher.publishEvent(event.withId(id))`.
2. **Outbox publish (after commit, async, retried):** `RemoteOutboxListener.handleRemoteEvent` fires only for `RemoteEvent`, only `AFTER_COMMIT`, on a background thread → `RabbitOutboxRouter.resolve` reads the `{exchange: restaurant, routingKey: newRestaurantKey}` binding from `application.yaml` → `RabbitTemplate.convertAndSend` publishes JSON to the **`restaurant`** exchange → the exchange routes to the **`newRestaurant`** queue (direct exchange, exact routing-key match) → `event_store` row marked `PUBLISHED` (or `FAILED`+retry_count via `@Recover`, or `UNROUTABLE` if no binding).
3. **Consumer side (Ordering context):** `NewRestaurantAddedListener` (`@RabbitListener(queues = "newRestaurant")`) pulls the message → `CustomMessageConverter` (trusted-packages `"*"`) deserialises JSON → record → forwards to the stub `OrderRestaurantApplicationService.addNewRestaurant` (which, in a real system, would upsert the `order_restaurant` snapshot).

This is the **Remote Subscriber** pattern that Lecture 7 previewed: producer and consumer are decoupled across a broker and a network boundary, with **eventual consistency** (the consumer is updated asynchronously, after the producer's transaction commits), an **outbox** (local `event_store` + status machine) for reliability, and **retry/recover** for transient broker failures.

---

## 7. User Preferences & Corrections

- **KSBs tracked silently** — do not mention KSBs during conversation unless Raf explicitly asks
- **KSB scope** — only map KSBs to university work, BT work, and explicitly mentioned projects; never to personal hobbies
- **No condensing or summarising** — every handoff must preserve all detail at full fidelity. Raf explicitly stated this from the very first session: "make sure no detail is condensed or summarised — everything needs to be covered in full detail"
- **Coding preferences** — Raf has a `raf-coding` skill file for general coding standards (always apply when writing code)
- **Schedule/diet** — Raf has a `raf-schedule` skill file for scheduling and nutrition context (apply when relevant)
- **Keanu project** — Raf works on a Spring Boot 4.0.2 REST API project at BT called Keanu (Neo4j, RabbitMQ); `keanu` skill file should be applied for BT backend work
- **COMP60055 Assignment 2** — Raf has an active Management of Change report assignment; `comp60055-a2` skill applies to all A2 work

---

## 8. Blockers & Issues

- **Assessment brief not yet uploaded** — Lecture 3 explicitly references the assessment: "For the assessment — we know what bounded contexts we need (see assessment)". The activity on p.28 cannot be fully actioned until the assessment brief is available.
- **Address tests not written** — this is an outstanding exercise from Lecture 2 (left deliberately blank in PDF). No blockers to doing it — just hasn't been actioned yet.
- **Case study code not uploaded** — Lectures 4, 5, 6, 7 and 8 repeatedly refer to "the case study code" (e.g. "… rest of the class omitted here" throughout Lecture 5, "see code file for the rest of this class" in Lecture 4's `BuyerJpaToDTOMapper`, the list of value objects refactored to records, in Lecture 6 the `… removing the other methods from here` in `ContextFacade` plus the un-shown `Order` aggregate constructor / `OrderStatus` enum / `OrderItemJpa` / `OrderJpaToDTOMapper`, and in Lecture 8 the `… remaining code omitted` on the `Restaurant`/`Buyer`/`Order` aggregate snippets and `RestaurantApplicationService`, plus the referenced-but-unshown `RestaurantDomainToJpaMapper`/`RestaurantRepository`/`OrderRestaurantRepository`/`AddNewRestaurantCommand`). Only the handout PDFs have been provided so far, so the code itself cannot be inspected. This blocks the Lecture 4 record-inspection exercise, the Lecture 5 Order-endpoints review exercise, the Lecture 6 design-choice inspection, the Lecture 7 Postman-testing exercise, and the Lecture 8 CloudAMQP/RabbitMQ demo.
- **CloudAMQP account / external broker not set up** — Lecture 8's demo needs a live CloudAMQP (RabbitMQ) instance and the exchange/queue/binding built in the LavinMQ Manager GUI, plus the real host/username/password/vhost in `application.yaml`. This is external to the chat environment, so the remote-event round trip can't be run here even once the case-study code arrives.
- No other blockers at this stage.

---

## 9. Context & Background

- Raf is a software engineering apprentice at **BT** on the **DTS standard** (Digital and Technology Solutions)
- He is also studying at **Staffordshire University** as part of his apprenticeship
- He is working towards his **End Point Assessment (EPA)** and needs to evidence all KSBs
- This module (**Enterprise Application Development**) covers Spring Boot / Java REST API development and Domain Driven Design — directly relevant to his BT work on the Keanu project (also Spring Boot)
- Raf is building up full lecture context across multiple chat sessions, uploading PDFs lecture by lecture
- The project file in Claude contains all PDFs already; context is accumulated here across sessions

---

## 10. Conversation Highlights

- Raf uploaded three PDFs for Lecture 1 and confirmed these are the first lecture materials for the Enterprise Application Development module
- Raf requested the handoff with the explicit instruction: **"make sure no detail is condensed or summarised — everything needs to be covered in full detail"**
- No questions were asked, no coding was done in the Lecture 1 session — purely context loading
- Raf uploaded two PDFs for Lecture 2 in a subsequent session: DDD Overview (part 1) and Entities & Value Objects (part 2)
- Lecture 2 introduced the full DDD conceptual framework (domain models, ubiquitous language, bounded contexts, layered architecture, declarative vs reactive) and then went deep into the concrete Java implementation (value objects, entities, supertypes, identity, guards, unit testing patterns with full code examples)
- **Key outstanding exercise from Lecture 2**: Address Tests class — explicitly left incomplete in the PDF for students to write themselves
- Raf uploaded the Aggregates PDF (Lecture 3) in this session — the third session — and asked for the handoff to be updated with Lecture 3 content included at full fidelity, with all previous content preserved
- Lecture 3 introduced aggregates, aggregate roots, the Ordering bounded context (Buyer + Order aggregates), full code for all classes, invariants, domain rules, and an activity tied to the assessment
- Raf loaded this handoff and uploaded the Lecture 4 PDF (CQRS preparation handout) in the fourth session, following the same pattern — no message, just the handoff plus the next lecture PDF, with the expectation that it is incorporated at full fidelity and all prior content preserved
- Lecture 4 is a **preparation handout, not the CQRS lecture itself** — it sets up everything needed before CQRS is discussed: records vs classes, DTOs, data mappers, the modulith structure, and the façade/OHS/shared kernel patterns. It explicitly signposts that the service layer "is split up as query handlers and command handlers" and that the `ContextFacade` currently only talks to **query handlers** ("at least for this week")
- Lecture 4 contains **no numbered activity task** for students (unlike Lectures 2 and 3) — only the implicit "take a look at the refactored value objects" exercise on p.10
- Lecture 4 corrects/evolves earlier material: `ValueObject` changes from an **abstract class to an interface**, value objects become **records**, and the earlier Lecture 1 simple API is explicitly called out as having "omitted certain best practices and patterns" (serialising JPA entities directly, controller talking straight to a repository)
- Raf loaded this handoff and uploaded the Lecture 5 PDF (**CQRS: Queries**) in the fifth session. Same pattern as always: the prior handoff plus the next lecture PDF, incorporated at full fidelity with all prior content preserved. In this session both PDFs arrived rendered in-context (text + figures visible directly), so the figure descriptions for Lecture 5 were verified against the actual images rather than reconstructed from raster extraction
- Lecture 5 is the **first "doing" handout** — it builds the whole case-study application from an empty Spring Boot project: module creation (common + ordering), validation dependency, `application.yaml`, `schema.sql`/`data.sql`, custom exceptions, a much-expanded `GlobalExceptionHandler`, and then three fully-traced query flows. It carries an **Academic Disclosure**: the ERD and sequence diagrams were initially AI-generated from Phil's own source code, then amended/corrected
- Lecture 5 explicitly scopes itself to **Queries** and defers **Commands** ("we are choosing to discuss Queries separately from Commands … we do not need to focus on the domain layer … as these are not involved in query retrieval"). The query side never touches the domain/aggregate layer — it goes Repository → JPA entity → mapper → DTO. Commands (which must validate through the domain) are the expected next handout
- Lecture 5 introduces the concrete port (**8900**), the seed buyer **"Ivor Menu" (id 0000)** with two payment methods and two orders, and the endpoint shapes: `GET /buyers`, `GET /buyers/{id}`, `GET /buyers/{id}/paymentDetails`
- Minor inconsistencies noted verbatim in the handoff: the handout labels two different figures "Figure 9"; the payment-details narrative note mentions `FullName` while describing a `PaymentMethodJpa` mapper; the query handler calls `convertToPaymentMethodDTOs` (plural) while the shown mapper method is singular; the modulith reminder adds a `dto` item and writes `AggregateRoot` where Lecture 4 wrote `Aggregate`
- Raf loaded this handoff and uploaded the Lecture 6 PDF (**CQRS: Commands**) in the sixth session, with the brief message "here is the next lecture material too". Same pattern: the prior handoff plus the next lecture PDF, incorporated at full fidelity with all prior content preserved. Both the handoff and the PDF arrived rendered in-context; figure descriptions were additionally verified against rasterised pages, and the single embedded hyperlink (`http://localhost:8900/orders`, p.22) confirmed via PyMuPDF
- Lecture 6 is the **Commands** side that Lecture 5 deferred. Where queries never touch the domain layer, **commands route through the domain model so validation/invariants decide whether the write is valid** (Bertrand Meyer's Command-Query Separation is quoted). It builds only **one** command end-to-end — *Adding a new order* (`POST /orders`) — and states the remaining Ordering-context writes happen **via events** (next handout)
- Lecture 6 adds the **restaurant** module (three modules now: common + ordering + restaurant) and **extends the schema/data** with the Restaurant context: new `order_restaurant`, `restaurant`, `menu_item` tables; `buyer`/`order_from_user` PKs promoted to `VARCHAR(36)`; and the Lecture 5 `create sequence payment_method_sequence_id …` line dropped from `data.sql`. These supersede/extend Lecture 5's schema
- Key DDD point restated: **contexts don't join across tables**, so `order_restaurant` holds a local snapshot of restaurant data inside the Ordering context, kept in sync from the Restaurant context via a `NewRestaurantAddedEvent`. This keeps a future microservice split easy. `OrderRestaurant` is a value **record** (`@Embeddable`, `implements ValueObject`, compact constructor) — explicitly not an aggregate and not an entity
- The command flow (`OrderApplicationService.addNewOrder`) is fully traced: look up the local restaurant → generate an `Identity<Order>` → build & validate the `Order` aggregate (with `OrderRestaurant`, `FullName`, `Address`, and order lines mapped via `OrderDTOToDomain`) → map back to `OrderJpa` via `OrderDomainToJpaMapper` → `save`. The `ContextFacade` now holds a third dependency, `OrderApplicationService`, alongside the two query handlers
- Verbatim quirks flagged in the handoff: `OrderRestaurantNotFoundException`'s constructor parameter is named `buyer_id` (thrown for a missing *restaurant*); both `OrderRestaurant` message constants read "Restaurant name cannot be empty" (the `_ID_` one included); a comment says "using factory method" while the code uses the `new OrderRestaurant(...)` constructor; the *Find Order 1111* JSON key is `buyerID` and `orderStatus` serialises as the enum name `"APPROVED"` despite the int `2` being stored; the Postman payload sets `paymentMethodId` to `"visa"`; and the new-order sequence diagram labels `newOrderRestaurant` as a "(Domain Aggregate)" even though `OrderRestaurant` is a value object
- Raf loaded this handoff and uploaded the Lecture 7 PDF (**Local Domain Events**) in the seventh session, with the brief message "this is the next lecture". Same pattern as always: the prior handoff plus the next lecture PDF, incorporated at full fidelity with all prior content preserved. Both the handoff and the PDF arrived rendered in-context; figures were additionally verified against rasterised pages (the Order→Buyer event diagram p.8, Vaughn's *Aggregates create Events* diagram p.9, and the three Postman captures pp.30–31), and the single embedded hyperlink (`https://foojay.io/today/equals-and-hashcode-implementation-considerations/`, p.27) confirmed via PyMuPDF
- Lecture 7 is the **events handout Lecture 6 deferred to** — and the module's **first events lecture**. It is scoped strictly to **local** domain events (raised and consumed **inside one bounded context**, here Ordering, between the `Order` and `Buyer` aggregates) using the **Store-and-Forward (Simpler Subscriber)** pattern. It repeatedly signposts a future **remote events** lecture (`RemoteEvent`, message brokers, network boundaries, eventual consistency, XA/2PC, published-language/serialised JSON) which it previews but does **not** implement
- The worked feature: adding a new order makes the `Order` aggregate raise a `DeliveryAddressAddedEvent`; a Spring listener passes the address to the `Buyer` aggregate, which keeps a **de-duplicated `Set<DeliveryAddress>`** of every address it has used. Demonstrated end-to-end via Postman: GET one address → POST a new order → GET two addresses; re-posting the identical order adds **no** duplicate (the `Set` de-dup)
- **Supersedes earlier material:** `AggregateRoot` is promoted from a marker **interface** (Lecture 6) to an **abstract class** so it can hold `List<Event>` and expose add/remove/list/clear/exists methods; `Order` and `Buyer` now `extends AggregateRoot<T>`. A **new** value-object supertype `IdentifiedValueObject` appears for the first time (implemented by the new `DeliveryAddress` record, which carries a surrogate `Long id` for the ORM). These supersede/extend the earlier super-type definitions
- Key architecture points: aggregates are **uni-directional** (they store events but never dispatch them — the `ApplicationService` extracts and dispatches after the command); the `Order` factory is split into **`orderOf`** (read path, no event) vs **`OrderOfWithEvent`** (write path, raises event) so reconstituting from persistence never re-fires events; dispatch goes through **`DomainEventManager`** (append to `event_store` via `EventStoreService` **and** publish via Spring's `ApplicationEventPublisher`), all in **one `@Transactional`** boundary; the listener is `@Component` + `@Async` + `@TransactionalEventListener(AFTER_COMMIT)`
- Spring Modulith plumbing note: the **`event_publication`** registry table (previously created implicitly) must now be **created manually** in `schema.sql` — the handout adds it (plus the optional `event_store` table) and says the registry "causes an issue that requires 1 of 2 solutions", using the manual-table solution
- Teaching point flagged in the handout: using a **`HashSet`** of JPA entities with a DB-**generated** id causes a **hash-mutation pitfall** (id is null → hashes to 0 → `.save()` changes the id → the entity is now in the wrong bucket, so `contains`/`remove` fail). Fix (from foojay): make `hashCode()` return a **constant** (`getClass().hashCode()`) and `equals` compare by id once persisted — shown on `DeliveryAddressJpa`
- Raf loaded this handoff and uploaded the Lecture 8 PDF (**Working with Remote Events**) in the eighth session, with the brief message "this is the next lecture". Same pattern as always: the prior handoff plus the next lecture PDF, incorporated at full fidelity with all prior content preserved. The PDF was processed via the usual pipeline (pdftotext layout + raw, pages rasterised for figure verification, PyMuPDF for hyperlinks). Figures verified against rasterised pages: the re-organised `common` folder tree (p.4), the CQRS/Event-subscriber Figure 1 (p.5), the CloudAMQP AMQP-details panel (p.13), and the captured RabbitMQ message payload (p.33). Three embedded hyperlinks confirmed: `https://www.rabbitmq.com/getstarted.html` (p.10), `https://www.cloudamqp.com/` (p.11), `https://customer.cloudamqp.com/instance` (p.17)
- Lecture 8 is the **remote-events handout Lecture 7 deferred to** — the module's first **cross-context / integration** events lecture. Where Lecture 7 kept events **local** (in-process, `ApplicationEventPublisher`, inside one bounded context), Lecture 8 crosses the **context + network boundary** via a **message broker** (CloudAMQP running RabbitMQ). It delivers the previously-outstanding **`NewRestaurantAddedEvent`** that syncs the Ordering context's `order_restaurant` snapshot from the Restaurant context (`restaurantId` + `restaurantName` only). It is the concrete **Remote Subscriber** pattern (broker, outbox, eventual consistency) that Lecture 7 previewed but didn't build
- The remote-event **round trip**: `POST /restaurant` → `RestaurantApplicationService` (`@Transactional`) → `Restaurant.RestaurantOfWithEvent` stages a `NewRestaurantAddedEvent` → save → `DomainEventManager.append` to `event_store` (**PENDING**) + Spring-publish `event.withId(id)` → **after commit**, on an `@Async` thread with `@Retryable`, **`RemoteOutboxListener`** resolves `{exchange: restaurant, routingKey: newRestaurantKey}` from `application.yaml` (via `RabbitOutboxRouter`) and `RabbitTemplate.convertAndSend`s the JSON to RabbitMQ, marking the row **PUBLISHED**/**FAILED**(+retry_count via `@Recover`)/**UNROUTABLE** → the **`newRestaurant`** queue holds it → Ordering-context **`NewRestaurantAddedListener`** (`@RabbitListener`) consumes it, `CustomMessageConverter` (trusted-packages `"*"`) deserialises JSON→record, forwards to the **stub** `OrderRestaurantApplicationService` (logs only)
- **Supersedes/extends earlier material:** the `common` module is re-organised into `domain`/`dto`/`events` sub-packages, made cross-context-visible via `@ApplicationModule(type = OPEN)` on `common`'s `package-info.java`; the `event_store` schema/`EventStoreJpa` gain **`status`** (+ a `StatusOfMessageDelivery` enum: PENDING/PUBLISHED/FAILED/UNROUTABLE) and **`retry_count`**; the `Event` interface gains a **`withId(Long)`** "wither" so immutable record events can receive the ORM surrogate id; a new **`RemoteEvent`** marker interface (sibling of `LocalEvent`) lets `RemoteOutboxListener` fire only for remote events; `EventStoreService.append` now stores the **simple** class name into `event_type` (Lecture 7's version was flagged for a longer name). The `Restaurant` aggregate gains the `RestaurantOf`/`RestaurantOfWithEvent` factory split (mirroring Lecture 7's `orderOf`/`OrderOfWithEvent`)
- **AMQP/broker theory introduced:** producer/consumer/exchange/queue; the four exchange routing types (**direct** — exact routing-key↔binding-key match, used here; topic — wildcard patterns; fanout — ignore key, copy to all bound queues; headers — match on message headers); point-to-point vs publish/subscribe; the **outbox** reliability pattern (local event store + status machine + retry) and **eventual consistency** across the boundary. The `restaurant` exchange is a **Direct** exchange bound to the `newRestaurant` queue on key `newRestaurantKey`
- **Security angle (ties to the module's third learning outcome):** Spring AMQP's Jackson converter refuses to deserialise classes outside its **trusted packages** (guards against deserialising arbitrary types off the broker); the handout uses `"*"` to trust everything and notes "we can be more targeted". TLS to CloudAMQP is on (`ssl.enabled: true`, port 5671)
- Verbatim quirks flagged in the handoff: the `Event` interface declares **`Long Id();`** (capital I) but the records `@Override public Long getId()` **and** expose the record accessor `id()` — three id-accessor spellings coexist and the records don't implement `Id()` as written; `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT) )` has a **stray extra `)`**; the `@Retryable(includes=…, maxRetries=2, delay=500, multiplier=2.0)` attribute names are **not** the real spring-retry `@Retryable` API (which uses `retryFor`/`include`, `maxAttempts`, and a nested `@Backoff`), so it wouldn't compile as printed; `RestaurantOfWithEvent` takes a **raw** `Identity` (no generic) while the others take `Identity<Restaurant>`; `RestaurantApplicationService` calls the unshown `RestaurantDomainToJpaMapper.map(...)` (the reverse of the mapper actually printed, `RestaurantJpaToDomainMapper`); `EventStoreService.updateStatus`'s comment says "called from the RemoteOutbox**Router**" but it's called from the **Listener**; the `NewRestaurantAddedListener` log says "received NewRestaurant**Created**Event" though the type is `NewRestaurantAddedEvent`; the captured message's `__TypeId__` header path (`…restaurant.domain.events.…`) doesn't match the class's stated package (`…common.events.…`); and the queue screenshot's vhost (`pcefeoux`) differs from the AMQP-details/YAML vhost (`kssfwiov`) because screenshots were captured across different free-tier instances

- Raf loaded this handoff and uploaded the Lecture 9 PDF (**Identity and Access Management**) in the ninth session, with the brief message "this is the next lecture". Same pattern as always: the prior handoff plus the next lecture PDF, incorporated at full fidelity with all prior content preserved. The PDF was processed via the usual pipeline (pdftotext layout + raw, pages rasterised for figure verification, PyMuPDF for hyperlinks). Figures verified against rasterised pages: the decoded JWT payload screenshot (p.4), the Firebase project setup screenshots (pp.7–12), the Firebase Authentication sign-in method setup (p.16), the Identity module folder structure tree (p.17), the Postman register-admin request/response (pp.25, 28–29), the Postman login-as-admin request (p.30), the LoginResponse sample output (p.32), the Postman Scripts tab JWT token storage (p.33), the Postman collection-level and per-test Bearer Token setup (p.34). Seventeen embedded hyperlinks confirmed via PyMuPDF (listed in the Section 3 entry above)
- Lecture 9 is the **security handout** that the previous lectures' "third learning outcome" thread anticipated. It delivers the module's final stated learning outcome: *"Implement an enterprise application that includes relevant security features."* It is the module's **first non-DDD context** — the Identity context explicitly has no domain folder and no aggregates. Instead it uses **Firebase** (cloud provider) for account management/authentication and **Spring Security** for RBAC within the app
- The architecture: Firebase handles user accounts (email/password sign-in), stores custom claims (role) on the user record, and issues **Firebase ID tokens** (JWTs signed by Google). The Spring Boot app initialises Firebase Admin SDK via `FirebaseConfig`, validates incoming tokens in two places — `FirebaseTokenFilter` (pre-security-chain filter, verifies via `FirebaseAuth.verifyIdToken`) and `SecurityConfig`'s OAuth2 resource server (`NimbusJwtDecoder` validating issuer+audience against Google's JWKS). `@PreAuthorize` annotations on façade methods control role-based access (ADMIN/MANAGER/USER). Registration creates a Firebase user with custom claims; login hits the Firebase REST Identity Toolkit API and returns an ID token + refresh token
- **Relates to Lecture 8's security angle:** Lecture 8 touched security lightly (trusted-packages `"*"`, TLS). Lecture 9 delivers the full authentication/authorisation implementation, and the Postman scripts/Bearer-Token setup show how the secured endpoints now require a JWT obtained from the `/auth/login` endpoint before accessing Restaurant or Ordering context APIs
- **Key design point — the Identity context as a supporting context:** the handout explicitly says "in many instances a domain driven application utilises an existing identity system within the business that becomes a supporting context". Here it's a greenfield app, so they build their own using Firebase, but the Identity module intentionally does **not** follow DDD patterns (no aggregates, no domain layer). The `AuthController` has no façade — it talks directly to `FirebaseAuthService`
- **Differentiation between CQRS commands and conventional request DTOs:** listed as Lecture 9's fifth objective (p.1) but not explicitly addressed in the handout body. The DTOs in the Identity context (`RegisterRequest`, `LoginRequest`, `LoginResponse`, etc.) are conventional request/response DTOs, not CQRS commands — they don't go through an `ApplicationService` command handler or raise domain events. This implicitly illustrates the contrast, though the handout doesn't call it out
- **`application.yaml` now shows all accumulated config:** server port 8900, `ddl-auto: none`, RabbitMQ connection (host `seal-01.lmq.cloudamqp.com`, user/vhost `kssfwiov`, `ssl.enabled: true`), the outbox exchange/routing-key bindings, and the new `firebase.web-api-key` property. Teaching note on p.13 says "should put this in an environment variable"
- **Spring Security's least-privilege default:** adding the Spring Security starter immediately mandates authentication on all endpoints (returns 401 to unauthenticated requests). The `SecurityConfig.filterChain` then opens up `/auth/**` via `permitAll()` while keeping everything else authenticated — this is the principle of least privilege
- **Postman testing infrastructure:** the handout introduces a full Postman testing setup: a new "Identity" collection with register/login tests; a Scripts tab on the login request that extracts the ID token from the response and stores it as `pm.globals.set("jwt_admin_token", token)` — `pm.globals` (not `pm.collections`) so the token is visible across all Postman collections; collection-level or per-test Bearer Token auth referencing `{{jwt_admin_token}}`
- **`FirebaseTokenFilter` vs `SecurityConfig` JWT validation — dual validation:** the handout has **two** JWT validation paths that appear to overlap: (1) `FirebaseTokenFilter` extends `OncePerRequestFilter`, runs before the security chain, calls `FirebaseAuth.getInstance().verifyIdToken(token)` which does Firebase's own cryptographic verification, and sets the `SecurityContextHolder` with a `UsernamePasswordAuthenticationToken`; (2) `SecurityConfig`'s `.oauth2ResourceServer(…).jwt(…)` uses `NimbusJwtDecoder` with Google's JWKS endpoint and issuer/audience validators. Both run for every request — the filter pre-populates the security context, and the OAuth2 resource server re-validates the JWT. The handout doesn't address this overlap or whether one path is intended to replace the other
- **Role-based access summary across both façades:**
  - Restaurant: `findAllRestaurants`/`findRestaurantById` → ADMIN + MANAGER + USER; `addNewRestaurant`/`updateMenu` → ADMIN + MANAGER
  - Ordering: `findAllBuyers` → ADMIN + MANAGER; `findBuyerById`/`findDeliveryAddressesForBuyerById`/`findPaymentsForBuyerById`/`findOrdersByBuyerId`/`findOrderByOrderId` → ADMIN + MANAGER + USER; `addNewOrder` → ADMIN + MANAGER
  - Identity: `register`/`login` → permitAll (no auth required); `roleCheck` → isAuthenticated (any valid token, role-agnostic)
- Verbatim quirks flagged in the handoff: `@GetMapping("/role-check\`")` contains a **stray backtick** in the path (confirmed in both pdftotext extraction and rasterised p.35 — the rendered text clearly shows `"/role-check\`"`); the service account key file is named `serviceAcccountKey.json` with a **triple 'c'** — the handout acknowledges this typo ("apologies for the spelling of this", p.11); `FirebaseAuthService.registerUser` computes `confirmedRole` via `Role.fromString(role).getAuthority()` but **never uses** `confirmedRole` — the custom claims `Map.of("role", role, …)` store the original `role` string, not the `ROLE_`-prefixed version; the custom claims set `"admin", false` regardless of the actual role being ADMIN; `HttpStatus. BAD_REQUEST` in the `GlobalExceptionHandler` has a **space before `BAD_REQUEST`** (formatting artefact); the `loginUser` method catches `HttpClientErrorException` but `RestClient` throws `HttpClientErrorException` only when using the older `RestTemplate`-style error handling — `RestClient`'s default behaviour is to throw `RestClientResponseException` subtypes; the fifth objective on p.1 ("Differentiate between CQRS commands and conventional requests DTOs") is listed but not explicitly addressed in the body

---

### LECTURE 9 CONTENT — FULL DETAIL

---

#### Document 1: Enterprise Application Development — Identity and Access Management

**PDF Metadata:** 38 pages, Author "Philip James", Creator "Microsoft® Word for Microsoft 365", created 8 August 2026, PDF version 1.7, A4 (595.32 × 841.92 pts), not encrypted, tagged: yes.

**Learning Outcomes (p.1):**
- Critically evaluate development approaches to solutions to enterprise applications
- Design an enterprise application, critically evaluating alternatives and justifying selections
- Implement an enterprise application that includes relevant security features

**KSB Mapping (p.1):**
- K21 — How to operate at all stages of the software development life cycle and how each stage is applied in a range of contexts. For example, requirements analysis, design, development, testing, implementation.
- K22 — Principles of a range of development techniques, for each stage of the software development cycle that produce artefacts and the contexts in which they can be applied. For example, UML, unit testing, programming, debugging, frameworks, architectures.
- S18 — Use appropriate analysis methods, approaches and techniques in software engineering projects to deliver an outcome that meets requirements.
- S19 — Implement software engineering projects using appropriate software engineering methods, approaches and techniques.
- S21 — Determine, refine, adapt and use appropriate software engineering methods, approaches and techniques to evaluate software engineering project outcomes.

**Objectives (p.1):**
- Review identity and access management considerations from previous modules
- Implement a non DDD module to manage the identity creation and retrieval
- Add our project to a cloud project to enable cloud authentication
- Add Spring Security to our application to control user authentication and authorisation
- Differentiate between CQRS commands and conventional requests DTOs

**What is JWT? (reminder) (p.3):**
- JSON Web Token (JWT) is an open standard (RFC 7519) that defines a compact and self-contained way for securely transmitting information between parties as a JSON object
- JWTs can make use of encrypted (JWE) or signed (JWS) tokens:
  - Signed token can verify the integrity of the claims contained within it
  - Encrypted tokens hide those claims from other parties
  - You can sign AND encrypt if you want to
- Claims include 7 standard but not required registered claims, as well as any custom claims about the subject (user)
- Link: JSON Web Token Claims (auth0.com)

**Example of a Signed Token (p.3):**
- The token consists of 3 parts, each separated by a full stop/dot. It follows the JSON Web Signature (JWS) format (Header, Payload, Signature)
- Verbatim example token: `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ0b2tlbiI6eyJlbWFpbCI6ImFkbWluQGVtYWlsLmNvbSIsInJvbGUiOnsiaWQiOjIsIm5hbWUiOiJhZG1pbiJ9fSwiaWF0IjoxNzQ0Mjc5MTI5LCJleHAiOjE3NDQyODk5Mjl9.SFM9FsSFj3uxymc7gcvkv4JrPAtKnbcYnOJxhPU2kqo`
- **Header:** contains metadata — the type (always JWT) and signing algorithm; here HMAC (Hash Based Message Authentication Code) as it is not encrypted
- **Payload:** information (known as claims) about the bearer/client that the application requires to authorise access to various resources/end points of the API (e.g. a role) plus other data, including creation (issued at) and expiry (exp) date/time
- **Signature:** used to verify the integrity and authenticity of the token, created by taking the encoded header, the encoded payload, a secret and the algorithm specified in the header then signing
- As it is a signed token, the data is not in a human readable format — but it is not encrypted — it can be decoded (anyone can decode it) — so avoid passing any sensitive/valuable information in them
- **So why sign and not encrypt?** The JWS proves the authenticity of the token — that the token is genuine and that the data within was not tampered with

**When should you use JSON Web Tokens? (reminder) (p.4):**
- **Authentication:** When a user successfully logs in using their credentials, an ID token is returned. According to the OpenID Connect (OIDC) specs, an ID token is always a JWT.
- **Authorisation:** This is the most common scenario for using JWT. Once the user is logged in, each subsequent request will include the JWT, allowing the user to access routes, services, and resources that are permitted with that token. Single Sign On is a feature that widely uses JWT because of its small overhead and its ability to be easily used across different domains.
- **Information Exchange:** JSON Web Tokens are a good way of securely transmitting information between parties. Because JWTs can be signed, for example, using public/private key pairs, you can be sure the senders are who they say they are. Additionally, as the signature is calculated using the header and the payload, you can also verify that the content hasn't been tampered with.
- Links: https://jwt.io/introduction and JSON Web Tokens (auth0.com)
- Decoded payload viewable at JSON Web Tokens - jwt.io
- **Figure: Decoded payload (p.4)** — jwt.io screenshot showing a decoded JWT with fields: `"role": "USER"`, `"surname": "surname1"`, `"id": "0000"`, `"first_name": "first1"`, `"email": "user@email.com"`, `"username": "user"`, `"sub": "user"`, `"iat": 1755167406`, `"exp": 1755185406`
- **More Information on JWT** box: test JWT info – encode/decode, https://jwt.io/

**How do JSON Web Tokens work? (reminder) (p.5):**
- In authentication, when the user successfully logs in using their credentials, a JSON Web Token will be returned
- Since tokens are credentials, care must be taken to prevent security issues. In general, you should not keep tokens longer than required (so implement and set a short expiry time for them)
- Due to lack of security, you also should not store sensitive session data in browser storage
- Whenever the user wants to access a protected route or resource, the user agent should send the JWT, typically in the Authorization (US spelling) header, using the Bearer schema
- The content of the header should look like: `Authorization: Bearer <token>`
- This can be, in certain cases, a stateless authorisation mechanism. The server's protected routes will check for a valid JWT in the Authorization header, and if it's present, the user will be allowed to access protected resources
- If the JWT contains the necessary data (confirmation that the user is authorised), the need to query the database for certain operations may be reduced (no need to keep checking user credentials for each request), though this may not always be the case
- Link: Securing JWTs: Comprehensive Guide to Generating and Managing Secret Keys using Spring boot | by Davoud Badamchi | Medium

**IETF — Internet Current Best Practice (p.5):**
- **§2.2 Weak Symmetric Keys:** In addition, some applications use a keyed Message Authentication Code (MAC) algorithm, such as "HS256", to sign tokens but supply a weak symmetric key with insufficient entropy (such as a human-memorable password). Such keys are vulnerable to offline brute-force or dictionary attacks once an attacker gets hold of such a token [Langkemper].
- Source: https://datatracker.ietf.org/doc/html/rfc8725#name-do-not-trust-received-claim
- Password considerations: https://www.rfc-editor.org/rfc/rfc7518#section-8.8

**Creating an Identity Context (p.6):**
- Whilst we are creating a module for our identity context it is not expected that this context will be domain driven in design (i.e. no domain folder with aggregates)
- In many instances a domain driven application utilises an existing identity system within the business that becomes a supporting context for our application. In our case we have a greenfield application so we can design our identity management as we see fit
- We will hold information about users and roles within a cloud provider and let it manage account authentication, whilst the application handles role-based access

**Add our project to Google Firebase (pp.7–12):**
- We are going to use a cloud provider to manage user access. The following will be somewhat familiar from the Creating Mobile Applications module
- Go to the Firebase console: https://console.firebase.google.com/
- Step-by-step walkthrough (all with screenshots):
  1. Click "Create a new Firebase project"
  2. Enter Project name: `case-study-example` (auto-generates id `case-study-example-d7050`)
  3. Click Continue (ignore the screen re enable Gemini in Firebase)
  4. Google Analytics for your Firebase project — click Continue (enable Google Analytics recommended)
  5. Configure Google Analytics — choose Default Account for Firebase — click Create project
  6. Wait for the project to be created, then click continue
  7. Select Add app → Select Web App
  8. Register your app (App nickname: `com.example.demo` — refer to the top line of your code in your Spring project for the App nickname)
  9. Click "Continue to the console"
  10. From the Firebase menu select Settings → Service Accounts (shown below), then select Generate new private key — confirm ("Your private key gives access to your project's Firebase services. Keep it confidential and never store it in a public repository.")
  11. This will result in a json file being downloaded (e.g. `case-study-example-d7050-firebase-adminsdk-fbsvc-19e08af700.json`)
  12. Move the json file to the resources folder of your project
  13. Rename the file to: `serviceAcccountKey.json` (note: "apologies for the spelling of this" — triple 'c')
  14. **Figure (p.11):** resources folder tree showing: `static/`, `templates/`, `application.yaml`, `data.sql`, `schema.sql`, `serviceAcccountKey.json`
  15. Next, retrieve the web API key: In Firebase → Select your project → Settings → General → Copy the apiKey value from the "SDK setup and configuration" section (Config tab)
  16. **Figure (p.12):** Firebase SDK setup and configuration panel showing apiKey, authDomain, projectId, storageBucket, messagingSenderId, appId, measurementId
- **Admin SDK configuration snippet shown (p.10, Java selected):**
```java
FileInputStream serviceAccount =
    new FileInputStream("path/to/serviceAccountKey.json");

FirebaseOptions options = new FirebaseOptions.Builder()
    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
    .build();

FirebaseApp.initializeApp(options);
```

**application.yaml (amend) (p.13):**
- Full verbatim config shown:
```yaml
server:
  port: 8900
spring:
  application:
    name: api
  jpa:
    hibernate:
      ddl-auto: none
  rabbitmq:
    host: seal-01.lmq.cloudamqp.com
    username: kssfwiov
    password: pAEROs5Anj2JXW24q2LsIz9HIzA4r58y
    virtual-host: kssfwiov
    ssl:
      enabled: true
# exchange and queue
rabbitmq:
  outbox:
    bindings:
      "[com.example.demo.common.events.NewRestaurantAddedEvent]":
        exchange: "restaurant"
        routing-key: "newRestaurantKey"
firebase: #should put this in an environment variable
  web-api-key: add your key here
```
- Add the highlighted lines (firebase section) — and replace the red part with your api key

**Spring Security (p.14):**
- "Spring Security is a powerful and highly customizable authentication and access-control framework. It is the de-facto standard for securing Spring-based applications."
- "Spring Security is a framework that focuses on providing both authentication and authorization to Java applications. Like all Spring projects, the real power of Spring Security is found in how easily it can be extended to meet custom requirements"
- **Features:**
  - Comprehensive and extensible support for both Authentication and Authorization
  - Protection against attacks like session fixation, clickjacking, cross site request forgery, etc
  - Servlet API integration
  - Optional integration with Spring Web MVC
- Links: Spring Security (spring.io/projects/spring-security), Spring Security Tutorial - GeeksforGeeks

**pom.xml (amend) (p.15):**
- Add Spring Security and OAuth2 Authorization Server via IntelliJ "Add Starters" (under Security category, check Spring Security and OAuth2 Authorization Server)
- **Note:** In adding Spring Security we effectively mandate that (until we tell it otherwise) all the end points that are in the case study now require authentication (least privilege). Anyone trying to access previously available end points will now get a 401 unauthorised response. We will need to decide what end points are available to non-authorised users in our SecurityConfig file (more on that later on).
- Additionally, add the following to pom.xml to enable admin access to Firebase:
```xml
<!-- Source: https://mvnrepository.com/artifact/com.google.firebase/firebase-admin -->
<dependency>
    <groupId>com.google.firebase</groupId>
    <artifactId>firebase-admin</artifactId>
    <version>9.10.0</version>
    <scope>compile</scope>
</dependency>
```
- **Note:** Check the link shown in case there is a more up-to-date version

**Firebase (p.16):**
- Select Security → Authentication → Sign-in method
- Enable email/password
- **Note:** whilst there are better methods than email/password, we just want to illustrate the process here
- **Figure (p.16):** Firebase Authentication panel showing Sign-in providers — Email/Password enabled

**Identity module (p.17):**
- The following image represents the folder structure adopted in the case study for the identity module (you might want to arrange it differently)
- **Verbatim folder structure:**
```
identity/
├── authService/
│   ├── FirebaseAuthService
│   ├── FirebaseConfig
│   └── FirebaseTokenFilter
├── dto/
│   ├── ErrorResponse
│   ├── LoginRequest
│   ├── LoginResponse
│   ├── RegisterRequest
│   └── RegisterResponse
├── security/
│   ├── FirebaseJwtAuthenticationConverter
│   ├── Role
│   └── SecurityConfig
└── AuthController
```

**FirebaseConfig (authService) (pp.18–19):**
- Our application needs to communicate with Firebase, specifically Firebase Auth so we need to define singletons for this. We also need to define something to be responsible for decoding our JWT tokens from Firebase
- `@Configuration` defines a file containing one or more Bean definitions (known as a blueprint). Each `@Bean` is a managed object, so that the application context can locate and inject these as dependencies as needed
- When the application starts, a scan is made to identify any `@Configuration` files and the beans are registered with the IoC as singletons

```java
@Configuration
@Slf4j
public class FirebaseConfig {
    public final String FIREBASE_CREDENTIALS_FILE_MISSING = "Firebase credentials file missing";
    public final String SERVICE_ACCOUNT_DOES_NOT_CONTAIN_VALID_PROJECT_ID = "Make sure your service account JSON contains a valid project_id";

    @Bean
    public FirebaseApp firebaseApp() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }
        final String RESOURCE_FILE = "serviceAcccountKey.json";
        ClassPathResource resource = new ClassPathResource(RESOURCE_FILE);
        if (!resource.exists()) {
            throw new FileNotFoundException(FIREBASE_CREDENTIALS_FILE_MISSING);
        }
        try (InputStream serviceAccount = resource.getInputStream()) { // read the contents
            // parse the credentials into an instance of GoogleCredentials
            GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);
            String projectId = null;
            if (credentials instanceof ServiceAccountCredentials sac) {
                projectId = sac.getProjectId();
            }
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(credentials)
                    .setProjectId(projectId)
                    .build();
            return FirebaseApp.initializeApp(options); // Register this instance as a FirebaseApp bean
        }
    }

    @Bean
    public FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }

    @Bean // Used by the security chain filter in SecurityConfig
    public JwtDecoder jwtDecoder(FirebaseApp firebaseApp) {
        String projectId = firebaseApp.getOptions().getProjectId();
        if (projectId == null || projectId.isBlank()) {
            throw new IllegalStateException(SERVICE_ACCOUNT_DOES_NOT_CONTAIN_VALID_PROJECT_ID);
        }
        String jwkSetUri = "https://www.googleapis.com/service_accounts/v1/jwk/securetoken@system.gserviceaccount.com";
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        String issuerUri = "https://securetoken.google.com/" + projectId;
        // Ensures the token was issued by Google for your specific Firebase project
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
        // Ensures the token was intended specifically for your application's project ID (intended audience)
        OAuth2TokenValidator<Jwt> withAudience = new JwtClaimValidator<List<String>>(
                "aud", audList -> audList != null && audList.contains(projectId)
        );
        // Combine and add to default checks (timestamp + expiration checks)
        OAuth2TokenValidator<Jwt> combinedValidator =
                new DelegatingOAuth2TokenValidator<>(withIssuer, withAudience);
        jwtDecoder.setJwtValidator(combinedValidator);
        return jwtDecoder;
    }
}
```
- Annotation notes from the handout:
  - `firebaseApp()`: This bean returns an instance of FirebaseApp. If the FirebaseApp is already initialised e.g. from integration tests, hot reloads, etc then this instance is returned
  - `firebaseAuth(FirebaseApp)`: Return an instance of FirebaseAuth by using the getInstance factory method. This requires the firebase admin dependency and it also requires the FirebaseApp bean (discussed above)
  - `jwtDecoder`: Retrieve the project id from the Firebase bean as we will need this in a moment. JSON Web Key Set — Download and cache Google's public keys to verify JWT signatures in memory to avoid making network calls for every request. Nimbus fetches Google's public keys, caches them, and automatically handles key rotation using the HTTP Cache-Control headers provided by Google

**Role (security) (p.20):**
- An enum class with not just the enum values but also a helper method (`fromString`) to convert the role from a String to a Role enum

```java
public enum Role {
    USER,
    MANAGER,
    ADMIN;

    public static final String PREFIX = "ROLE_";

    public String getAuthority() {
        return PREFIX + name();
    }

    @JsonCreator
    public static Role fromString(String roleAsString) {
        if (roleAsString == null || roleAsString.isBlank()) {
            throw new IllegalArgumentException("Role cannot be null or empty");
        }
        try {
            return Role.valueOf(roleAsString.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role " + roleAsString);
        }
    }
}
```
- **Why does `getAuthority()` add `ROLE_` before the Role name?** Spring Security differentiates between general authorities and roles. Authorities starting with `ROLE_` identify them as roles. Other authorities such as `READ_PRIVILEGE`, etc refer to what you might call a more fine grained action
- In practice we can apply these to methods like this: `@PreAuthorize("hasRole('ADMIN')")`

**FirebaseJwtAuthenticationConverter (security) (p.21):**
- When a user authenticates with Firebase, custom user claims (we have `"role": "ADMIN"`) are embedded inside the decoded JWT object
- Spring Security needs to convert those JWT claims into Spring Security `GrantedAuthority` objects (e.g. `ROLE_ADMIN`), so that annotations such as `@PreAuthorize("hasRole('ADMIN')")`, or `.requestMatchers().hasRole()` can grant or deny access
- `@Component` are defined to support a service — in this case to be used by `SecurityConfig` (next page)

```java
@Component
public class FirebaseJwtAuthenticationConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {
    @Override // implement convertor from jwt to AbstractAuthenticationToken
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String roleClaim = Objects.requireNonNull(jwt.getClaimAsString("role")); // retrieve a custom claim from token e.g. ADMIN
        Collection<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority(roleClaim));
        return new JwtAuthenticationToken(jwt,
                authorities,
                Objects.requireNonNull(jwt.getSubject()));
    }
}
```
- Authentication token consists of the raw jwt object, the list of authorities retrieved from the role custom claim (in the right format), the firebase user id (subject)

**SecurityConfig (security) (p.22):**
- A further configuration class to define a bean that controls access to our end points. This will make use of the `JwtAuthenticationConverter` defined on the previous page

```java
@Configuration
@EnableWebSecurity /// Activate Spring Security Web Security
@EnableMethodSecurity // Enables @PreAuthorize in façade or controller
@AllArgsConstructor
public class SecurityConfig { // define HTTP security policies
    private final Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http
                .csrf(AbstractHttpConfigurer::disable) // JWTs are stateless
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll() // end points not requiring authentication
                        .anyRequest().authenticated() // non specified end points require authentication
                )
                .oauth2ResourceServer(oauth2 -> oauth2 // intercept request
                        // apply custom convertor
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                )
                .build();
    }
}
```

**FirebaseTokenFilter (authService) (pp.23–24):**

```java
@Component
@Slf4j
public class FirebaseTokenFilter
        extends OncePerRequestFilter { // filter executes once per HTTP request
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        final String AUTH_HEADER = request.getHeader("Authorization"); // remember US spelling
        final String BEARER = "Bearer ";
        if (AUTH_HEADER != null && AUTH_HEADER.startsWith(BEARER)) {
            // Token follows BEARER string
            final String token = AUTH_HEADER.substring(BEARER.length());
            try {
                UsernamePasswordAuthenticationToken authentication =
                        getUsernamePasswordAuthenticationToken(token);
                SecurityContextHolder.getContext() // retrieve or create if needed
                        .setAuthentication(authentication);
            } catch (FirebaseAuthException e) {
                SecurityContextHolder.clearContext(); // always clear context data
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private static UsernamePasswordAuthenticationToken
    getUsernamePasswordAuthenticationToken(String token)
            throws FirebaseAuthException {
        FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);
        final String role = (String) decodedToken.getClaims().get("role");
        log.info("role retrieved from token " + role);
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
        // use of the following constructor sets authenticated = true in UsernamePasswordAuthenticationToken
        return new UsernamePasswordAuthenticationToken(
                decodedToken.getUid(), // principal
                null, // credentials are cleared after authentication for security reasons
                authorities
        );
    }
}
```
- Annotation notes from the handout:
  - Verify the token — if successful then the request is authenticated. Might be unsuccessful due to token is expired, token is invalid/tampered with
  - Store jwtAuthenticationToken into the context — contains principal (firebase uid), authorities and authentication flag (set to true — see notes later on)
  - Pass request and response to next filter in the security chain, or to the rest controller (if there are no more filters)
  - `verifyIdToken`: Performs cryptographic signature validation and checks expiration timestamps (exp). If this is valid it will return a FirebaseToken instance representing the parsed claims
  - Whilst we only have one authority (role) we need to store this (after formatting with `ROLE_`) in a list (as you could have more than one authority per user)

**Registering a new admin account (p.25):**
- **Figure: Registering a new admin account** — Postman screenshot showing `POST http://localhost:8900/auth/register` with raw JSON body: `{"username":"admin1", "email":"admin@email.com", "password":"password123", "role": "ADMIN"}`
- Caption: "Figure: Registering a new admin account (clearly we need something more robust for admin)"
- "We would add further tests to our identity collection in Postman test USER and MANAGER."

**RegisterRequest (dto) (p.25):**
```java
public record RegisterRequest(
        String username,
        String email,
        String password,
        String role
) {
    // add validation
}
```

**AuthController — part 1: register (p.26):**
- Note: I am not using a façade in this module. Additionally, the module should not follow DDD.

```java
@RestController
@RequestMapping("/auth")
@AllArgsConstructor
@Slf4j
public class AuthController {
    public final String USER_CREATED_CONFIRMATION = "User created successfully";
    private final FirebaseAuthService firebaseAuthService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) throws Exception {
        log.info("Registering user {}", request);
        UserRecord userRecord = firebaseAuthService.registerUser(request.username(),
                request.email(),
                request.password(),
                request.role()
        );
        RegisterResponse response = new RegisterResponse(userRecord.getUid(),
                userRecord.getEmail(),
                userRecord.getDisplayName(),
                USER_CREATED_CONFIRMATION
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }
    // … rest of the class is omitted here (but included later and in the zip)
```

**FirebaseAuthService (authService) (p.27):**
- Registering a new user account

```java
@Service
@Slf4j
public class FirebaseAuthService {
    private final FirebaseAuth firebaseAuth;
    private final RestClient restClient;

    @Value("${firebase.web-api-key}")
    private String firebaseApiKey;

    public FirebaseAuthService(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
        this.restClient = RestClient.create();
    }

    public UserRecord registerUser(String username,
                                    String email,
                                    String password,
                                    String role) throws Exception {
        CreateRequest createRequest = new CreateRequest().setEmail(email)
                .setPassword(password)
                .setDisplayName(username)
                .setEmailVerified(false);
        UserRecord userRecord = firebaseAuth.createUser(createRequest);
        // Confirm role passed in request exists
        String confirmedRole = role != null
                ? Role.fromString(role).getAuthority()
                : Role.USER.name();
        // Custom claims
        Map<String, Object> customClaims = Map.of(
                "role", role,
                "admin", false
        );
        firebaseAuth.setCustomUserClaims(userRecord.getUid(), customClaims);
        return userRecord;
    }
    // … rest of the class is omitted here (but included later and in the zip)
```
- `CreateRequest` is not a user defined class (Firebase SDK)
- `UserRecord` is not a user defined class — it is worth looking at the code for this to better understand what it contains

**RegisterResponse (dto) (p.28):**
```java
public record RegisterResponse(
        String uid,
        String email,
        String username,
        String message
) {
    // add validation
}
```
- **Figure (p.28):** Postman response — 201 Created: `{"uid": "ZFQgdKaP30e4aFJ1wHQr3oD7ezG2", "email": "admin@email.com", "username": "admin1", "message": "User created successfully"}`

**ErrorResponse (dto) (p.28):**
- A record of this type is created in the register or login method of AuthController, if the Firebase auth service is unable to action the user request successfully

```java
public record ErrorResponse(
        String error,
        String message
) {
    public ErrorResponse(String error) {
        this(error, null);
    }
}
```
- **Figure (p.28):** Postman response (testing by submitting the same new user details twice) — 400 Bad Request: `{"error": "Bad Request", "message": "The user with the provided email already exists (EMAIL_EXISTS).", "status": 400, "timestamp": "2026-08-06T11:05:09.974780900Z"}`

**Testing the Code (p.29):**
- **Figure (p.29):** Postman request screenshot — Identity → register admin, `POST http://localhost:8900/auth/register`, raw JSON body with same admin credentials
- **Figure (p.29):** Postman response — 201 Created (same response as p.28)

**AuthController — part 2: login (p.30):**
- We will create a new end point in our auth controller to allow a user to log in and confirm their identity

```java
@RestController
@RequestMapping("/auth")
@AllArgsConstructor
@Slf4j
public class AuthController {
    private final FirebaseAuthService firebaseAuthService;

    // … omitting the register method code for simplicity

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        LoginResponse response = firebaseAuthService.loginUser(
                request.emailOrUsername(),
                request.password()
        );
        return ResponseEntity.ok(response);
    }
}
```

**LoginRequest (p.30):**
```java
public record LoginRequest(
        String emailOrUsername,
        String password
) {
    // add validation
}
```
- **Figure (p.30):** Postman screenshot — Identity → login as admin, `POST http://localhost:8900/auth/login`, raw JSON body: `{"emailOrUsername": "admin@email.com", "password":"password123"}`

**FirebaseAuthService — part 2: loginUser (p.31):**

```java
@Service
@Slf4j
public class FirebaseAuthService {
    private final FirebaseAuth firebaseAuth;
    private final RestClient restClient;

    @Value("${firebase.web-api-key}")
    private String firebaseApiKey;

    public FirebaseAuthService(FirebaseAuth firebaseAuth) {
        this.firebaseAuth = firebaseAuth;
        this.restClient = RestClient.create();
    }

    // … omitting the registerUser method code for simplicity

    public LoginResponse loginUser(String email,
                                    String password) {
        if (email == null || password == null) {
            throw new IllegalArgumentException("Email and password must not be null");
        }
        String firebaseLoginUrl =
                "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=" + firebaseApiKey;
        Map<String, Object> requestBody = Map.of(
                "email", email,
                "password", password,
                "returnSecureToken", true
        );
        try {
            return restClient.post()
                    .uri(firebaseLoginUrl)
                    .body(requestBody)
                    .retrieve()
                    .body(LoginResponse.class);
        } catch (HttpClientErrorException e) {
            log.error("Firebase Auth error [{}] {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalArgumentException("Authentication failed: " + e.getResponseBodyAsString());
        }
    }
}
```

**LoginResponse (dto) (p.32):**
- `@JsonProperty` allows us to map the values from the original response (in this case from Firebase) and store in a variable of a different name (if needed)

```java
public record LoginResponse(
        @JsonProperty("localId") String uid,
        String email,
        @JsonProperty("displayName") String username,
        @JsonProperty("idToken") String accessToken,
        String refreshToken,
        @JsonProperty("expiresIn") String expiresInSeconds
) {
    // add validation
}
```
- **Figure (p.32):** Postman response — 200 OK, 336 ms, 1.68 KB, showing `localId`, `email`, `displayName`, `idToken` (long JWT string), `refreshToken`, `expiresIn: "3600"`

**Testing — Before We Go Any Further (pp.33–34):**
- As Spring Security has locked down all of our end points (except those on our `/auth/` path) we need to pass our JWT token to any requests to the Restaurant or Ordering bounded contexts
- In the **Scripts** table for login we will add the following:

```javascript
if (pm.response) {
    try {
        const response = pm.response.json();
        const token = response.idToken || response.token;
        if (token) {
            pm.globals.set("jwt_admin_token", token);
            console.log(token);
        } else {
            console.error("idToken not found");
        }
    } catch (e) {
        console.error("Failed to parse response", e);
    }
} else {
    console.error("No response received");
}
```
- Note that this is `pm.globals` not `pm.collections` — as we want the token to be visible across all collections we are using, using `pm.collections` would make that variable only visible in our identity collection folder
- **Figure (p.33):** Postman screenshot — Identity → login as admin, Scripts tab, "Before request" section with the JavaScript code
- Now we could click on the restaurant collection → Amend the Auth type to Bearer Token and refer to our token
- **Figure (p.34):** Restaurant collection → Authorization tab → Auth type: Bearer Token → Token: `{{jwt_admin_token}}`
- This will, of course, apply the admin token to all tests in the restauration collection — which we might not want
- If we want to do it on a test by test basis then don't do the above but instead select a specific test and click the Authorization tab
- **Figure (p.34):** Restaurant → All restaurants, GET `http://localhost:8900/restaurant`, Authorization tab → Auth type: Bearer Token → Token: `{{jwt_admin_token}}`
- We can implement the same approach for the Ordering module

**Applying Role Based Access to the end points (pp.35–37):**

**AuthController (identity) (p.35):**
- We will add a new end point to our auth controller
- `@PreAuthorize("isAuthenticated()")`: This method level annotation is a security gate that requires a user to be authenticated — it is not concerned with what role the user has, only that they have successfully signed in and have a valid token. The method is not executed if the user has not successfully authenticated themselves
- This security gate requires `@EnableMethodSecurity` to be applied to the `SecurityConfig` class
- It is also important that `JwtAuthenticationFilter` (in our case, `FirebaseJwtAuthenticationConverter`) has decoded the Bearer token before this end point is requested
- A user can have more than one authority at the same time, hence the collection. Here we are simply concatenating these with a comma (presently we only assign one role as an authority to a user)
- `Authentication` — Spring Security injects the authentication object into this method. This object contains the user credentials and the granted authorities (roles)

```java
@PreAuthorize("isAuthenticated()")
@GetMapping("/role-check`")
public ResponseEntity<String> roleCheck(Authentication authentication) {
    String roles = authentication.getAuthorities().stream() // return collection of granted authorities
            .map(GrantedAuthority::getAuthority) // extract the authority as a string e.g. ROLE_ADMIN
            .collect(Collectors.joining(", ")); // concatenate
    return ResponseEntity.ok(roles + " access granted");
}
```

**RestaurantContextFacade (restaurant) (p.36):**
- Note: Whilst in this example I have preauthorised different roles, we could instead assign `@PreAuthorize("isAuthenticated()")` to end points that all user roles apply to
- We could also amend the security code so that a user had multiple authorities (so a person who was an admin, was also granted the roles of manager and user) then if an end point was a user the admin could also access those end points (similar idea for manager although they would be user and manager for their roles)

```java
@Component
@AllArgsConstructor
public class RestaurantContextFacade {
    private RestaurantQueryHandler restaurantQueryHandler;
    private RestaurantApplicationService restaurantApplicationService;

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public Iterable<RestaurantDTO> findAllRestaurants() {
        return restaurantQueryHandler.findAllRestaurants();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public RestaurantDTO findRestaurantById(@PathVariable String restaurant_id) {
        return restaurantQueryHandler.findRestaurantById(restaurant_id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public void addNewRestaurant(AddNewRestaurantCommand command) {
        restaurantApplicationService.addNewRestaurant(command);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public void updateMenu(UpdateMenuCommand command) throws RestaurantNotFoundException {
        restaurantApplicationService.updateMenu(command);
    }
}
```

**ContextFacade (ordering) (p.37):**

```java
@Component
@AllArgsConstructor
public class ContextFacade {
    private final BuyerQueryHandler buyerQueryHandler;
    private final OrderQueryHandler orderQueryHandler;
    private final OrderApplicationService orderApplicationService;

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Iterable<BuyerDTO> findAllBuyers() {
        return buyerQueryHandler.findAllBuyers();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public BuyerDTO findBuyerById(String buyerId) {
        return buyerQueryHandler.findBuyerById(buyerId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public Iterable<AddressDTO> findDeliveryAddressesForBuyerById(String buyerId) {
        return buyerQueryHandler.findSavedDeliveryAddressesForBuyerById(buyerId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public List<PaymentMethodDTO> findPaymentsForBuyerById(String buyerId) {
        return buyerQueryHandler.findPaymentsForBuyerById(buyerId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public Iterable<OrderDTO> findOrdersByBuyerId(String buyerId) {
        return orderQueryHandler.findOrdersByBuyerId(buyerId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER', 'USER')")
    public OrderDTO findOrderByOrderId(String orderId) {
        return orderQueryHandler.findOrderByOrderId(orderId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public void addNewOrder(AddNewOrderCommand addNewOrderCommand) {
        orderApplicationService.addNewOrder(addNewOrderCommand);
    }
}
```

**GlobalExceptionHandler (modify) (p.38):**
- We are going to add a specific FirebaseAuthException type

```java
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(FirebaseAuthException.class)
    public ResponseEntity<Map<String, Object>>
    handleFirebaseAuthException(FirebaseAuthException ex) {
        log.warn("Firebase authentication error: {}", ex.getMessage());
        return createErrorResponse(HttpStatus. BAD_REQUEST, ex.getMessage(), null);
    }
```

**Embedded hyperlinks confirmed via PyMuPDF (17 total):**
- Page 3: `https://auth0.com/docs/secure/tokens/json-web-tokens/json-web-token-claims`
- Page 4: `https://auth0.com/docs/secure/tokens/id-tokens`
- Page 4: `https://openid.net/specs/openid-connect-core-1_0.html#IDToken`
- Page 4: `https://jwt.io/introduction`
- Page 4: `https://auth0.com/docs/secure/tokens/json-web-tokens`
- Page 4: `https://jwt.io/` (×2)
- Page 5: `https://medium.com/@davoud.badamchi/securing-jwts-comprehensive-guide-to-generating-and-managing-secret-keys-using-spring-boot-0f943186f4b0` (×2)
- Page 5: `https://datatracker.ietf.org/doc/html/rfc8725#section-2.2`
- Page 5: `https://datatracker.ietf.org/doc/html/rfc8725#name-weak-symmetric-keys`
- Page 5: `https://www.sjoerdlangkemper.nl/2016/09/28/attacking-jwt-authentication/`
- Page 5: `https://datatracker.ietf.org/doc/html/rfc8725#name-do-not-trust-received-claim`
- Page 5: `https://www.rfc-editor.org/rfc/rfc7518#section-8.8`
- Page 7: `https://console.firebase.google.com/`
- Page 14: `https://spring.io/projects/spring-security`
- Page 14: `https://www.geeksforgeeks.org/advance-java/spring-security-tutorial/`

---

**To continue:** Paste this document at the start of a new chat and say "load this handoff" or "continue from this handoff". Then upload the next lecture's PDF(s) and Claude will have full context. With Lecture 9 (Identity and Access Management) loaded, **all three module learning outcomes now have dedicated implementation content**: (1) critically evaluate development approaches (Lectures 1–4), (2) design an enterprise application (Lectures 3–8: aggregates, CQRS, events), and (3) implement relevant security features (Lecture 9: JWT/Firebase/Spring Security RBAC). Threads still open for possible future handouts: (a) the **Outbox poller** (scheduled re-attempt of FAILED events); (b) the Ordering-context consumer **stub** (`OrderRestaurantApplicationService` only logs); (c) **event sourcing**; (d) the fifth Lecture 9 objective ("Differentiate between CQRS commands and conventional requests DTOs") was listed but not explicitly addressed. Also still awaited: the **case study code** (needed to action the Lecture 4 record-inspection, Lecture 5 Order-endpoints, Lecture 6 design-choice, Lecture 7 Postman-testing, Lecture 8 CloudAMQP/RabbitMQ, and Lecture 9 Firebase/Spring Security exercises) and the **assessment brief** (which Lecture 3 explicitly references and is needed to action the Lecture 3 activity).

---

## 11. Assignment Briefs, Marking Criteria and Lecturer Guidance - Full Detail

**Added:** 2026-08-24

### 11.1 Source documents now loaded

The following four assessment documents have now been loaded in full. They are treated as authoritative additions to the lecture material already recorded above:

1. `COMP60047 Enterprise Application Development - assignment 1 (1).docx` - the formal Assignment 1 report brief and mark scheme.
2. `COMP60047 Guidance Document assignment 1 2025-26.pdf` - the five-page lecturer guidance document for Assignment 1.
3. `COMP60047 Enterprise Application Development - assignment 2 (1).docx` - the formal Assignment 2 presentation/VIVA brief and mark scheme.
4. `COMP60047 Guidance Document assignment 2 2025-26.pdf` - the four-page lecturer guidance document for Assignment 2.

The source documents are reproduced in full in Sections 11.8 to 11.11 so that no formal requirement, scenario detail, KSB statement, grade-band descriptor, lecturer note or repeated point is lost. The structured interpretation in Sections 11.2 to 11.7 is additional context and does not replace the full source transcriptions.

### 11.2 Assessment relationship, weighting, formats and deadlines

- **Module:** Enterprise Application Development.
- **Module code:** COMP60047.
- **School:** School of Digital Technologies, Innovation and Business.
- **Assignment 1 weighting:** 40% of the module.
- **Assignment 1 format:** a report of a minimum of 1,500 words documenting the design, implementation and testing decisions made during development of the solution.
- **Assignment 2 weighting:** 60% of the module.
- **Assignment 2 format:** a presentation of the development and demonstration of the finished solution, showcasing the finished application, answering questions about its construction and evaluating the Knowledge, Skills and Behaviours (KSBs) developed during the module.
- **Submission deadline for Assignment 1:** 21 September 2026 at 09:30 via Blackboard.
- **Submission deadline for Assignment 2 presentation material:** 21 September 2026 at 09:30 via Blackboard.
- **Presentation/VIVA duration:** a maximum of 30 minutes, excluding any adjustments provided through an appropriate Student Inclusion Plan. Presentation slots will be communicated or agreed by the tutor.
- **Practical relationship between assessments:** Assignment 1 records and critically justifies the design, implementation and testing decisions. Assignment 2 demonstrates the resulting application, runs and explains the testing evidence, revisits important report decisions, answers tutor questions and evaluates the specified KSBs. The Assignment 2 guidance explicitly says to "dip into the report" and warns that the tutor may form questions from the report and request clarification or justification.

### 11.3 Shared module learning outcomes

Both assignments assess the same three module learning outcomes:

1. Critically evaluate development approaches to solutions to enterprise applications.
2. Design an enterprise application, critically evaluating alternatives and justifying selections.
3. Implement an enterprise application that includes relevant security features.

These outcomes mean that merely producing functioning code is insufficient. The work must demonstrate critical comparison, justified selection, implementation competence and the correct application of relevant security. The Assignment 1 guidance describes this as a Level 6 module in which design and architecture choices matter as much as the implementation that follows. It stresses that the student must indicate what was considered, what was included or excluded under each criterion and why.

### 11.4 Fundamental authorship, submission and AI-use requirements

#### Assignment 1

- All fundamental requirements must be satisfied; the brief warns that failure could result in zero marks.
- The student must be the sole author of submitted work.
- Any submitted code resulting from generative AI must be clearly identified so that assessors can establish whether the work demonstrates the learner's own knowledge and capabilities.
- Help may not be obtained from any person other than members of the regular teaching team.
- The work must be submitted through the module's Blackboard presence using the links in the Assessment section.
- The AI declaration is **Limited**. Permitted uses are narrowly defined as initial idea development outside assessed work, early-stage learning support, clarification and explanation of difficult terms or concepts, and suggestions of further readings or references.
- AI must not replace the student's work or constitute plagiarism under the University's Academic Conduct Procedure.
- Where AI supports initial ideas, the student must be able to explain the reasoning behind the work.
- Any permitted AI use must be declared.

#### Assignment 2

- All fundamental requirements must be satisfied; the brief warns that failure could result in zero marks.
- Help may not be obtained from any person other than members of the regular teaching team.
- The AI declaration is **Limited**.
- The explicitly accepted use is idea development outside assessed work, with the requirement that the student can explain the reasoning during the presentation.
- Any AI use must be declared.

### 11.5 Assignment 1 mark structure and evidence implications

Assignment 1 has three marking categories:

1. **Design Decisions: 25 marks.** The report must use appropriate images, diagrams and tables where helpful, and should reflect on how previous learning influenced decisions. The 70%+ band requires excellent design and critical discussion, an excellent justification for deviations from the brief, clear and well-justified architecture and responsibilities, appropriate class and sequence diagrams, a very good and justified range of patterns, an accurate and justified ERD and data dictionary, complete endpoint specifications, and well-justified message queues whose subscriber use and message data are clear.
2. **Implementation Decisions: 45 marks.** The 70%+ band requires a very close match between implementation and design, professionally implemented classes conforming to the selected architecture and patterns, high-quality conversion/mapping, transformation, validation and error handling, correct and functioning security, appropriate CRUD functionality across the tables, and well-implemented event monitoring and messaging. Reflection on previous learning remains expected.
3. **Testing Decisions: 30 marks.** The 70%+ band requires comprehensive testing and critical discussion, comprehensive automated unit tests following best practice, automated integration testing wherever needed to an excellent standard, comprehensive API testing using an appropriate tool and best practice, and reflection on earlier learning.

The guidance adds the following specific expectations:

- Identify the chosen scenario.
- For each role, list or tabulate every action from the brief.
- Explicitly identify any omitted, additional or altered actions and justify each change.
- Identify and justify any added or amended roles.
- Keep the design section focused on proposed design. Do not place implementation evidence in it.
- Explain bounded contexts as core, supporting/subdomain and generic contexts such as Identity.
- Compare architectural/responsibility options including monolith, modulith and microservices; CQRS and Service Layer; event sourcing and possible Outbox use; and direct communication through a facade.
- State and justify the proposed folder structure.
- Explain the design impact of security, error handling, logging and event handling.
- Focus diagrams on the core context.
- Use a class diagram to communicate structural separation of classes, interfaces and enums. If large, split it for the report or host it as a web-based diagram/image.
- Include two or three correctly constructed sequence diagrams that explain behaviour and architectural separation; a sequence diagram is not required for every interaction.
- Identify and justify selected design patterns and evaluate at least some plausible patterns that were rejected.
- Strategic patterns named by the guidance are bounded context, ubiquitous language, shared kernel, event aggregator and Outbox.
- Tactical patterns named are entity, value object, aggregate/aggregate root, DTO and domain event.
- Other named patterns are factory, controller, singleton, repository, DAO, facade/Open Host Service, Anti-Corruption Layer, Database per Service, Saga and Domain Service.
- Include an ERD and a separate data dictionary for each entity/table. Record field/attribute name, type, primary or foreign key status, indexes, constraints and a description explaining why the field exists.
- List the core-context endpoints with parameters, returned format and error messages.
- Identify proposed message queues, explain why cross-context communication needs them, state the information in each message and explain subscriber use. The guidance associates this criterion with implementing event sourcing.
- In implementation, include a diagram of the overall folder structure and explain how it communicates the core, supporting/subdomain and generic bounded contexts.
- Provide examples of mappings, validation, centralised error handling, custom errors, entity-to-DTO conversion, and Commands and Requests where CQRS is used.
- Explain the implemented security code, why it represents good practice, how Firebase or the selected alternative integrates, what intended security was present or absent and any extra security introduced.
- Explain create, read, update and delete handling using the core domain. The guidance notes that indiscriminate deletion is not necessarily appropriate.
- Explain implemented local and remote events, event record contents and their rationale, the selected broker and the broker's effect on code structure.
- Where relevant, connect decisions to earlier patterns and learning, including object-oriented programming, SOLID and GRASP.
- Testing does not require 100% code coverage, but it must be comprehensive. Include coverage evidence and a rationale for the selected unit and integration tests.
- API tests should cover every endpoint, valid and invalid data, and role-dependent behaviour.

### 11.6 Assignment 2 mark structure and evidence implications

Assignment 2 has three marking categories:

1. **Design/Implementation: 50 marks.** The 70%+ band requires excellent application scope, demonstration of all features with valid and invalid responses, excellent discussion of structural and responsibility decisions, correct demonstration of all appropriate security, appropriate data operations through a web service, compliance with industry best practice and the recommended architecture across all specified areas, and excellent answers about the subject and the student's own code.
2. **Testing: 30 marks.** The 70%+ band requires successful demonstration of all comprehensive API tests through Postman or a similar tool, successful demonstration of all automated tests, and excellent answers demonstrating ownership and understanding of test code and principles.
3. **Evaluation of KSBs: 20 marks.** The 70%+ band requires clear evaluation of every specified KSB with suitable evidence and lessons learned, plus appropriately justified, realistic improvements grounded in the current application.

The guidance adds the following demonstration expectations:

- Demonstrate that the application fulfils ideally all functional and non-functional requirements and handles issues appropriately.
- Revisit relevant decisions from the report and be ready for questions based on it.
- Demonstrate security in operation, show the code, explain how it works and discuss challenges.
- If using Firebase or an alternative Identity and Access Management mechanism, explain the choice and implementation.
- The guidance interprets the web-service criterion as including message-broker integration. It also expects discussion of database table construction and repositories.
- State whether the solution is a monolith, modulith, microservice or another structure and explain the architectural consequences.
- State whether CQRS was used and explain why and how it affected the architecture.
- State whether event sourcing was used and explain how it affected the architecture.
- Run API tests collection by collection. Explain body/payload values, status codes and API-testing best practice.
- Run all unit and integration tests and use them to demonstrate code coverage.
- Show representative test code and explain best practice, including Arrange-Act-Assert (AAA), Object Mother where used, naming and separation into methods.
- Prepare a table containing K22, K25, K26, S18, S19 and S22. For each, retain evidence, lessons learned, what would be done differently and how the competence will be enhanced.
- If KSB evidence has already been demonstrated in implementation or testing, the table may briefly remind the audience of it, but evidence and lessons learned must still be present.
- Provide three or four numbered, realistic and time-based recommendations. Each recommendation must connect to the scenario, current application state and evidence discussed earlier.

### 11.7 Scenario decision required for Assignment 1

The brief offers two alternative prototype scenarios. One must be selected and the chosen scenario must be developed as a domain-driven RESTful enterprise application within the deliberately limited scope.

- **Scenario 1: Leave Booking System.** Core context: Leave Management, centred on LeaveRequest and LeaveAllowance. Supporting context: Staff Management. Generic/non-domain-driven context: Identity and Access Control. Roles include staff, managers and admins. The workflow centres on annual-leave requests moving from Pending to Approved or Rejected, while allowing cancellation and allowance reporting. The brief also requires HR approval for specific requests, manager alerts for pending requests, and staff alerts for approved or cancelled requests.
- **Scenario 2: Staff Skill Auditor.** Core context: Staff Skill Management, centred on Skill and Skill Portfolio. Supporting context: Staff Management. Generic/non-domain-driven context: Identity and Access Control. Roles include staff, managers, Skill Manager/HR and admins. The workflow centres on skills being allocated to portfolios, edited, approved/verified, unverified, rejected, expired or deactivated, together with filtering and organisation-level management.

Both scenarios require authenticated and role-authorised endpoints, JWT authentication, logging of unauthorised access, request limiting for a specific endpoint, obfuscation of server-version HTTP headers, error and event logging, semantic REST URLs and verbs, JSON responses and meaningful HTTP status codes. Both also invite identification and implementation of local and remote events.


### 11.8 Formal Assignment 1 Brief and Mark Scheme - Full Transcription

The content below preserves the complete textual content and tables extracted from the formal Assignment 1 brief. Original wording, repetition and apparent grammatical or typographical issues have not been silently corrected.

**School of Digital Technologies, Innovation and Business**

Module Name: **Enterprise Application Development**

Module Code: **COMP60047**

Title of Assignment: **Assignment 1 - <u>report</u> (minimum of 1500 words) documenting the design, implementation and testing decisions made during the development of this solution.**

**Module Learning Outcomes for This Assignment**

| **Outcome**                                                                                     |
|-------------------------------------------------------------------------------------------------|
| Critically evaluate development approaches to solutions to enterprise applications.             |
| Design an enterprise application, critically evaluating alternatives and justifying selections. |
| Implement an enterprise application that includes relevant security features.                   |

# [University of Staffordshire Module Descriptors](https://evision.staffs.ac.uk/module_descriptors/COMP60047.html)

# Submission deadlines

**<span class="mark">21<sup>st</sup> of September 2026 – 09:30AM via Blackboard</span>**

### Fundamental requirements

In this assignment, you must satisfy **all** the following fundamental requirements, otherwise you could be awarded zero marks for the assignment.

1.  You must be the only author of the work you submit for assessment. <u>Any code submitted that is the result of generative AI must be clearly identified to establish if the work demonstrates the knowledge and capabilities of the learner</u>. You must not have help with this assignment from any person except for members of the regular teaching team. You are reminded of the university’s policy about academic misconduct, as described at:  
    > <http://www.staffs.ac.uk/assets/academic_misconduct%201516_tcm44-84941.pdf>

2.  You must submit your assignment via this module’s Blackboard presence using the links provided in the Assessment section.

**  
**

**AI Declaration**

| **AI Declaration**  | **Definition**                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
|---------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Limited             | AI tools may be used in a narrow or specific way which does not replace your own work or constitute plagiarism as defined by the Institution’s [academic misconduct procedure.](https://eur03.safelinks.protection.outlook.com/?url=https%3A%2F%2Fwww.staffs.ac.uk%2Fstudents%2Fcourse-administration%2Facademic-policies-and-regulations%2FAcademic-Conduct-Procedure&data=05%7C02%7Cphillip.james%40staffs.ac.uk%7C0c81dd1e51014037ac0908de4ecf706a%7C57af78f2c87d4466b7bb6b6cc99ed124%7C0%7C0%7C639034850029222586%7CUnknown%7CTWFpbGZsb3d8eyJFbXB0eU1hcGkiOnRydWUsIlYiOiIwLjAuMDAwMCIsIlAiOiJXaW4zMiIsIkFOIjoiTWFpbCIsIldUIjoyfQ%3D%3D%7C0%7C%7C%7C&sdata=%2BNrleZzIWN%2FXU%2FCh%2BOUlz%2BVbi9%2FoF9NaSjbtvIv%2B3ws%3D&reserved=0)   |

Acceptable (limited uses of AI) are:

- **Idea development (not assessed work):** AI may be used to support the development of initial ideas – however, <u>you will need to explain the reasoning behind their work</u>. 

<!-- -->

- **Learning support:** AI may be used in the early stages of preparation, helping to explore complex concepts, reinforce understanding, and strengthen learning before tackling assessments. 

<!-- -->

- **Clarification and explanation:** You can use AI to check your understanding of challenging terms, phrases, or concepts (as a tool to aid comprehension, not a substitute for the deeper engagement and analysis required in assessed tasks). 

<!-- -->

- **Exploring further resources:** AI <u>may</u> be used to suggest additional readings or references to broaden your research and deepen your engagement with the subject. 

**Note**: You will need to declare where ANY of the above uses has been used.

**<u>Deliverables for this assignment</u>**

- Working on your own *design*, implement and test an enterprise application making sure to identify and include *relevant security features.*

- A **presentation/demonstration** discussing the stages of the enterprise artefact development including design (*critically evaluating alternatives and justifying selections*), implementation and testing.

Your employer has asked you to put together a prototype enterprise application that demonstrates the range of knowledge and techniques that you have developed through this module.

# Scenario 1 - Leave Booking System

To demonstrate your knowledge of domain driven designs concepts and patterns, you have been asked to develop a domain driven version a the Leave Booking System API. Leave Booking systems are part of Enterprise Resource Planning (ERP).

<u>The following scenario limits the scope to certain core state requirements in order for it to be achievable by a single developer in this time window.</u>

**Note**: Your employer has stated that at this stage several things are required to be included – HR approval of specific staff requests, manager alerts re pending requests or staff alerts for approved/cancelled requests. 

**This system will enable staff to perform the following actions:  **

1.  Request leave (for now it is just annual leave that will be available) – a valid request is subject to approval by their manager (via the system) 

<!-- -->

2.  Cancel a leave request – whether approved or otherwise 

<!-- -->

3.  View the status of their leave requests – show all requests for a staff member including the status of those requests (Pending, Approved, Rejected) 

<!-- -->

4.  View their remaining annual leave and/or days used – a value representing the number of days left for the particular business year

**  
Note**: Leave requests will effectively go through a simple review cycle/approval chain (from submitting by staff as Pending to Approved or Rejected by the manager or admin). 

**Managers will be able to perform the following actions: **

1.  View outstanding requests for annual leave for their assigned members of staff (could be enhanced with start and end dates to reduce the reporting period) 

<!-- -->

2.  Approve a request for annual leave 

<!-- -->

3.  Reject a request for annual leave 

<!-- -->

4.  View the amount of annual leave remaining for a member of staff 

**Admins will be able to perform the following actions: **

1.  Add a new member of staff 

<!-- -->

2.  Amend the role or department of a member of staff 

<!-- -->

3.  View all outstanding leave requests filtered by staff member, manager’s team or across the company 

<!-- -->

4.  Amend the amount of annual leave assigned to a member of staff 

<!-- -->

5.  Approve requests on behalf of managers and track system-wide usage. 

From the above list we can identify a number of local and remote events.

## Suggested Bounded Contexts and Further Information

**<u>Leave Management Context (core)</u>**

- **LeaveRequest** - This will contain the id of the staff member requesting leave, the time slot of the leave, the reason, and the type of leave requested.

- **LeaveAllowance** - This will contain the identity of the staff member (first name, surname), their manager id, how many days leave they are entitled per year, balance of remaining days. Other fields that might be added are department or team which would help simplify reporting from that context.

**<u>Staff Management Context (supporting)</u>**

- **Staff Member** - In practice this context is a façade to a bigger HR information system (not featured in this assessment though you will generate events (messages) that are subscribed to by this context which add staff members or update their personal details – a solution to this will be demonstrated in the lectures). This will contain the identity of the staff member (first name, surname and email), the organisation (hire date, department that they work for, line manager id), placement (current role, start date of current role, job level and employment type), employment status (active, on leave, terminated)

<u>**Identity and Access Control**</u>

This will be handled by a module that is not domain driven. An example of this will be demonstrated in the lecture on identity. The module will feature User and Role entities.

# Scenario 2 – Staff Skill Auditor

To demonstrate your knowledge of domain driven designs concepts and patterns, you have been asked to develop a Staff Skill Auditor. This application allows a manager, HR or others to determine the skills currently held by a particular staff member, team or wider organisation.

<u>The following scenario limits the scope to certain core state requirements in order for it to be achievable by a single developer in this time window.</u>

For the moment this consists of a proposed RESTful API only (no front end).

**Staff to perform the following actions:  **

1.  View list of skills

2.  Allocate a skill to their own portfolio (pending verification/approval by manager)

3.  Edit an existing skill from their own portfolio (will require approval by manager after editing)

**Managers will be able to perform the following actions: **

1.  View all skills currently pending approval

2.  View all skills that have expired

3.  View all skills filtered by team member, skill, skill level

4.  Add notes to a specific skill submitted by a staff team member

5.  Verify skill submitted by staff team member

6.  Unverify skill submitted by staff team member

7.  Reject skill submitted by staff team member

**  
Skill Manager (Includes HR)**

- Create a skill

- Edit a skill

- Deactivate a skill

- View all skills filtered by team member, skill, skill level

- View all skills that have expired

**Admins will be able to perform the following actions: **

1.  Amend the department of a staff member

2.  Amend the personal details of a staff member

3.  Amend the placement of a staff member including role, job level or employment type

From the above list we can identify a number of local and remote events.

## Suggested Bounded Contexts and Further Information

**<u>Staff Skill Management Context (core)</u>**

- **Skill Aggregate** - This will contain the name of the skill along with a description, a category and a status that indicates if that skill’s is active or otherwise.

- **Skill Portfolio** - This will hold the record of skills for each staff member including their id, the id of the skill, the current level of that skill, when that skill expires (if applicable), any notes regarding that skill, as well as if that particular skill/level has been verified by a manager.

**<u>Staff Management Context (supporting)</u>**

- **Staff Member** - In practice this context is a façade to a bigger HR information system (not featured in this assessment though you will generate events (messages) that are subscribed to by this context which add staff members or update their personal details – a solution to this will be demonstrated in the lectures). This will contain the identity of the staff member (first name, surname and email), the organisation (hire date, department that they work for, line manager id), placement (current role, start date of current role, job level and employment type), employment status (active, on leave, terminated)

<u>**Identity and Access Control**</u>

This will be handled by a module that is not domain driven. An example of this will be demonstrated in the lecture on identity. The module will feature User and Role entities.

## For either of the scenarios – consider how you will implement the following areas:

**Security **

- Users will have to log in to view any API end points (i.e. all end points other than to login require authentication and authorisation by role) 

<!-- -->

- JWT tokens will be used for authentication. 

<!-- -->

- Requests will check the role of the user to establish if they are authorised to view a particular end point – a log should be kept of unauthorised access to end points 

<!-- -->

- The system will limit the number of requests from a specific end point 

<!-- -->

- The system will adjust its HTTP headers so that the server version is obfuscated 

**  
Logging/Monitoring **

- It is expected that logs will be kept of any errors and events for further examination 

**General Points About the API Design **

- Apply clear, semantic URL paths and HTTP verbs (GET, POST, DELETE, PUT, PATCH) 

- Use JSON as your response format 

<!-- -->

- Use meaningful HTTP response status codes for clarity 

# Mapping to Knowledge, Skills and Behaviours

<table>
<colgroup>
<col style="width: 8%" />
<col style="width: 91%" />
</colgroup>
<thead>
<tr class="header">
<th><strong>Key</strong></th>
<th><strong>Knowledge, Skills and Behaviour</strong></th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><blockquote>
<p>K21</p>
</blockquote></td>
<td><blockquote>
<p>How to operate at all stages of the software development life cycle and how each stage is applied in a range of contexts. For example, requirements analysis, design, development, testing, implementation.</p>
</blockquote></td>
</tr>
<tr class="even">
<td><blockquote>
<p>K22</p>
</blockquote></td>
<td><blockquote>
<p>Principles of a range of development techniques, for each stage of the software development cycle that produce artefacts and the contexts in which they can be applied. For example, UML, unit testing, programming, debugging, frameworks, architectures.</p>
</blockquote></td>
</tr>
<tr class="odd">
<td><blockquote>
<p>S18</p>
</blockquote></td>
<td><blockquote>
<p>Use appropriate analysis methods, approaches and techniques in software engineering projects to deliver an outcome that meets requirements.</p>
</blockquote></td>
</tr>
<tr class="even">
<td>S19</td>
<td>Implement software engineering projects using appropriate software engineering methods, approaches and techniques.</td>
</tr>
<tr class="odd">
<td>S21</td>
<td>Determine, refine, adapt and use appropriate software engineering methods, approaches and techniques to evaluate software engineering project outcomes.</td>
</tr>
</tbody>
</table>





## Mark Scheme

<table>
<colgroup>
<col style="width: 12%" />
<col style="width: 25%" />
<col style="width: 21%" />
<col style="width: 0%" />
<col style="width: 19%" />
<col style="width: 16%" />
<col style="width: 3%" />
</colgroup>
<thead>
<tr class="header">
<th><strong>Criterion (KSB)</strong></th>
<th><blockquote>
<p>&gt;=70%</p>
</blockquote></th>
<th colspan="2">50-69%</th>
<th><blockquote>
<p>40-49%</p>
</blockquote></th>
<th><blockquote>
<p>&lt;40%</p>
</blockquote></th>
<th><blockquote>
<p>Mark</p>
</blockquote></th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td rowspan="8"><p><strong>Design Decisions</strong></p>
<blockquote>
<p>Appropriate images, diagrams and tables (where appropriate) are used to aid communication.</p>
</blockquote>
<p>Where possible we will see reflection on how previous learning has influenced the decision making here.</p></td>
<td><p>Excellent design and a <strong>critical discussion</strong> of the following areas:</p>
<p>Clear overview and excellent justification of any changes to the original requirements from the brief.</p></td>
<td colspan="2"><p>Design that allows the requirements to be <strong>mostly implemented</strong> and justified.</p>
<p>Overview and justification of the any changes to the original requirements.</p></td>
<td><p>Design that allows the requirements to be <strong>somewhat implemented</strong>:</p>
<p>App requirements are discussed.</p></td>
<td rowspan="6">Little or no evidence of one or more of the criterion included under 40-49%.</td>
<td rowspan="6"><blockquote>
<p>/25</p>
</blockquote></td>
</tr>
<tr class="even">
<td><p>The architecture and various responsibilities are identified and justified well.</p>
<p>Use appropriate diagrams e.g. class and sequence to help illustrate this.</p></td>
<td colspan="2">The architecture and various responsibilities are identified and justified.</td>
<td>The architecture and various responsibilities are identified.</td>
</tr>
<tr class="odd">
<td>A very good range of design patterns are identified and justified.</td>
<td colspan="2">A number of relevant design patterns are identified/justified.</td>
<td>Several patterns are identified.</td>
</tr>
<tr class="even">
<td><p>ERD included as well as a data dictionary – discussed and justified.</p>
<p>Tables and attributes have no errors and are appropriate.</p></td>
<td colspan="2"><p>ERD included as well as a data dictionary should be included.</p>
<p>Tables and attributes have almost no errors and are appropriate.</p></td>
<td>Tables designs have minor errors.</td>
</tr>
<tr class="odd">
<td>API end points with parameters, data format to be returned and error messaging are provided.</td>
<td colspan="2">API end points with parameters, data format to be returned and error messaging are provided with very few errors.</td>
<td>API end points are described but are missing some of the parameters, data format to be returned and error messaging.</td>
</tr>
<tr class="even">
<td><p>A number of message queues are identified/justified well and the data selected is entirely appropriate.</p>
<p>It is clear why these are chosen and how they will be used by the subscriber.</p></td>
<td><p>One or two message queues are identified and the data selected is appropriate.</p>
<p>It is somewhat clear why these are chosen and how they will be used by the subscriber.</p></td>
<td colspan="2">One message queue is identified. The data selected is somewhat appropriate.</td>
</tr>
<tr class="odd">
<td colspan="4">Where possible (with any of the above) we will see reflection on how previous learning has influenced the decision making here.</td>
<td></td>
<td></td>
</tr>
<tr class="even">
<td colspan="5"></td>
<td></td>
</tr>
<tr class="odd">
<td rowspan="7"><p><strong>Implementation Decisions</strong></p>
<p>Where possible we will see reflection on how previous learning has influenced the decision making here.</p></td>
<td><p>Requirements implemented with <strong>a very close match</strong> to the design:</p>
<p>All classes implemented following identified architectural approach, implement proposed design patterns and are of a professional standard.</p></td>
<td colspan="2"><p>Requirements <strong>mostly implemented</strong> with a good match to the design:</p>
<p>Classes implemented following identified architectural approach, mostly apply design patterns identified in the design - and are of a good standard.</p></td>
<td><p>Requirements <strong>partially implemented</strong> with a match to the design:</p>
<p>Some classes implemented somewhat following an architectural approach, evidence of design patterns being implemented - and are of a satisfactory standard.</p></td>
<td rowspan="6">Little or no evidence of one or more of the criterion included under 40-49%.</td>
<td rowspan="5"><blockquote>
<p>/45</p>
</blockquote></td>
</tr>
<tr class="even">
<td>Examples of Conversion, transformation, validation and error handling are applied to a high standard.</td>
<td colspan="2">Examples of conversion, transformation, validation and error handling applied to a good standard.</td>
<td>Conversion, transformation, validation and error handling applied in some cases.</td>
</tr>
<tr class="odd">
<td>Correct utilisation and functioning of relevant security features.</td>
<td colspan="2">Utilisation of some security features or some features not implemented correctly.</td>
<td>Limited security features implemented.</td>
</tr>
<tr class="even">
<td>Functionality to perform appropriate CRUD activities on all tables implemented.</td>
<td colspan="2">Ability to perform CRUD activities on most tables are implemented.</td>
<td>Some application of CRUD activities implemented.</td>
</tr>
<tr class="odd">
<td>Event monitoring and messaging is well implemented.</td>
<td colspan="2">Event monitoring and messaging is mostly implemented.</td>
<td>Event monitoring and messaging is largely absent or poorly implemented.</td>
</tr>
<tr class="even">
<td colspan="4">Where possible (with any of the above) we will see reflection on how previous learning has influenced the decision making here.</td>
<td></td>
</tr>
<tr class="odd">
<td colspan="5"></td>
<td></td>
</tr>
<tr class="even">
<td rowspan="5"><p><strong>Testing Decisions</strong></p>
<p>Where possible we will see reflection on how previous learning has influenced the decision making here.</p></td>
<td>The application will be <strong>comprehensively</strong> <strong>tested</strong> and your presentation will critically discuss this stage.</td>
<td colspan="2">The following will be <strong>mostly tested</strong> and your presentation will discuss this stage.</td>
<td>The following will be <strong>satisfactorily tested</strong> and your presentation will discuss this stage.</td>
<td rowspan="5">Little or no evidence of one or more of the criterion included under 40-49%</td>
<td rowspan="5"><blockquote>
<p>/30</p>
</blockquote></td>
</tr>
<tr class="odd">
<td>Comprehensive automated unit testing. Follows best practice.</td>
<td colspan="2">Automated unit testing implemented for most applicable areas.</td>
<td>Automated unit testing – some testing.</td>
</tr>
<tr class="even">
<td>Automated integration testing where needed and applied to an excellent standard.</td>
<td colspan="2">Automated integration testing – is mostly present where applicable.</td>
<td>Integration testing – may be missing.</td>
</tr>
<tr class="odd">
<td>API (if used) is comprehensively tested using an appropriate tool. Follows best practice.</td>
<td colspan="2">API (if used) is mostly tested using an appropriate tool.</td>
<td>API (if used) is partially tested using an appropriate tool.</td>
</tr>
<tr class="even">
<td colspan="4">Where possible (with any of the above) we will see reflection on how previous learning has influenced the decision making here.</td>
</tr>
<tr class="odd">
<td></td>
<td colspan="5"></td>
<td></td>
</tr>
</tbody>
</table>


### 11.9 Formal Assignment 2 Brief and Mark Scheme - Full Transcription

The content below preserves the complete textual content and tables extracted from the formal Assignment 2 brief. Original wording, repetition and apparent grammatical or typographical issues have not been silently corrected.

**School of Digital Technologies, Innovation and Business**

Module Name: **Enterprise Application Development**

Module Code: **COMP60047**

Title of Assignment: **Assignment 2 - <u>presentation</u> of the development and <u>demonstration</u> of the finished solution - showcasing the finished application and answer <u>questions</u> about its construction. It will also <u>include an evaluation of the KSBs developed during this module</u>.**

**Module Learning Outcomes for This Assignment**

| **Outcome**                                                                                     |
|-------------------------------------------------------------------------------------------------|
| Critically evaluate development approaches to solutions to enterprise applications.             |
| Design an enterprise application, critically evaluating alternatives and justifying selections. |
| Implement an enterprise application that includes relevant security features.                   |

# [University of Staffordshire Module Descriptors](https://evision.staffs.ac.uk/module_descriptors/COMP60047.html)

# Submission deadlines

**<span class="mark">21<sup>st</sup> of September 2026 – 09:30AM via Blackboard</span> (for the submission of any presentation material for the above assessment).**

**<span class="mark">Your presentation/VIVA will be a maximum of 30 minutes in duration (excluding appropriate student inclusion plans) – slots for this will be communicated/agreed by your tutor.</span>**

### Fundamental requirements

In this assignment, you must satisfy **all** the following fundamental requirements, otherwise you could be awarded zero marks for the assignment.

1.  You must not have help with this assignment from any person except for members of the regular teaching team. You are reminded of the university’s policy about academic misconduct, as described at:  
    > <http://www.staffs.ac.uk/assets/academic_misconduct%201516_tcm44-84941.pdf>

**  
**

**AI Declaration**

| **Declaration**  | **Definition**                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                           |
|------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Limited          | AI tools may be used in a narrow or specific way which does not replace your own work or constitute plagiarism as defined by the Institution’s [academic misconduct procedure.](https://eur03.safelinks.protection.outlook.com/?url=https%3A%2F%2Fwww.staffs.ac.uk%2Fstudents%2Fcourse-administration%2Facademic-policies-and-regulations%2FAcademic-Conduct-Procedure&data=05%7C02%7Cphillip.james%40staffs.ac.uk%7C0c81dd1e51014037ac0908de4ecf706a%7C57af78f2c87d4466b7bb6b6cc99ed124%7C0%7C0%7C639034850029222586%7CUnknown%7CTWFpbGZsb3d8eyJFbXB0eU1hcGkiOnRydWUsIlYiOiIwLjAuMDAwMCIsIlAiOiJXaW4zMiIsIkFOIjoiTWFpbCIsIldUIjoyfQ%3D%3D%7C0%7C%7C%7C&sdata=%2BNrleZzIWN%2FXU%2FCh%2BOUlz%2BVbi9%2FoF9NaSjbtvIv%2B3ws%3D&reserved=0)   |

Acceptable (limited uses of AI) are:

- **Idea development (not assessed work):** AI may be used to support the development of initial ideas – however, <u>you will need to explain the reasoning behind their work</u> during the presentation.

**Note**: You will need to declare where ANY of the above uses has been used.

# Mapping 

<table>
<colgroup>
<col style="width: 8%" />
<col style="width: 91%" />
</colgroup>
<thead>
<tr class="header">
<th><strong>Key</strong></th>
<th><strong>Knowledge, Skills and Behaviours</strong></th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td><blockquote>
<p>K22</p>
</blockquote></td>
<td><blockquote>
<p>Principles of a range of development techniques, for each stage of the software development cycle that produce artefacts and the contexts in which they can be applied. For example, UML, unit testing, programming, debugging, frameworks, architectures.</p>
</blockquote></td>
</tr>
<tr class="even">
<td><blockquote>
<p>K25</p>
</blockquote></td>
<td><blockquote>
<p>The factors affecting product quality and approaches for how to control them throughout the development process. For example, security, code quality, coding standards.</p>
</blockquote></td>
</tr>
<tr class="odd">
<td><blockquote>
<p>K26</p>
</blockquote></td>
<td><blockquote>
<p>How to select and apply a range of software tools used in Software Engineering.</p>
</blockquote></td>
</tr>
<tr class="even">
<td><blockquote>
<p>S18</p>
</blockquote></td>
<td><blockquote>
<p>Use appropriate analysis methods, approaches and techniques in software engineering projects to deliver an outcome that meets requirements.</p>
</blockquote></td>
</tr>
<tr class="odd">
<td><blockquote>
<p>S19</p>
</blockquote></td>
<td><blockquote>
<p>Implement software engineering projects using appropriate software engineering methods, approaches and techniques.</p>
</blockquote></td>
</tr>
<tr class="even">
<td>S22</td>
<td>Evaluate learning points arising from software engineering work undertaken on a project including use of methods, analysis undertaken, selection of approach and the outcome achieved to identify both lessons learnt and recommendations for improvements to future projects.</td>
</tr>
</tbody>
</table>

## 

## Mark Scheme

<table style="width:100%;">
<colgroup>
<col style="width: 12%" />
<col style="width: 22%" />
<col style="width: 24%" />
<col style="width: 21%" />
<col style="width: 14%" />
<col style="width: 3%" />
</colgroup>
<thead>
<tr class="header">
<th><strong>Criterion (KSB)</strong></th>
<th><blockquote>
<p>&gt;=70%</p>
</blockquote></th>
<th>50-69%</th>
<th><blockquote>
<p>40-49%</p>
</blockquote></th>
<th><blockquote>
<p>&lt;40%</p>
</blockquote></th>
<th><blockquote>
<p>Mark</p>
</blockquote></th>
</tr>
</thead>
<tbody>
<tr class="odd">
<td rowspan="7"><strong>Design/<br />
Implementation</strong></td>
<td><ul>
<li><blockquote>
<p>App scope is of an excellent standard.</p>
</blockquote></li>
<li><blockquote>
<p>Demonstration of all the features with all valid and invalid responses</p>
</blockquote></li>
</ul></td>
<td><p>App scope is of a good standard, and most agreed features have been implemented identified use cases implemented.</p>
<ul>
<li><blockquote>
<p>Demonstration of the features, with all valid and invalid responses</p>
</blockquote></li>
</ul></td>
<td><p>App scope is of a satisfactory standard, and some agreed features have been implemented identified use cases implemented.</p>
<ul>
<li><blockquote>
<p>Demonstration of some of the features</p>
</blockquote></li>
</ul></td>
<td rowspan="6">Little or no evidence of one or more of the criterion included under 40-49%</td>
<td><blockquote>
<p>/50</p>
</blockquote></td>
</tr>
<tr class="even">
<td>Excellent discussion of the decision making around the structure and separation of responsibilities</td>
<td>Good discussion of the decision making around the structure and separation of responsibilities</td>
<td>Some discussion of the decision making around the structure and separation of responsibilities</td>
<td></td>
</tr>
<tr class="odd">
<td>Correct utilisation of all appropriate security features demonstrated.</td>
<td>Utilisation of all appropriate security features demonstrated.</td>
<td>Utilisation of all appropriate security features demonstrated.</td>
<td></td>
</tr>
<tr class="even">
<td>Ability to perform an appropriate data operations using a web service.</td>
<td>Ability to perform an appropriate data operations using a web service.</td>
<td>Ability to perform an appropriate data operations using a web service.</td>
<td></td>
</tr>
<tr class="odd">
<td>Follows specified industry best practice and recommended architecture – all specified areas.</td>
<td>Follows specified industry best practice and recommended architecture – most areas.</td>
<td>Follows specified industry best practice and recommended architecture – some areas.</td>
<td></td>
</tr>
<tr class="even">
<td>Answers to any questions demonstrate an excellent level of understanding of the subject matter and their own code.</td>
<td>Answers to any questions demonstrate a good level of understanding of the subject matter and their own code.</td>
<td>Answers to any questions demonstrate a good level of understanding of the subject matter and their own code.</td>
<td></td>
</tr>
<tr class="odd">
<td colspan="4"></td>
<td></td>
</tr>
<tr class="even">
<td rowspan="4"><strong>Testing</strong></td>
<td>Demonstration of ALL API tests via Postman or similar is successful – and tests are comprehensive.</td>
<td>Demonstration of most tests via Postman or similar is successful – and tests are fairly comprehensive.</td>
<td>Demonstration of some tests via Postman or similar is successful.</td>
<td rowspan="3"><blockquote>
<p>Little or no evidence of one or more of the criterion included under 40-49%</p>
</blockquote></td>
<td rowspan="3"><blockquote>
<p>/30</p>
</blockquote></td>
</tr>
<tr class="odd">
<td>Demonstration of ALL automated tests is successful</td>
<td>Demonstration of most automated tests are successful – and tests are fairly comprehensive/follow best practice</td>
<td>Demonstration of some automated tests is successful</td>
</tr>
<tr class="even">
<td>Answers to any questions demonstrate an excellent level of understanding of the subject matter and their own code.</td>
<td>Answers to most questions demonstrate an excellent level of understanding of the subject matter and their own code.</td>
<td>Answers to questions demonstrate a satisfactory level of understanding of the subject matter and their own code.</td>
</tr>
<tr class="odd">
<td colspan="4"></td>
<td></td>
</tr>
<tr class="even">
<td rowspan="3"><p><strong>Evaluation of KSBs</strong></p>
<p><strong>(if you have not identified and addressed some of these in the implementation and testing sections)</strong></p>
<p><strong>K22, K25, K26, S18, S19, S22</strong></p></td>
<td>Clear evaluations of every KSB identified in the assignment and how these were developed with appropriate evidence including lessons learned</td>
<td>Evaluations of most KSB identified in the assignment and how these were developed with appropriate evidence including lessons learned</td>
<td>Evaluations of some KSB identified in the assignment and how these were developed with some evidence</td>
<td rowspan="2">Little or no evidence of one or more of the criterion included under 40-49%</td>
<td rowspan="2">/20</td>
</tr>
<tr class="odd">
<td>Appropriate and realistic recommendations for improvements are appropriately justified and are based on the current state of the app.</td>
<td>Appropriate recommendations for improvements are included and are based on the current state of the app</td>
<td>Recommendations for improvements are included</td>
</tr>
<tr class="even">
<td colspan="4"></td>
<td></td>
</tr>
</tbody>
</table>


### 11.10 Assignment 2 Guidance Document - Full Layout-Preserving Transcription

The following block retains the full four-page guidance text in reading order. Page breaks are marked explicitly. The spacing reflects the two-column criterion-and-guidance layout of the original PDF.

~~~text
COMP60047: Enterprise Application Development – Assignment 2 [Presentation/VIVA] Guidance
Document

Assignment 2 [Presentation/VIVA] weighting 60%
Presentation of the development and demonstration of the finished solution - showcasing the
finished application and answer questions about its construction. It will also include an evaluation of
the KSBs developed during this module.

 Outcome

 Critically evaluate development approaches to solutions to enterprise applications.

 Design an enterprise application, critically evaluating alternatives and justifying selections.

 Implement an enterprise application that includes relevant security features.



Mapping to Knowledge, Skills and Behaviours
 Key       Knowledge, Skills and Behaviours

           Principles of a range of development techniques, for each stage of the software development
 K22       cycle that produce artefacts and the contexts in which they can be applied. For example, UML,
           unit testing, programming, debugging, frameworks, architectures.

           The factors affecting product quality and approaches for how to control them throughout the
 K25
           development process. For example, security, code quality, coding standards.

 K26       How to select and apply a range of software tools used in Software Engineering.

           Use appropriate analysis methods, approaches and techniques in software engineering projects
 S18
           to deliver an outcome that meets requirements.

           Implement software engineering projects using appropriate software engineering methods,
 S19
           approaches and techniques.

           Evaluate learning points arising from software engineering work undertaken on a project
           including use of methods, analysis undertaken, selection of approach and the outcome
 S22
           achieved to identify both lessons learnt and recommendations for improvements to future
           projects.




                                                   1


[Page break]

COMP60047: Enterprise Application Development – Assignment 2 [Presentation/VIVA] Guidance
Document

Design/Implementation 50%
Excellent design and a critical discussion of the following areas: explain what things from the module
you will implement and why others have not been, or have been done differently – this is a level 6
module where you are demonstrating competence, critical thinking and good decision making.

 App scope is of an excellent standard.     Demonstration of the app shows that its has fulfilled (ideally)
                                            all of the expected requirements (both functional and non-
 Demonstration of all the features with all
                                            functional) and handles any issues, etc appropriately.
 valid and invalid responses

 Excellent discussion of the decision      Dip into the report here.
 making around the structure and
                                           The tutor may have questions in mind from the report and will
 separation of responsibilities
                                           ask for clarification or justification as needed.

 Correct utilisation of all appropriate    Demonstrate how these work. Show the code, explain how it
 security features demonstrated.           works.

                                           Any challenges.

                                           The tutor may have questions in mind from the report and will
                                           ask for clarification or justification as needed.

 Ability to perform an appropriate data    Whilst the Identity and Access Management example from the
 operations using a web service.           case study used Firebase auth, you might have something
                                           different – explain.

                                           Web service here relates to the message broker – so discuss its
                                           integration into your solution.

                                           I would also look at how the database aspect of the project is
                                           managed here – re table construction, repositories.

 Follows specified industry best practice A lot of this will likely come out from the criterion above.
 and recommended architecture – all
                                          Highlight the architectural aspects here – is your solution a
 specified areas.
                                          monolith, modulith, microservice, etc – how did this affect the
                                          architecture?

                                           Have you used CQRS? Why? – how did this affect the
                                           architecture?

                                           Have you used event sourcing – how did this affect the
                                           architecture?

 Answers to any questions demonstrate As stated in the assignment 1 guidance document – the tutor
 an excellent level of understanding of will ask questions in relation to your implementation.
 the subject matter and their own code.



Testing 30% - focus on the core context



                                                   2


[Page break]

COMP60047: Enterprise Application Development – Assignment 2 [Presentation/VIVA] Guidance
Document

 Demonstration of ALL API tests via       Run the tests. Collection by collection.
 Postman or similar is successful – and
                                          Demonstrate how you are following best practice here.
 tests are comprehensive.
                                          Discuss the body/payload values.

                                          Discuss the status codes.

 Demonstration of ALL automated tests is Run the tests (unit or integration) to demonstrate the code
 successful                              coverage.

                                          Show examples of the tests themselves to identify good
                                          practice, re test construction, patterns implemented (e.g. AAA,
                                          object mother), naming, separation into methods, etc.

 Answers to any questions demonstrate As stated in the assignment 1 guidance document – the tutor
 an excellent level of understanding of will ask questions in relation to your tests.
 the subject matter and their own code.




                                                 3


[Page break]

COMP60047: Enterprise Application Development – Assignment 2 [Presentation/VIVA] Guidance
Document

Evaluation of KSBs 20%
If you identified and addressed some of these in the implementation and testing sections K22,
K25, K26, S18, S19, S22 – then

 Clear evaluations of every KSB identified Table with each of the following KSBs:
 in the assignment and how these were
                                           K22, K25, K26, S18, S19, S22
 developed with appropriate evidence
 including lessons learned                 If you identified these during the implementation and testing
                                           sections – the table can just remind the audience of that but
                                           we need to make sure that we have both evidence of these
                                           plus the lessons learned from them (what would we do
                                           differently, what are we going to do to further enhance these,
                                           etc).

 Appropriate and realistic                Recommendations (3 to 4) – a table, numbered and these
 recommendations for improvements are recommendations are realistic, clearly link to what has been
 appropriately justified and are based on discussed so far and in relation to the scenario, and time
 the current state of the app.            based.




                                                 4


[Page break]

~~~


### 11.11 Assignment 1 Guidance Document - Full Layout-Preserving Transcription

The following block retains the full five-page guidance text in reading order. Page breaks are marked explicitly. The spacing reflects the two-column criterion-and-guidance layout of the original PDF.

~~~text
COMP60047: Enterprise Application Development – Assignment 1 [Report] Guidance Document



Assignment 1 [Report] (weighting 40%)
Minimum of 1500 words documenting the design, implementation and testing decisions made
during the development of this solution.

This is a level 6 module and as I said its as much about the design/architecture choices as it is about
the implementation that follows.

I have provided a lot of guidance in this document, that does not have to equate to writing great
quantities of words for each section. What is important is that you show what you have considered –
included or excluded in each criterion – and why.

 Outcome

 Critically evaluate development approaches to solutions to enterprise applications.

 Design an enterprise application, critically evaluating alternatives and justifying selections.

 Implement an enterprise application that includes relevant security features.

Mapping to Knowledge, Skills and Behaviours
 Key       Knowledge, Skills and Behaviour

           How to operate at all stages of the software development life cycle and how each stage is
 K21       applied in a range of contexts. For example, requirements analysis, design, development,
           testing, implementation.

           Principles of a range of development techniques, for each stage of the software development
 K22       cycle that produce artefacts and the contexts in which they can be applied. For example, UML,
           unit testing, programming, debugging, frameworks, architectures.

           Use appropriate analysis methods, approaches and techniques in software engineering projects
 S18
           to deliver an outcome that meets requirements.

           Implement software engineering projects using appropriate software engineering methods,
 S19
           approaches and techniques.

           Determine, refine, adapt and use appropriate software engineering methods, approaches and
 S21
           techniques to evaluate software engineering project outcomes.




                                                   1


[Page break]

COMP60047: Enterprise Application Development – Assignment 1 [Report] Guidance Document


Design Decisions (25%)
“Appropriate images, diagrams and tables (where appropriate) are used to aid communication.”

Excellent design and a critical discussion of the following areas: explain what things from the module
you will implement and why others have not been, or have been done differently – this is a level 6
module where you are demonstrating competence, critical thinking and good decision making.

 Clear overview and excellent          Identify the scenario chosen. The title of the scenario here is fine.
 justification of any changes to the
                                       Then for each role (staff, manager, admin) using a list or table, specify
 original requirements from the brief.
                                       the actions stated in the assignment brief. Identify at this stage, if any
                                       of the proposed actions from the scenario are not in your solution, also
                                       identify if there are any additional ones that you have chosen to
                                       include and why.

                                         Also identify here any additional or amended user roles in your
                                         proposed solution – and why.

 The architecture and various            This is the design stage, so no implementation evidence should be
 responsibilities are identified and     present.
 justified well.
                                         Separation of bounded contexts – core domain, sub domain, generic
 Use appropriate diagrams e.g. class     sub domains (identity).
 and sequence to help illustrate this.
                                         What separation of responsibilities are you proposing? Why/how did
                                         you decide on this. We have the following patterns:
 You may very well find yourselves       monolith vs modulith vs microservices (or other).
 naturally discussing design patterns at CQRS vs service (strictly speaking CQRS is architectural but service is
 this point – which is ok – just make tactical).
 sure to signpost what patterns you Event sourcing (possibly including outbox pattern) vs direct comms
 are looking at here. Some may wait via façade.
 until the next section.
                                         What folder structure are you proposing? Why/how did you decide on
                                         this.

                                         There is talk here of responsibilities (all of which impact on folders,
                                         class separation etc) – what security aspects do you need to include
                                         and how does this affect the design? how is error handling handled – is
                                         it centralised or localised – if so why is that a good idea? How are you
                                         handling logging? How are events being handled?

                                         Diagrams (Focus on the core context)

                                         The class diagram will show the structural separation – classes,
                                         interfaces, enums, etc. This might be large therefore you will likely
                                         need to split it up for inclusion in the report, or you can include these
                                         as web based diagrams/images if you wish

                                         Sequence diagrams will show the behaviour – I don’t want a sequence
                                         diagram for every interaction but I would like to see 2 or 3 (correctly
                                         constructed) sequence diagrams that you can use to explain the




                                                   2


[Page break]

COMP60047: Enterprise Application Development – Assignment 1 [Report] Guidance Document


                                         separation/architecture. You can include these as web-based
                                         diagrams/images if you wish.

 A very good range of design patterns What design patterns are you going to use. Briefly explain the value of
 are identified and justified.        each of these in the context of the solution.

 Strategic patterns included: bounded I would use a table with the pattern name, why it would make sense to
 context, ubiquitous language, shared include in your solution. Should link up well to the previous point re
 kernel, event aggregator, outbox      architecture/responsibilities.
 pattern
                                       Given the range of patterns we have looked at I would appreciate you
 Tactical patterns included:           identifying why a particular pattern was not chosen as well. This is the
 entity, value object, aggregate/root, evaluation.
 data transfer object, domain event.

 GOF patterns - factory, controller,
 singleton, repository, data access
 object, façade/open host service

 Other ones you might have looked at:
 anti-corruption layer, database per
 service, saga, domain service.

 ERD included as well as a data          Include an entity relationship diagram.
 dictionary – discussed and justified.
                                      Make sure that you have a data dictionary (table per entity) that
 Tables and attributes have no errors describes each of the attributes/fields, what type it is, is it a key
 and are appropriate.                 (primary or foreign), any indexes, constraints, etc, add a description
                                      field that explains what that attribute/field is necessary.

 API end points with parameters, data List of the end points for the core context.
 format to be returned and error
 messaging are provided.

 A number of message queues are         Requires event sourcing to be implemented.
 identified/justified well and the data
                                        What message queues do you propose?
 selected is entirely appropriate.
                                        Why are these needed? Re comms between contexts.
 It is clear why these are chosen and
 how they will be used by the           What information would be included?
 subscriber.

 Where possible (with any of the      Hopefully this is self-explanatory.
 above) we will see reflection on how
 previous learning has influenced the
 decision making here.




                                                   3


[Page break]

COMP60047: Enterprise Application Development – Assignment 1 [Report] Guidance Document


Implementation Decisions (45%)
Requirements implemented with a very close This is the evidence that the code and separation matches
match to the design:                         the architectural approach and design patterns discussed in
                                             the design section; and that what was discussed follows good
                                             practice – and that it works.
All classes implemented following identified
architectural approach, implement proposed A diagram of the overall folder structure with a brief
design patterns and are of a professional    discussion of how this shows the various bounded contexts
standard.                                    (core, sub domain, generic).

Examples of Conversion, transformation,        Examples of mapping, validation, error handling here.
validation and error handling are applied to a
                                               So you might, for example, refer to the code that centrally
high standard.
                                               manages the error handling, any custom error classes.
                                               Conversion of entities to DTOs. Commands and Requests
                                               (dependent upon where or not you implemented CQRS).

Correct utilisation and functioning of relevant What code was implemented to manage the security. Why is
security features.                              this good practice?

                                              How was Firebase (or whatever you chose) implemented to
                                              work with your application. What was present or missing
                                              from your final solution?

                                              Anything additional you added?

Functionality to perform appropriate CRUD     Not necessary to perform CRUD for everything as deletion
activities on all tables implemented.         might not be a good idea in some situations.

                                              But identify how data is created, read, updated and deleted –
                                              use the core domain to discuss this.

Event monitoring and messaging is well        How were the events discussed in the design implemented.
implemented.
                                              We have remote and (potentially) local events – what events
                                              did you chose?

                                              What records did you define for those events – what did they
                                              consist of and why?

                                              What message broker did you use?

                                              How did the inclusion of a message broker impact on the
                                              code?

Where possible (with any of the above) we Hopefully this is self-explanatory but where possible
will see reflection on how previous learning make brief reference to previous patterns learned, OOP
has influenced the decision making here.     principles, SOLID, GRASP, etc.




                                                  4


[Page break]

COMP60047: Enterprise Application Development – Assignment 1 [Report] Guidance Document


Testing Decisions (30%)
The application will be comprehensively      This section will clearly identify that your different types
tested and your presentation will critically of tests each comprehensively cover the system – if not
discuss this stage.                          you will explain why.

Comprehensive automated unit testing.         It’s comprehensive – good though not 100% code
Follows best practice.                        coverage. We can include an image (or images to show
                                              that) – you will demonstrate this in the presentation.
                                              A brief rationale as to the types of tests included.

Automated integration testing where           We can include an image (or images to show what was
needed and applied to an excellent            tested) – you will demonstrate this in the presentation.
standard.                                     A brief rationale as to the types of tests included and
                                              why.

API (if used) is comprehensively tested      Coverage of all end points – considering both valid data
using an appropriate tool. Follows best      (which might have different roles to take into
practice.                                    consideration) as well as invalid data.
                                             A brief rationale as to the types of tests included and
                                             why.
Where possible (with any of the above) we Hopefully this is self-explanatory.
will see reflection on how previous learning
has influenced the decision making here.




                                                   5


[Page break]

~~~
