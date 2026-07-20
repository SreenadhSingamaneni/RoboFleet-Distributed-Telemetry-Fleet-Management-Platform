import pytest

from robot_simulator.config import SimulatorConfig


def test_rejects_invalid_robot_count(monkeypatch: pytest.MonkeyPatch) -> None:
    monkeypatch.setenv("SIMULATOR_ROBOT_COUNT", "0")
    with pytest.raises(ValueError, match="ROBOT_COUNT"):
        SimulatorConfig.from_environment()

