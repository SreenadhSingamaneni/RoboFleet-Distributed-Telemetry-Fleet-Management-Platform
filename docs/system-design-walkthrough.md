# System design walkthrough

## System overview

The platform processes telemetry from 1,000 autonomous hospital robots. A stateful Python simulator publishes versioned telemetry to Kafka using robot ID as the key, preserving per-robot ordering and decoupling bursty producers from downstream processing. A Java 21 Spring Boot backend validates and consumes records in batches, stores durable time-partitioned history in PostgreSQL, maintains latest state in Redis, and evaluates extensible alert rules. The delivery model is at least once, so PostgreSQL writes are idempotent. Alert events use a transactional outbox to avoid losing messages between a database commit and Kafka publication. Every backend replica also has a unique Kafka fan-out group so WebSocket clients receive a complete live stream after horizontal scaling. The system includes a React command center, Prometheus and Grafana, CI and security workflows, Docker Compose, and an AWS ECS, MSK, RDS, and ElastiCache Terraform architecture.

## Architecture flow

1. Operational requirements call for durable audit history and a low-latency operator view.
2. Kafka decouples robot producers from backend workers and absorbs traffic bursts.
3. Robot-key partitioning preserves per-robot ordering while supporting parallel processing.
4. JPA manages control-plane aggregates while JDBC batching handles the telemetry hot path.
5. At-least-once delivery is made safe through idempotent inserts.
6. Redis provides disposable current-state caching.
7. Alert aggregate transitions and outbox records commit atomically.
8. Per-replica Kafka groups provide complete WebSocket fan-out.
9. Metrics, failure tests, and AWS services complete the operational model.

## Design decisions

### Why Kafka instead of sending telemetry directly to REST?

Kafka absorbs bursts, lets producers and consumers fail independently, preserves per-key ordering, supports replay, and allows ingestion, alerting, analytics, and WebSocket consumers to evolve independently. Direct REST would couple 1,000 devices to backend availability and push buffering/retry complexity onto every robot.

### Why not claim exactly once?

Kafka and PostgreSQL do not share one atomic commit in this design. The consumer can crash after the database commits but before the offset commits, so Kafka can redeliver. The correct guarantee is at least once with an idempotent database effect. That is measurable and survives the actual failure window.

### Why is the primary key composite?

PostgreSQL unique constraints on a partitioned table must include the partition key. The table is partitioned by `recorded_at`, so `(event_id, recorded_at)` is the primary key. A redelivery repeats both values and conflicts safely.

### Why use JPA and JDBC together?

Alerts benefit from aggregate mapping, optimistic locking, and repository queries. Telemetry is append-only and high volume; entity creation and dirty checking add cost without domain value. JDBC batching keeps the hot path explicit while sharing Spring transactions and HikariCP.

### What problem does the outbox solve?

Without it, committing an alert and publishing Kafka are two independent writes. A crash between them either loses the event or publishes an event for rolled-back state. The outbox stores the integration event in the alert transaction, and a retryable relay publishes it later.

### Can the outbox publish duplicates?

Yes. If Kafka accepts an event and the process fails before `published_at` commits, it is retried. The stable alert ID makes consumers idempotent. The outbox prevents loss; it does not magically provide global exactly-once behavior.

### What happens when Redis is down?

PostgreSQL ingestion and Kafka acknowledgment continue. Cache failures are logged and measured, and latest-state reads can temporarily return no content. New telemetry warms Redis after recovery. This is intentional because cache availability should not control durable ingestion availability.

### How do WebSockets work with several backend replicas?

A shared ingestion group distributes partitions, so a replica sees only part of the stream. Each replica therefore also has a distinct fan-out group based on hostname; it receives all telemetry and alert events and broadcasts them to its local connections. REST polling reconciles anything missed during reconnection.

### Why a modular monolith?

The current scope is owned by one engineering team and uses one cohesive deployment. Separate services would add network failure, version coordination, deployment, and observability costs before independent scaling is proven. Ports and modules establish extraction seams if metrics later show ingestion, query, alerting, or WebSockets need separate ownership or capacity.

### How would you support 100,000 robots?

Increase Kafka partitions based on throughput and ordering needs, separate ingestion workers from query APIs, benchmark larger JDBC batches, move long-term raw history to S3/Parquet, use short PostgreSQL retention plus rollups, scale WebSocket gateways independently, add Schema Registry, and load test the full pipeline. I would size from observed event bytes, p99 latency, lag, and storage write amplification rather than robot count alone.

### How would real robots authenticate?

Use per-device identities and short-lived or rotated certificates, mutual TLS at an IoT gateway or managed IoT service, authorization scoped to the robot's own topic, revocation, secure provisioning, and audit logs. The local simulator deliberately excludes production device identity; real deployments require a dedicated device-security architecture.

### What healthcare-specific concern matters?

Keep operational telemetry separate from protected health information. Use opaque mission IDs, least privilege, encryption, auditability, and explicit retention. Device logs should never contain patient names or clinical payloads.

## Known trade-offs

- JSON is easy to inspect but lacks registry-enforced compatibility and is larger than Avro/Protobuf.
- PostgreSQL is excellent for recent operational history but not economical for indefinite raw retention at tens of millions of rows per day.
- A simple local STOMP broker is convenient; high connection counts justify an external relay or dedicated gateway.
- API-key authentication is a local demonstration mechanism, not production user identity.
- One NAT Gateway reduces the reference environment's cost but is an AZ dependency; production can use one per AZ plus VPC endpoints.

## Architecture review scenarios

1. Analyze the crash window between database commit and offset commit.
2. Compare shared ingestion groups with unique fan-out groups.
3. Calculate daily event volume from robot count and telemetry rate.
4. Evaluate how 12 partitions limit useful consumers.
5. Design 30-day retention and S3 archival.
6. Add robot commands with replay protection and authorization.
7. Define SLOs for ingestion availability, freshness, and alert latency.
