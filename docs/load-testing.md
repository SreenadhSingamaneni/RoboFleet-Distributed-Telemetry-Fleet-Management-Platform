# Load-testing guide

## Goals

Measure sustainable throughput, end-to-end freshness, database growth, consumer lag, and behavior during dependency faults. A single “requests per second” number is not sufficient for a streaming system.

## Baseline scenarios

| Scenario | Robots | Rate per robot | Total rate | Duration |
| --- | ---: | ---: | ---: | ---: |
| Portfolio demo | 1,000 | 1 Hz | 1,000 events/s | 15 min |
| Peak traffic | 1,000 | 5 Hz | 5,000 events/s | 30 min |
| Scale exploration | 10,000 | 1 Hz | 10,000 events/s | 30 min |
| Soak | 1,000 | 1 Hz | 1,000 events/s | 8 hr |

Change `.env` and recreate the simulator:

```bash
SIMULATOR_ROBOT_COUNT=1000
SIMULATOR_RATE_HZ=5
docker compose up -d --force-recreate simulator
```

The local database seeds 1,000 robots. For more robots, add a test-only seed migration or provisioning step before publishing; otherwise foreign-key enforcement correctly rejects unknown IDs.

## Success criteria

- accepted rate matches producer rate after warm-up
- sustained consumer lag is near zero at baseline
- no invalid or DLT records during the healthy scenario
- ingestion p95 stays below the agreed service objective
- dashboard freshness remains within a few seconds
- PostgreSQL connections remain below pool and server limits
- no unbounded outbox backlog
- memory stabilizes during soak rather than growing continuously

## Fault scenarios

1. Stop Redis for two minutes. Accepted rate should remain stable.
2. Stop PostgreSQL for one minute. Lag should rise, then drain without duplicate history.
3. Kill one backend replica. The group should rebalance and clients reconnect.
4. Inject malformed messages. Valid records in the same batch should still progress.
5. Constrain backend CPU. Verify autoscaling or lag alerts fire before user impact.

## Report template

Record commit SHA, environment, instance sizes, Kafka partitions, database settings, producer configuration, start/end time, total events, p50/p95/p99 ingestion latency, maximum lag, recovery duration, error count, and cost estimate. Preserve Grafana screenshots and relevant query plans with the report.

