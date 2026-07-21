from __future__ import annotations

import math
import random
import uuid
from dataclasses import dataclass, field
from datetime import UTC, datetime
from typing import Any

MISSION_STATES = ("IDLE", "ASSIGNED", "IN_PROGRESS", "CHARGING", "COMPLETED")


@dataclass(slots=True)
class RobotState:
    robot_id: str
    floor: int
    x: float
    y: float
    heading_degrees: float
    battery_percent: float
    temperature_celsius: float
    mission_state: str = "IDLE"
    connectivity: str = "CONNECTED"
    sequence_number: int = 0
    speed_mps: float = 0.0
    error_codes: list[str] = field(default_factory=list)
    _fault_ticks_remaining: int = 0

    @classmethod
    def create(cls, index: int, rng: random.Random, width: float, height: float) -> RobotState:
        return cls(
            robot_id=f"RBT-{index:04d}",
            floor=1 + index % 5,
            x=rng.uniform(0, width),
            y=rng.uniform(0, height),
            heading_degrees=rng.uniform(0, 360),
            battery_percent=rng.uniform(25, 100),
            temperature_celsius=rng.uniform(32, 46),
        )

    def advance(
        self,
        interval_seconds: float,
        rng: random.Random,
        width: float,
        height: float,
        fault_probability: float,
    ) -> None:
        self.sequence_number += 1
        self._transition_mission(rng)
        self._inject_or_decay_fault(rng, fault_probability)

        if self.mission_state == "CHARGING":
            self.speed_mps = 0
            self.battery_percent = min(100, self.battery_percent + 0.35 * interval_seconds)
        elif self.mission_state == "IN_PROGRESS" and "NAVIGATION_STALLED" not in self.error_codes:
            self.speed_mps = max(0.1, min(1.8, rng.gauss(0.85, 0.18)))
            self.heading_degrees = (self.heading_degrees + rng.uniform(-12, 12)) % 360
            radians = math.radians(self.heading_degrees)
            self.x += math.cos(radians) * self.speed_mps * interval_seconds
            self.y += math.sin(radians) * self.speed_mps * interval_seconds
            self._bounce_inside_map(width, height)
            self.battery_percent = max(0, self.battery_percent - 0.015 * interval_seconds)
        else:
            self.speed_mps = 0
            self.battery_percent = max(0, self.battery_percent - 0.002 * interval_seconds)

        if self.battery_percent < 8 and self.mission_state != "CHARGING":
            self.mission_state = "CHARGING"

        temperature_target = 43 if self.speed_mps > 0 else 36
        self.temperature_celsius += (temperature_target - self.temperature_celsius) * 0.08
        self.temperature_celsius += rng.uniform(-0.2, 0.2)
        if "MOTOR_OVERTEMP" in self.error_codes:
            self.temperature_celsius = max(self.temperature_celsius, 76)

    def as_event(self, recorded_at: datetime | None = None) -> dict[str, Any]:
        timestamp = recorded_at or datetime.now(UTC)
        return {
            "eventId": str(uuid.uuid4()),
            "schemaVersion": 1,
            "robotId": self.robot_id,
            "sequenceNumber": self.sequence_number,
            "recordedAt": timestamp.isoformat().replace("+00:00", "Z"),
            "x": round(self.x, 3),
            "y": round(self.y, 3),
            "floor": self.floor,
            "batteryPercent": round(self.battery_percent, 2),
            "speedMps": round(self.speed_mps, 3),
            "headingDegrees": round(self.heading_degrees, 2) % 360.0,
            "temperatureCelsius": round(self.temperature_celsius, 2),
            "missionState": self.mission_state,
            "connectivity": self.connectivity,
            "errorCodes": list(self.error_codes),
        }

    def _transition_mission(self, rng: random.Random) -> None:
        if rng.random() > 0.006:
            return
        transitions = {
            "IDLE": ("ASSIGNED",),
            "ASSIGNED": ("IN_PROGRESS",),
            "IN_PROGRESS": ("COMPLETED", "CHARGING"),
            "COMPLETED": ("IDLE",),
            "CHARGING": ("IDLE",),
        }
        self.mission_state = rng.choice(transitions.get(self.mission_state, MISSION_STATES))

    def _inject_or_decay_fault(self, rng: random.Random, probability: float) -> None:
        if self._fault_ticks_remaining > 0:
            self._fault_ticks_remaining -= 1
            if self._fault_ticks_remaining == 0:
                self.error_codes.clear()
                self.connectivity = "CONNECTED"
            return
        if rng.random() >= probability:
            return
        fault = rng.choice(("NAVIGATION_STALLED", "MOTOR_OVERTEMP", "LIDAR_OCCLUDED"))
        self.error_codes = [fault]
        self._fault_ticks_remaining = rng.randint(8, 30)
        if fault == "NAVIGATION_STALLED":
            self.mission_state = "IN_PROGRESS"
            self.speed_mps = 0
        elif fault == "LIDAR_OCCLUDED":
            self.connectivity = "DEGRADED"

    def _bounce_inside_map(self, width: float, height: float) -> None:
        if self.x < 0 or self.x > width:
            self.heading_degrees = (180 - self.heading_degrees) % 360
            self.x = min(width, max(0, self.x))
        if self.y < 0 or self.y > height:
            self.heading_degrees = (-self.heading_degrees) % 360
            self.y = min(height, max(0, self.y))

