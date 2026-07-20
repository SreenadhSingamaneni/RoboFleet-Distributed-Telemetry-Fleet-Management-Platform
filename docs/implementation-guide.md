# Implementation and annotation guide

This guide is the learning companion to the code. Read it in order, open each referenced file, and explain the file back in your own words before moving on.

## Milestone 1: repository and runtime boundaries

The root is a monorepo because one portfolio owner needs atomic changes to the event contract, producer, consumer, and UI. Separate `backend`, `simulator`, and `dashboard` build contexts preserve independent deployability.

- `.editorconfig` makes whitespace deterministic across editors.
- `.gitignore` excludes secrets, local state, dependencies, and build output.
- `.env.example` documents local configuration without containing a real secret.
- `Makefile` provides memorable development commands; it delegates to the native build tools.
- `docker-compose.yml` is the local integration environment, not a production scheduler.

Stop and be able to answer: why is a monorepo not the same thing as a monolith?

## Milestone 2: Spring Boot foundation

`FleetTelemetryApplication` is the process entry point.

- `@SpringBootApplication` combines configuration, auto-configuration, and component scanning from `com.roboverse.fleet` downward.
- `@ConfigurationPropertiesScan` finds typed property records such as `FleetProperties`.
- `@EnableScheduling` activates the stale-robot monitor and outbox relay.
- `SpringApplication.run` creates the application context, wires beans, and starts the embedded web server and non-web listeners.

`FleetProperties` maps the `fleet.*` YAML namespace into immutable nested records. This replaces scattered `@Value` strings with one validated, discoverable configuration model and lets rules receive related thresholds together.

`TimeConfig` exposes a `Clock`. Depending on `Clock` rather than calling `Instant.now()` everywhere makes time-driven behavior deterministic in unit tests.

### Maven dependencies

| Dependency | Why it exists |
| --- | --- |
| `spring-boot-starter-web` | REST controllers, JSON serialization, embedded Tomcat |
| `spring-boot-starter-websocket` | STOMP endpoints, broker abstraction, message template |
| `spring-boot-starter-security` | stateless request filter chain, CORS, headers |
| `spring-boot-starter-validation` | Jakarta Bean Validation for API and Kafka contracts |
| `spring-boot-starter-data-jpa` | robot/alert repositories, specifications, optimistic locking |
| `spring-boot-starter-data-redis` | Redis connection pooling, serialization, pipelining |
| `spring-kafka` | consumers, producers, retries, topic declarations |
| `flyway-core` + PostgreSQL module | ordered, immutable schema migration |
| PostgreSQL driver | JDBC protocol and PostgreSQL JSONB support |
| Actuator + Prometheus registry | health probes and operational metrics |
| Reactor Netty | STOMP broker-relay TCP support when enabled |
| Spring Boot test | JUnit, AssertJ, Mockito, Spring test utilities |
| Testcontainers PostgreSQL | integration tests against real PostgreSQL behavior |

The Maven Enforcer rule requires Java 21. The Spring Boot plugin creates a layered executable JAR for efficient container layers. JaCoCo creates coverage reports during `verify`.

Stop and explain: what does auto-configuration do, and how is it different from component scanning?

## Milestone 3: domain model

The domain package contains vocabulary rather than transport formats.

- `Robot` is an immutable current view.
- `TelemetryPoint` is an immutable validated observation. Its compact constructor defensively copies `errorCodes`.
- `Alert` is a stateful aggregate. Only its methods can acknowledge, resolve, or retrigger it.
- enums prevent invalid string values from flowing through business logic.
- `PageSlice` avoids leaking Spring Data's `Page` type into application ports.

`Alert.open` creates a valid initial aggregate. `rehydrate` is the explicit persistence reconstruction path. `retrigger` implements cooldown suppression but immediately persists a severity escalation. `acknowledge` rejects an already resolved alert. `@Version` is not placed on the domain object; the persistence entity carries that annotation and the domain retains only the version value.

### Alert rule strategy

`TelemetryAlertRule` is an extension point. The engine depends on a list of the interface, so adding a rule does not change the orchestration service.

Each implementation uses `@Component` so Spring discovers it and injects the complete list. The rules are stateless. A rule returns `Optional.empty()` when the condition is healthy and an `AlertCandidate` when the condition is active.

This applies the open/closed principle: rule behavior is extended by a class, not by adding another branch to one giant method.

Stop and explain: why is `Alert` a class with methods while `TelemetryPoint` is a record?

## Milestone 4: application ports and services

Outbound ports describe what use cases need without prescribing technology:

