# ADR 0004: Give each backend replica a Kafka fan-out group

- Status: Accepted
- Date: 2026-07-19

## Context

A shared ingestion group assigns different partitions to different replicas. WebSocket clients connected to one replica still need the complete fleet stream.

## Decision

Keep the shared ingestion group for durable work and add a fan-out group whose name contains the container hostname. Every replica receives the complete telemetry and alert topics and broadcasts to its own connections.

## Consequences

Horizontal replicas show complete live state without duplicating durable writes. Kafka egress and consumer work grow linearly with WebSocket replicas. At much larger scale, a broker relay or dedicated gateway becomes preferable.

