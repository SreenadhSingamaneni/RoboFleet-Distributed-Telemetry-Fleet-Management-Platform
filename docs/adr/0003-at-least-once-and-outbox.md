# ADR 0003: Use at-least-once processing, idempotent writes, and an outbox

- Status: Accepted
- Date: 2026-07-19

## Context

Kafka offsets and PostgreSQL writes cannot be committed atomically without adding a different coordination model. Alert state and alert Kafka publication also form a dual write.

## Decision

Commit Kafka offsets only after processing, suppress exact telemetry redelivery with the database primary key, and store alert integration events in a transactional outbox. Relay outbox rows with `FOR UPDATE SKIP LOCKED`.

## Consequences

Telemetry side effects are idempotent and alert events are not lost after alert commit. Alert events can be duplicated, so consumers must use stable IDs. The guarantee is accurately described as at least once.