- `RobotRepositoryPort`: robot reads, stale detection, summary
- `TelemetryStorePort`: idempotent batch write and history query
- `LatestTelemetryCachePort`: current-point cache
- `AlertRepositoryPort`: alert lifecycle persistence and search
- `AlertEventPublisherPort`: enqueue integration events

`@Service` marks use-case classes. It is semantically more descriptive than generic `@Component` but participates in the same component-scanning mechanism.

`@Transactional` on alert operations creates a database transaction around aggregate and outbox changes. `readOnly = true` documents query intent and lets persistence infrastructure optimize where supported.

### `TelemetryIngestionService`

The service orchestrates one accepted batch:

1. ask the durable store to save it;
2. count accepted versus duplicate rows;
3. update Redis only for accepted rows;
4. evaluate alerts only for accepted rows.

The Micrometer `Timer` wraps the whole application pipeline. Counters are monotonic; Prometheus derives rates from them.

### `AlertEvaluationService`

The service fetches active alerts once for all robot IDs in a batch, builds an in-memory `(robot, rule type)` map, and reconciles every observation. `LinkedHashSet` avoids saving the same aggregate repeatedly when a batch contains multiple points for one robot.

The alert rows are saved before `AlertEventPublisherPort.publish`. The outbox adapter uses the same transaction, so a failure rolls back both.

### `RobotConnectivityMonitor`

`@Scheduled` runs after the previous invocation completes because `fixedDelay` is used. A robot that stops producing has no event on which a normal rule could operate, so a time-driven monitor is necessary.

Before scanning, the monitor asks `ClusterLockPort` for a PostgreSQL transaction-scoped advisory lock. Only one backend replica performs a given scan, and the lock is released automatically at transaction end or connection failure.

Stop and explain: why does the cache update happen after the database write, and why must cache failure not fail Kafka acknowledgment?

## Milestone 5: PostgreSQL and persistence adapters

Flyway runs before JPA validation. Migrations are versioned SQL and should never be edited after release; create a new migration instead. Because this repository has not yet been released, the initial files define the baseline.

### JPA control plane

`@Entity` tells JPA that a class maps to a database row. `@Table` names the table explicitly. `@Id` marks identity. `@Column` captures names and null/length details. `@Enumerated(EnumType.STRING)` stores readable stable enum names rather than fragile ordinal numbers. `@Version` enables optimistic concurrency.

Protected no-argument constructors exist because JPA instantiates entities reflectively. Application code maps entities to domain objects instead of exposing managed entities through controllers.

Spring Data repository interfaces generate common CRUD behavior. `JpaSpecificationExecutor` provides composable optional filters. `@Query` handles queries that are clearer in JPQL. `@Modifying` marks an update query and `clearAutomatically` prevents stale managed state after a bulk update.

Adapters carry `@Repository`, which identifies persistence components and enables Spring exception translation.

### JDBC telemetry hot path

`TelemetryJdbcStore` uses one prepared statement template for an entire batch. Placeholders keep data separate from SQL and let the driver reuse plans. PostgreSQL `JSONB` is supplied through `PGobject` rather than string concatenation.

`@Transactional` covers both append inserts and robot snapshot updates. `BatchPreparedStatementSetter` avoids building one SQL string per point. Returned row counts distinguish accepted events from duplicates.

The history query is bounded and ordered. API limits are capped even if a caller requests a larger value.

Stop and explain: what overhead would Hibernate add for a telemetry point, and when would JPA batching still be acceptable?

## Milestone 6: Kafka ingestion

`KafkaConfig` declares topics for local bootstrap and configures listener factories.

- `NewTopic` is an idempotent admin declaration.
- telemetry has 12 partitions to permit consumer parallelism.
- `DefaultErrorHandler` applies exponential backoff.
- `DeadLetterPublishingRecoverer` routes infrastructure failures after retry exhaustion.
- manual immediate acknowledgment commits offsets only after the service succeeds.

`TelemetryEvent` is the wire contract. Jakarta annotations constrain IDs, numeric physical ranges, enums, timestamp presence, and error-code shape. `@JsonIgnoreProperties(ignoreUnknown = false)` makes accidental schema drift visible instead of silently discarding fields.

`@KafkaListener` creates managed consumer containers. The ingestion method receives the whole poll. It validates records independently, publishes invalid records with reasons, processes valid domain objects, and acknowledges the poll.

Kafka ordering is per partition, not global. Robot ID is the key because ordering matters within one robot's state history.

Stop and explain: what happens if there are 12 partitions, two replicas, and concurrency four on each replica?

## Milestone 7: transactional outbox

Writing an alert row and publishing Kafka directly are two independent commits. Either ordering has a failure window. `OutboxAlertEventPublisher` instead inserts serialized integration events using the same Spring transaction as the alert.

