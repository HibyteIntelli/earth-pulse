# Developer Guide

## Local Setup

### 1. Configure credentials

Copy `.env.model` to `.env` and fill in your values:
```
POSTGRES_DB=notifier_db
POSTGRES_USER=...
POSTGRES_PASSWORD=...
POSTGRES_PORT=5433
```

`.env` is gitignored — never commit it.

### 2. Configure the app

Copy `src/main/resources/application-local.properties.model` to `application-local.properties` in the same folder and fill in the same database credentials:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5433/<POSTGRES_DB>
spring.datasource.username=<POSTGRES_USER>
spring.datasource.password=<POSTGRES_PASSWORD>

spring.jpa.hibernate.ddl-auto=update

spring.mail.host=localhost
spring.mail.port=1025
spring.mail.properties.mail.smtp.auth=false
spring.mail.properties.mail.smtp.starttls.enable=false

spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8080/.well-known/jwks.json

notifier.internal-secret=change-me

# Auth Service — called to find watches matching an incoming event.
# app.auth-service.internal-secret must match the secret configured in the Auth Service.
app.auth-service.url=http://localhost:8080
app.auth-service.internal-secret=change-me

# LLM Service — called to generate the briefing for each matched watch.
# app.llm-service.internal-secret must match the secret configured in the LLM Service.
app.llm-service.url=http://localhost:8082
app.llm-service.internal-secret=change-me

# Frontend — used to build event links in notification emails
app.frontend.base-url=http://localhost:4200
```

`application-local.properties` is gitignored — never commit it.

> **First run only:** set `spring.jpa.hibernate.ddl-auto=create` to let Hibernate create the schema from scratch, then switch back to `update`.

### 3. Start the infrastructure

```bash
docker compose up -d
```

Docker Compose reads credentials from `.env` automatically and starts:

| Service  | Description             | Ports                        |
|----------|-------------------------|------------------------------|
| Postgres | Notifier database       | `5433` (host) → `5432` (container) |
| Mailpit  | Local SMTP server (dev) | `1025` (SMTP), `8025` (UI)   |

Mailpit UI — inspect sent emails at: http://localhost:8025

### 4. Run the app

```bash
./mvnw spring-boot:run
```

The app starts on port **8084**.

---

## Running tests

Tests use Testcontainers — Docker must be running but no local Postgres or Mailpit instance is needed.

```bash
./mvnw test
```
