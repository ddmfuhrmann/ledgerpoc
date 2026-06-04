# Skill: Local Environment Orchestration

## Prerequisites

- Docker Desktop running
- Java 21 on PATH
- Maven on PATH (or use `./mvnw`)

## Startup

```bash
# 1. Start Postgres
docker compose up -d

# 2. Verify healthy
docker compose ps
# postgres should show: healthy

# 3. Run migrations + start application
mvn spring-boot:run

# Or just run migrations (without the app):
mvn flyway:migrate
```

## Postgres details

| Setting | Value |
|---|---|
| Host | localhost:5432 |
| Database | ledgerpoc |
| Username | ledgerpoc |
| Password | ledgerpoc |

Direct psql access:
```bash
psql -h localhost -U ledgerpoc -d ledgerpoc
```

## Run tests

```bash
# All tests (Testcontainers spins its own Postgres automatically)
mvn test

# Single test class
mvn test -Dtest=CashOutCommandServiceIntegrationTest
```

Tests do not require the docker-compose Postgres — Testcontainers manages its own container.

## Tear down

```bash
docker compose down           # stop containers, keep volume
docker compose down -v        # stop and remove volume (clean slate)
```

## Seed data

See `.skills/database-seeding.md` for inserting realistic volumes for performance testing.
