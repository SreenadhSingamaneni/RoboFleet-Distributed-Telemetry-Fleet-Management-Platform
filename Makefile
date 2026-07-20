.PHONY: help up down logs ps backend-test dashboard-test simulator-test test build clean

help:
	@echo "make up              Start the complete local platform"
	@echo "make down            Stop containers and keep volumes"
	@echo "make test            Run all unit/integration checks"
	@echo "make logs            Follow service logs"

up:
	docker compose up --build -d

down:
	docker compose down

logs:
	docker compose logs -f --tail=200

ps:
	docker compose ps

backend-test:
	cd backend && ./mvnw verify

dashboard-test:
	cd dashboard && npm ci && npm run test && npm run build

simulator-test:
	cd simulator && python -m pip install -e ".[dev]" && ruff check . && pytest

test: backend-test dashboard-test simulator-test

build:
	docker compose build

clean:
	docker compose down -v --remove-orphans

