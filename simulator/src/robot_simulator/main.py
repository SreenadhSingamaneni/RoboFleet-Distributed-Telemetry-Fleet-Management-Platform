from __future__ import annotations

import json
import logging
import random
import signal
import threading
import time
from datetime import UTC, datetime

from confluent_kafka import KafkaError, KafkaException, Producer
from prometheus_client import Counter, Gauge, Histogram, start_http_server

from robot_simulator.config import SimulatorConfig
from robot_simulator.model import RobotState

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)s %(name)s %(message)s",
)
LOGGER = logging.getLogger("robot-simulator")

PRODUCED = Counter("simulator_events_produced_total", "Telemetry delivery successes")
DELIVERY_FAILURES = Counter("simulator_delivery_failures_total", "Telemetry delivery failures")
ACTIVE_ROBOTS = Gauge("simulator_active_robots", "Configured robot count")
TICK_DURATION = Histogram("simulator_tick_duration_seconds", "Time to enqueue a simulator tick")
QUEUE_DEPTH = Gauge("simulator_producer_queue_depth", "Messages waiting in the producer queue")


class FleetSimulator:
    def __init__(self, config: SimulatorConfig) -> None:
        self.config = config
        self.rng = random.Random(config.random_seed)
        self.robots = [
            RobotState.create(index, self.rng, config.map_width_meters, config.map_height_meters)
            for index in range(1, config.robot_count + 1)
        ]
        self.producer = Producer(
            {
                "bootstrap.servers": config.bootstrap_servers,
                "security.protocol": config.kafka_security_protocol,
                "client.id": "hospital-robot-simulator",
                "acks": "all",
                "enable.idempotence": True,
                "compression.type": "lz4",
                "linger.ms": 20,
                "batch.size": 131_072,
                "queue.buffering.max.messages": max(100_000, config.robot_count * 10),
                "message.timeout.ms": 30_000,
            }
        )
        self.stop_event = threading.Event()

    def run(self) -> None:
        interval = 1.0 / self.config.rate_hz
        ACTIVE_ROBOTS.set(len(self.robots))
        LOGGER.info(
            "Starting %s robots at %.2f Hz (%s events/second)",
            len(self.robots),
            self.config.rate_hz,
            int(len(self.robots) * self.config.rate_hz),
        )
        next_tick = time.monotonic()
        while not self.stop_event.is_set():
            with TICK_DURATION.time():
                self._produce_tick(interval)
            next_tick += interval
            self.stop_event.wait(max(0, next_tick - time.monotonic()))
            if time.monotonic() - next_tick > interval:
                next_tick = time.monotonic()

        remaining = self.producer.flush(15)
        if remaining:
            LOGGER.warning("Shutdown timed out with %s undelivered events", remaining)

    def stop(self, *_: object) -> None:
        LOGGER.info("Shutdown requested")
        self.stop_event.set()

    def _produce_tick(self, interval: float) -> None:
        tick_time = datetime.now(UTC)
        for robot in self.robots:
            robot.advance(
                interval,
                self.rng,
                self.config.map_width_meters,
                self.config.map_height_meters,
                self.config.fault_probability,
            )
            payload = json.dumps(robot.as_event(tick_time), separators=(",", ":")).encode()
            while True:
                try:
                    self.producer.produce(
                        self.config.topic,
                        key=robot.robot_id.encode(),
                        value=payload,
                        on_delivery=self._delivery_report,
                    )
                    break
                except BufferError:
                    self.producer.poll(0.05)
            self.producer.poll(0)
        QUEUE_DEPTH.set(len(self.producer))

    @staticmethod
    def _delivery_report(error: KafkaError | None, _message: object) -> None:
        if error is None:
            PRODUCED.inc()
        else:
            DELIVERY_FAILURES.inc()
            LOGGER.error("Kafka delivery failed: %s", error)


def main() -> None:
    config = SimulatorConfig.from_environment()
    start_http_server(config.metrics_port)
    simulator = FleetSimulator(config)
    signal.signal(signal.SIGTERM, simulator.stop)
    signal.signal(signal.SIGINT, simulator.stop)
    try:
        simulator.run()
    except KafkaException:
        LOGGER.exception("Fatal Kafka error")
        raise


if __name__ == "__main__":
    main()
