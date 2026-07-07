# Earth Pulse — Notifier Service

Natural disaster and natural event tracker. Users see live events on a world map, get AI briefings, and subscribe to regions/categories to receive email notifications.

## This component: Notifier Service

Matches new events against user watches and delivers email notifications — either immediately or as a daily digest, depending on each watch's `digest_mode`.

## Architecture

Five Spring Boot services + Angular frontend. Each service has its own PostgreSQL database. No cross-database joins — data from other services is fetched via REST.

```
earth-pulse/
├── frontend/        # Angular
├── ingestion/       # NASA EONET polling, event storage
├── llm/             # Ollama briefing generation
├── auth/            # Auth (JWT), watches, account
└── notifier/        # This service
```

## Inter-service communication (Notifier)

| Direction            | Purpose                                         |
|----------------------|-------------------------------------------------|
| Ingestion → Notifier | Push new events when ingested                   |
| Notifier → Ingestion | Poll for new events (fallback)                  |
| Notifier → User      | Query which watches match an incoming event     |
| Notifier → LLM       | Fetch briefing for an event + reading_level     |
| FE → Notifier        | Fetch notification history                      |

## Tech stack

- Java 25, Spring Boot
- PostgreSQL (own database, no shared access)
- Spring Data JPA
- Spring Mail (SMTP → Mailpit in dev)
- Thymeleaf (HTML email templates only, not for serving pages)
- `@EnableScheduling` for daily digest job
- `spring-boot-starter-oauth2-resource-server` for JWT validation

## Authentication

The Auth Service is the identity provider. This service validates JWTs locally using the Auth Service's JWKS endpoint. No custom JWT code needed here.

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: http://auth-service:8080/.well-known/jwks.json
```

## Database

```
POSTGRES_DB:   notifier_db
POSTGRES_USER: notifier_user
POSTGRES_PORT: 5433 (host-mapped, container runs on 5432)
```

Credentials in `application.properties` (gitignored).

## Core responsibilities

### Event matching
- Receive push from Ingestion when new events arrive (primary)
- Poll Ingestion as fallback
- For each event, call Auth Service to get all watches whose bounding box + category match

### Notification delivery
Delivery mode is per-watch (`digest_mode` field), not per-user:

- `immediate` — send email immediately with the single event + full briefing
- `daily_digest` — buffer the match; a scheduled job at configured time rolls up all buffered matches for that watch into one digest email

One user can have both modes active simultaneously on different watches — handled independently.

### Briefing fetch
Call LLM Service with `(event_id, reading_level)` where `reading_level` comes from the matched watch, not the user.

### Email content
Both immediate and digest emails include:
- Full briefing: `summary`, `impact`, `severity` badge, `precautions`
- Link to the event on the map

Digest-specific layout:
- Header: "N events matched: X high, Y moderate, Z low"
- Events sorted by severity desc, then date desc

### Deduplication
Maintain a delivery audit log. A `(watch_id, event_id)` pair must never be delivered twice. First write wins; subsequent attempts for the same pair are no-ops.

## Key domain rules

- Notifier queries watches, not users — it never looks up user-level preferences
- `reading_level` per watch drives the LLM briefing variant
- Digest job delivery time is configurable via properties
- Immediate emails: single event inline
- Digest emails: all matches for that watch in the last 24 hours

## Email (Mailpit in dev)

Mailpit runs as a local SMTP server. UI at `http://localhost:8025`.

```properties
spring.mail.host=localhost
spring.mail.port=1025
```

## Out of scope for v1

- Non-email channels (webhooks, SMS, push)
- User-level notification preferences (only per-watch)
- Refresh tokens
