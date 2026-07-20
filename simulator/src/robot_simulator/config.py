from __future__ import annotations

import os
from dataclasses import dataclass


@dataclass(frozen=True, slots=True)
class SimulatorConfig:
    bootstrap_servers: str = "localhost:29092"
    topic: str = "fleet.telemetry.v1"
    kafka_security_protocol: str = "PLAINTEXT"
    robot_count: int = 1_000
    rate_hz: float = 1.0
    metrics_port: int = 8_000
    random_seed: int = 42
    fault_probability: float = 0.002
    map_width_meters: float = 200.0
    map_height_meters: float = 120.0

    @classmethod
    def from_environment(cls) -> SimulatorConfig:
        defaults = cls()
        config = cls(
            bootstrap_servers=os.getenv("KAFKA_BOOTSTRAP_SERVERS", defaults.bootstrap_servers),
            topic=os.getenv("TELEMETRY_TOPIC", defaults.topic),
            kafka_security_protocol=os.getenv(
                "KAFKA_SECURITY_PROTOCOL", defaults.kafka_security_protocol
            ),
            robot_count=int(os.getenv("SIMULATOR_ROBOT_COUNT", str(defaults.robot_count))),
            rate_hz=float(os.getenv("SIMULATOR_RATE_HZ", str(defaults.rate_hz))),
            metrics_port=int(os.getenv("SIMULATOR_METRICS_PORT", str(defaults.metrics_port))),
            random_seed=int(os.getenv("SIMULATOR_RANDOM_SEED", str(defaults.random_seed))),
            fault_probability=float(
                os.getenv("SIMULATOR_FAULT_PROBABILITY", str(defaults.fault_probability))
            ),
            map_width_meters=float(os.getenv("MAP_WIDTH_METERS", str(defaults.map_width_meters))),
            map_height_meters=float(
                os.getenv("MAP_HEIGHT_METERS", str(defaults.map_height_meters))
            ),
        )
        if not 1 <= config.robot_count <= 100_000:
            raise ValueError("SIMULATOR_ROBOT_COUNT must be between 1 and 100000")
        if not 0.1 <= config.rate_hz <= 100:
            raise ValueError("SIMULATOR_RATE_HZ must be between 0.1 and 100")
        if not 0 <= config.fault_probability <= 1:
            raise ValueError("SIMULATOR_FAULT_PROBABILITY must be between 0 and 1")
        return config
