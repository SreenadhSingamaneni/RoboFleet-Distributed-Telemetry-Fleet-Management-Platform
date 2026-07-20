# ADR 0002: Use JDBC batching for telemetry and JPA for control-plane data

- Status: Accepted
- Date: 2026-07-19

## Context

Telemetry is append-only, high-volume data with no meaningful per-row aggregate lifecycle. Robots and alerts have query, state-transition, and concurrency behavior that benefits from object mapping.

## Decision

Use `JdbcTemplate` prepared batches for telemetry inserts and robot snapshot updates. Use Spring Data JPA for robot queries and alert lifecycle persistence.

## Consequences

The hot path avoids persistence-context overhead and makes SQL visible. Mapping and SQL require more explicit code. Both approaches share HikariCP and Spring transactions.

