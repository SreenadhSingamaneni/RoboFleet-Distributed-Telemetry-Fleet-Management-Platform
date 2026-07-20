import random
from datetime import UTC, datetime

from robot_simulator.model import RobotState


def test_event_matches_backend_contract() -> None:
    rng = random.Random(42)
    robot = RobotState.create(1, rng, 200, 120)
    robot.advance(1, rng, 200, 120, 0)

    event = robot.as_event(datetime(2026, 1, 1, tzinfo=UTC))

    assert event["robotId"] == "RBT-0001"
    assert event["schemaVersion"] == 1
    assert event["recordedAt"] == "2026-01-01T00:00:00Z"
    assert 0 <= event["batteryPercent"] <= 100
    assert 0 <= event["headingDegrees"] < 360


def test_robot_remains_inside_hospital_map() -> None:
    rng = random.Random(7)
    robot = RobotState.create(17, rng, 10, 8)
    robot.mission_state = "IN_PROGRESS"

    for _ in range(2_000):
        robot.advance(1, rng, 10, 8, 0)

    assert 0 <= robot.x <= 10
    assert 0 <= robot.y <= 8

