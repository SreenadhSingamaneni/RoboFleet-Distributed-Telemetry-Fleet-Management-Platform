# Operations runbook

## First response checklist

1. Confirm the user-visible symptom and affected time window.
2. Check backend readiness and current deployment health.
3. Compare produced rate, accepted rate, rejected rate, and Kafka lag.
4. Check PostgreSQL connections/storage and Redis availability.
5. Inspect recent backend and simulator logs using correlation time and robot ID.
6. Mitigate before performing a risky repair; record every action.

## Telemetry stopped

```bash
docker compose ps
docker compose logs --since=10m simulator kafka backend
curl -fsS http://localhost:8080/actuator/health/readiness
```

Interpretation:

- producer rate zero: simulator/device side
- producer rate healthy and consumer lag rising: backend capacity or dependency failure
- accepted rate healthy but dashboard stale: WebSocket/fan-out path
- rejected rate rising: contract or clock/schema issue

Do not reset consumer offsets as a first response. That can create a replay storm. Identify the failed dependency and estimate backlog drain rate first.

## High Kafka lag

1. Confirm partition-level lag; one hot partition suggests key skew.
2. Compare ingestion p95 with baseline.
3. Inspect Hikari active/pending connections and PostgreSQL write latency.
4. Scale consumers only while unassigned partitions remain.
5. If the database is saturated, adding consumers makes contention worse.
6. After mitigation, calculate drain time: `lag / (consume_rate - produce_rate)`.

## PostgreSQL pressure

- Check active connections and slow queries.
- Confirm queries prune telemetry partitions with `EXPLAIN (ANALYZE, BUFFERS)` in a safe environment.
- Confirm future partitions exist and the default partition is not accumulating data.
- Reduce nonessential history-query limits before changing durability settings.
- Never drop a telemetry partition until its archive and retention approval are verified.

## Redis unavailable

Expected behavior is durable ingestion with cache-failure metrics. The latest-telemetry endpoint may return `204`, but history remains available. Restore Redis, then allow new telemetry to warm keys naturally; bulk-loading every historical point is unnecessary.

## Outbox backlog

```sql
SELECT count(*) AS unpublished, min(occurred_at) AS oldest
FROM outbox_events
WHERE published_at IS NULL;
```

Check Kafka availability and `fleet.outbox.failures`. Do not mark rows published manually. Once Kafka recovers, `FOR UPDATE SKIP LOCKED` workers drain the backlog safely.

## Invalid telemetry spike

Consume a small sample from `fleet.telemetry.invalid.v1` and group by rejection reason. Common causes are unsupported `schemaVersion`, invalid robot ID, future device clock, enum changes, or values outside physical limits. Fix the producer or add a backward-compatible consumer before replaying.

## Recovery exercises

Run these in a nonproduction environment:

- kill one backend during load and verify lag recovers
- pause PostgreSQL for one minute and verify retry/idempotency
- stop Redis and verify durable rate remains stable
- stop the simulator and verify connectivity alerts
- block alert-topic publication and verify outbox accumulation/drain
- restore an RDS snapshot and measure recovery time

An architecture is not resilient merely because services are configured for high availability; recovery must be exercised and timed.

