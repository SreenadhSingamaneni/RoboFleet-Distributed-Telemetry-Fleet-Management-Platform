# ADR 0001: Begin with a modular monolith

- Status: Accepted
- Date: 2026-07-19

## Context

The platform has ingestion, query, alert, and live-update responsibilities, but one portfolio owner develops and deploys them together. Premature services would require distributed tracing, inter-service contracts, deployment coordination, and extra failure handling.

## Decision

Use one Spring Boot deployment with domain, application, API, and infrastructure modules enforced by package direction and ports.

## Consequences

Local development and transactions remain simple. One service can affect all responsibilities during a bad deployment. Ports, Kafka topics, and separate ECS task definitions provide extraction seams when independent scaling or ownership is demonstrated.