`OutboxRelay` polls unpublished rows. `FOR UPDATE SKIP LOCKED` lets several replicas relay concurrently without waiting on the same rows. It waits for Kafka broker acknowledgment before setting `published_at`. Published rows are retained for seven days for diagnosis and then cleaned.

The relay provides at-least-once publication. Downstream consumers must be idempotent.

Stop and draw the crash points before and after database commit, Kafka acknowledgment, and `published_at` update.

## Milestone 8: Redis latest state

`RedisConfig` creates a typed template with string keys and JSON values. `RedisLatestTelemetryCache` uses pipelining so 1,000 updates do not require 1,000 network round trips. `SETEX` writes the value and TTL atomically.

The adapter catches `DataAccessException`, increments a metric, and returns. This is a deliberate availability decision: Redis is an acceleration layer, not the source of truth.

Stop and explain: why is cache-aside history unnecessary for the latest-point endpoint here?

## Milestone 9: REST and security

`@RestController` combines controller discovery and JSON response bodies. `@RequestMapping` provides a versioned resource prefix. `@GetMapping`/`@PostMapping` bind HTTP methods. `@PathVariable` and `@RequestParam` bind URL data. `@RequestBody` binds JSON.

`@Validated` activates method-parameter constraints such as `@Min` and `@Max`. `@Valid` recursively validates a request body. `@NotBlank` rejects null, empty, and whitespace-only operator names.

DTOs keep the external contract independent from domain and persistence shapes. `PagedResponse.from` centralizes page mapping.

`ApiExceptionHandler` uses `@RestControllerAdvice` to produce consistent `ProblemDetail` responses. A resource absence maps to 404; invalid input maps to 400.

`ApiKeyAuthenticationFilter` extends `OncePerRequestFilter`, so it runs once per HTTP request. It limits itself to `/api/`, compares secrets in constant time, and creates a stateless authenticated principal. `SecurityConfig` disables CSRF because this demo does not use cookie authentication, disables server sessions, configures CORS, and adds defensive headers.

The local API key is not a production identity system. Use OIDC/Cognito and authorization roles in a real deployment.

## Milestone 10: WebSockets

`@EnableWebSocketMessageBroker` activates STOMP messaging. `/ws` is the handshake endpoint, `/topic/*` is server broadcast, and `/app/*` is reserved for client-to-server application messages.

`WebSocketConfig` uses the simple broker locally and can switch to a STOMP broker relay through configuration.

`WebSocketKafkaFanout` has two listeners. Its group contains the container hostname, making the group unique per replica. Every replica therefore receives the complete stream and publishes to its own `SimpMessagingTemplate` connections.

This is intentionally different from the shared durable ingestion group.

## Milestone 11: simulator

`SimulatorConfig` is an immutable slotted dataclass. Environment conversion is centralized and validated at startup.

`RobotState` is mutable because it models a changing device. A seeded `random.Random` makes runs reproducible. Movement respects map bounds, missions transition probabilistically, charging changes battery behavior, and injected faults persist for several ticks.

The Confluent producer uses all acknowledgments, idempotence, LZ4, batching, and a bounded queue. When the queue is full, the loop polls callbacks and waits instead of losing data silently. Signals set a stop event and shutdown flushes pending deliveries.

Prometheus counters measure delivery success/failure; gauges expose active robots and queue depth; a histogram measures tick time.

## Milestone 12: React dashboard

React Query owns server-state polling, retry, and cache invalidation. Axios centralizes timeout and API headers. The STOMP hook owns connection lifecycle, heartbeat, reconnect, and subscriptions.

Live telemetry is stored by robot ID for O(1) replacement. REST results remain the reconciliation source. Alert events invalidate alert and summary queries rather than attempting to duplicate all server lifecycle logic in the browser.

The Recharts detail panel is loaded with `React.lazy`, keeping the initial bundle smaller. TypeScript interfaces mirror stable response contracts. CSS is responsive without a component framework.

## Milestone 13: observability and delivery

Actuator readiness is used by Compose and the ALB. Prometheus scrapes application and infrastructure exporters. Grafana provisioning makes dashboards reproducible. Alert rules convert symptoms into operator signals.

GitHub Actions separates component checks so failures are isolated. Container builds run only after language tests. AWS uses GitHub OIDC rather than long-lived access keys. Images use immutable commit-SHA tags.

## How to study this repository

For each milestone:

1. run the relevant test;
2. set a breakpoint or add a temporary log at the boundary;
3. trigger the happy path;
4. break one dependency and predict behavior before observing it;
5. explain the guarantee without using framework jargon;
6. write down one tradeoff and one next-scale change.

If you can explain the failure paths—not just the annotations—you can defend the design in a CTO-level interview.
